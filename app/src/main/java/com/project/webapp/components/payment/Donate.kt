package com.project.webapp.components.payment

import android.graphics.Bitmap
import android.util.Log
import android.widget.Toast
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
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.project.webapp.R
import com.project.webapp.Viewmodel.createDonationRecord
import com.project.webapp.components.createDonationNotification
import com.project.webapp.components.generateQRCode
import com.project.webapp.datas.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DonationScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    directBuyProductId: String? = null,
    directBuyPrice: String? = null,
    cartItemsJson: String? = null,
    totalPrice: Double? = null,
    userTypeNav: String? = null,
    sellerNamesJson: String? = null,
    paymentMethod: String? = null,
    referenceIdNav: String? = null,
    organizationJson: String? = null,
    isDonation: Boolean = false,
    orderNumber: String? = null
) {
    // UI and state setup
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotalState by cartViewModel.totalCartPrice.collectAsState()
    val currentUser by cartViewModel.currentUser.collectAsStateWithLifecycle()

    val sellerNames = remember { mutableStateMapOf<String, String>() }
    val userType = userTypeNav ?: if (directBuyProductId != null) "direct_donation" else "cart_donation"

    var directBuyProduct by remember { mutableStateOf<Product?>(null) }
    val selectedOrganization = remember { mutableStateOf<Organization?>(null) }

    var qrCodeBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var paymentStatus by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var referenceId by remember { mutableStateOf(referenceIdNav ?: "") }
    var paymentTimestamp by remember { mutableStateOf("") }
    var showErrorDialog by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    val themeColor = Color(0xFF0DA54B)
    val firestore = FirebaseFirestore.getInstance()
    val transactionId = remember { UUID.randomUUID().toString() }
    val context = LocalContext.current

    val resolvedTotalPrice = totalPrice ?: directBuyPrice?.toDoubleOrNull() ?: cartTotalState

    val gcashApiConfig = remember {
        GCashApiConfig(
            clientId = "mock_client_id",
            clientSecret = "mock_client_secret",
            merchantId = "mock_merchant_id",
            redirectUrl = "com.project.webapp://payment_callback"
        )
    }

    // Deserialize navigation arguments
    val displayItems = remember(cartItemsJson) {
        cartItemsJson?.let {
            runCatching {
                val type = object : TypeToken<List<CartItem>>() {}.type
                Gson().fromJson<List<CartItem>>(it, type)
            }.getOrElse {
                Log.e("DonationScreen", "Error parsing cartItemsJson: ${it.message}", it)
                emptyList()
            }
        } ?: emptyList()
    }

    val organization = remember(organizationJson) {
        organizationJson?.let {
            runCatching {
                Gson().fromJson(it, Organization::class.java)
            }.getOrNull()
        }
    }

    val sellerNamesMap = remember(sellerNamesJson) {
        sellerNamesJson?.let {
            runCatching {
                val type = object : TypeToken<Map<String, String>>() {}.type
                Gson().fromJson<Map<String, String>>(it, type)
            }.getOrDefault(emptyMap())
        } ?: emptyMap()
    }


    // Update local seller names
    LaunchedEffect(sellerNamesMap) {
        sellerNames.clear()
        sellerNames.putAll(sellerNamesMap)
    }

    // Handle direct donation receipt view
    if (isDonation && orderNumber != null) {
        cartViewModel.getReceiptData(orderNumber)?.let { receiptData ->
            ReceiptScreen(
                navController = navController,
                cartViewModel = cartViewModel,
                cartItems = receiptData.cartItems ?: emptyList(),
                totalPrice = receiptData.totalPrice ?: 0.0,
                userType = receiptData.userType ?: "Unknown",
                sellerNames = receiptData.sellerNames ?: emptyMap(),
                paymentMethod = receiptData.paymentMethod ?: "GCash",
                referenceId = receiptData.referenceId ?: "",
                organization = receiptData.organization,
                isDonation = receiptData.isDonation ?: false,
                orderNumber = orderNumber
            )
            return
        } ?: run {
            Log.e("DonationScreen", "No receipt data found for orderNumber: $orderNumber")
            navController.navigate("errorScreen") {
                popUpTo(navController.graph.startDestinationId)
            }
            return
        }
    }

    // List of organizations (could be fetched from a repository instead)
    val organizations = remember {
        listOf(
            Organization("1", "Red Cross", "...", R.drawable.redcross_icon),
            Organization("2", "Food for the Hungry", "...", R.drawable.fh_icon),
            Organization("3", "Central Kitchen Valenzuela", "...", R.drawable.val_logo),
            Organization("4", "Angat Kabataan", "...", R.drawable.angat_kabataan)
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

    // Determine local display items
    val localDisplayItems: List<CartItem> = remember(directBuyProduct, cartItems) {
        directBuyProduct?.let {
            listOf(
                CartItem(
                    productId = it.prodId,
                    name = it.name,
                    price = it.price,
                    quantity = 1,
                    imageUrl = it.imageUrl,
                    isDirectBuy = true
                )
            )
        } ?: cartItems
    }

    localDisplayItems.forEach { item ->
        cartViewModel.getProductById(item.productId) { product ->
            product?.let { prod ->
                cartViewModel.getUserById(prod.ownerId) { user ->
                    user?.let {
                        sellerNames[prod.prodId] = "${user.firstname} ${user.lastname}"
                    }
                }
            }
        }
    }

//    // Generate QR code
//    LaunchedEffect(selectedOrganization, resolvedTotalPrice) {
//        selectedOrganization.value?.let { org ->
//            try {
//                qrCodeBitmap = generateQRCode(
//                    "GCash Donation\nOrganization: ${org.name}\nAmount: ₱$resolvedTotalPrice\nTxnID: $transactionId"
//                )
//                if (qrCodeBitmap == null) {
//                    Log.e("DonationScreen", "QR Code generation returned null")
//                    errorMessage = "Failed to generate QR code"
//                    showErrorDialog = true
//                }
//            } catch (e: Exception) {
//                Log.e("DonationScreen", "QR Code generation error: ${e.message}", e)
//                errorMessage = "QR code generation failed: ${e.message}"
//                showErrorDialog = true
//            }
//        }
//    }

    fun processSuccessfulDonation(
        referenceId: String,
        displayItems: List<CartItem>,
        organization: Organization,
        firestore: FirebaseFirestore,
        cartViewModel: CartViewModel,
        userType: String,
        totalPrice: Double,
        setPaymentStatus: (String) -> Unit,
        setPaymentTimestamp: (String) -> Unit,
        setErrorMessage: (String) -> Unit,
        setIsLoading: (Boolean) -> Unit,
        setShowErrorDialog: (Boolean) -> Unit,
        setShowReceiptDialog: (String) -> Unit
    ) {
        if (displayItems.isEmpty()) {
            Log.e("DonationScreen", "Cannot process donation: Empty display items")
            setPaymentStatus("Error: No items to process")
            setIsLoading(false)
            setErrorMessage("Unable to process donation: No items found.")
            setShowErrorDialog(true)
            return
        }

        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            Log.e("DonationScreen", "No authenticated user found")
            setErrorMessage("Please log in to process the donation")
            setShowErrorDialog(true)
            setIsLoading(false)
            return
        }

        val orderNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()
        val paymentId = "DPAY-$orderNumber"
        val donationId = "DON-$orderNumber"
        val transactionId = "TXN-$orderNumber"

        val paymentData = hashMapOf(
            "donationId" to donationId,
            "referenceId" to referenceId,
            "status" to "Completed",
            "amount" to totalPrice,
            "organizationId" to organization.id,
            "organizationName" to organization.name,
            "timestamp" to Timestamp.now(),
            "paymentMethod" to "GCash",
            "userId" to userId
        )

        val itemName = if (displayItems.size == 1) {
            displayItems.first().name
        } else {
            displayItems.joinToString(", ") { it.name }
        }
        val totalQuantity = displayItems.sumOf { it.quantity }

        // Launch coroutine to fetch full product info for all items
        cartViewModel.viewModelScope.launch {
            try {
                // Suspend function version of getProductById (needs to be implemented in your ViewModel)
                suspend fun getProductByIdSuspend(productId: String): Product? {
                    return try {
                        val doc = firestore.collection("products").document(productId).get().await()
                        doc.toObject(Product::class.java)?.apply { prodId = doc.id }
                    } catch (e: Exception) {
                        null
                    }
                }

                val products = displayItems.map { item ->
                    async { getProductByIdSuspend(item.productId) }
                }.awaitAll()

                // Build transaction items with full product data for unit and weight
                val transactionItems = displayItems.mapIndexed { index, cartItem ->
                    val product = products[index]
                    mapOf(
                        "productId" to cartItem.productId,
                        "sellerId" to (product?.ownerId),
                        "price" to cartItem.price,
                        "name" to cartItem.name,
                        "quantity" to cartItem.quantity,
                        "unit" to (product?.quantity ?: ""),
                        "weight" to (product?.quantityUnit ?: 0)
                    )
                }

                val transactionData = hashMapOf<String, Any?>(
                    "timestamp" to Timestamp.now(),
                    "buyerId" to userId,
                    "status" to "Completed",
                    "transactionType" to "donation",
                    "totalAmount" to totalPrice,
                    "quantity" to totalQuantity,
                    "organization" to organization.name,
                    "paymentMethod" to "GCash",
                    "referenceId" to referenceId,
                    "item" to itemName,
                    "items" to transactionItems
                )

                Log.d("DonationScreen", "Processing donation: paymentId=$paymentId, transactionId=$transactionId, items=${displayItems.size}, totalPrice=$totalPrice")
                displayItems.forEach { item ->
                    Log.d("DonationScreen", "Item: ${item.name}, ID: ${item.productId}, Quantity: ${item.quantity}, Price: ${item.price}, Unit: ${item.unit}, Weight: ${item.weight}")
                }

                // Save donation payment
                firestore.collection("donation_payments")
                    .document(paymentId)
                    .set(paymentData)
                    .addOnSuccessListener {
                        Log.d("DonationScreen", "Donation payment record created successfully: $paymentId")
                        // Save transaction with full items
                        firestore.collection("transactions")
                            .document(transactionId)
                            .set(transactionData)
                            .addOnSuccessListener {
                                Log.d("DonationScreen", "Transaction saved successfully: $transactionId")
                                setPaymentStatus("Donation successful!")
                                val dateFormat = SimpleDateFormat("MMM dd, yyyy hh:mm:ss a", Locale.getDefault())
                                setPaymentTimestamp(dateFormat.format(Date()))

                                // Create notifications and donation records
                                displayItems.forEachIndexed { idx, cartItem ->
                                    val product = products[idx]
                                    product?.let { prod ->
                                        createDonationNotification(
                                            firestore = firestore,
                                            product = prod,
                                            donatorId = userId,
                                            organizationName = organization.name,
                                            transactionId = transactionId
                                        )
                                        createDonationRecord(
                                            userId = userId,
                                            productId = prod.prodId,
                                            productName = prod.name,
                                            organizationId = organization.id,
                                            organizationName = organization.name,
                                            quantity = cartItem.quantity,
                                            orderNumber = orderNumber,
                                            firestore = firestore
                                        )
                                    }
                                }

                                cartViewModel.completePurchase(userType, "GCash")
                                val activity = hashMapOf(
                                    "userId" to userId,
                                    "description" to "Donate an order worth ₱${totalPrice.toInt()} via Gcash.",
                                    "timestamp" to Timestamp.now()
                                )

                                firestore.collection("activities")
                                    .add(activity)
                                    .addOnSuccessListener {
                                        Log.d("RecentActivity", "Activity logged successfully.")
                                    }
                                    .addOnFailureListener { e ->
                                        Log.e("RecentActivity", "Failed to log activity: ${e.message}")
                                    }

                                if (displayItems.isNotEmpty() && totalPrice > 0 && sellerNames.isNotEmpty()) {
                                    cartViewModel.setReceiptData(
                                        orderNumber = orderNumber,
                                        cartItems = displayItems,
                                        totalPrice = totalPrice,
                                        userType = userType,
                                        sellerNames = sellerNames.toMap(),
                                        paymentMethod = "GCash",
                                        referenceId = referenceId,
                                        organization = organization,
                                        isDonation = true
                                    )
                                    setShowReceiptDialog(orderNumber)
                                } else {
                                    setErrorMessage("Invalid donation data for receipt")
                                    setShowErrorDialog(true)
                                    setIsLoading(false)
                                    Log.e("DonationScreen", "Invalid data for receipt: items=${displayItems.size}, totalPrice=$totalPrice, sellerNames=$sellerNames")
                                }
                            }
                            .addOnFailureListener { e ->
                                Log.e("DonationScreen", "Error saving transaction: ${e.message}", e)
                                setPaymentStatus("Error saving transaction")
                                setErrorMessage("Donation processing error: ${e.message}")
                                setShowErrorDialog(true)
                                setIsLoading(false)
                            }
                    }
                    .addOnFailureListener { e ->
                        Log.e("DonationScreen", "Error saving donation payment: ${e.message}", e)
                        setPaymentStatus("Error saving donation record")
                        setErrorMessage("Donation processing error: ${e.message}")
                        setShowErrorDialog(true)
                        setIsLoading(false)
                    }
            } catch (e: Exception) {
                Log.e("DonationScreen", "Error fetching product details: ${e.message}", e)
                setPaymentStatus("Error processing donation")
                setErrorMessage("Error fetching product details: ${e.message}")
                setShowErrorDialog(true)
                setIsLoading(false)
            }
        }
    }

    fun initiateGCashDonation() {
        if (localDisplayItems.isEmpty() || selectedOrganization == null) {
            Log.e("DonationScreen", "Cannot initiate donation: Empty items (${localDisplayItems.size}) or no organization selected")
            paymentStatus = "Error: Invalid donation details"
            errorMessage = "Please select an organization and items"
            showErrorDialog = true
            Toast.makeText(context, "Please select an organization and items", Toast.LENGTH_SHORT).show()
            return
        }

        isLoading = true
        paymentStatus = "Processing..."

        val amountInCents = (totalPrice ?: 0.0) * 100
        val paymentRequest = GCashPaymentRequest(
            amount = amountInCents.toInt(),
            currency = "PHP",
            description = "Donation to ${selectedOrganization.value?.name ?: "Unknown Organization"}",
            transactionId = transactionId,
            merchantId = gcashApiConfig.merchantId
        )

        CoroutineScope(Dispatchers.IO).launch {
            try {
                referenceId = "REF_${System.currentTimeMillis()}"
                delay(1000) // Simulate payment processing

                withContext(Dispatchers.Main) {
                    isLoading = false
                    Toast.makeText(
                        context,
                        "Mock: Processing GCash donation.",
                        Toast.LENGTH_SHORT
                    ).show()

                    // Access the value of the selectedOrganization correctly
                    val org = selectedOrganization.value
                    if (org != null) {  // Proceed only if the organization is selected
                        if (resolvedTotalPrice <= 0 || localDisplayItems.isEmpty() || localDisplayItems.any { !sellerNames.containsKey(it.productId) }) {
                            errorMessage = "Invalid donation data: Check items, price, or seller information"
                            showErrorDialog = true
                            Log.e("DonationScreen", "Invalid donation data: totalPrice=$resolvedTotalPrice, items=${localDisplayItems.size}, sellerNames=$sellerNames")
                        }

                        processSuccessfulDonation(
                            referenceId = referenceId,
                            displayItems = localDisplayItems,
                            organization = org,
                            firestore = firestore,
                            cartViewModel = cartViewModel,
                            userType = userType,
                            totalPrice = resolvedTotalPrice,
                            setPaymentStatus = { paymentStatus = it },
                            setPaymentTimestamp = { paymentTimestamp = it },
                            setErrorMessage = { errorMessage = it },
                            setIsLoading = { isLoading = it },
                            setShowErrorDialog = { showErrorDialog = it },
                            setShowReceiptDialog = { orderNumber ->
                                CoroutineScope(Dispatchers.Main).launch {
                                    delay(200) // Delay to stabilize UI
                                    try {
                                        navController.navigate("receiptScreen/$orderNumber")
                                        Log.d("DonationScreen", "Navigated to receiptScreen with orderNumber=$orderNumber")
                                    } catch (e: Exception) {
                                        Log.e("DonationScreen", "Navigation to receiptScreen failed: ${e.message}", e)
                                        errorMessage = "Failed to display receipt: ${e.message}"
                                        showErrorDialog = true
                                    }
                                }
                            }
                        )
                    } else {
                        errorMessage = "No organization selected"
                        showErrorDialog = true
                        Log.e("DonationScreen", "No organization selected for donation")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isLoading = false
                    paymentStatus = "Error: ${e.localizedMessage}"
                    errorMessage = "Donation initiation failed: ${e.message}"
                    showErrorDialog = true
                    Log.e("DonationScreen", "Donation initiation failed: ${e.message}", e)
                }
            }
        }

    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Donation") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
                // Donation Introduction
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

                // Items to Donate
                SectionTitle(title = "Items to Donate (${localDisplayItems.size})")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        localDisplayItems.forEachIndexed { index, item ->
                            ItemCard(
                                item = item,
                                sellerName = sellerNames[item.productId] ?: "Loading seller...",
                                themeColor = themeColor
                            )
                            if (index < localDisplayItems.size - 1) {
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

                // Select Organization
                SectionTitle(title = "Select an Organization")
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(vertical = 8.dp)) {
                        organizations.forEach { org ->
                            OrganizationItem(
                                organization = org,
                                isSelected = selectedOrganization?.value?.id == org.id, // Use `.value` to access the selectedOrganization
                                themeColor = themeColor,
                                onSelect = { selectedOrganization?.value = org } // Update the selectedOrganization's value
                            )
                        }
                    }
                }

                // Donor Information
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
                // Seller Information
                SectionTitle(title = "Seller Information")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Sellers for Donated Items",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        if (selectedOrganization.value != null && localDisplayItems.isNotEmpty()) {
                            if (localDisplayItems.all { sellerNames.containsKey(it.productId) }) {
                                localDisplayItems.forEach { item ->
                                    // Fetch Product for additional details (if needed)
                                    var product by remember { mutableStateOf<Product?>(null) }
                                    LaunchedEffect(item.productId) {
                                        cartViewModel.getProductById(item.productId) { prod ->
                                            product = prod
                                            Log.d("DonationScreen", "Fetched product for ${item.productId}: ${prod?.name}")
                                        }
                                    }

                                    val sellerName = sellerNames[item.productId] ?: "Unknown Seller"
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp)
                                    ) {
                                        // Seller Name
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(bottom = 4.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Person,
                                                contentDescription = null,
                                                tint = themeColor,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Seller: $sellerName",
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        // Product Details
                                        Text(
                                            text = "Item: ${item.name}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        Text(
                                            text = "Price: ₱${item.price}0",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.Gray
                                        )
                                        // Product details from fetched Product object
                                        product?.let { prod ->
                                            Text(
                                                text = "Category: ${prod.category}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "Quantity: ${prod.quantity.toInt()}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                            Text(
                                                text = "Unit: ${prod.quantityUnit}",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = Color.Gray
                                            )
                                        }
                                    }
                                    Divider(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 8.dp),
                                        color = Color(0xFFEEEEEE)
                                    )
                                }
                            } else {
                                Text(
                                    text = "Loading seller information...",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            Text(
                                text = "Please select an organization and items",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                // Donation Payment
                SectionTitle(title = "Donation Payment")
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Payment Details",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Safely access the name property of the selected organization
                        ReceiptRow(
                            label = "Organization:",
                            value = selectedOrganization?.value?.name ?: "Select an organization"
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        ReceiptRow(label = "Amount:", value = "₱$resolvedTotalPrice")
                        Spacer(modifier = Modifier.height(4.dp))
                        ReceiptRow(label = "Transaction ID:", value = transactionId.take(8) + "...")
                    }
                }

                if (paymentStatus.isNotEmpty()) {
                    Text(
                        text = paymentStatus,
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (paymentStatus.contains("successful")) themeColor else
                            if (paymentStatus.contains("Error")) Color.Red else Color.Blue,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                Button(
                    onClick = { initiateGCashDonation() },
                    enabled = !isLoading && selectedOrganization != null && localDisplayItems.isNotEmpty(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = themeColor,
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
                        contentColor = themeColor
                    )
                ) {
                    Text("Cancel Donation")
                }

                Spacer(modifier = Modifier.height(16.dp))
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
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor)
                ) {
                    Text("OK")
                }
            }
        )
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
            Image(
                painter = painterResource(id = organization.iconResId),
                contentDescription = organization.name,
                modifier = Modifier.size(40.dp)
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



