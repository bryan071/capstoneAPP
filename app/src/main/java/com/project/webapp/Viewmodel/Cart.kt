import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Organization
import com.project.webapp.datas.Product
import com.project.webapp.datas.ReceiptData
import com.project.webapp.datas.UserData
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlin.math.min

class CartViewModel : ViewModel() {
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val userId: String? get() = auth.currentUser?.uid

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

    // Debounce cart refresh
    private var lastCartRefresh = 0L
    private val debounceInterval = 500L

    // Receipt data storage
    private val receiptDataMap = mutableMapOf<String, ReceiptData>()

    init {
        loadCurrentUser()
        loadCartItems()
    }

    private fun loadCurrentUser() {
        val uid = userId ?: run {
            _cartLoadError.value = "User not authenticated"
            return
        }

        viewModelScope.launch {
            retryIO(3) {
                firestore.collection("users").document(uid).get().await()
                    .toObject(UserData::class.java)
            }.onSuccess { user ->
                _currentUser.value = user
            }.onFailure { e ->
                _cartLoadError.value = "Failed to load user: ${e.message}"
            }
        }
    }

    private suspend fun <T> retryIO(retries: Int, block: suspend () -> T): Result<T> {
        var attempt = 0
        while (attempt < retries) {
            try {
                return Result.success(block())
            } catch (e: Exception) {
                attempt++
                if (attempt == retries) return Result.failure(e)
                delay(min(1000L shl (attempt - 1), 4000L)) // Exponential backoff
            }
        }
        return Result.failure(Exception("Operation failed after $retries attempts"))
    }

    fun loadCartItems() {
        val uid = userId ?: run {
            _isCartLoading.value = false
            _cartLoadError.value = "User not logged in"
            _cartItems.value = emptyList()
            _totalCartPrice.value = 0.0
            return
        }

        _isCartLoading.value = true
        _cartLoadError.value = null

        firestore.collection("carts").document(uid).collection("items")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _isCartLoading.value = false
                    _cartLoadError.value = e.message
                    _cartItems.value = emptyList()
                    _totalCartPrice.value = 0.0
                    return@addSnapshotListener
                }

