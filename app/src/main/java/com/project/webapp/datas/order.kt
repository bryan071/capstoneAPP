import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import com.google.firebase.Timestamp


data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalAmount: Double = 0.0,
    val paymentMethod: String = "",
    val deliveryAddress: String = "",
    val status: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

data class OrderItemDetails(
    val title: String,
    val icon: ImageVector,
    val status: String,
    val timestamp: Timestamp = Timestamp.now(),
    val quantity: Int,
    val details: String
)

data class DialogDetails(
    val title: String,
    val status: String,
    val timestamp: String,
    val detailsContent: @Composable () -> Unit
)
