package com.vardash.mafimushkil.auth

import android.content.Context
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
    val photoOffsetY: Float = 0f
)

sealed class ProfileState {
    object Idle : ProfileState()
    object Loading : ProfileState()
    object Success : ProfileState()
    data class Error(val message: String) : ProfileState()
}

class ProfileViewModel : ViewModel() {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private val _profileState = MutableStateFlow<ProfileState>(ProfileState.Idle)
    val profileState: StateFlow<ProfileState> = _profileState.asStateFlow()

    fun loadUserProfile() {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                val snapshot = firestore.collection("users").document(uid).get().await()
                if (snapshot.exists()) {
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
                        photoOffsetY = snapshot.getDouble("photoOffsetY")?.toFloat() ?: 0f
                    )
                }
                _profileState.value = ProfileState.Idle
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to load profile")
            }
        }
    }

    fun updateName(name: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                firestore.collection("users").document(uid)
                    .update("name", name).await()
                _userProfile.value = _userProfile.value.copy(name = name)
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update name")
            }
        }
    }

    fun updateEmail(email: String) {
        val uid = auth.currentUser?.uid ?: return
        val user = auth.currentUser ?: return
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
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

    fun checkEmailVerification() {
        viewModelScope.launch {
            try {
                auth.currentUser?.reload()?.await()
                val isVerified = auth.currentUser?.isEmailVerified ?: false
                _userProfile.value = _userProfile.value.copy(isEmailVerified = isVerified)
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateGender(gender: String) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                firestore.collection("users").document(uid)
                    .update("gender", gender).await()
                _userProfile.value = _userProfile.value.copy(gender = gender)
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to update gender")
            }
        }
    }

    // Upload profile photo to Cloudinary
    fun uploadProfilePhoto(imageUri: Uri, context: Context, scale: Float = 1f, offsetX: Float = 0f, offsetY: Float = 0f) {
        val uid = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            try {
                _profileState.value = ProfileState.Loading
                
                // Upload to Cloudinary (only if it's a new URI, otherwise just update metadata)
                val downloadUrl = if (imageUri.toString().startsWith("http")) {
                    imageUri.toString()
                } else {
                    CloudinaryManager.uploadImage(context, imageUri, uid)
                }
                
                // Save URL and adjustment metadata to Firestore
                firestore.collection("users").document(uid)
                    .update(
                        mapOf(
                            "profilePhoto" to downloadUrl,
                            "photoScale" to scale,
                            "photoOffsetX" to offsetX,
                            "photoOffsetY" to offsetY
                        )
                    ).await()

                _userProfile.value = _userProfile.value.copy(
                    profilePhoto = downloadUrl,
                    photoScale = scale,
                    photoOffsetX = offsetX,
                    photoOffsetY = offsetY
                )
                _profileState.value = ProfileState.Success
            } catch (e: Exception) {
                _profileState.value = ProfileState.Error(e.message ?: "Failed to upload photo")
            }
        }
    }

    fun resetState() {
        _profileState.value = ProfileState.Idle
    }
}
