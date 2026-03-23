package com.vardash.mafimushkil.auth

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ApplicationViewModel : ViewModel() {
    companion object {
        private const val TAG = "ApplicationViewModel"
    }

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    private val _applicationState = MutableStateFlow<ApplicationState>(ApplicationState.Idle)
    val applicationState: StateFlow<ApplicationState> = _applicationState.asStateFlow()

    fun submitWorkerApplication(
        fullName: String,
        phone: String,
        email: String,
        services: String,
        experience: String,
        bio: String
    ) {
        viewModelScope.launch {
            _applicationState.value = ApplicationState.Loading
            try {
                val uid = auth.currentUser?.uid.orEmpty()
                require(uid.isNotBlank()) { "User must be signed in to apply as a worker." }

                val applicationData = hashMapOf<String, Any?>(
                    "uid" to uid,
                    "name" to fullName,
                    "phone" to phone,
                    "email" to email,
                    "workerState" to "pending",
                    "workerExperience" to experience,
                    "workerServices" to services,
                    "workerSubmittedAt" to FieldValue.serverTimestamp(),
                    "workerUpdatedAt" to FieldValue.serverTimestamp()
                )

                Log.d(TAG, "Submitting worker application for user $uid")

                firestore.collection("users")
                    .document(uid)
                    .set(applicationData, SetOptions.merge())
                    .await()

                Log.d(TAG, "Worker application saved to 'users' collection for $uid")
                _applicationState.value = ApplicationState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit worker application", e)
                _applicationState.value = ApplicationState.Error(e.message ?: "Failed to submit application")
            }
        }
    }

    fun submitCompanyApplication(
        companyName: String,
        ownerName: String,
        phone: String,
        email: String,
        city: String,
        serviceType: String,
        registrationNumber: String,
        description: String
    ) {
        viewModelScope.launch {
            _applicationState.value = ApplicationState.Loading
            try {
                // Save to 'companies' collection instead of 'applications'
                val document = firestore.collection("companies").document()
                val applicationData = hashMapOf<String, Any?>(
                    "companyId" to document.id,
                    "userId" to auth.currentUser?.uid.orEmpty(),
                    "companyName" to companyName,
                    "ownerName" to ownerName,
                    "phone" to phone,
                    "email" to email,
                    "city" to city,
                    "serviceType" to serviceType,
                    "registrationNumber" to registrationNumber,
                    "description" to description,
                    "status" to "pending",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )

                Log.d(TAG, "Submitting company application ${document.id}")

                document
                    .set(applicationData)
                    .await()

                Log.d(TAG, "Company application saved to 'companies' collection: ${document.id}")
                _applicationState.value = ApplicationState.Success
            } catch (e: Exception) {
                Log.e(TAG, "Failed to submit company application", e)
                _applicationState.value = ApplicationState.Error(e.message ?: "Failed to submit application")
            }
        }
    }

    fun resetState() {
        _applicationState.value = ApplicationState.Idle
    }
}
