package com.vardash.mafimushkil.auth

import android.content.Context
import android.net.Uri
import android.util.Log
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

object CloudinaryManager {

    fun init(context: Context) {
        val config = mapOf(
            "cloud_name" to "devbcg9y8",
            "api_key" to "311559561289735",
            "api_secret" to "xJ3t5biuuAZi6R4GEJ3ybS89mIc",
            "secure" to true
        )
        try {
            MediaManager.init(context, config)
        } catch (e: Exception) {
            // Already initialized
        }
    }

    suspend fun uploadImage(context: Context, imageUri: Uri, uid: String): String {
        return suspendCoroutine { continuation ->
            // Use a unique public_id for each upload to avoid caching issues
            val publicId = "profile_photos/${uid}_${System.currentTimeMillis()}"
            
            MediaManager.get()
                .upload(imageUri)
                .option("public_id", publicId)
                .option("upload_preset", "kyd4zvli")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {
                        Log.d("CloudinaryManager", "Upload started")
                    }
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as String
                        Log.d("CloudinaryManager", "Upload success: $url")
                        continuation.resume(url)
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        Log.e("CloudinaryManager", "Upload error: ${error.description}")
                        continuation.resumeWithException(Exception(error.description))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch(context)
        }
    }

    suspend fun uploadOrderImage(context: Context, imageUri: Uri): String {
        val randomId = UUID.randomUUID().toString()
        return suspendCoroutine { continuation ->
            MediaManager.get()
                .upload(imageUri)
                .option("folder", "order_photos")
                .option("public_id", randomId)
                .option("upload_preset", "kyd4zvli")
                .callback(object : UploadCallback {
                    override fun onStart(requestId: String) {}
                    override fun onProgress(requestId: String, bytes: Long, totalBytes: Long) {}
                    override fun onSuccess(requestId: String, resultData: Map<*, *>) {
                        val url = resultData["secure_url"] as String
                        continuation.resume(url)
                    }
                    override fun onError(requestId: String, error: ErrorInfo) {
                        continuation.resumeWithException(Exception(error.description))
                    }
                    override fun onReschedule(requestId: String, error: ErrorInfo) {}
                })
                .dispatch(context)
        }
    }
}
