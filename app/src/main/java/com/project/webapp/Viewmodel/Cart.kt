import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    private val _totalPrice = MutableStateFlow(0.0)
    val totalPrice: StateFlow<Double> = _totalPrice

    private val _cartIconShake = MutableStateFlow(false)
    val cartIconShake: StateFlow<Boolean> = _cartIconShake

    private val _showSnackbar = MutableStateFlow(false)
    val showSnackbar: StateFlow<Boolean> = _showSnackbar

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    val totalCartPrice: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)


    init {
        loadCartItems()
    }

    private fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("carts").document(userId).collection("items")
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                val items = snapshot.documents.mapNotNull { it.toObject(CartItem::class.java) }
                _cartItems.value = items
                calculateTotal(items)
            }
    }

    fun addToCart(product: Product) {
        triggerCartShake()
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            val cartRef = firestore.collection("carts").document(userId).collection("items")
                .document(product.prodId)

            cartRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val newQuantity = (document.getLong("quantity") ?: 1) + 1
                    cartRef.update("quantity", newQuantity)
                } else {
                    val cartItem = hashMapOf(
                        "productId" to product.prodId,
                        "name" to product.name,
                        "price" to product.price,
                        "quantity" to 1,
                        "imageUrl" to product.imageUrl,
                        "sellerId" to product.ownerId
                    )
                    cartRef.set(cartItem)
                }
                Log.d("Cart", "Product added to cart successfully!")

                // Trigger cart icon animation
                _cartIconShake.value = true

                // Show Snackbar notification
                _snackbarMessage.value = "${product.name} added to cart!"
                _showSnackbar.value = true
            }.addOnFailureListener { e ->
                Log.e("Cart", "Failed to add to cart: ${e.message}")
            }
        }
    }

    fun removeFromCart(itemId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            firestore.collection("carts").document(userId).collection("items").document(itemId)
                .delete()
        }
    }

    private fun calculateTotal(items: List<CartItem>) {
        _totalPrice.value = items.sumOf { it.price * it.quantity }
    }

    fun completePurchase() {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            val orderRef =
                firestore.collection("orders").document(userId).collection("userOrders").document()
            val orderData = hashMapOf(
                "items" to _cartItems.value,
                "totalPrice" to _totalPrice.value,
                "timestamp" to System.currentTimeMillis()
            )

            orderRef.set(orderData).addOnSuccessListener {
                Log.d("Order", "Order placed successfully!")
                clearCart(userId)
            }.addOnFailureListener { e ->
                Log.e("Order", "Failed to place order: ${e.message}")
            }
        }
    }

    private fun clearCart(userId: String) {
        firestore.collection("carts").document(userId).collection("items")
            .get()
            .addOnSuccessListener { snapshot ->
                for (document in snapshot.documents) {
                    document.reference.delete()
                }
                _cartItems.value = emptyList()
                _totalPrice.value = 0.0
            }
            .addOnFailureListener { e ->
                Log.e("Cart", "Failed to clear cart: ${e.message}")
            }
    }

    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
        _showSnackbar.value = false
    }

    fun triggerCartShake() {
        _cartIconShake.value = true
    }

    // Reset cart icon animation after it runs
    fun resetCartIconShake() {
        _cartIconShake.value = false
    }
    fun getTotalCartPrice(): Double {
        return totalCartPrice.value
    }

    fun completePurchase(paymentMethod: String, gcashRef: String) {
        val userId = auth.currentUser?.uid ?: return
        val orderId = firestore.collection("orders").document().id // Generate order ID

        val order = hashMapOf(
            "userId" to userId,
            "orderId" to orderId,
            "items" to cartItems.value.map { item ->
                hashMapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "price" to item.price,
                    "quantity" to item.quantity
                )
            },
            "totalPrice" to totalPrice.value,
            "paymentMethod" to paymentMethod,
            "gcashReference" to if (paymentMethod == "GCash") gcashRef else null,
            "status" to "Pending",
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("orders").document(orderId).set(order)
            .addOnSuccessListener {
                // Clear the cart after order is placed
                clearCart()
                Log.d("Order", "Order placed successfully!")
            }
            .addOnFailureListener { e ->
                Log.e("Order", "Failed to place order: ${e.message}")
            }
    }

    fun clearCart() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("carts").document(userId).collection("items")
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    document.reference.delete()
                }
                _cartItems.value = emptyList()
                _totalPrice.value = 0.0
            }
    }
}