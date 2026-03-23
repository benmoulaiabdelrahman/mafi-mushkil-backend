package com.vardash.mafimushkil.auth

import android.util.Log
import com.google.firebase.Timestamp
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
        val token = currentToken()
        saveToken(uid, token)
    }

    suspend fun saveTokenForCurrentUser(token: String) {
        val uid = auth.currentUser?.uid ?: return
        saveToken(uid, token)
    }

    suspend fun currentToken(): String {
        return FirebaseMessaging.getInstance().token.await()
    }

    private suspend fun saveToken(uid: String, token: String) {
        try {
            firestore.collection("users")
                .document(uid)
                .set(
                    mapOf(
                        "fcmToken" to token,
                        "updatedAt" to Timestamp.now()
                    ),
                    SetOptions.merge()
                )
                .await()
            syncTokenToOrders(uid, token)
        } catch (e: Exception) {
            Log.e("FcmTokenManager", "Failed to save FCM token: ${e.message}", e)
        }
    }

    private suspend fun syncTokenToOrders(uid: String, token: String) {
        try {
            val snapshot = firestore.collection("orders")
                .whereEqualTo("userId", uid)
                .get()
                .await()

            if (snapshot.isEmpty) return

            val now = Timestamp.now()
            val batch = firestore.batch()
            snapshot.documents.forEach { document ->
                batch.set(
                    document.reference,
                    mapOf(
                        "fcmToken" to token,
                        "updatedAt" to now
                    ),
                    SetOptions.merge()
                )
            }
            batch.commit().await()
        } catch (e: Exception) {
            Log.e("FcmTokenManager", "Failed to sync FCM token to orders: ${e.message}", e)
        }
    }
}
