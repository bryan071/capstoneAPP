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
import com.project.webapp.R
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GcashScreen(
    navController: NavController,
    totalPrice: String,
    firestore: FirebaseFirestore,
    ownerId: String,
    cartViewModel: CartViewModel = viewModel()
) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
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

    fun processSuccessfulPayment(transactionId: String, referenceId: String, cartItems: List<CartItem>) {

        val paymentData = hashMapOf(
            "transactionId" to transactionId,
            "referenceId" to referenceId,
            "status" to "COMPLETED",
            "amount" to totalPrice,
            "seller" to ownerId,
            "timestamp" to FieldValue.serverTimestamp()
        )

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return

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

                        // ✅ use cartItems here
                        cartItems.forEach { cartItem ->
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
                                    updateProductInventory(prod.prodId, cartItem.quantity)
                                }
                            }
                        }

                        // ✅ and here
                        createOrderRecord(userId, cartItems, "GCash", totalPrice.toFloat(), userAddress)
                        showReceiptDialog = true
                    }
                    .addOnFailureListener { e ->
                        Log.e("GCashScreen", "Error fetching user data", e)
                        createOrderRecord(userId, cartItems, "GCash", totalPrice.toFloat(), "No address provided")
                        showReceiptDialog = true
                    }
            }
            .addOnFailureListener { e ->
                Log.e("GCashScreen", "Error updating payment: ${e.message}")
                paymentStatus = "Error saving payment record"
            }
    }


    // Function to initiate GCash payment and proceed directly
    fun initiateGCashPayment(cartItems: List<CartItem>) {
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
                    processSuccessfulPayment(transactionId, referenceId, cartItems)
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
                    onClick = { initiateGCashPayment(cartItems) },
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
                            processSuccessfulPayment(transactionId, referenceId, cartItems)
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