                viewModelScope.launch {
                    val invalidDocs = mutableListOf<com.google.firebase.firestore.DocumentReference>()
                    val items = snapshot?.documents?.mapNotNull { doc ->
                        val item = doc.toObject(CartItem::class.java)?.also { cartItem ->
                            if (cartItem.productId.isEmpty()) cartItem.productId = doc.id
                            if (cartItem.price <= 0 || cartItem.quantity <= 0 || cartItem.productId.isEmpty()) {
                                invalidDocs.add(doc.reference)
                                null
                            } else {
                                cartItem
                            }
                        }
                        item
                    } ?: emptyList()

                    // Batch delete invalid items
                    if (invalidDocs.isNotEmpty()) {
                        val batch = firestore.batch()
                        invalidDocs.forEach { batch.delete(it) }
                        batch.commit().await()
                    }

                    _cartItems.value = items
                    _totalCartPrice.value = items.sumOf { it.price * it.quantity }
                    _checkoutItems.value = items
                    _isCartLoading.value = false
                }
            }
    }

    fun verifyCartLoaded(onCartLoaded: (List<CartItem>) -> Unit, onEmptyCart: () -> Unit, onError: (String) -> Unit) {
        when {
            _isCartLoading.value -> onError("Cart is still loading")
            _cartLoadError.value != null -> onError(_cartLoadError.value ?: "Unknown error")
            _cartItems.value.isEmpty() -> onEmptyCart()
            else -> onCartLoaded(_cartItems.value)
        }
    }

    fun getProductById(productId: String, onResult: (Product?) -> Unit) {
        viewModelScope.launch {
            try {
                val document = firestore.collection("products").document(productId).get().await()
                val product = document.toObject(Product::class.java)?.apply { prodId = document.id }
                onResult(product)
            } catch (e: Exception) {
                onResult(null)
            }
        }
    }

    fun addToCart(product: Product, userType: String, navController: NavController) {
        val uid = userId ?: return
        triggerCartShake()

        if (userType == "direct_buying") {
            setDirectBuyItem(product)
            navController.navigate("paymentScreen/${product.prodId}/${product.price}")
            return
        }

        viewModelScope.launch {
            val cartRef = firestore.collection("carts").document(uid).collection("items").document(product.prodId)
            try {
                val document = cartRef.get().await()
                if (document.exists()) {
                    val newQuantity = (document.getLong("quantity") ?: 1) + 1
                    cartRef.update("quantity", newQuantity).await()
                } else {
                    val cartItem = CartItem(
                        productId = product.prodId,
                        name = product.name,
                        price = product.price,
                        weight = product.quantity,
                        unit = product.quantityUnit,
                        quantity = 1,
                        imageUrl = product.imageUrl,
                        sellerId = product.ownerId,
                        isDirectBuy = false
                    )
                    cartRef.set(cartItem).await()
                }
                _snackbarMessage.value = "${product.name} added to cart!"
                _showSnackbar.value = true
            } catch (e: Exception) {
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
        _directBuyItem.value = directItem
        _checkoutItems.value = listOf(directItem)
        _purchasedItems.value = listOf(directItem)
    }

    fun removeFromCart(itemId: String) {
        val uid = userId ?: return
        viewModelScope.launch {
            try {
                firestore.collection("carts").document(uid).collection("items").document(itemId)
                    .delete().await()
            } catch (e: Exception) {
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

        if (itemsToPurchase.isEmpty()) return

        _purchasedItems.value = itemsToPurchase
        viewModelScope.launch {
            clearCart()
            _directBuyItem.value = null
            _checkoutItems.value = emptyList()
            _purchasedItems.value = emptyList()
        }
    }

    fun clearCart() {
        val uid = userId ?: return
        viewModelScope.launch {
            try {
                val cartRef = firestore.collection("carts").document(uid).collection("items")
                val snapshot = cartRef.get().await()
                val batch = firestore.batch()
                snapshot.documents.forEach { batch.delete(it.reference) }
                batch.commit().await()
                _cartItems.value = emptyList()
                _totalCartPrice.value = 0.0
            } catch (e: Exception) {
                _cartLoadError.value = "Failed to clear cart: ${e.message}"
                _cartItems.value = emptyList()
                _totalCartPrice.value = 0.0
            }
        }
    }

    fun clearPurchasedItems() {
        _purchasedItems.value = emptyList()
    }

    fun getUserById(userId: String, onResult: (UserData?) -> Unit) {
        viewModelScope.launch {
            retryIO(3) {
                firestore.collection("users").document(userId).get().await()
                    .toObject(UserData::class.java)
            }.onSuccess { user ->
                onResult(user)
            }.onFailure { e ->
                onResult(null)
            }
        }
    }

    fun updateCartItemQuantity(productId: String, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeFromCart(productId)
            return
        }

        val uid = userId ?: return
        viewModelScope.launch {
            try {
                val cartRef = firestore.collection("carts").document(uid).collection("items").document(productId)
                cartRef.update("quantity", newQuantity).await()

                val currentCart = _cartItems.value.toMutableList()
                val index = currentCart.indexOfFirst { it.productId == productId }
                if (index != -1) {
                    currentCart[index] = currentCart[index].copy(quantity = newQuantity)
                    _cartItems.value = currentCart
                    _checkoutItems.value = currentCart
                    _totalCartPrice.value = currentCart.sumOf { it.price * it.quantity }
                }
            } catch (e: Exception) {
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

    fun getTotalCartPrice(): Double = _totalCartPrice.value

    fun setCheckoutItems(items: List<CartItem>) {
        _checkoutItems.value = items
        _purchasedItems.value = items
    }

    fun refreshCartItems() {
        val now = System.currentTimeMillis()
        if (now - lastCartRefresh < debounceInterval) return
        lastCartRefresh = now
        loadCartItems()
    }

    fun setReceiptData(
        orderNumber: String,
        cartItems: List<CartItem>,
        totalPrice: Double,
        userType: String,
        sellerNames: Map<String, String>,
        paymentMethod: String,
        referenceId: String,
        organization: Organization?,
        isDonation: Boolean
    ) {
        receiptDataMap[orderNumber] = ReceiptData(
            cartItems = cartItems,
            totalPrice = totalPrice,
            userType = userType,
            sellerNames = sellerNames,
            paymentMethod = paymentMethod,
            referenceId = referenceId,
            organization = organization,
            isDonation = isDonation
        )
    }

    fun getReceiptData(orderNumber: String): ReceiptData? = receiptDataMap[orderNumber]

    fun clearReceiptData(orderNumber: String) {
        receiptDataMap.remove(orderNumber)
    }
}