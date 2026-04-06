package com.vardash.mafimushkil.auth

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.vardash.mafimushkil.screens.Promotion
import com.vardash.mafimushkil.screens.PromotionStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class PromotionViewModel : ViewModel() {

    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var promotionsListener: ListenerRegistration? = null

    private val _promotions = MutableStateFlow<List<Promotion>>(emptyList())
    val promotions: StateFlow<List<Promotion>> = _promotions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun loadPromotions(context: Context? = null) {
        // Initial load from cache if available
        context?.let {
            val cached = getCachedPromotions(it)
            if (cached.isNotEmpty() && _promotions.value.isEmpty()) {
                _promotions.value = cached
            }
        }

        promotionsListener?.remove()
        _isLoading.value = true

        promotionsListener = firestore.collection("promotions")
            .whereEqualTo("isActive", true)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false
                if (error != null) {
                    Log.e("PromotionViewModel", "Listen failed: ${error.message}")
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val list = snapshot.documents.mapNotNull { doc ->
                        try {
                            Promotion(
                                id = doc.id,
                                title = doc.getString("title") ?: "",
                                description = doc.getString("description") ?: "",
                                status = if (doc.getBoolean("isExpired") == true) PromotionStatus.EXPIRED else PromotionStatus.ACTIVE,
                                dateRange = doc.getString("dateRange")
                            )
                        } catch (e: Exception) {
                            null
                        }
                    }
                    _promotions.value = list
                    context?.let { savePromotionsToCache(it, list) }
                }
            }
    }

    private fun savePromotionsToCache(context: Context, list: List<Promotion>) {
        val prefs = context.getSharedPreferences("promotions_cache", Context.MODE_PRIVATE)
        val json = Gson().toJson(list)
        prefs.edit().putString("cached_list", json).apply()
    }

    private fun getCachedPromotions(context: Context): List<Promotion> {
        val prefs = context.getSharedPreferences("promotions_cache", Context.MODE_PRIVATE)
        val json = prefs.getString("cached_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Promotion>>() {}.type
        return try { Gson().fromJson(json, type) } catch (e: Exception) { emptyList() }
    }

    override fun onCleared() {
        super.onCleared()
        promotionsListener?.remove()
    }
}
