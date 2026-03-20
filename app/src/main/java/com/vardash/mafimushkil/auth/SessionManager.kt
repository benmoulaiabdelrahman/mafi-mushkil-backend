package com.vardash.mafimushkil.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SessionManager(context: Context) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "mafi_mushkil_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_UID = "user_uid"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SESSION_TOKEN = "session_token"
    }

    fun saveSession(uid: String, phone: String, token: String) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_PHONE, phone)
            .putString(KEY_SESSION_TOKEN, token)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun getUserUid(): String? = prefs.getString(KEY_UID, null)
    fun getUserPhone(): String? = prefs.getString(KEY_PHONE, null)
    fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
