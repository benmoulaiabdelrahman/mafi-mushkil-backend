package com.vardash.mafimushkil.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

data class UserProfile(
    val uid: String = "",
    val phone: String = "",
    val name: String = "",
    val email: String = "",
    val gender: String = "",
    val profilePhoto: String = "",
    val isEmailVerified: Boolean = false,
    val photoScale: Float = 1f,
    val photoOffsetX: Float = 0f,
    val photoOffsetY: Float = 0f,
    val workerState: String = "not",
    val workerExperience: String = "",
    val workerServices: String = "",
    val companyState: String = "not",
    val companyName: String = ""
)

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object Success : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    private fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val activeNetwork = connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            activeNetwork.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    fun loadUserProfile(context: Context? = null) {
        val uid = auth.currentUser?.uid ?: return
        
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
                    val companySnapshot = firestore.collection("companies")
                        .whereEqualTo("userId", uid)
                        .limit(1)
                        .get()
                        .await()
                    val companyDoc = companySnapshot.documents.firstOrNull()
                    
                    val workerState = snapshot.getString("workerState") ?: "not"
                    val companyState = companyDoc?.getString("status") ?: "not"
                    
                    // Sync to local session if context is provided
                    context?.let {
                        SessionManager(it).saveUserStates(workerState, companyState)
                    }

                    _userProfile.value = UserProfile(
                        uid = snapshot.getString("uid") ?: "",
                        phone = snapshot.getString("phone") ?: "",
                        name = snapshot.getString("name") ?: "",
                        email = snapshot.getString("email") ?: "",
                        gender = snapshot.getString("gender") ?: "",
                        profilePhoto = snapshot.getString("profilePhoto") ?: "",
                        isEmailVerified = auth.currentUser?.isEmailVerified ?: false,
                        photoScale = snapshot.getDouble("photoScale")?.toFloat() ?: 1f,
                        photoOffsetX = snapshot.getDouble("photoOffsetX")?.toFloat() ?: 0f,
                        photoOffsetY = snapshot.getDouble("photoOffsetY")?.toFloat() ?: 0f,
                        workerState = workerState,
                        workerExperience = snapshot.getString("workerExperience") ?: "",
                        workerServices = snapshot.getString("workerServices") ?: "",
                        companyState = companyState,
                        companyName = companyDoc?.getString("companyName") ?: ""
                    )
                }
                _profileState.value = ProfileState.Idle
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun checkEmailVerification() {
        val user = auth.currentUser ?: return
        user.reload().addOnCompleteListener {
            _userProfile.value = _userProfile.value.copy(isEmailVerified = user.isEmailVerified)
        }
    }

    fun updateName(name: String, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        
        _profileState.value = ProfileState.Loading

        if (!isNetworkAvailable(context)) {
            _profileState.value = ProfileState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("name", name).await()
                _userProfile.value = _userProfile.value.copy(name = name)
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update name")
            }
        }
    }

    fun updateEmail(email: String, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        val user = auth.currentUser ?: return
        
        _profileState.value = ProfileState.Loading

        if (!isNetworkAvailable(context)) {
            _profileState.value = ProfileState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("email", email).await()
                user.sendEmailVerification().await()
                _userProfile.value = _userProfile.value.copy(
                    email = email,
                    isEmailVerified = false
                )
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update email")
            }
        }
    }

    fun updateGender(gender: String, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        
        _profileState.value = ProfileState.Loading

        if (!isNetworkAvailable(context)) {
            _profileState.value = ProfileState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update("gender", gender).await()
                _userProfile.value = _userProfile.value.copy(gender = gender)
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update gender")
            }
        }
    }

    fun uploadProfilePhoto(uri: Uri, context: Context, scale: Float, offsetX: Float, offsetY: Float) {
        val uid = auth.currentUser?.uid ?: return
        
        _profileState.value = ProfileState.Loading

        if (!isNetworkAvailable(context)) {
            _profileState.value = ProfileState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        viewModelScope.launch {
            try {
                val photoUrl = CloudinaryManager.uploadImage(context, uri, uid)
                updateProfilePhoto(photoUrl, scale, offsetX, offsetY, context)
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to upload photo")
            }
        }
    }

    fun updateProfilePhoto(photoUrl: String, scale: Float, offsetX: Float, offsetY: Float, context: Context) {
        val uid = auth.currentUser?.uid ?: return
        
        _profileState.value = ProfileState.Loading

        if (!isNetworkAvailable(context)) {
            _profileState.value = ProfileState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update(
                        mapOf(
                            "profilePhoto" to photoUrl,
                            "photoScale" to scale,
                            "photoOffsetX" to offsetX,
                            "photoOffsetY" to offsetY
                        )
                    ).await()
                _userProfile.value = _userProfile.value.copy(
                    profilePhoto = photoUrl,
                    photoScale = scale,
                    photoOffsetX = offsetX,
                    photoOffsetY = offsetY
                )
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update profile photo")
            }
        }
    }

    fun revokeWorkerRegistration(context: Context) {
        val uid = auth.currentUser?.uid ?: return

        _profileState.value = ProfileState.Loading

        if (!isNetworkAvailable(context)) {
            _profileState.value = ProfileState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        viewModelScope.launch {
            try {
                firestore.collection("users").document(uid)
                    .update(
                        mapOf(
                            "workerState" to "not",
                            "workerExperience" to "",
                            "workerServices" to ""
                        )
                    ).await()
                
                SessionManager(context).saveUserStates("not", _userProfile.value.companyState)

                _userProfile.value = _userProfile.value.copy(
                    workerState = "not",
                    workerExperience = "",
                    workerServices = ""
                )
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to revoke worker registration")
            }
        }
    }

    fun revokeCompanyRegistration(context: Context) {
        val uid = auth.currentUser?.uid ?: return

        _profileState.value = ProfileState.Loading

        if (!isNetworkAvailable(context)) {
            _profileState.value = ProfileState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        viewModelScope.launch {
            try {
                val companySnapshot = firestore.collection("companies")
                    .whereEqualTo("userId", uid)
                    .limit(1)
                    .get()
                    .await()

                companySnapshot.documents.firstOrNull()?.reference?.delete()?.await()

                SessionManager(context).saveUserStates(_userProfile.value.workerState, "not")

                _userProfile.value = _userProfile.value.copy(
                    companyState = "not",
                    companyName = ""
                )
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to revoke company registration")
            }
        }
    }

    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}
