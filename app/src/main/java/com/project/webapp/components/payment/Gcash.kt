    package com.project.webapp.components.payment

    import android.graphics.Bitmap
    import android.util.Log
    import android.widget.Toast
    import androidx.compose.foundation.BorderStroke
    import androidx.compose.foundation.Image
    import androidx.compose.foundation.border
    import androidx.compose.foundation.layout.Arrangement
    import androidx.compose.foundation.layout.Box
    import androidx.compose.foundation.layout.Column
    import androidx.compose.foundation.layout.Row
    import androidx.compose.foundation.layout.Spacer
    import androidx.compose.foundation.layout.fillMaxSize
    import androidx.compose.foundation.layout.fillMaxWidth
    import androidx.compose.foundation.layout.height
    import androidx.compose.foundation.layout.padding
    import androidx.compose.foundation.layout.size
    import androidx.compose.foundation.layout.width
    import androidx.compose.material.icons.Icons
    import androidx.compose.material.icons.filled.ArrowBack
    import androidx.compose.material.icons.filled.CheckCircle
    import androidx.compose.material3.AlertDialog
    import androidx.compose.material3.Button
    import androidx.compose.material3.ButtonDefaults
    import androidx.compose.material3.Card
    import androidx.compose.material3.CardDefaults
    import androidx.compose.material3.CircularProgressIndicator
    import androidx.compose.material3.Divider
    import androidx.compose.material3.ExperimentalMaterial3Api
    import androidx.compose.material3.Icon
    import androidx.compose.material3.IconButton
    import androidx.compose.material3.LinearProgressIndicator
    import androidx.compose.material3.MaterialTheme
    import androidx.compose.material3.OutlinedButton
    import androidx.compose.material3.Scaffold
    import androidx.compose.material3.Text
    import androidx.compose.material3.TextButton
    import androidx.compose.material3.TopAppBar
    import androidx.compose.runtime.Composable
    import androidx.compose.runtime.LaunchedEffect
    import androidx.compose.runtime.getValue
    import androidx.compose.runtime.mutableStateMapOf
    import androidx.compose.runtime.mutableStateOf
    import androidx.compose.runtime.remember
    import androidx.compose.runtime.setValue
    import androidx.compose.ui.Alignment
    import androidx.compose.ui.Modifier
    import androidx.compose.ui.graphics.Color
    import androidx.compose.ui.graphics.asImageBitmap
    import androidx.compose.ui.platform.LocalContext
    import androidx.compose.ui.res.painterResource
    import androidx.compose.ui.text.TextStyle
    import androidx.compose.ui.text.font.FontWeight
    import androidx.compose.ui.text.style.TextAlign
    import androidx.compose.ui.unit.dp
    import androidx.compose.ui.window.Dialog
    import androidx.lifecycle.compose.collectAsStateWithLifecycle
    import androidx.lifecycle.viewmodel.compose.viewModel
    import androidx.navigation.NavController
    import com.google.firebase.auth.FirebaseAuth
    import com.google.firebase.firestore.FieldValue
    import com.google.firebase.firestore.FirebaseFirestore
    import com.google.gson.Gson
    import com.project.webapp.R
    import com.project.webapp.Viewmodel.createOrderRecord
    import com.project.webapp.components.createSaleNotification
    import com.project.webapp.components.generateQRCode
    import com.project.webapp.datas.CartItem
    import com.project.webapp.datas.GCashApiConfig
    import com.project.webapp.datas.GCashPaymentRequest
    import kotlinx.coroutines.CoroutineScope
    import kotlinx.coroutines.Dispatchers
    import kotlinx.coroutines.delay
    import kotlinx.coroutines.launch
    import kotlinx.coroutines.withContext
    import java.net.URLEncoder
    import java.text.SimpleDateFormat
    import java.util.Date
    import java.util.Locale
    import java.util.UUID


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun GcashScreen(
        navController: NavController,
        totalPrice: Double,
        ownerId: String,
        cartViewModel: CartViewModel = viewModel()
    ) {
        val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
        val isCartLoading by cartViewModel.isCartLoading.collectAsStateWithLifecycle()
        val cartLoadError by cartViewModel.cartLoadError.collectAsStateWithLifecycle()
        val firestore = FirebaseFirestore.getInstance()

        var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
        var paymentStatus by remember { mutableStateOf("") }
        var isLoading by remember { mutableStateOf(false) }
        var showPaymentDialog by remember { mutableStateOf(false) }
        var showReceiptDialog by remember { mutableStateOf(false) }
        var referenceId by remember { mutableStateOf("") }
        var paymentTimestamp by remember { mutableStateOf("") }
        var showErrorDialog by remember { mutableStateOf(false) }
        var errorMessage by remember { mutableStateOf("") }

        val gcashGreen = Color(0xFF0DA54B)
        val transactionId = remember { UUID.randomUUID().toString() }
        val sellerNames = remember { mutableStateMapOf<String, String>() }
        val context = LocalContext.current
        val gcashApiConfig = remember {
            GCashApiConfig(
                clientId = "mock_client_id",
                clientSecret = "mock_client_secret",
                merchantId = "mock_merchant_id",
                redirectUrl = "com.project.webapp://payment_callback"
            )
        }

        LaunchedEffect(cartItems) {
            Log.d("GCashDebug", "Cart items updated in GcashScreen: ${cartItems.size} items")
            cartItems.forEach { item ->
                Log.d("GCashDebug", "Cart item: ${item.name}, ID: ${item.productId}, Quantity: ${item.quantity}, Price: ${item.price}, isDirectBuy: ${item.isDirectBuy}")
            }
        }

        LaunchedEffect(ownerId, totalPrice) {
            cartViewModel.getUserById(ownerId) { user ->
                user?.let {
                    sellerNames[ownerId] = "${it.firstname} ${it.lastname}"
                    val content = "GCash Payment\nName: ${sellerNames[ownerId]}\nAmount: ₱$totalPrice\nTxnID: $transactionId"
                    qrCodeBitmap = generateQRCode(content)
                } ?: run {
                    sellerNames[ownerId] = "Mock Seller"
                    val content = "GCash Payment\nName: Mock Seller\nAmount: ₱$totalPrice\nTxnID: $transactionId"
                    qrCodeBitmap = generateQRCode(content)
                }
            }
        }

        fun processSuccessfulPayment(
            transactionId: String,
            referenceId: String,
            displayItems: List<CartItem>
        ) {
            if (displayItems.isEmpty()) {
                Log.e("GCashDebug", "Cannot process payment with empty items!")
                paymentStatus = "Error: No items to process"
                isLoading = false
                errorMessage = "Unable to process payment: No items found."
                showErrorDialog = true
                return
            }

            val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return
            val userType = if (displayItems.any { it.isDirectBuy }) "direct_buying" else "cart_checkout"
            val paymentData = hashMapOf(
                "transactionId" to transactionId,
                "referenceId" to referenceId,
                "status" to "COMPLETED",
                "amount" to totalPrice,
                "seller" to ownerId,
                "timestamp" to FieldValue.serverTimestamp(),
                "paymentMethod" to "GCash"
            )

            Log.d("GCashDebug", "Processing payment with ${displayItems.size} items")
            displayItems.forEach { item ->
                Log.d("GCashDebug", "Item: ${item.name}, ID: ${item.productId}, Quantity: ${item.quantity}, Price: ${item.price}")
            }

            firestore.collection("payments")
                .document(transactionId)
                .set(paymentData)
                .addOnSuccessListener {
                    Log.d("GCashScreen", "Payment record created successfully")
                    paymentStatus = "Payment successful!"
                    val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
                    paymentTimestamp = dateFormat.format(Date())

                    firestore.collection("users").document(userId).get()
                        .addOnSuccessListener { userDocument ->
                            val userAddress = userDocument.getString("address") ?: "No address provided"
                            val orderItems = displayItems.toList()

                            Log.d("GCashDebug", "Creating order with ${orderItems.size} items")

                            displayItems.forEach { cartItem ->
                                cartViewModel.getProductById(cartItem.productId) { product ->
                                    product?.let { prod ->
                                        createSaleNotification(
                                            firestore = firestore,
                                            product = prod,
                                            buyerId = userId,
                                            quantity = cartItem.quantity,
                                            paymentMethod = "GCash",
                                            deliveryAddress = userAddress,
                                            message = "Your product was sold! Payment method: GCash (Ref: $referenceId)"
                                        )
                                    }
                                }
                            }

                            createOrderRecord(userId, orderItems, "GCash", totalPrice.toFloat(), userAddress)
                            cartViewModel.completePurchase(userType, "GCash", referenceId)

                            // Pass orderItems as a JSON string
                            val itemsJson = Gson().toJson(orderItems)
                            val encodedItems = URLEncoder.encode(itemsJson, "UTF-8")
                            navController.navigate(
                                "checkoutScreen/$userType/${totalPrice.toFloat()}?paymentMethod=GCash&referenceId=$referenceId&items=$encodedItems"
                            )
                        }
                        .addOnFailureListener { e ->
                            Log.e("GCashScreen", "Error fetching user data", e)
                            val orderItems = displayItems.toList()
                            createOrderRecord(userId, orderItems, "GCash", totalPrice.toFloat(), "No address provided")
                            cartViewModel.completePurchase(userType, "GCash", referenceId)

                            // Pass orderItems as a JSON string
                            val itemsJson = Gson().toJson(orderItems)
                            val encodedItems = URLEncoder.encode(itemsJson, "UTF-8")
                            navController.navigate(
                                "checkoutScreen/$userType/${totalPrice.toFloat()}?paymentMethod=GCash&referenceId=$referenceId&items=$encodedItems"
                            )
                        }
                }
                .addOnFailureListener { e ->
                    Log.e("GCashScreen", "Error saving payment: ${e.message}")
                    paymentStatus = "Error saving payment record"
                    errorMessage = "Payment processing error: ${e.message}"
                    showErrorDialog = true
                }
        }

        fun initiateGCashPayment() {
            val itemsToProcess = cartItems.filter { it.sellerId == ownerId }

            if (itemsToProcess.isEmpty()) {
                Log.e("GCashDebug", "Cannot initiate payment with empty items!")
                paymentStatus = "Error: No items to process"
                Toast.makeText(context, "Cannot process payment: No items found", Toast.LENGTH_SHORT).show()
                return
            }

            Log.d("GCashDebug", "Initiating payment with ${itemsToProcess.size} items")

            val currentContext = context
            isLoading = true
            paymentStatus = "Processing..."

            val amountInCents = totalPrice * 100
            val paymentRequest = GCashPaymentRequest(
                amount = amountInCents.toInt(),
                currency = "PHP",
                description = "Payment to ${sellerNames[ownerId] ?: "Merchant"}",
                transactionId = transactionId,
                merchantId = gcashApiConfig.merchantId
            )

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    referenceId = "REF_${System.currentTimeMillis()}"
                    delay(1000)

                    withContext(Dispatchers.Main) {
                        isLoading = false
                        Toast.makeText(
                            currentContext,
                            "Mock: Processing GCash payment of ₱$totalPrice",
                            Toast.LENGTH_LONG
                        ).show()

                        val itemsForProcessing = itemsToProcess.toList()
                        Log.d("GCashDebug", "About to process payment with ${itemsForProcessing.size} items")
                        processSuccessfulPayment(transactionId, referenceId, itemsForProcessing)
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        paymentStatus = "Error: ${e.localizedMessage}"
                    }
                }
            }
        }

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

                    if (paymentStatus.isNotEmpty()) {
                        Text(
                            text = paymentStatus,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (paymentStatus.contains("successful")) gcashGreen else
                                if (paymentStatus.contains("Error")) Color.Red else Color.Blue,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }

                    Button(
                        onClick = { initiateGCashPayment() },
                        enabled = !isLoading && cartItems.any { it.sellerId == ownerId },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
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

        if (showPaymentDialog) {
            AlertDialog(
                onDismissRequest = {
                    showPaymentDialog = false
                    paymentStatus = "Payment cancelled"
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
                            showPaymentDialog = false
                            val itemsToProcess = cartItems.filter { it.sellerId == ownerId }
                            processSuccessfulPayment(transactionId, referenceId, itemsToProcess)
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
                            showPaymentDialog = false
                            paymentStatus = "Payment cancelled"
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
                        Column(
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            ReceiptRow(
                                label = "Date & Time:",
                                value = paymentTimestamp
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ReceiptRow(
                                label = "Merchant:",
                                value = sellerNames[ownerId] ?: "Merchant"
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ReceiptRow(
                                label = "Amount:",
                                value = "₱$totalPrice",
                                valueStyle = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.Bold
                                )
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            ReceiptRow(
                                label = "Transaction ID:",
                                value = transactionId
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
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
                            Button(
                                onClick = {
                                    navController.navigate("order") {
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

        if (showErrorDialog) {
            AlertDialog(
                onDismissRequest = { showErrorDialog = false },
                title = { Text("Error") },
                text = { Text(errorMessage) },
                confirmButton = {
                    Button(
                        onClick = { showErrorDialog = false },
                        colors = ButtonDefaults.buttonColors(containerColor = gcashGreen)
                    ) {
                        Text("OK")
                    }
                }
            )
        }
    }
    @Composable
     fun ReceiptRow(
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
