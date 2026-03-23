package com.vardash.mafimushkil.auth

import android.content.Context
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
import com.vardash.mafimushkil.models.toEpochMillis
import com.vardash.mafimushkil.models.toOrder
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class OrderState {
    object Idle : OrderState()
    object Loading : OrderState()
    object Success : OrderState()
    data class Error(val message: String) : OrderState()
}

/*
Firestore schema used by the app and backend:

orders/{orderId}
{
  orderId: string,
  userId: string,
  status: "pending" | "accepted" | "confirmed" | "assigned" | "in_progress" | "completed" | "cancelled",
  address: string,
  details: string,
  createdAt: Timestamp,
  updatedAt: Timestamp,
  confirmedByCustomer: boolean,
  fcmToken: string,
  cancellationReason: string,
  categories: [
    { id: string, name: string, iconName: string }
  ],
  photoUrls: [string],
  bookedServices: [
    { id: string, name: string, price: number, quantity: number }
  ],
  workers: [
    { id: string, name: string, role: string, photoUrl: string }
  ],
  payments: [
    {
      id: string,
      title: string,
      amount: number,
      dueDate: number,
      status: "pending" | "paid",
      method: string,
      paidDate: number,
      checkoutUrl: string,
      reference: string
    }
  ],
  tax: number,
  discount: number,
  totalPrice: number
}

workers/{workerId}
{
  name: string,
  photoUrl: string,
  role: string,
  phoneNumber: string,
  isActive: boolean
}
*/
class OrderViewModel : ViewModel() {

    private val auth by lazy { FirebaseAuth.getInstance() }
    private val firestore by lazy { FirebaseFirestore.getInstance() }
    private var ordersListener: ListenerRegistration? = null
    private var orderListener: ListenerRegistration? = null

    private val _categories = MutableStateFlow<List<Category>>(emptyList())
    val categories: StateFlow<List<Category>> = _categories.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _pendingOrders = MutableStateFlow<List<Order>>(emptyList())
    val pendingOrders: StateFlow<List<Order>> = _pendingOrders.asStateFlow()

    private val _completedOrders = MutableStateFlow<List<Order>>(emptyList())
    val completedOrders: StateFlow<List<Order>> = _completedOrders.asStateFlow()

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    val allOrders: StateFlow<List<Order>> = _allOrders.asStateFlow()

    private val _selectedOrder = MutableStateFlow<Order?>(null)
    val selectedOrder: StateFlow<Order?> = _selectedOrder.asStateFlow()

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
            _isLoading.value = true
            _error.value = null
            try {
                val snapshot = firestore.collection("categories")
                    .whereEqualTo("isActive", true)
                    .get().await()

                if (snapshot.isEmpty) {
                    Log.d("OrderViewModel", "No categories found in Firestore. Attempting to seed...")
                    seedCategoriesInternal()
                    val newSnapshot = firestore.collection("categories")
                        .whereEqualTo("isActive", true)
                        .get().await()
                    _categories.value = newSnapshot.documents.mapNotNull { doc ->
                        doc.toObject(Category::class.java)?.copy(id = doc.id)
                    }
                } else {
                    _categories.value = snapshot.documents.mapNotNull { doc ->
                        doc.toObject(Category::class.java)?.copy(id = doc.id)
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error loading categories: ${e.message}", e)
                _error.value = e.message
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun loadUserOrders() {
        val uid = auth.currentUser?.uid ?: return
        ordersListener?.remove()

        ordersListener = firestore.collection("orders")
            .whereEqualTo("userId", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("OrderViewModel", "Listen failed: ${error.message}", error)
                    _error.value = error.message
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

                    _pendingOrders.value = allOrders.filter {
                        it.status.lowercase() in listOf("pending", "confirmed", "assigned", "accepted", "in_progress")
                    }
                    _completedOrders.value = allOrders.filter {
                        it.status.lowercase() in listOf("completed", "cancelled")
                    }
                    _allOrders.value = allOrders
                    updateUnreadNotificationCount(allOrders)
                }
            }
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

    fun observeOrder(orderId: String) {
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

    fun clearObservedOrder() {
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

        viewModelScope.launch {
            try {
                _orderState.value = OrderState.Loading

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

    private fun getDefaultCategories() = listOf(
        hashMapOf("name" to "Cleaning", "iconName" to "cleaning", "isActive" to true),
        hashMapOf("name" to "Electrician", "iconName" to "electrician", "isActive" to true),
        hashMapOf("name" to "Plumber", "iconName" to "plumber", "isActive" to true),
        hashMapOf("name" to "Carpenter", "iconName" to "carpenter", "isActive" to true),
        hashMapOf("name" to "Painter", "iconName" to "painter", "isActive" to true),
        hashMapOf("name" to "Mason", "iconName" to "mason", "isActive" to true),
        hashMapOf("name" to "Roofing", "iconName" to "roofing", "isActive" to true),
        hashMapOf("name" to "AC Repair", "iconName" to "ac_repair", "isActive" to true),
        hashMapOf("name" to "Glazier", "iconName" to "glazier", "isActive" to true),
        hashMapOf("name" to "Cook", "iconName" to "cook", "isActive" to true),
        hashMapOf("name" to "Babysitter", "iconName" to "babysitter", "isActive" to true),
        hashMapOf("name" to "Home Nurse", "iconName" to "nurse", "isActive" to true),
        hashMapOf("name" to "Car Wash", "iconName" to "car_wash", "isActive" to true),
        hashMapOf("name" to "Moving", "iconName" to "moving", "isActive" to true),
        hashMapOf("name" to "Gardener", "iconName" to "gardener", "isActive" to true),
        hashMapOf("name" to "Mechanic", "iconName" to "mechanic", "isActive" to true),
        hashMapOf("name" to "Delivery", "iconName" to "delivery", "isActive" to true),
        hashMapOf("name" to "Errands", "iconName" to "errands", "isActive" to true)
    )

    fun seedCategoriesIfEmpty() {
        viewModelScope.launch {
            try {
                val snapshot = firestore.collection("categories").get().await()
                if (snapshot.isEmpty) {
                    seedCategoriesInternal()
                } else {
                    val existingNames = snapshot.documents.mapNotNull {
                        it.getString("name")?.lowercase()?.trim()
                    }
                    val defaultCategories = getDefaultCategories()
                    defaultCategories.forEach { category ->
                        val name = category["name"] as String
                        if (name.lowercase().trim() !in existingNames) {
                            firestore.collection("categories").add(category).await()
                            Log.d("OrderViewModel", "Seeded missing category: $name")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("OrderViewModel", "Error seeding categories: ${e.message}", e)
            }
        }
    }

    private suspend fun seedCategoriesInternal() {
        val defaultCategories = getDefaultCategories()
        for (category in defaultCategories) {
            firestore.collection("categories").add(category).await()
            Log.d("OrderViewModel", "Seeded category: ${category["name"]}")
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
