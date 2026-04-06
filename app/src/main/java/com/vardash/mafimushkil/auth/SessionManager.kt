package com.vardash.mafimushkil.auth

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vardash.mafimushkil.models.Category
import com.vardash.mafimushkil.models.Order

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

    private val gson = Gson()

    companion object {
        private const val KEY_UID = "user_uid"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_SESSION_TOKEN = "session_token"
        private const val KEY_WORKER_STATE = "worker_state"
        private const val KEY_COMPANY_STATE = "company_state"
        private const val KEY_CACHED_ORDERS = "cached_orders"
        private const val KEY_CACHED_SERVICE_ORDERS = "cached_service_orders"
        private const val KEY_CACHED_CATEGORIES = "cached_categories"
    }

    fun saveSession(uid: String, phone: String, token: String) {
        prefs.edit()
            .putString(KEY_UID, uid)
            .putString(KEY_PHONE, phone)
            .putString(KEY_SESSION_TOKEN, token)
            .putBoolean(KEY_IS_LOGGED_IN, true)
            .apply()
    }

    fun saveUserStates(workerState: String, companyState: String) {
        prefs.edit()
            .putString(KEY_WORKER_STATE, workerState)
            .putString(KEY_COMPANY_STATE, companyState)
            .apply()
    }

    fun saveOrders(orders: List<Order>) {
        val json = gson.toJson(orders)
        prefs.edit().putString(KEY_CACHED_ORDERS, json).apply()
    }

    fun getOrders(): List<Order> {
        val json = prefs.getString(KEY_CACHED_ORDERS, null) ?: return emptyList()
        val type = object : TypeToken<List<Order>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun saveServiceOrders(orders: List<Order>) {
        val json = gson.toJson(orders)
        prefs.edit().putString(KEY_CACHED_SERVICE_ORDERS, json).apply()
    }

    fun getServiceOrders(): List<Order> {
        val json = prefs.getString(KEY_CACHED_SERVICE_ORDERS, null) ?: return emptyList()
        val type = object : TypeToken<List<Order>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun saveCategories(categories: List<Category>) {
        val json = gson.toJson(categories)
        prefs.edit().putString(KEY_CACHED_CATEGORIES, json).apply()
    }

    fun getCategories(): List<Category> {
        val json = prefs.getString(KEY_CACHED_CATEGORIES, null) ?: return emptyList()
        val type = object : TypeToken<List<Category>>() {}.type
        return try { gson.fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    fun getUserUid(): String? = prefs.getString(KEY_UID, null)
    fun getUserPhone(): String? = prefs.getString(KEY_PHONE, null)
    fun getSessionToken(): String? = prefs.getString(KEY_SESSION_TOKEN, null)
    fun isLoggedIn(): Boolean = prefs.getBoolean(KEY_IS_LOGGED_IN, false)
    fun getWorkerState(): String = prefs.getString(KEY_WORKER_STATE, "not") ?: "not"
    fun getCompanyState(): String = prefs.getString(KEY_COMPANY_STATE, "not") ?: "not"

    fun clearSession() {
        prefs.edit().clear().apply()
    }
}
