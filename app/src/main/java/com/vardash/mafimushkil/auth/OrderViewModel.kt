package com.vardash.mafimushkil.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.vardash.mafimushkil.models.Category
import com.vardash.mafimushkil.models.Order
import com.vardash.mafimushkil.models.SelectedCategory
import com.vardash.mafimushkil.models.defaultServiceCategories
import com.vardash.mafimushkil.models.toEpochMillis
import com.vardash.mafimushkil.models.toOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.SetOptions

sealed class OrderState {
    object Idle : OrderState()
    object Loading : OrderState()
    object Success : OrderState()
    data class Error(val message: String) : OrderState()
}

open class OrderViewModel : ViewModel() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var ordersListener: ListenerRegistration? = null
    private var orderListener: ListenerRegistration? = null

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _categoriesReady = MutableStateFlow(false)
    val categoriesReady: StateFlow<Boolean> = _categoriesReady.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    open val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingOrders = MutableStateFlow<List<Order>>(emptyList())
    val pendingOrders: StateFlow<List<Order>> = _pendingOrders.asStateFlow()

    private val _completedOrders = MutableStateFlow<List<Order>>(emptyList())
    val completedOrders: StateFlow<List<Order>> = _completedOrders.asStateFlow()

    private val _servicePendingOrders = MutableStateFlow<List<Order>>(emptyList())
    val servicePendingOrders: StateFlow<List<Order>> = _servicePendingOrders.asStateFlow()

    private val _serviceCompletedOrders = MutableStateFlow<List<Order>>(emptyList())
    val serviceCompletedOrders: StateFlow<List<Order>> = _serviceCompletedOrders.asStateFlow()

    private val _serviceAllOrders = MutableStateFlow<List<Order>>(emptyList())
    val serviceAllOrders: StateFlow<List<Order>> = _serviceAllOrders.asStateFlow()

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders.asStateFlow()

    private val _isUserOrdersLoaded = MutableStateFlow(false)
    val isUserOrdersLoaded: StateFlow<Boolean> = _isUserOrdersLoaded.asStateFlow()

    private val _isServiceOrdersLoaded = MutableStateFlow(false)
    val isServiceOrdersLoaded: StateFlow<Boolean> = _isServiceOrdersLoaded.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    open val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

    private val _orderState = MutableStateFlow<OrderState>(OrderState.Idle)
    val orderState: StateFlow<OrderState> = _orderState.asStateFlow()

    private val notifiableStatuses = setOf("accepted", "confirmed", "assigned", "in_progress", "completed", "cancelled")
    private var notificationsLastSeenAt: Long = 0L

    val pendingCategories = mutableStateListOf<SelectedCategory>()
    val pendingPhotos = mutableStateListOf<Uri>()
    var pendingAddress by mutableStateOf("")
    var pendingDetails by mutableStateOf("")

    private var initialCategoryIdAdded: String? = null

    fun addInitialCategory(id: String, name: String, iconName: String) {
        if (id.isNotEmpty() && id != "{categoryId}" && initialCategoryIdAdded != id) {
            initialCategoryIdAdded = id
            addCategory(id, name, iconName)
        }
    }

    fun addCategory(id: String, name: String, iconName: String) {
        if (pendingCategories.none { it.id == id }) {
            pendingCategories.add(SelectedCategory(id, name, iconName))
        }
    }

    fun removeCategory(id: String) {
        pendingCategories.removeIf { it.id == id }
    }

    fun addPhoto(uri: Uri) {
        if (pendingPhotos.size < 5 && !pendingPhotos.contains(uri)) {
            pendingPhotos.add(uri)
        }
    }

    fun removePhoto(index: Int) {
        if (index < pendingPhotos.size) pendingPhotos.removeAt(index)
    }

    fun clearPendingOrder() {
        pendingCategories.clear()
        pendingPhotos.clear()
        pendingAddress = ""
        pendingDetails = ""
        initialCategoryIdAdded = null
    }

    fun loadCategories() {
        viewModelScope.launch {
            _categoriesReady.value = false
            _isLoading.value = true
            _error.value = null
            try {
                val snapshot = firestore.collection("categories")
                    .whereEqualTo("isActive", true)
                    .get().await()

                if (snapshot.isEmpty) {
                    Log.d("OrderViewModel", "No categories found in Firestore.")
                    _categories.value = emptyList()
                } else {
                    _categories.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Category::class.java)?.copy(id = doc.id)?.withFixedIcon()
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error loading categories: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
                _categoriesReady.value = true
            }
        }
    }

    fun loadUserOrders(context: Context? = null) {
        val uid = auth.currentUser?.uid ?: return
        
        // Initial load from cache
        context?.let {
            val cached = SessionManager(it).getOrders()
            if (cached.isNotEmpty()) {
                updateOrdersState(cached)
                _isUserOrdersLoaded.value = true
            }
        }

        ordersListener?.remove()
        ordersListener = firestore.collection("orders")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderViewModel", "Listen failed: ${error.message}", error)
                    _error.value = error.message
                    _isUserOrdersLoaded.value = true
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val allOrders = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toOrder()
                        } catch (e: Exception) {
                            Log.e("OrderViewModel", "Error parsing doc ${doc.id}: ${e.message}", e)
                            null
                        }
                    }.sortedByDescending { it.createdAt.toEpochMillis() }

                    updateOrdersState(allOrders)
                    _isUserOrdersLoaded.value = true
                    context?.let { SessionManager(it).saveOrders(allOrders) }
                } else {
                    _isUserOrdersLoaded.value = true
                }
            }
    }

    private fun updateOrdersState(orders: List<Order>) {
        _pendingOrders.value = orders.filter {
            it.status.lowercase() in listOf("pending", "confirmed", "assigned", "accepted", "in_progress")
        }
        _completedOrders.value = orders.filter {
            it.status.lowercase() in listOf("completed", "cancelled")
        }
        _allOrders.value = orders
        updateUnreadNotificationCount(orders)
    }

    fun loadServiceOrders(context: Context? = null) {
        val uid = auth.currentUser?.uid ?: return

        // Initial load from cache
        context?.let {
            val cached = SessionManager(it).getServiceOrders()
            if (cached.isNotEmpty()) {
                updateServiceOrdersState(cached)
                _isServiceOrdersLoaded.value = true
            }
        }

        ordersListener?.remove()
        ordersListener = firestore.collection("orders")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderViewModel", "Listen failed: ${error.message}", error)
                    _error.value = error.message
                    _isServiceOrdersLoaded.value = true
                    return@addSnapshotListener
                }

                if (snapshot != null) {
                    val assignedOrders = snapshot.documents.mapNotNull { doc ->
                        try {
                            doc.toOrder()
                        } catch (e: Exception) {
                            Log.e("OrderViewModel", "Error parsing service doc ${doc.id}: ${e.message}", e)
                            null
                        }
                    }.filter { order ->
                        order.displayWorkers.any { it.id == uid } ||
                            order.workers.any { it.id == uid } ||
                            order.assignedWorkers.any { it.id == uid }
                    }.sortedByDescending { it.createdAt.toEpochMillis() }

                    updateServiceOrdersState(assignedOrders)
                    _isServiceOrdersLoaded.value = true
                    context?.let { SessionManager(it).saveServiceOrders(assignedOrders) }
                } else {
                    _isServiceOrdersLoaded.value = true
                }
            }
    }

    private fun updateServiceOrdersState(orders: List<Order>) {
        _servicePendingOrders.value = orders.filter {
            it.status.lowercase() in listOf("pending", "confirmed", "assigned", "accepted", "in_progress")
        }
        _serviceCompletedOrders.value = orders.filter {
            it.status.lowercase() in listOf("completed", "cancelled")
        }
        _serviceAllOrders.value = orders
    }

    fun markNotificationsSeen() {
        val latestNotificationTime = _allOrders.value
            .filter { it.status.lowercase() in notifiableStatuses }
            .maxOfOrNull { notificationTimestamp(it) }
            ?: System.currentTimeMillis()

        notificationsLastSeenAt = latestNotificationTime
        updateUnreadNotificationCount(_allOrders.value)
    }

    private fun updateUnreadNotificationCount(orders: List<Order>) {
        val unreadCount = orders.count { order ->
            order.status.lowercase() in notifiableStatuses &&
                notificationTimestamp(order) > notificationsLastSeenAt
        }
        NotificationBadgeStore.setUnreadCount(unreadCount)
    }

    private fun notificationTimestamp(order: Order): Long {
        val updatedAt = order.updatedAt.toEpochMillis()
        val createdAt = order.createdAt.toEpochMillis()
        return maxOf(updatedAt, createdAt)
    }

    open fun observeOrder(orderId: String) {
        if (orderId.isBlank()) return

        orderListener?.remove()
        _isLoading.value = true
        _error.value = null

        orderListener = firestore.collection("orders")
            .document(orderId)
            .addSnapshotListener { snapshot, error ->
                _isLoading.value = false

                if (error != null) {
                    Log.e("OrderViewModel", "Order listener failed: ${error.message}", error)
                    _error.value = error.message
                    return@addSnapshotListener
                }

                if (snapshot != null && snapshot.exists()) {
                    _selectedOrder.value = snapshot.toOrder()
                } else {
                    _selectedOrder.value = null
                }
            }
    }

    open fun clearObservedOrder() {
        orderListener?.remove()
        orderListener = null
        _selectedOrder.value = null
    }

    fun confirmOrderDetails(orderId: String) {
        if (orderId.isBlank()) return

        viewModelScope.launch {
            _orderState.value = OrderState.Loading
            try {
                firestore.collection("orders")
                    .document(orderId)
                    .update(
                        mapOf(
                            "status" to "confirmed",
                            "confirmedByCustomer" to true,
                            "updatedAt" to Timestamp.now()
                        )
                    )
                    .await()

                _orderState.value = OrderState.Success
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error confirming order: ${e.message}", e)
                _orderState.value = OrderState.Error(e.message ?: "Failed to confirm order")
            }
        }
    }

    fun markPaymentCompleted(
        orderId: String,
        paymentId: String,
        checkoutUrl: String = "",
        reference: String = ""
    ) {
        viewModelScope.launch {
            completePayment(orderId, paymentId, checkoutUrl, reference)
        }
    }

    suspend fun completePayment(
        orderId: String,
        paymentId: String,
        checkoutUrl: String = "",
        reference: String = ""
    ) {
        if (orderId.isBlank()) return

        try {
            Log.d(
                "OrderViewModel",
                "Marking payment completed for order=$orderId payment=$paymentId reference=${reference.ifBlank { "none" }}"
            )
            val document = firestore.collection("orders").document(orderId).get().await()
            val order = document.toOrder()
            val nowMillis = System.currentTimeMillis()
            val nowTimestamp = Timestamp.now()
            val completedPaymentId = paymentId.takeIf { it.isNotBlank() }
                ?: order.payments.firstOrNull {
                    it.status.lowercase() !in setOf("paid", "cleared", "completed")
                }?.id
            val shouldMarkPayment = { paymentIdCandidate: String? ->
                completedPaymentId == null && order.payments.size == 1 ||
                    completedPaymentId != null && paymentIdCandidate == completedPaymentId
            }

            val updatedPayments = order.payments.map { payment ->
                if (shouldMarkPayment(payment.id)) {
                    payment.copy(
                        status = "paid",
                        paidDate = nowMillis,
                        checkoutUrl = if (checkoutUrl.isBlank()) payment.checkoutUrl else checkoutUrl,
                        reference = if (reference.isBlank()) payment.reference else reference
                    )
                } else {
                    payment
                }
            }

            val paymentMaps = updatedPayments.map { payment ->
                mapOf(
                    "id" to payment.id,
                    "title" to payment.title,
                    "amount" to payment.amount,
                    "dueDate" to payment.dueDate,
                    "status" to payment.status,
                    "method" to payment.method,
                    "paidDate" to if (shouldMarkPayment(payment.id)) nowTimestamp else payment.paidDate,
                    "checkoutUrl" to payment.checkoutUrl,
                    "reference" to payment.reference
                )
            }

            firestore.collection("orders")
                .document(orderId)
                .update(
                    mapOf(
                        "payments" to paymentMaps,
                        "status" to "completed",
                        "updatedAt" to nowTimestamp
                    )
                )
                .await()

            Log.d("OrderViewModel", "Order $orderId marked completed after payment $paymentId")
        } catch (e: Exception) {
            Log.e("OrderViewModel", "Error completing payment: ${e.message}", e)
            _error.value = e.message
        }
    }

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

    fun placeOrder(
        context: Context,
        selectedCategories: List<SelectedCategory>,
        address: String,
        details: String,
        photoUris: List<Uri> = emptyList()
    ) {
        val uid = auth.currentUser?.uid ?: run {
            _orderState.value = OrderState.Error("User not logged in")
            return
        }

        if (!isNetworkAvailable(context)) {
            // Use a unique error message or timestamp to ensure StateFlow emits if retrying while offline
            _orderState.value = OrderState.Error("network error: No internet connection (${System.currentTimeMillis()})")
            return
        }

        // Set loading only after connectivity is confirmed.
        _orderState.value = OrderState.Loading

        viewModelScope.launch {
            try {
                // Already in loading state from above
                val uploadedUrls = photoUris.map { uri ->
                    CloudinaryManager.uploadOrderImage(context, uri)
                }
                val fcmToken = runCatching { FcmTokenManager.currentToken() }.getOrDefault("")

                val nowTimestamp = Timestamp.now()
                val orderData = hashMapOf(
                    "userId" to uid,
                    "categories" to selectedCategories.map { category ->
                        mapOf(
                            "id" to category.id,
                            "name" to category.name,
                            "iconName" to category.iconName
                        )
                    },
                    "address" to address,
                    "details" to details,
                    "photoUrls" to uploadedUrls,
                    "status" to "pending",
                    "createdAt" to nowTimestamp,
                    "updatedAt" to nowTimestamp,
                    "bookedServices" to emptyList<Map<String, Any>>(),
                    "workers" to emptyList<Map<String, Any>>(),
                    "assignedWorkers" to emptyList<Map<String, Any>>(),
                    "payments" to emptyList<Map<String, Any>>(),
                    "tax" to 0.0,
                    "discount" to 0.0,
                    "totalPrice" to 0.0,
                    "confirmedByCustomer" to false,
                    "fcmToken" to fcmToken,
                    "cancellationReason" to ""
                )

                val docRef = firestore.collection("orders").add(orderData).await()
                docRef.update("orderId", docRef.id).await()

                _orderState.value = OrderState.Success
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error placing order: ${e.message}", e)
                _orderState.value = OrderState.Error(e.message ?: "Failed to place order")
            }
        }
    }

    private fun getDefaultCategories() = defaultServiceCategories

    fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            try {
                val defaultCategories = getDefaultCategories()
                val snapshot = firestore.collection("categories").get().await()
                val existingCategoryIds = snapshot.documents.map { it.id }.toSet()

                if (snapshot.isEmpty) {
                    Log.d("OrderViewModel", "Firestore categories collection is empty. Seeding all default categories.")
                    for (category in defaultCategories) {
                        firestore.collection("categories").document(category.id).set(category, SetOptions.merge()).await()
                        Log.d("OrderViewModel", "Seeded category: ${category.name} with ID: ${category.id}")
                    }
                } else {
                    Log.d("OrderViewModel", "Firestore categories collection is not empty. Checking for missing default categories.")
                    for (category in defaultCategories) {
                        if (category.id !in existingCategoryIds) {
                            firestore.collection("categories").document(category.id).set(category, SetOptions.merge()).await()
                            Log.d("OrderViewModel", "Seeded missing default category: ${category.name} with ID: ${category.id}")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error seeding categories: ${e.message}", e)
            }
        }
    }

    private fun Category.withFixedIcon(): Category {
        return when (name.lowercase().trim()) {
            "pest control" -> copy(iconName = "cockroach")
            else -> this
        }
    }

    fun resetState() {
        _orderState.value = OrderState.Idle
    }

    override fun onCleared() {
        super.onCleared()
        ordersListener?.remove()
        orderListener?.remove()
    }
}
