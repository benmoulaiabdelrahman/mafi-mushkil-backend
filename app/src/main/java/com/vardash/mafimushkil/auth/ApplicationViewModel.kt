package com.vardash.mafimushkil.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ApplicationViewModel : ViewModel() {

    // Use lazy initialization to prevent crashes during Compose Previews 
    // where FirebaseApp is not initialized.
    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _applicationState = MutableStateFlow<ApplicationState>(ApplicationState.Idle)
    val applicationState: StateFlow<ApplicationState> = _applicationState.asStateFlow()

    fun submitWorkerApplication(
        fullName: String,
        phone: String,
        city: String,
        specialization: String,
        experience: String,
        bio: String
    ) {
        viewModelScope.launch {
            _applicationState.value = ApplicationState.Loading
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val applicationData = hashMapOf(
                    "userId" to userId,
                    "type" to "worker",
                    "fullName" to fullName,
                    "phone" to phone,
                    "city" to city,
                    "specialization" to specialization,
                    "experience" to experience,
                    "bio" to bio,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
                
                firestore.collection("applications")
                    .add(applicationData)
                    .await()
                
                _applicationState.value = ApplicationState.Success
            } catch (e: Exception) {
                _applicationState.value = ApplicationState.Error(e.message ?: "Failed to submit application")
            }
        }
    }

    fun submitCompanyApplication(
        companyName: String,
        ownerName: String,
        phone: String,
        city: String,
        serviceType: String,
        registrationNumber: String,
        description: String
    ) {
        viewModelScope.launch {
            _applicationState.value = ApplicationState.Loading
            try {
                val userId = auth.currentUser?.uid ?: throw Exception("User not logged in")
                val applicationData = hashMapOf(
                    "userId" to userId,
                    "type" to "company",
                    "companyName" to companyName,
                    "ownerName" to ownerName,
                    "phone" to phone,
                    "city" to city,
                    "serviceType" to serviceType,
                    "registrationNumber" to registrationNumber,
                    "description" to description,
                    "status" to "pending",
                    "createdAt" to System.currentTimeMillis()
                )
                
                firestore.collection("applications")
                    .add(applicationData)
                    .await()
                
                _applicationState.value = ApplicationState.Success
            } catch (e: Exception) {
                _applicationState.value = ApplicationState.Error(e.message ?: "Failed to submit application")
            }
        }
    }

    fun resetState() {
        _applicationState.value = ApplicationState.Idle
    }
}
