import com.google.firebase.Timestamp
import com.project.webapp.Viewmodel.OrderStatus

data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalAmount: Double = 0.0,
    val status: String = OrderStatus.PAYMENT_RECEIVED.name,
    val createdAt: Long = 0,
    val paymentMethod: String = "",
    val deliveryAddress: String = "",
    val trackingNumber: String = "",
    val estimatedDelivery: Timestamp? = null,  // updated type
    val updatedAt: Long = 0
) {
    constructor() : this(
        orderId = "",
        buyerId = "",
        sellerId = "",
        items = emptyList(),
        totalAmount = 0.0,
        status = OrderStatus.PAYMENT_RECEIVED.name,
        createdAt = 0,
        paymentMethod = "",
        deliveryAddress = "",
        trackingNumber = "",
        estimatedDelivery = null,
        updatedAt = 0
    )
}
