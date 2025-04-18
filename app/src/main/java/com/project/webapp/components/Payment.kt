package com.project.webapp.components

import android.graphics.Bitmap
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.runtime.getValue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.R
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.GCashApiConfig
import com.project.webapp.datas.GCashPaymentRequest
import com.project.webapp.datas.Organization
import com.project.webapp.datas.Product
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID
import kotlin.random.Random


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    directBuyProductId: String? = null,
    directBuyPrice: String? = null
) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotalState by cartViewModel.totalCartPrice.collectAsState()
    val currentUser by cartViewModel.currentUser.collectAsStateWithLifecycle()
    val shippingFee = 50.0
    val amount = (directBuyPrice?.toDoubleOrNull() ?: cartTotalState) + shippingFee
    val formattedAmount = "%.2f".format(amount)
    val sellerNames = remember { mutableStateMapOf<String, String>() }
    val userType = if (directBuyProductId != null) "direct_buying" else "cart_checkout"
    var directBuyProduct by remember { mutableStateOf<Product?>(null) }

    // Define the theme color
    val themeColor = Color(0xFF0DA54B)

    // Fetch direct buy product
    LaunchedEffect(directBuyProductId) {
        directBuyProductId?.let { id ->
            cartViewModel.getProductById(id) { product ->
                directBuyProduct = product
            }
        }
    }

    // Prepare items to display
    val displayItems: List<CartItem> = remember(directBuyProduct, cartItems) {
        if (directBuyProduct != null) {
            listOf(
                CartItem(
                    productId = directBuyProduct!!.prodId,
                    name = directBuyProduct!!.name,
                    price = directBuyProduct!!.price,
                    quantity = 1,
                    imageUrl = directBuyProduct!!.imageUrl,
                    isDirectBuy = true
                )
            )
        } else {
            cartItems
        }
    }

    // Fetch seller info for each item and map it by productId
    LaunchedEffect(displayItems) {
        displayItems.forEach { item ->
            cartViewModel.getProductById(item.productId) { product ->
                product?.let { prod ->
                    cartViewModel.getUserById(prod.ownerId) { user ->
                        user?.let {
                            sellerNames[prod.prodId] = "${it.firstname} ${it.lastname}"
                        }
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
                    // Back Button
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Section: Order Summary
                SectionTitle(title = "Order Summary")

                // Section: Shipping Info
                currentUser?.let { user ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "Shipping Address",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            Text(
                                user.address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )

                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 12.dp),
                                color = Color(0xFFEEEEEE)
                            )

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${user.firstname} ${user.lastname}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    user.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Section: Items to purchase
                SectionTitle(title = "Items (${displayItems.size})")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 16.dp)
                    ) {
                        displayItems.forEachIndexed { index, item ->
                            ItemCard(
                                item = item,
                                sellerName = sellerNames[item.productId] ?: "Loading seller...",
                                themeColor = themeColor
                            )

                            if (index < displayItems.size - 1) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = Color(0xFFEEEEEE)
                                )
                            }
                        }
                    }
                }

                // Section: Payment Methods
                SectionTitle(title = "Payment Methods")

                var ownerId by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(directBuyProduct, displayItems) {
                    if (directBuyProduct != null) {
                        ownerId = directBuyProduct?.ownerId
                    } else if (displayItems.isNotEmpty()) {
                        // For cart: we need to get the product first to access its ownerId
                        cartViewModel.getProductById(displayItems.first().productId) { product ->
                            product?.let {
                                ownerId = product.ownerId
                            }
                        }
                    }
                }

                var ownerName by remember { mutableStateOf("Loading...") }

                LaunchedEffect(ownerId) {
                    ownerId?.let {
                        fetchOwnerName(FirebaseFirestore.getInstance(), it) { name ->
                            ownerName = name
                        }
                    }
                }

                Text(
                    text = "Pay to: $ownerName",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Button(
                    onClick = {
                        ownerId?.let { id ->
                            navController.navigate("gcashScreen/$formattedAmount/$id")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Pay with GCash",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                    }
                }



                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = {
                        cartViewModel.setCheckoutItems(displayItems)
                        // Save the checkout information to database
                        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@Button

                        // Get current user delivery address from Firestore
                        val firestore = FirebaseFirestore.getInstance()
                        firestore.collection("users").document(userId).get()
                            .addOnSuccessListener { userDocument ->
                                val userAddress = userDocument.getString("address") ?: "No address provided"

                                // Process each item and create notifications for the sellers
                                displayItems.forEach { cartItem ->
                                    cartViewModel.getProductById(cartItem.productId) { product ->
                                        product?.let { prod ->
                                            // Create a sale notification for the product owner
                                            createSaleNotification(
                                                firestore = firestore,
                                                product = prod,
                                                buyerId = userId,
                                                quantity = cartItem.quantity,
                                                paymentMethod = "Cash on Delivery",
                                                deliveryAddress = userAddress,
                                                message = "Your product was sold! Payment method: Cash on Delivery"
                                            )

                                            // Update product inventory in database
                                            updateProductInventory(prod.prodId, cartItem.quantity)
                                        }
                                    }
                                }

                                // Create order record
                                createOrderRecord(userId, displayItems, "Cash on Delivery", amount.toFloat(), userAddress)

                                // Navigate to checkout confirmation
                                navController.navigate("checkoutScreen/$userType/${amount.toFloat()}")
                            }
                            .addOnFailureListener { e ->
                                Log.e("Payment", "Error fetching user data", e)
                                // Create order with default address
                                createOrderRecord(userId, displayItems, "Cash on Delivery", amount.toFloat(), "No address provided")
                                navController.navigate("checkoutScreen/$userType/${amount.toFloat()}")
                            }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    enabled = displayItems.isNotEmpty()
                ) {
                    Text(
                        text = "Pay with Cash on Delivery",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

// Helper function to update product inventory after purchase
private fun updateProductInventory(productId: String, quantitySold: Int) {
    val firestore = FirebaseFirestore.getInstance()

    // Get current product data
    firestore.collection("products").document(productId)
        .get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val currentQuantity = document.getLong("quantity")?.toInt() ?: 0
                val newQuantity = (currentQuantity - quantitySold).coerceAtLeast(0)

                // Update the quantity
                firestore.collection("products").document(productId)
                    .update("quantity", newQuantity)
                    .addOnSuccessListener {
                        Log.d("Inventory", "Product quantity updated for $productId")

                        // If quantity is now 0, mark as sold out or remove from active listings
                        if (newQuantity == 0) {
                            firestore.collection("products").document(productId)
                                .update("status", "sold_out")
                                .addOnSuccessListener {
                                    Log.d("Inventory", "Product marked as sold out: $productId")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("Inventory", "Error updating product quantity", e)
                    }
            }
        }
}

// Create order record function
private fun createOrderRecord(
    userId: String,
    items: List<CartItem>,
    paymentMethod: String,
    totalAmount: Float,
    deliveryAddress: String
) {
    val firestore = FirebaseFirestore.getInstance()

    val orderItems = items.map { item ->
        hashMapOf(
            "productId" to item.productId,
            "name" to item.name,
            "price" to item.price,
            "quantity" to item.quantity
        )
    }

    val orderData = hashMapOf(
        "userId" to userId,
        "items" to orderItems,
        "totalAmount" to totalAmount,
        "paymentMethod" to paymentMethod,
        "status" to "pending",
        "timestamp" to System.currentTimeMillis(),
        "deliveryAddress" to deliveryAddress
    )

    firestore.collection("orders")
        .add(orderData)
        .addOnSuccessListener { documentReference ->
            Log.d("Orders", "Order created with ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Orders", "Error creating order", e)
        }
}

@Composable
private fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
private fun PriceLine(
    title: String,
    value: String,
    isBold: Boolean = false,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            color = if (isBold) MaterialTheme.colorScheme.onSurface else Color.Gray
        )

        Text(
            text = value,
            style = if (isBold) MaterialTheme.typography.titleMedium else MaterialTheme.typography.bodyMedium,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            color = if (isBold) themeColor else Color.Gray
        )
    }
}

@Composable
private fun ItemCard(
    item: CartItem,
    sellerName: String,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F0F0))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Seller: $sellerName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₱${String.format("%.2f", item.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )

                Text(
                    text = "Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    totalPrice: Double,
    cartItems: List<CartItem>,
    userType: String
) {
    var showReceipt by remember { mutableStateOf(false) }
    val sellerNames = remember { mutableStateMapOf<String, String>() }
    val currentUser by cartViewModel.currentUser.collectAsState()

    // Define the theme color - same as in PaymentScreen
    val themeColor = Color(0xFF0DA54B)

    // Match PaymentScreen logic exactly
    LaunchedEffect(cartItems) {
        cartItems.forEach { item ->
            cartViewModel.getProductById(item.productId) { product ->
                product?.ownerId?.let { sellerId ->
                    if (sellerId !in sellerNames) {
                        cartViewModel.getUserById(sellerId) { user ->
                            user?.let {
                                sellerNames[sellerId] = "${it.firstname} ${it.lastname}"
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReceipt) {
        ReceiptScreen(navController, cartViewModel, cartItems, totalPrice, userType, sellerNames)
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Order Confirmation") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.backbtn),
                                contentDescription = "Back",
                                tint = Color.Unspecified
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8F8F8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Order Status Section
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Cash on Delivery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Please confirm your order details below",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    // Shipping Address Section
                    currentUser?.let { user ->
                        SectionTitle(title = "Delivery Address")

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${user.firstname} ${user.lastname}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 12.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        user.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        user.address,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Order Items Section
                    SectionTitle(title = "Order Items (${cartItems.size})")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            cartItems.forEachIndexed { index, item ->
                                val sellerName = sellerNames[item.sellerId] ?: "Loading..."

                                CheckoutItemCard(
                                    item = item,
                                    sellerName = sellerName,
                                    themeColor = themeColor
                                )

                                if (index < cartItems.size - 1) {
                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        color = Color(0xFFEEEEEE)
                                    )
                                }
                            }
                        }
                    }

                    // Payment Summary Section
                    SectionTitle(title = "Order Summary")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            PriceLine(
                                title = "Items Total",
                                value = "₱${"%.2f".format(totalPrice - 50.0)}",
                                themeColor = themeColor
                            )

                            PriceLine(
                                title = "Shipping Fee",
                                value = "₱50.00",
                                themeColor = themeColor
                            )

                            Divider(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                color = Color(0xFFEEEEEE)
                            )

                            PriceLine(
                                title = "Total Amount",
                                value = "₱${"%.2f".format(totalPrice)}",
                                isBold = true,
                                themeColor = themeColor
                            )

                            PriceLine(
                                title = "Payment Method",
                                value = "Cash on Delivery",
                                themeColor = themeColor
                            )
                        }
                    }

                    // Confirmation Button
                    Button(
                        onClick = {
                            cartViewModel.completePurchase("COD", "", "")
                            showReceipt = true
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                        enabled = cartItems.isNotEmpty()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Confirm Order",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun CheckoutItemCard(
    item: CartItem,
    sellerName: String,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F0F0))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Seller: $sellerName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₱${String.format("%.2f", item.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )

                Text(
                    text = "Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReceiptScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    cartItems: List<CartItem>,
    totalPrice: Double,
    userType: String = "defaultUser",
    sellerNames: Map<String, String>
) {
    // Define the theme color to match other screens
    val themeColor = Color(0xFF0DA54B)
    val currentUser by cartViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Order Complete") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigate("market") {
                        popUpTo("market") { inclusive = true }
                    }}) {
                        Icon(
                            painter = painterResource(id = R.drawable.backbtn),
                            contentDescription = "Back to market",
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Success card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Thank You!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your order has been placed successfully",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )

                        // Generate random order number
                        val orderNumber = remember {
                            val random = Random.nextInt(100000, 999999)
                            "ORD-$random"
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Order #: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = orderNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Order items section
                SectionTitle(title = "Order Items (${cartItems.size})")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        cartItems.forEachIndexed { index, item ->
                            val sellerName = sellerNames[item.sellerId] ?: "Loading..."

                            ReceiptItemCard(
                                item = item,
                                sellerName = sellerName,
                                themeColor = themeColor
                            )

                            if (index < cartItems.size - 1) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = Color(0xFFEEEEEE)
                                )
                            }
                        }
                    }
                }

                // Shipping Address Section
                currentUser?.let { user ->
                    SectionTitle(title = "Delivery Address")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${user.firstname} ${user.lastname}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    user.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    user.address,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Payment Summary
                SectionTitle(title = "Payment Summary")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PriceLine(
                            title = "Items Total",
                            value = "₱${"%.2f".format(totalPrice - 50.0)}",
                            themeColor = themeColor
                        )

                        PriceLine(
                            title = "Shipping Fee",
                            value = "₱50.00",
                            themeColor = themeColor
                        )

                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = Color(0xFFEEEEEE)
                        )

                        PriceLine(
                            title = "Total Amount",
                            value = "₱${"%.2f".format(totalPrice)}",
                            isBold = true,
                            themeColor = themeColor
                        )

                        PriceLine(
                            title = "Payment Method",
                            value = "Cash on Delivery",
                            themeColor = themeColor
                        )

                        // Add estimated delivery date
                        val calendar = Calendar.getInstance()
                        calendar.add(Calendar.DAY_OF_MONTH, 3)
                        val deliveryDate = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(calendar.time)

                        Divider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            color = Color(0xFFEEEEEE)
                        )

                        PriceLine(
                            title = "Estimated Delivery",
                            value = deliveryDate,
                            themeColor = themeColor
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = { navController.navigate("orderHistory") },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEEEEEE),
                            contentColor = Color.Black
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("View Orders")
                        }
                    }

                    Button(
                        onClick = {
                            navController.navigate("market") {
                                popUpTo("market") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Continue Shopping",
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun ReceiptItemCard(
    item: CartItem,
    sellerName: String,
    themeColor: Color = MaterialTheme.colorScheme.primary
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Product Image
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(item.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = item.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFFF0F0F0))
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = item.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Seller: $sellerName",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "₱${String.format("%.2f", item.price)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = themeColor
                )

                Text(
                    text = "Qty: ${item.quantity}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    directBuyProductId: String? = null,
    directBuyPrice: String? = null
) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotalState by cartViewModel.totalCartPrice.collectAsState()
    val currentUser by cartViewModel.currentUser.collectAsStateWithLifecycle()
    val sellerNames = remember { mutableStateMapOf<String, String>() }
    val userType = if (directBuyProductId != null) "direct_donation" else "cart_donation"
    var directBuyProduct by remember { mutableStateOf<Product?>(null) }
    var selectedOrganization by remember { mutableStateOf<Organization?>(null) }
    var showDonationReceipt by remember { mutableStateOf(false) }

    // Define the theme color
    val themeColor = Color.Unspecified

    // Sample list of organizations
    val organizations = remember {
        listOf(
            Organization("1", "Red Cross", "Humanitarian organization providing emergency assistance", R.drawable.redcross_icon),
            Organization("2", "Food for the Hungry", "Hungry children urgently need your help. Flip the odds against hunger today.", R.drawable.fh_icon),
            Organization("3", "Central Kitchen Valenzuela", "Organization providing food for needy families", R.drawable.val_logo),
            Organization("4", "Angat Kabataan", "Lead the Change, Be the Change", R.drawable.angat_kabataan)
        )
    }

    // Fetch direct buy product
    LaunchedEffect(directBuyProductId) {
        directBuyProductId?.let { id ->
            cartViewModel.getProductById(id) { product ->
                directBuyProduct = product
            }
        }
    }

    // Prepare items to display
    val displayItems: List<CartItem> = remember(directBuyProduct, cartItems) {
        if (directBuyProduct != null) {
            listOf(
                CartItem(
                    productId = directBuyProduct!!.prodId,
                    name = directBuyProduct!!.name,
                    price = directBuyProduct!!.price,
                    quantity = 1,
                    imageUrl = directBuyProduct!!.imageUrl,
                    isDirectBuy = true
                )
            )
        } else {
            cartItems
        }
    }

    // Fetch seller info for each item and map it by productId
    LaunchedEffect(displayItems) {
        displayItems.forEach { item ->
            cartViewModel.getProductById(item.productId) { product ->
                product?.let { prod ->
                    cartViewModel.getUserById(prod.ownerId) { user ->
                        user?.let {
                            sellerNames[prod.prodId] = "${it.firstname} ${it.lastname}"
                        }
                    }
                }
            }
        }
    }

    if (showDonationReceipt) {
        DonationReceiptScreen(
            navController = navController,
            cartViewModel = cartViewModel,
            cartItems = displayItems,
            organization = selectedOrganization!!,
            sellerNames = sellerNames
        )
    } else {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = { Text("Donation") },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                painter = painterResource(id = R.drawable.backbtn),
                                contentDescription = "Back",
                                tint = Color.Unspecified
                            )
                        }
                    },
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color(0xFFF8F8F8))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Section: Donation Introduction
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(40.dp)
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                "Make a Difference",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                "Your donation can help someone in need",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                        }
                    }

                    // Section: Items to donate
                    SectionTitle(title = "Items to Donate (${displayItems.size})")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 16.dp)
                        ) {
                            displayItems.forEachIndexed { index, item ->
                                ItemCard(
                                    item = item,
                                    sellerName = sellerNames[item.productId] ?: "Loading seller...",
                                    themeColor = themeColor
                                )

                                if (index < displayItems.size - 1) {
                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        color = Color(0xFFEEEEEE)
                                    )
                                }
                            }
                        }
                    }

                    // Section: Select organization
                    SectionTitle(title = "Select an Organization")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            organizations.forEach { org ->
                                OrganizationItem(
                                    organization = org,
                                    isSelected = selectedOrganization?.id == org.id,
                                    themeColor = themeColor,
                                    onSelect = { selectedOrganization = org }
                                )
                            }
                        }
                    }

                    // Section: Shipping Info
                    currentUser?.let { user ->
                        SectionTitle(title = "Your Information")

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(20.dp)) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        "${user.firstname} ${user.lastname}",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Home,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        user.address,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = themeColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        user.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    // Donation method (GCash only)
                    SectionTitle(title = "Donation Method")

                    Button(
                        onClick = {
                            if (selectedOrganization != null && displayItems.isNotEmpty()) {
                                val firstItem = displayItems[0]
                                cartViewModel.getProductById(firstItem.productId) { product ->
                                    product?.let {
                                        val sellerId = it.ownerId
                                        val priceToPay = directBuyPrice ?: cartTotalState
                                        navController.navigate("gcashScreen/$priceToPay/$sellerId")
                                    }
                                }
                            }
                        },

                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF)),
                        enabled = selectedOrganization != null
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CreditCard,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Proceed with GCash",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White
                            )
                        }
                    }

