package com.vardash.mafimushkil.auth

import android.app.Activity
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthViewModel : ViewModel() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private var storedVerificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null

    fun checkAuthState(context: Context) {
        val sessionManager = SessionManager(context)
        val currentUser = auth.currentUser
        if (currentUser != null && sessionManager.isLoggedIn()) {
            _authState.value = AuthState.Success(currentUser.uid)
        } else {
            _authState.value = AuthState.Idle
        }
    }

    fun sendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential, activity)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _authState.value = AuthState.Error(e.message ?: "Verification failed")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = token
                _authState.value = AuthState.CodeSent(verificationId)
            }
        }

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun verifyOtp(code: String, activity: Activity) {
        val verificationId = storedVerificationId ?: run {
            _authState.value = AuthState.Error("Verification ID not found. Please request a new code.")
            return
        }

        _authState.value = AuthState.Loading
        val credential = PhoneAuthProvider.getCredential(verificationId, code)
        signInWithCredential(credential, activity)
    }

    fun resendOtp(phoneNumber: String, activity: Activity) {
        _authState.value = AuthState.Loading
        val token = resendToken

        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                signInWithCredential(credential, activity)
            }

            override fun onVerificationFailed(e: FirebaseException) {
                _authState.value = AuthState.Error(e.message ?: "Resend failed")
            }

            override fun onCodeSent(
                verificationId: String,
                newToken: PhoneAuthProvider.ForceResendingToken
            ) {
                storedVerificationId = verificationId
                resendToken = newToken
                _authState.value = AuthState.CodeSent(verificationId)
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        if (token != null) {
            optionsBuilder.setForceResendingToken(token)
        }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    private fun signInWithCredential(credential: PhoneAuthCredential, activity: Activity) {
        viewModelScope.launch {
            try {
                val result = auth.signInWithCredential(credential).await()
                val user = result.user ?: throw Exception("User is null after sign in")
                val token = user.getIdToken(false).await().token ?: ""

                val sessionManager = SessionManager(activity)
                sessionManager.saveSession(user.uid, user.phoneNumber ?: "", token)

                try {
                    saveUserToFirestore(user.uid, user.phoneNumber ?: "")
                    updateActiveSession(user.uid, token)
                    FcmTokenManager.fetchAndStoreToken()
                } catch (_: Exception) {
                }

                _authState.value = AuthState.Success(user.uid)
            } catch (e: Exception) {
                _authState.value = AuthState.Error(e.message ?: "Sign in failed")
            }
        }
    }

    private suspend fun saveUserToFirestore(uid: String, phone: String) {
        try {
            val userRef = firestore.collection("users").document(uid)
            val snapshot = userRef.get().await()
            if (!snapshot.exists()) {
                val userData = hashMapOf(
                    "uid" to uid,
                    "phone" to phone,
                    "name" to "",
                    "email" to "",
                    "gender" to "",
                    "profilePhoto" to "",
                    "fcmToken" to "",
                    "workerState" to "not",
                    "workerExperience" to "",
                    "workerServices" to "",
                    "createdAt" to Timestamp.now()
                )
                userRef.set(userData).await()
            }
        } catch (_: Exception) {
        }
    }

    private suspend fun updateActiveSession(uid: String, token: String) {
        try {
                firestore.collection("sessions").document(uid)
                    .set(
                        hashMapOf(
                            "activeToken" to token,
                            "lastLogin" to Timestamp.now(),
                            "device" to android.os.Build.MODEL
                        )
                    ).await()
        } catch (_: Exception) {
        }
    }

    fun logout(context: Context) {
        viewModelScope.launch {
            try {
                val uid = auth.currentUser?.uid
                auth.signOut()
                SessionManager(context).clearSession()
                if (uid != null) {
                    firestore.collection("sessions").document(uid).delete().await()
                }
            } catch (_: Exception) {
            }
            _authState.value = AuthState.LoggedOut
        }
    }

    fun resetState() {
        _authState.value = AuthState.Idle
    }
}
