import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
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

    private val _cartIconShake = MutableStateFlow(false)
    val cartIconShake: StateFlow<Boolean> = _cartIconShake

    private val _showSnackbar = MutableStateFlow(false)
    val showSnackbar: StateFlow<Boolean> = _showSnackbar

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    private val _checkoutItems = mutableStateListOf<CartItem>()
    val checkoutItems: List<CartItem> get() = _checkoutItems

    val totalCartPrice: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    init {
        loadCartItems()
    }

    fun loadCartItems() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("carts").document(userId).collection("items")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CartViewModel", "Error loading cart items: ${e.message}")
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CartItem::class.java)?.also {
                        Log.d("CartViewModel", "Loaded cart item: $it")
                    }
                } ?: emptyList()

                _cartItems.value = items
            }
    }

    // ✅ Function to fetch a product by its ID
    fun getProductById(productId: String, onResult: (Product?) -> Unit) {
        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                val product = document.toObject(Product::class.java)
                onResult(product)
            }
            .addOnFailureListener {
                Log.e("CartViewModel", "Error fetching product by ID: ${it.message}")
                onResult(null)
            }
    }

    fun addToCart(product: Product, userType: String, navController: NavController) {
        triggerCartShake()
        val userId = auth.currentUser?.uid ?: return

        if (userType == "direct_buying") {
            val totalPrice = product.price
            navController.navigate("paymentScreen/${product.prodId}/$totalPrice")
            return
        }

        viewModelScope.launch {
            val cartRef = firestore.collection("carts").document(userId).collection("items")
                .document(product.prodId)

            cartRef.get().addOnSuccessListener { document ->
                if (document.exists()) {
                    val newQuantity = (document.getLong("quantity") ?: 1) + 1
                    cartRef.update("quantity", newQuantity).addOnSuccessListener {
                        Log.d("Cart", "Quantity updated in cart")
                        loadCartItems()
                    }
                } else {
                    val cartItem = hashMapOf(
                        "productId" to product.prodId,
                        "name" to product.name,
                        "price" to product.price,
                        "weight" to product.quantity,
                        "unit" to product.quantityUnit,
                        "quantity" to 1,
                        "imageUrl" to product.imageUrl,
                        "sellerId" to product.ownerId,
                        "isDirectBuy" to false
                    )
                    cartRef.set(cartItem).addOnSuccessListener {
                        Log.d("Cart", "New product added to cart!")
                        loadCartItems()
                    }
                }

                _cartIconShake.value = true
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


    fun completePurchase(userType: String, paymentMethod: String, gcashRef: String) {
        val userId = auth.currentUser?.uid ?: return
        val orderId = firestore.collection("orders").document().id

        val itemsToPurchase = if (userType == "direct_buying") {
            cartItems.value.filter { it.isDirectBuy }
        } else {
            cartItems.value
        }

        if (itemsToPurchase.isEmpty()) {
            Log.e("Order", "No items to purchase!")
            return
        }

        val order = hashMapOf(
            "userId" to userId,
            "orderId" to orderId,
            "items" to itemsToPurchase.map { item ->
                hashMapOf(
                    "productId" to item.productId,
                    "name" to item.name,
                    "price" to item.price,
                    "quantity" to item.quantity
                )
            },
            "totalPrice" to itemsToPurchase.sumOf { it.price * it.quantity },
            "paymentMethod" to paymentMethod,
            "gcashReference" to if (paymentMethod == "GCash") gcashRef else null,
            "status" to "Pending",
            "timestamp" to System.currentTimeMillis()
        )

        firestore.collection("orders").document(orderId).set(order)
            .addOnSuccessListener {
                clearCartAfterPurchase(userId, isDirectBuy = userType == "direct_buying")
                Log.d("Order", "Order placed successfully!")
            }
            .addOnFailureListener { e ->
                Log.e("Order", "Failed to place order: ${e.message}")
            }
    }

    private fun clearCartAfterPurchase(userId: String, isDirectBuy: Boolean) {
        if (isDirectBuy) {
            val directBuyItem = cartItems.value.firstOrNull { it.isDirectBuy }
            directBuyItem?.let {
                firestore.collection("carts").document(userId).collection("items")
                    .document(it.productId).delete()
            }
        } else {
            firestore.collection("carts").document(userId).collection("items").get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        document.reference.delete()
                    }
                }
        }
    }



    fun clearSnackbarMessage() {
        _snackbarMessage.value = null
        _showSnackbar.value = false
    }

    fun triggerCartShake() {
        _cartIconShake.value = true
    }

    fun resetCartIconShake() {
        _cartIconShake.value = false
    }

    fun getTotalCartPrice(): Double {
        return totalCartPrice.value
    }

    fun setCheckoutItems(items: List<CartItem>) {
        _checkoutItems.clear()
        _checkoutItems.addAll(items)
    }


}