// For testing purposes, direct complete button
                    OutlinedButton(
                        onClick = {
                            if (selectedOrganization != null) {
                                showDonationReceipt = true

                                // Create donation records and notifications when using the test button
                                val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@OutlinedButton
                                val firestore = FirebaseFirestore.getInstance()

                                selectedOrganization?.let { organization ->
                                    displayItems.forEach { cartItem ->
                                        cartViewModel.getProductById(cartItem.productId) { product ->
                                            product?.let { prod ->
                                                // Now use organization instead of selectedOrganization
                                                createDonationNotification(
                                                    firestore = firestore,
                                                    product = prod,
                                                    donatorId = userId,
                                                    organizationName = organization.name,
                                                    quantity = cartItem.quantity,
                                                    message = "Your product was donated to ${organization.name}. " +
                                                            "Thank you for supporting sustainable agriculture and helping those in need!"
                                                )

                                                // Update product inventory
                                                updateProductInventory(
                                                    prod.prodId,
                                                    cartItem.quantity
                                                )

                                                // Create donation record
                                                createDonationRecord(
                                                    userId = userId,
                                                    productId = prod.prodId,
                                                    productName = prod.name,
                                                    organizationId = organization.id,
                                                    organizationName = organization.name,
                                                    quantity = cartItem.quantity
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        enabled = selectedOrganization != null
                    ) {
                        Text("Complete Donation (For Testing)")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun OrganizationItem(
    organization: Organization,
    isSelected: Boolean,
    themeColor: Color,
    onSelect: () -> Unit
) {
    val borderModifier = if (isSelected) {
        Modifier.border(2.dp, themeColor, RoundedCornerShape(8.dp))
    } else {
        Modifier
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .then(borderModifier)
            .clickable { onSelect() },
        shape = RoundedCornerShape(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Organization icon/image
            Image(
                painter = painterResource(id = organization.iconResId),
                contentDescription = organization.name,
                modifier = Modifier
                    .size(40.dp)
            )


            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = organization.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = organization.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Selected",
                    tint = themeColor
                )
            }
        }
    }
}

// Create donation record function
private fun createDonationRecord(
    userId: String,
    productId: String,
    productName: String,
    organizationId: String,
    organizationName: String,
    quantity: Int
) {
    val firestore = FirebaseFirestore.getInstance()

    val donationData = hashMapOf(
        "userId" to userId,
        "productId" to productId,
        "productName" to productName,
        "organizationId" to organizationId,
        "organizationName" to organizationName,
        "quantity" to quantity,
        "timestamp" to System.currentTimeMillis(),
        "status" to "completed"
    )

    firestore.collection("donations")
        .add(donationData)
        .addOnSuccessListener { documentReference ->
            Log.d("Donations", "Donation record created with ID: ${documentReference.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Donations", "Error creating donation record", e)
        }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationReceiptScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    cartItems: List<CartItem>,
    organization: Organization,
    sellerNames: Map<String, String>
) {
    // Define the theme color to match other screens
    val themeColor = Color(0xFF0DA54B)
    val currentUser by cartViewModel.currentUser.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Donation Complete") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("market") {
                            popUpTo("market") { inclusive = true }
                        }
                    }) {
                        Icon(
                            painter = painterResource(id = R.drawable.backbtn),
                            contentDescription = "Back to market",
                            tint = Color.Unspecified
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFFF8F8F8))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Success card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            tint = themeColor,
                            modifier = Modifier.size(72.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Thank You for Your Donation!",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = "Your items have been donated to ${organization.name}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        // Generate random donation number
                        val donationNumber = remember {
                            val random = Random.nextInt(100000, 999999)
                            "DON-$random"
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFFF5F5F5), RoundedCornerShape(8.dp))
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "Donation #: ",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )
                            Text(
                                text = donationNumber,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                        }
                    }
                }

                // Organization section
                SectionTitle(title = "Organization Details")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                organization.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            organization.description,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Donated items section
                SectionTitle(title = "Donated Items (${cartItems.size})")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        cartItems.forEachIndexed { index, item ->
                            val sellerName = sellerNames[item.productId] ?: "Loading..."

                            ReceiptItemCard(
                                item = item,
                                sellerName = sellerName,
                                themeColor = themeColor
                            )

                            if (index < cartItems.size - 1) {
                                Divider(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    color = Color(0xFFEEEEEE)
                                )
                            }
                        }
                    }
                }

                // Donor Information
                currentUser?.let { user ->
                    SectionTitle(title = "Donor Information")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(bottom = 12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "${user.firstname} ${user.lastname}",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }

                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Phone,
                                    contentDescription = null,
                                    tint = themeColor,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    user.phoneNumber,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }

                // Payment info
                SectionTitle(title = "Payment Information")

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        PriceLine(
                            title = "Payment Method",
                            value = "GCash",
                            themeColor = themeColor
                        )

                        PriceLine(
                            title = "Donation Date",
                            value = SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault()).format(Calendar.getInstance().time),
                            themeColor = themeColor
                        )
                    }
                }

                // Action buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Button(
                        onClick = {
                            navController.navigate("market") {
                                popUpTo("market") { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Return to Home",
                                color = Color.White
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GcashScreen(
    navController: NavController,
    totalPrice: String,
    firestore: FirebaseFirestore,
    ownerId: String,
    cartViewModel: CartViewModel = viewModel()
) {
    // State to hold the owner's name, QR code, and payment status
    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var paymentStatus by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var showReceiptDialog by remember { mutableStateOf(false) }
    var referenceId by remember { mutableStateOf("") }
    var paymentTimestamp by remember { mutableStateOf("") }

    // GCash brand color
    val gcashGreen = Color(0xFF0DA54B)

    // Generate a unique transaction ID
    val transactionId = remember { UUID.randomUUID().toString() }

    // Use mutableStateMapOf to store seller names by their IDs
    val sellerNames = remember { mutableStateMapOf<String, String>() }

    // GCash API configuration - using mock values
    val context = LocalContext.current
    val gcashApiConfig = remember {
        GCashApiConfig(
            clientId = "mock_client_id",
            clientSecret = "mock_client_secret",
            merchantId = "mock_merchant_id",
            redirectUrl = "com.project.webapp://payment_callback"
        )
    }

    // Fetch the owner's name and generate QR code when the screen is first composed
    LaunchedEffect(ownerId, totalPrice) {
        // Use the existing method from CartViewModel to get user info
        cartViewModel.getUserById(ownerId) { user ->
            user?.let {
                sellerNames[ownerId] = "${it.firstname} ${it.lastname}"

                // Generate dynamic QR code with payment details
                val content = "GCash Payment\nName: ${sellerNames[ownerId]}\nAmount: ₱$totalPrice\nTxnID: $transactionId"
                qrCodeBitmap = generateQRCode(content)
            } ?: run {
                sellerNames[ownerId] = "Mock Seller"

                // Generate QR code even if seller name isn't found
                val content = "GCash Payment\nName: Mock Seller\nAmount: ₱$totalPrice\nTxnID: $transactionId"
                qrCodeBitmap = generateQRCode(content)
            }
        }
    }

    // Function to process a successful payment
    fun processSuccessfulPayment(transactionId: String, referenceId: String) {
        // Update payment status in Firestore
        val paymentData = hashMapOf(
            "transactionId" to transactionId,
            "referenceId" to referenceId,
            "status" to "COMPLETED",
            "amount" to totalPrice,
            "seller" to ownerId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        firestore.collection("payments")
            .document(transactionId)
            .set(paymentData)
            .addOnSuccessListener {
                Log.d("GCashScreen", "Payment record created successfully")
                paymentStatus = "Payment successful!"

                // Set timestamp for receipt
                val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
                paymentTimestamp = dateFormat.format(Date())

                // Show receipt dialog
                showReceiptDialog = true

                // Original navigation is now handled via receipt dialog buttons
            }
            .addOnFailureListener { e ->
                Log.e("GCashScreen", "Error updating payment: ${e.message}")
                paymentStatus = "Error saving payment record"
            }
    }

    // Function to initiate GCash payment and proceed directly
    fun initiateGCashPayment() {
        val currentContext = context
        isLoading = true
        paymentStatus = "Processing..."

        val amountInCents = (totalPrice.toDoubleOrNull() ?: 0.0) * 100
        val paymentRequest = GCashPaymentRequest(
            amount = amountInCents.toInt(),
            currency = "PHP",
            description = "Payment to ${sellerNames[ownerId] ?: "Merchant"}",
            transactionId = transactionId,
            merchantId = gcashApiConfig.merchantId
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Generate reference ID immediately for mock flow
                referenceId = "REF_${System.currentTimeMillis()}"

                // Simulate network delay
                delay(1000)

                withContext(Dispatchers.Main) {
                    isLoading = false

                    // Show toast for GCash simulation
                    Toast.makeText(
                        currentContext,
                        "Mock: Processing GCash payment of ₱$totalPrice",
                        Toast.LENGTH_LONG
                    ).show()

                    // Simulate successful payment directly
                    processSuccessfulPayment(transactionId, referenceId)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    paymentStatus = "Error: ${e.localizedMessage}"
                }
            }
        }
    }

    // UI for GcashScreen
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("GCash Payment") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // GCash Header
                Image(
                    painter = painterResource(id = R.drawable.gcash_icon),
                    contentDescription = "GCash Logo",
                    modifier = Modifier
                        .height(60.dp)
                        .padding(bottom = 16.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Payment Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Merchant:", fontWeight = FontWeight.Bold)
                            Text(sellerNames[ownerId] ?: "Loading...")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Amount:", fontWeight = FontWeight.Bold)
                            Text("₱$totalPrice")
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Transaction ID:", fontWeight = FontWeight.Bold)
                            Text(transactionId.take(8) + "...")
                        }
                    }
                }

                // QR Code Section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Scan this QR Code",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        if (qrCodeBitmap != null) {
                            Image(
                                bitmap = qrCodeBitmap!!.asImageBitmap(),
                                contentDescription = "Payment QR Code",
                                modifier = Modifier
                                    .size(200.dp)
                                    .border(1.dp, Color.LightGray)
                                    .padding(8.dp)
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .size(50.dp)
                                    .padding(8.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = "Scan this QR code with your GCash app",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Payment status text if any
                if (paymentStatus.isNotEmpty()) {
                    Text(
                        text = paymentStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (paymentStatus.contains("successful")) gcashGreen else
                            if (paymentStatus.contains("Error")) Color.Red else Color.Blue,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                // Payment buttons
                Button(
                    onClick = { initiateGCashPayment() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gcashGreen,
                        contentColor = Color.White
                    )
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = Color.White
                        )
                    } else {
                        Text("Pay with GCash")
                    }
                }

                TextButton(
                    onClick = { navController.navigateUp() },
                    modifier = Modifier.padding(top = 8.dp),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = gcashGreen
                    )
                ) {
                    Text("Cancel Payment")
                }
            }
        }
    }

    // Payment verification dialog
    if (showPaymentDialog) {
        AlertDialog(
            onDismissRequest = {
                try {
                    showPaymentDialog = false
                    paymentStatus = "Payment cancelled"
                } catch (e: Exception) {
                    Log.e("GCashScreen", "Dialog dismiss error: ${e.message}")
                }
            },
            title = { Text("Payment Verification (MOCK)") },
            text = {
                Column {
                    Text("This is a mock payment flow. In a real app, you would complete the payment in the GCash app and return here.")

                    if (isLoading) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp)
                        )
                    }

                    if (paymentStatus.isNotEmpty() && !isLoading) {
                        Text(
                            text = paymentStatus,
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (paymentStatus.contains("successful")) gcashGreen else Color.Red
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            showPaymentDialog = false
                            processSuccessfulPayment(transactionId, referenceId)
                        } catch (e: Exception) {
                            Log.e("GCashScreen", "Payment processing error: ${e.message}")
                            paymentStatus = "Error processing payment"
                        }
                    },
                    enabled = !isLoading,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = gcashGreen
                    )
                ) {
                    Text("Simulate Completed Payment")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        try {
                            showPaymentDialog = false
                            paymentStatus = "Payment cancelled"
                        } catch (e: Exception) {
                            Log.e("GCashScreen", "Dialog cancel error: ${e.message}")
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = gcashGreen
                    )
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    // Receipt Dialog
    if (showReceiptDialog) {
        Dialog(
            onDismissRequest = { /* Prevent dismissal on outside click */ }
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Receipt Header
                    Image(
                        painter = painterResource(id = R.drawable.gcash_icon),
                        contentDescription = "GCash Logo",
                        modifier = Modifier
                            .height(40.dp)
                            .padding(bottom = 8.dp)
                    )

                    Text(
                        text = "Payment Receipt",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = gcashGreen
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Payment Status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success Icon",
                            tint = gcashGreen,
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Payment Successful",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = gcashGreen
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        color = Color.LightGray,
                        thickness = 1.dp
                    )

                    // Payment Details
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        // Date and Time
                        ReceiptRow(
                            label = "Date & Time:",
                            value = paymentTimestamp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Merchant
                        ReceiptRow(
                            label = "Merchant:",
                            value = sellerNames[ownerId] ?: "Merchant"
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Amount
                        ReceiptRow(
                            label = "Amount:",
                            value = "₱$totalPrice",
                            valueStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Transaction ID
                        ReceiptRow(
                            label = "Transaction ID:",
                            value = transactionId
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        // Reference ID
                        ReceiptRow(
                            label = "Reference No:",
                            value = referenceId
                        )
                    }

                    Divider(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        color = Color.LightGray,
                        thickness = 1.dp
                    )

                    // Actions
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // Back to Home Button
                        OutlinedButton(
                            onClick = {
                                navController.navigate("farmerdashboard") {
                                    popUpTo("farmerdashboard") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            border = BorderStroke(1.dp, gcashGreen),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = gcashGreen
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Home")
                        }

                        Spacer(modifier = Modifier.width(16.dp))

                        // Go to Success Screen Button
                        Button(
                            onClick = {
                                navController.navigate("payment_success/$transactionId") {
                                    popUpTo("payment_success") { inclusive = true }
                                    launchSingleTop = true
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = gcashGreen
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Continue")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptRow(
    label: String,
    value: String,
    valueStyle: TextStyle = MaterialTheme.typography.bodyMedium
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
        Text(
            text = value,
            style = valueStyle
        )
    }
}





