import android.util.Log
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Product
import com.project.webapp.datas.UserData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CartViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()

    // State flows for reactive UI
    private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
    val cartItems: StateFlow<List<CartItem>> = _cartItems

    // State tracking for cart loading status
    private val _isCartLoading = MutableStateFlow(true)
    val isCartLoading: StateFlow<Boolean> = _isCartLoading

    // State to track if cart failed to load
    private val _cartLoadError = MutableStateFlow<String?>(null)
    val cartLoadError: StateFlow<String?> = _cartLoadError

    private val _cartIconShake = MutableStateFlow(false)
    val cartIconShake: StateFlow<Boolean> = _cartIconShake

    private val _showSnackbar = MutableStateFlow(false)
    val showSnackbar: StateFlow<Boolean> = _showSnackbar

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage

    // Explicitly synchronize checkout items with cart items
    private val _checkoutItems = MutableStateFlow<List<CartItem>>(emptyList())
    val checkoutItems: StateFlow<List<CartItem>> = _checkoutItems

    val totalCartPrice: StateFlow<Double> = _cartItems.map { items ->
        items.sumOf { it.price * it.quantity }
    }.stateIn(viewModelScope, SharingStarted.Lazily, 0.0)

    private val _directBuyItem = MutableStateFlow<CartItem?>(null)
    val directBuyItem: StateFlow<CartItem?> = _directBuyItem

    private val _currentUser = MutableStateFlow<UserData?>(null)
    val currentUser: StateFlow<UserData?> = _currentUser

    private val _purchasedItems = MutableStateFlow<List<CartItem>>(emptyList())
    val purchasedItems: StateFlow<List<CartItem>> = _purchasedItems

    init {
        loadCartItems()
        loadCurrentUser()
    }

    private fun loadCurrentUser() {
        val userId = auth.currentUser?.uid ?: return
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserData::class.java)
                _currentUser.value = user
                Log.d("CartViewModel", "Current user loaded: ${user?.firstname} ${user?.lastname}")
            }
            .addOnFailureListener { e ->
                Log.e("CartViewModel", "Error loading user data: ${e.message}")
            }
    }

    fun loadCartItems() {
        val userId = auth.currentUser?.uid
        if (userId == null) {
            Log.e("CartViewModel", "Cannot load cart: User not logged in")
            _isCartLoading.value = false
            _cartLoadError.value = "User not logged in"
            return
        }

        _isCartLoading.value = true
        _cartLoadError.value = null

        Log.d("CartViewModel", "Starting to load cart items for user: $userId")

        firestore.collection("carts").document(userId).collection("items")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("CartViewModel", "Error loading cart items: ${e.message}")
                    _isCartLoading.value = false
                    _cartLoadError.value = e.message
                    return@addSnapshotListener
                }

                val items = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CartItem::class.java)?.also { item ->
                        // Ensure the productId is set if it's not in the document
                        if (item.productId.isEmpty()) {
                            item.productId = doc.id
                        }
                        Log.d("CartViewModel", "Loaded cart item: ${item.name}, ID: ${item.productId}, Quantity: ${item.quantity}")
                    }
                } ?: emptyList()

                Log.d("CartViewModel", "Total items loaded in cart: ${items.size}")
                _cartItems.value = items

                // Ensure checkout items are synchronized with cart items
                _checkoutItems.value = items

                _isCartLoading.value = false
            }
    }

    // Function to verify cart is loaded and contains items
    fun verifyCartLoaded(onCartLoaded: (List<CartItem>) -> Unit, onEmptyCart: () -> Unit, onError: (String) -> Unit) {
        if (_isCartLoading.value) {
            onError("Cart is still loading")
            return
        }

        if (_cartLoadError.value != null) {
            onError(_cartLoadError.value ?: "Unknown error loading cart")
            return
        }

        val currentItems = _cartItems.value
        if (currentItems.isEmpty()) {
            Log.e("CartViewModel", "Cart is empty when verifying")
            onEmptyCart()
            return
        }

        Log.d("CartViewModel", "Cart verification successful: ${currentItems.size} items")
        onCartLoaded(currentItems)
    }

    // Function to fetch a product by its ID
    fun getProductById(productId: String, onResult: (Product?) -> Unit) {
        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                val product = document.toObject(Product::class.java)
                if (product != null) {
                    // Ensure ID is set
                    product.prodId = document.id
                }
                Log.d("CartViewModel", "Product retrieved: ${product?.name}")
                onResult(product)
            }
            .addOnFailureListener { error ->
                Log.e("CartViewModel", "Error fetching product by ID: ${error.message}")
                onResult(null)
            }
    }

    fun addToCart(product: Product, userType: String, navController: NavController) {
        triggerCartShake()
        val userId = auth.currentUser?.uid ?: return

        if (userType == "direct_buying") {
            // Set as direct buy item
            setDirectBuyItem(product)
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

    fun setDirectBuyItem(product: Product) {
        val directItem = CartItem(
            productId = product.prodId,
            name = product.name,
            price = product.price,
            quantity = 1,
            imageUrl = product.imageUrl,
            isDirectBuy = true,
            sellerId = product.ownerId,
            unit = product.quantityUnit,
            weight = product.quantity
        )

        Log.d("CartViewModel", "Setting direct buy item: ${directItem.name}")
        _directBuyItem.value = directItem

        // Also add to checkout items to ensure consistency
        _checkoutItems.value = listOf(directItem)
    }

    fun removeFromCart(itemId: String) {
        val userId = auth.currentUser?.uid ?: return
        viewModelScope.launch {
            Log.d("CartViewModel", "Removing item from cart: $itemId")
            firestore.collection("carts").document(userId).collection("items").document(itemId)
                .delete()
                .addOnSuccessListener {
                    Log.d("CartViewModel", "Item removed from cart: $itemId")
                }
                .addOnFailureListener { e ->
                    Log.e("CartViewModel", "Failed to remove item from cart: ${e.message}")
                }
        }
    }

    fun completePurchase(userType: String, paymentMethod: String, gcashRef: String = "") {
        val userId = auth.currentUser?.uid ?: return
        val orderId = firestore.collection("orders").document().id

        // First verify we have items to purchase
        verifyCartLoaded(
            onCartLoaded = { items ->
                val itemsToPurchase = if (userType == "direct_buying") {
                    items.filter { it.isDirectBuy }
                } else {
                    items
                }

                if (itemsToPurchase.isEmpty()) {
                    Log.e("Order", "No items to purchase!")
                    return@verifyCartLoaded
                }

                Log.d("Order", "Creating order with ${itemsToPurchase.size} items")

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
                    "paymentMethod" to if (paymentMethod.isNullOrBlank() || paymentMethod != "GCash") "COD" else "GCash",
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
            },
            onEmptyCart = {
                Log.e("Order", "Cannot complete purchase with empty cart")
            },
            onError = { error ->
                Log.e("Order", "Error when completing purchase: $error")
            }
        )
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

    fun getUserById(userId: String, onResult: (UserData?) -> Unit) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                val user = document.toObject(UserData::class.java)
                onResult(user)
            }
            .addOnFailureListener { e ->
                Log.e("CartViewModel", "Error fetching user: ${e.message}")
                onResult(null)
            }
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Int) {
        val userId = auth.currentUser?.uid ?: return

        viewModelScope.launch {
            Log.d("CartViewModel", "Updating quantity for $productId to $newQuantity")

            // Update in Firestore first
            firestore.collection("carts").document(userId).collection("items")
                .document(productId)
                .update("quantity", newQuantity)
                .addOnSuccessListener {
                    Log.d("CartViewModel", "Quantity updated in database")
                }
                .addOnFailureListener { e ->
                    Log.e("CartViewModel", "Failed to update quantity: ${e.message}")
                }

            // Also update in local state for immediate UI feedback
            val currentCart = _cartItems.value.toMutableList()
            val index = currentCart.indexOfFirst { it.productId == productId }
            if (index != -1) {
                val updatedItem = currentCart[index].copy(quantity = newQuantity)
                currentCart[index] = updatedItem
                _cartItems.value = currentCart

                // Keep checkout items in sync
                _checkoutItems.value = currentCart
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
        Log.d("CartViewModel", "Setting checkout items: ${items.size} items")
        _checkoutItems.value = items
    }

    // Force a manual refresh of cart items from the database
    fun refreshCartItems() {
        Log.d("CartViewModel", "Manually refreshing cart items")
        loadCartItems()
    }
}