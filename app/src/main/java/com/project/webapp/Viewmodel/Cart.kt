        import android.util.Log
        import androidx.lifecycle.ViewModel
        import androidx.lifecycle.viewModelScope
        import androidx.navigation.NavController
        import com.google.firebase.auth.FirebaseAuth
        import com.google.firebase.firestore.FirebaseFirestore
        import com.project.webapp.datas.CartItem
        import com.project.webapp.datas.Product
        import com.project.webapp.datas.UserData
        import kotlinx.coroutines.flow.MutableStateFlow
        import kotlinx.coroutines.flow.StateFlow
        import kotlinx.coroutines.flow.asStateFlow
        import kotlinx.coroutines.launch
        import kotlinx.coroutines.tasks.await

        class CartViewModel : ViewModel() {
            private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
            private val auth: FirebaseAuth = FirebaseAuth.getInstance()

            // State flows for reactive UI
            private val _cartItems = MutableStateFlow<List<CartItem>>(emptyList())
            val cartItems: StateFlow<List<CartItem>> = _cartItems.asStateFlow()

            private val _isCartLoading = MutableStateFlow(true)
            val isCartLoading: StateFlow<Boolean> = _isCartLoading.asStateFlow()

            private val _cartLoadError = MutableStateFlow<String?>(null)
            val cartLoadError: StateFlow<String?> = _cartLoadError.asStateFlow()

            private val _cartIconShake = MutableStateFlow(false)
            val cartIconShake: StateFlow<Boolean> = _cartIconShake.asStateFlow()

            private val _showSnackbar = MutableStateFlow(false)
            val showSnackbar: StateFlow<Boolean> = _showSnackbar.asStateFlow()

            private val _snackbarMessage = MutableStateFlow<String?>(null)
            val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

            private val _checkoutItems = MutableStateFlow<List<CartItem>>(emptyList())
            val checkoutItems: StateFlow<List<CartItem>> = _checkoutItems.asStateFlow()

            private val _directBuyItem = MutableStateFlow<CartItem?>(null)
            val directBuyItem: StateFlow<CartItem?> = _directBuyItem.asStateFlow()

            private val _currentUser = MutableStateFlow<UserData?>(null)
            val currentUser: StateFlow<UserData?> = _currentUser.asStateFlow()

            private val _purchasedItems = MutableStateFlow<List<CartItem>>(emptyList())
            val purchasedItems: StateFlow<List<CartItem>> = _purchasedItems.asStateFlow()

            private val _totalCartPrice = MutableStateFlow(0.0)
            val totalCartPrice: StateFlow<Double> = _totalCartPrice.asStateFlow()

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
                        _cartLoadError.value = "Failed to load user: ${e.message}"
                    }
            }

            fun loadCartItems() {
                val userId = auth.currentUser?.uid
                if (userId == null) {
                    Log.e("CartViewModel", "Cannot load cart: User not logged in")
                    _isCartLoading.value = false
                    _cartLoadError.value = "User not logged in"
                    _cartItems.value = emptyList()
                    _totalCartPrice.value = 0.0
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
                            _cartItems.value = emptyList()
                            _totalCartPrice.value = 0.0
                            return@addSnapshotListener
                        }

                        val items = snapshot?.documents?.mapNotNull { doc ->
                            try {
                                val item = doc.toObject(CartItem::class.java)?.also { cartItem ->
                                    if (cartItem.productId.isEmpty()) {
                                        cartItem.productId = doc.id
                                    }
                                    if (cartItem.price <= 0 || cartItem.quantity <= 0 || cartItem.productId.isEmpty()) {
                                        Log.w("CartViewModel", "Invalid cart item: $cartItem, Firestore data: ${doc.data}")
                                        viewModelScope.launch {
                                            doc.reference.delete().await()
                                            Log.d("CartViewModel", "Deleted invalid cart item: ${doc.id}")
                                        }
                                        null
                                    } else {
                                        cartItem
                                    }
                                }
                                item
                            } catch (e: Exception) {
                                Log.e("CartViewModel", "Error parsing cart item: ${doc.data}, ${e.message}")
                                null
                            }
                        } ?: emptyList()

                        Log.d("CartViewModel", "Total items loaded in cart: ${items.size}")
                        _cartItems.value = items
                        _totalCartPrice.value = items.sumOf { it.price * it.quantity }
                        _checkoutItems.value = items
                        _isCartLoading.value = false
                    }
            }

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

            fun getProductById(productId: String, onResult: (Product?) -> Unit) {
                firestore.collection("products").document(productId)
                    .get()
                    .addOnSuccessListener { document ->
                        val product = document.toObject(Product::class.java)
                        if (product != null) {
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
                    setDirectBuyItem(product)
                    val totalPrice = product.price
                    navController.navigate("paymentScreen/${product.prodId}/$totalPrice")
                    return
                }

                viewModelScope.launch {
                    val cartRef = firestore.collection("carts").document(userId).collection("items")
                        .document(product.prodId)

                    try {
                        val document = cartRef.get().await()
                        if (document.exists()) {
                            val newQuantity = (document.getLong("quantity") ?: 1) + 1
                            cartRef.update("quantity", newQuantity).await()
                            Log.d("CartViewModel", "Quantity updated in cart: ${product.name}, New Quantity: $newQuantity")
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
                            cartRef.set(cartItem).await()
                            Log.d("CartViewModel", "New product added to cart: ${product.name}")
                        }

                        _cartIconShake.value = true
                        _snackbarMessage.value = "${product.name} added to cart!"
                        _showSnackbar.value = true
                    } catch (e: Exception) {
                        Log.e("CartViewModel", "Failed to add to cart: ${e.message}")
                        _cartLoadError.value = "Failed to add to cart: ${e.message}"
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
                _checkoutItems.value = listOf(directItem)
                _purchasedItems.value = listOf(directItem)
            }

            fun removeFromCart(itemId: String) {
                val userId = auth.currentUser?.uid ?: return
                viewModelScope.launch {
                    try {
                        Log.d("CartViewModel", "Removing item from cart: $itemId")
                        firestore.collection("carts").document(userId).collection("items").document(itemId)
                            .delete().await()
                        Log.d("CartViewModel", "Item removed from cart: $itemId")
                    } catch (e: Exception) {
                        Log.e("CartViewModel", "Failed to remove item from cart: ${e.message}")
                        _cartLoadError.value = "Failed to remove item: ${e.message}"
                    }
                }
            }

            fun completePurchase(userType: String, paymentMethod: String, referenceId: String = "") {
                val itemsToPurchase = if (userType == "direct_buying") {
                    _directBuyItem.value?.let { listOf(it) } ?: emptyList()
                } else {
                    _checkoutItems.value
                }

                if (itemsToPurchase.isEmpty()) {
                    Log.e("CartViewModel", "No items to complete purchase, DirectBuy: ${_directBuyItem.value}, Checkout: ${_checkoutItems.value.size}, Cart: ${_cartItems.value.size}")
                    clearCart()
                    return
                }

                // Log purchased items for debugging
                itemsToPurchase.forEach { item ->
                    Log.d("CartViewModel", "Purchased item: ${item.name}, ID: ${item.productId}, Quantity: ${item.quantity}, Price: ${item.price}")
                }

                // Set purchased items and clear cart
                _purchasedItems.value = itemsToPurchase
                viewModelScope.launch {
                    clearCart()
                    _directBuyItem.value = null
                    _checkoutItems.value = emptyList()
                    _purchasedItems.value = emptyList()
                    Log.d("CartViewModel", "Purchase completed and cart cleared")
                }
            }

            private fun clearCart() {
                viewModelScope.launch {
                    val userId = auth.currentUser?.uid ?: return@launch
                    val cartRef = firestore.collection("carts").document(userId).collection("items")

                    try {
                        val snapshot = cartRef.get().await()
                        Log.d("CartViewModel", "Cart items before clear: ${snapshot.documents.size}")
                        snapshot.documents.forEach { doc ->
                            Log.d("CartViewModel", "Firestore cart item: ${doc.id}, ${doc.data}")
                        }
                        val batch = firestore.batch()
                        snapshot.documents.forEach { doc ->
                            batch.delete(doc.reference)
                        }
                        batch.commit().await()
                        _cartItems.value = emptyList()
                        _totalCartPrice.value = 0.0
                        Log.d("CartViewModel", "Cart cleared successfully")
                    } catch (e: Exception) {
                        Log.e("CartViewModel", "Error clearing cart: ${e.message}")
                        _cartLoadError.value = "Failed to clear cart: ${e.message}"
                        _cartItems.value = emptyList()
                        _totalCartPrice.value = 0.0
                    }
                }
            }

            fun clearPurchasedItems() {
                _purchasedItems.value = emptyList()
                Log.d("CartViewModel", "Cleared purchased items")
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
                if (newQuantity <= 0) {
                    removeFromCart(productId)
                    return
                }

                val userId = auth.currentUser?.uid ?: return

                viewModelScope.launch {
                    try {
                        Log.d("CartViewModel", "Updating quantity for $productId to $newQuantity")
                        firestore.collection("carts").document(userId).collection("items")
                            .document(productId)
                            .update("quantity", newQuantity)
                            .await()
                        Log.d("CartViewModel", "Quantity updated in database")

                        val currentCart = _cartItems.value.toMutableList()
                        val index = currentCart.indexOfFirst { it.productId == productId }
                        if (index != -1) {
                            val updatedItem = currentCart[index].copy(quantity = newQuantity)
                            currentCart[index] = updatedItem
                            _cartItems.value = currentCart
                            _checkoutItems.value = currentCart
                            _totalCartPrice.value = currentCart.sumOf { it.price * it.quantity }
                        }
                    } catch (e: Exception) {
                        Log.e("CartViewModel", "Failed to update quantity: ${e.message}")
                        _cartLoadError.value = "Failed to update quantity: ${e.message}"
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
                Log.d("CartViewModel", "Setting checkout items: ${items.size} items, Items: ${items.joinToString { "${it.name} (ID: ${it.productId})" }}")
                _checkoutItems.value = items
                _purchasedItems.value = items
            }

            fun refreshCartItems() {
                Log.d("CartViewModel", "Manually refreshing cart items")
                loadCartItems()
            }
        }