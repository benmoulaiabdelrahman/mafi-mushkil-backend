package com.vardash.mafimushkil.auth

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

object FcmTokenManager {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }

    suspend fun fetchAndStoreToken() {
        val uid = auth.currentUser?.uid ?: return
        val token = FirebaseMessaging.getInstance().token.await()
        saveToken(uid, token)
    }

    suspend fun saveTokenForCurrentUser(token: String) {
        val uid = auth.currentUser?.uid ?: return
        saveToken(uid, token)
    }

    private suspend fun saveToken(uid: String, token: String) {
        try {
            firestore.collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "fcmToken" to token,
                        "updatedAt" to System.currentTimeMillis()
                    ),
                    SetOptions.merge()
                )
                .await()
        } catch (e: Exception) {
            Log.e("FcmTokenManager", "Failed to save FCM token: ${e.message}", e)
        }
    }
}
