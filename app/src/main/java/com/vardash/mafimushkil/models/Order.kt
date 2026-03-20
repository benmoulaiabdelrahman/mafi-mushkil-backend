package com.vardash.mafimushkil.models

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
    val role: String = "",
    val photoUrl: String = ""
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
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
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
