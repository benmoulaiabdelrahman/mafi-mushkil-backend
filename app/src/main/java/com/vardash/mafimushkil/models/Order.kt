package com.vardash.mafimushkil.models

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import java.util.Date

data class SelectedCategory(
    val id: String = "",
    val name: String = "",
    val iconName: String = ""
)

data class BookedService(
    val id: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1
)

data class Worker(
    val id: String = "",
    val name: String = "",
    val services: String = "",
    val photoUrl: String = "",
    val photoOffsetX: Float = 0f,
    val photoOffsetY: Float = 0f,
    val photoScale: Float = 1f
)

data class Payment(
    val id: String = "",
    val title: String = "",
    val amount: Double = 0.0,
    val dueDate: Long = 0L,
    val status: String = "pending", // "pending", "paid"
    val method: String = "", // e.g., "EDAHABIA", "CIB"
    val paidDate: Long = 0L,
    val checkoutUrl: String = "",
    val reference: String = ""
)

data class Order(
    val orderId: String = "",
    val userId: String = "",
    val categories: List<SelectedCategory> = emptyList(),
    val address: String = "",
    val details: String = "",
    val photoUrls: List<String> = emptyList(),
    val status: String = "pending",
    val createdAt: Any? = null,
    val updatedAt: Any? = null,
    val bookedServices: List<BookedService> = emptyList(),
    val tax: Double = 0.0,
    val discount: Double = 0.0,
    val totalPrice: Double = 0.0,
    val workers: List<Worker> = emptyList(),
    val assignedWorkers: List<Worker> = emptyList(),
    val payments: List<Payment> = emptyList(),
    val confirmedByCustomer: Boolean = false,
    val fcmToken: String = "",
    val cancellationReason: String = ""
) {
    val displayWorkers: List<Worker>
        get() = if (workers.isNotEmpty()) workers else assignedWorkers

    val subtotal: Double
        get() = bookedServices.sumOf { it.price * it.quantity }
}

fun DocumentSnapshot.toOrder(): Order {
    val data = data.orEmpty()
    return Order(
        orderId = id,
        userId = data.readString("userId"),
        categories = data.readList("categories").mapNotNull { it.asSelectedCategory() },
        address = data.readString("address"),
        details = data.readString("details"),
        photoUrls = data.readList("photoUrls").mapNotNull { it as? String },
        status = data.readString("status", "pending"),
        createdAt = data["createdAt"],
        updatedAt = data["updatedAt"],
        bookedServices = data.readList("bookedServices").mapNotNull { it.asBookedService() },
        tax = data.readDouble("tax"),
        discount = data.readDouble("discount"),
        totalPrice = data.readDouble("totalPrice"),
        workers = data.readList("workers").mapNotNull { it.asWorker() },
        assignedWorkers = data.readList("assignedWorkers").mapNotNull { it.asWorker() },
        payments = data.readList("payments").mapNotNull { it.asPayment() },
        confirmedByCustomer = data["confirmedByCustomer"] as? Boolean ?: false,
        fcmToken = data.readString("fcmToken"),
        cancellationReason = data.readString("cancellationReason")
    )
}

private fun Any?.asSelectedCategory(): SelectedCategory? {
    val map = this as? Map<*, *> ?: return null
    return SelectedCategory(
        id = map["id"]?.toString().orEmpty(),
        name = map["name"]?.toString().orEmpty(),
        iconName = map["iconName"]?.toString().orEmpty()
    )
}

private fun Any?.asBookedService(): BookedService? {
    val map = this as? Map<*, *> ?: return null
    return BookedService(
        id = map["id"]?.toString().orEmpty(),
        name = map["name"]?.toString().orEmpty(),
        price = (map["price"] as? Number)?.toDouble() ?: 0.0,
        quantity = (map["quantity"] as? Number)?.toInt() ?: 1
    )
}

private fun Any?.asWorker(): Worker? {
    val map = this as? Map<*, *> ?: return null
    return Worker(
        id = map["id"]?.toString().orEmpty(),
        name = map["name"]?.toString().orEmpty(),
        services = map["services"]?.toString().orEmpty(),
        photoUrl = map["photoUrl"]?.toString().orEmpty(),
        photoOffsetX = (map["photoOffsetX"] as? Number)?.toFloat() ?: 0f,
        photoOffsetY = (map["photoOffsetY"] as? Number)?.toFloat() ?: 0f,
        photoScale = (map["photoScale"] as? Number)?.toFloat() ?: 1f
    )
}

private fun Any?.asPayment(): Payment? {
    val map = this as? Map<*, *> ?: return null
    return Payment(
        id = map["id"]?.toString().orEmpty(),
        title = map["title"]?.toString().orEmpty(),
        amount = (map["amount"] as? Number)?.toDouble() ?: 0.0,
        dueDate = map.readEpochMillis("dueDate"),
        status = map["status"]?.toString().orEmpty().ifBlank { "pending" },
        method = map["method"]?.toString().orEmpty(),
        paidDate = map.readEpochMillis("paidDate"),
        checkoutUrl = map["checkoutUrl"]?.toString().orEmpty(),
        reference = map["reference"]?.toString().orEmpty()
    )
}

private fun Map<String, Any?>.readString(key: String, default: String = ""): String =
    (this[key] as? String)?.trim().orEmpty().ifBlank { default }

private fun Map<String, Any?>.readDouble(key: String): Double =
    (this[key] as? Number)?.toDouble() ?: 0.0

private fun Map<String, Any?>.readList(key: String): List<*> =
    this[key] as? List<*> ?: emptyList<Any?>()

private fun Map<*, *>.readEpochMillis(key: String): Long =
    this[key].toEpochMillis()

fun Any?.toEpochMillis(): Long = when (this) {
    is Long -> this
    is Int -> this.toLong()
    is Double -> this.toLong()
    is Float -> this.toLong()
    is Timestamp -> this.toDate().time
    is Date -> this.time
    else -> 0L
}

fun Any?.toFirebaseTimestamp(): Timestamp? = when (this) {
    is Timestamp -> this
    is Date -> this.time.toFirebaseTimestamp()
    is Long -> this.toFirebaseTimestamp()
    is Int -> this.toLong().toFirebaseTimestamp()
    is Double -> this.toLong().toFirebaseTimestamp()
    is Float -> this.toLong().toFirebaseTimestamp()
    else -> null
}

private fun Long.toFirebaseTimestamp(): Timestamp {
    val seconds = this / 1000
    val nanoseconds = ((this % 1000) * 1_000_000).toInt()
    return Timestamp(seconds, nanoseconds)
}
