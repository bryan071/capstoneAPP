package com.project.webapp.components.payment

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.runtime.getValue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.project.webapp.Viewmodel.createOrderRecord
import com.project.webapp.components.createSaleNotification
import com.project.webapp.components.fetchOwnerName
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Product
import java.net.URLEncoder
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

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
    var isLoading by remember { mutableStateOf(false) }
    var paymentStatus by remember { mutableStateOf("") }
    var paymentTimestamp by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }

    val themeColor = Color(0xFF0DA54B)
    val firestore = FirebaseFirestore.getInstance()

    LaunchedEffect(directBuyProductId) {
        directBuyProductId?.let { id ->
            cartViewModel.getProductById(id) { product ->
                directBuyProduct = product
            }
        }
    }

    val displayItems: List<CartItem> = remember(directBuyProduct, cartItems) {
        if (directBuyProduct != null) {
            listOf(
                CartItem(
                    productId = directBuyProduct!!.prodId,
                    name = directBuyProduct!!.name,
                    price = directBuyProduct!!.price,
                    quantity = 1,
                    imageUrl = directBuyProduct!!.imageUrl,
                    isDirectBuy = true,
                    sellerId = directBuyProduct!!.ownerId,
                    unit = directBuyProduct!!.quantityUnit,
                    weight = directBuyProduct!!.quantity
                )
            )
        } else {
            cartItems
        }
    }

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

    LaunchedEffect(directBuyProduct) {
        if (directBuyProduct != null && directBuyProductId != null) {
            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
            val cartRef = firestore.collection("carts").document(userId).collection("items")
                .document(directBuyProductId)

            val cartItem = hashMapOf(
                "productId" to directBuyProduct!!.prodId,
                "name" to directBuyProduct!!.name,
                "price" to directBuyProduct!!.price,
                "quantity" to 1,
                "imageUrl" to directBuyProduct!!.imageUrl,
                "sellerId" to directBuyProduct!!.ownerId,
                "isDirectBuy" to true,
                "unit" to directBuyProduct!!.quantityUnit,
                "weight" to directBuyProduct!!.quantity
            )

            cartRef.set(cartItem).addOnSuccessListener {
                Log.d("PaymentScreen", "Direct buy item added to Firestore cart: ${directBuyProduct!!.name}")
            }.addOnFailureListener { e ->
                Log.e("PaymentScreen", "Failed to add direct buy item to cart: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Checkout") },
                navigationIcon = {
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
                SectionTitle(title = "Order Summary")

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

                SectionTitle(title = "Payment Methods")

                var ownerId by remember { mutableStateOf<String?>(null) }

                LaunchedEffect(directBuyProduct, displayItems) {
                    if (directBuyProduct != null) {
                        ownerId = directBuyProduct?.ownerId
                    } else if (displayItems.isNotEmpty()) {
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
                        fetchOwnerName(firestore, it) { name ->
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
                            cartViewModel.setCheckoutItems(displayItems)
                            navController.navigate("gcashScreen/$formattedAmount/$id")
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF)),
                    enabled = displayItems.isNotEmpty() && !isLoading
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
                        isLoading = true
                        val transactionId = UUID.randomUUID().toString()
                        processCodPayment(
                            firestore = firestore,
                            cartViewModel = cartViewModel,
                            navController = navController,
                            transactionId = transactionId,
                            displayItems = displayItems,
                            totalPrice = amount.toFloat(),
                            ownerId = ownerId ?: "",
                            userType = userType,
                            onPaymentStatus = { status ->
                                paymentStatus = status
                                isLoading = false
                            },
                            onError = { error ->
                                errorMessage = error
                                showErrorDialog = true
                                isLoading = false
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    enabled = displayItems.isNotEmpty() && !isLoading
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(color = Color.White)
                    } else {
                        Text(
                            text = "Pay with Cash on Delivery",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (showErrorDialog) {
                    AlertDialog(
                        onDismissRequest = { showErrorDialog = false },
                        title = { Text("Payment Error") },
                        text = { Text(errorMessage) },
                        confirmButton = {
                            Button(onClick = { showErrorDialog = false }) {
                                Text("OK")
                            }
                        }
                    )
                }
            }
        }
    }
}

fun processCodPayment(
    firestore: FirebaseFirestore,
    cartViewModel: CartViewModel,
    navController: NavController,
    transactionId: String,
    displayItems: List<CartItem>,
    totalPrice: Float,
    ownerId: String,
    userType: String,
    onPaymentStatus: (String) -> Unit,
    onError: (String) -> Unit
) {
    if (displayItems.isEmpty()) {
        Log.e("PaymentDebug", "Cannot process COD payment with empty items!")
        onPaymentStatus("Error: No items to process")
        onError("Unable to process payment: No items found.")
        return
    }
    val orderNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()
    val transactionId = "TXN-$orderNumber"
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
    val userAddress = cartViewModel.currentUser.value?.address ?: "No address provided"
    val paymentData = hashMapOf(
        "transactionId" to transactionId,
        "userId" to userId,
        "status" to "PENDING",
        "amount" to totalPrice,
        "seller" to ownerId,
        "timestamp" to FieldValue.serverTimestamp(),
        "paymentMethod" to "COD",
        "deliveryAddress" to userAddress
    )

    Log.d("PaymentDebug", "Processing COD payment with ${displayItems.size} items")
    displayItems.forEach { item ->
        Log.d("PaymentDebug", "Item: ${item.name}, ID: ${item.productId}, Quantity: ${item.quantity}, Price: ${item.price}")
    }

    firestore.collection("payments")
        .document(transactionId)
        .set(paymentData)
        .addOnSuccessListener {
            Log.d("PaymentScreen", "COD payment record created successfully")
            onPaymentStatus("Order placed successfully!")

            val transactionData = hashMapOf(
                "transactionId" to transactionId,
                "buyerId" to userId,
                "sellerId" to ownerId,
                "amount" to totalPrice,
                "paymentMethod" to "COD",
                "status" to "Completed",
                "timestamp" to FieldValue.serverTimestamp(),
                "items" to displayItems.map { item ->
                    mapOf(
                        "productId" to item.productId,
                        "ownerId" to item.sellerId,
                        "name" to item.name,
                        "price" to item.price,
                        "quantity" to item.quantity,
                        "unit" to item.unit,
                        "weight" to item.weight
                    )
                }
            )

            firestore.collection("transactions")
                .document(transactionId)
                .set(transactionData)
                .addOnSuccessListener {
                    Log.d("Transaction", "Transaction record added successfully.")
                }
                .addOnFailureListener { e ->
                    Log.e("Transaction", "Failed to save transaction record: ${e.message}")
                }

            val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
            val paymentTimestamp = dateFormat.format(Date())

            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { userDocument ->
                    val orderItems = displayItems.toList()

                    Log.d("PaymentDebug", "Creating order with ${orderItems.size} items")

                    displayItems.forEach { cartItem ->
                        cartViewModel.getProductById(cartItem.productId) { product ->
                            product?.let { prod ->
                                createSaleNotification(
                                    firestore = firestore,
                                    product = prod,
                                    buyerId = userId,
                                    paymentMethod = "COD",
                                    deliveryAddress = userAddress

                                )
                            }
                        }
                    }

                    createOrderRecord(userId, orderItems, "COD", totalPrice, userAddress)
                    cartViewModel.completePurchase(userType, "COD")

                    val activity = hashMapOf(
                        "userId" to userId,
                        "description" to "Placed an order worth ₱${totalPrice.toInt()} via Cash on Delivery.",
                        "timestamp" to System.currentTimeMillis()
                    )

                    firestore.collection("activities")
                        .add(activity)
                        .addOnSuccessListener {
                            Log.d("RecentActivity", "Activity logged successfully.")
                        }
                        .addOnFailureListener { e ->
                            Log.e("RecentActivity", "Failed to log activity: ${e.message}")
                        }

                    val itemsJson = Gson().toJson(orderItems)
                    val encodedItems = URLEncoder.encode(itemsJson, "UTF-8")
                    navController.navigate(
                        "checkoutScreen/$userType/$totalPrice?paymentMethod=COD&referenceId=&items=$encodedItems"
                    )
                }
                .addOnFailureListener { e ->
                    Log.e("PaymentScreen", "Error fetching user data", e)
                    val orderItems = displayItems.toList()
                    createOrderRecord(userId, orderItems, "COD", totalPrice, "No address provided")
                    cartViewModel.completePurchase(userType, "COD")

                    val itemsJson = Gson().toJson(orderItems)
                    val encodedItems = URLEncoder.encode(itemsJson, "UTF-8")
                    navController.navigate(
                        "checkoutScreen/$userType/$totalPrice?paymentMethod=COD&referenceId=&items=$encodedItems"
                    )
                }
        }
        .addOnFailureListener { e ->
            Log.e("PaymentScreen", "Error saving COD payment: ${e.message}")
            onPaymentStatus("Error saving order")
            onError("Order processing error: ${e.message}")
        }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onBackground
    )
}

@Composable
fun PriceLine(
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
fun ItemCard(
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









