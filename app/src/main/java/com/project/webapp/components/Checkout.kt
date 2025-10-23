package com.project.webapp.components

import CartViewModel
import android.util.Log
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.gson.Gson
import com.project.webapp.R
import com.project.webapp.components.payment.PriceLine
import com.project.webapp.components.payment.ReceiptScreen
import com.project.webapp.components.payment.SectionTitle
import com.project.webapp.datas.CartItem
import kotlinx.coroutines.launch
import java.net.URLDecoder
import java.util.UUID
import kotlin.collections.contains
import kotlin.collections.forEach

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    totalPrice: Double,
    userType: String,
    paymentMethod: String = "COD",
    referenceId: String = "",
    itemsJson: String? = null
) {
    var showReceipt by remember { mutableStateOf(false) }
    val sellerNames = remember { mutableStateMapOf<String, String>() }
    val currentUser by cartViewModel.currentUser.collectAsState()
    val coroutineScope = rememberCoroutineScope()

    var shippingFeePaid by remember { mutableStateOf(false) }
    var showShippingFeeDialog by remember { mutableStateOf(false) }
    var isPayingShippingFee by remember { mutableStateOf(false) } // for loading simulation

    // Parse cartItems from JSON
    val cartItems: List<CartItem>? = remember(itemsJson) {
        itemsJson?.let {
            try {
                val decodedJson = URLDecoder.decode(it, "UTF-8")
                Gson().fromJson(decodedJson, Array<CartItem>::class.java).toList()
            } catch (e: Exception) {
                Log.e("CheckoutScreen", "Error parsing cartItems JSON: ${e.message}")
                null
            }
        }
    }

    // Debug log for tracking data flow
    LaunchedEffect(key1 = Unit) {
        Log.d(
            "CheckoutScreen",
            "Initial params - paymentMethod: $paymentMethod, referenceId: $referenceId"
        )
        Log.d("CheckoutScreen", "Direct cartItems size: ${cartItems?.size ?: 0}")
        Log.d(
            "CheckoutScreen",
            "ViewModel purchasedItems size: ${cartViewModel.purchasedItems.value.size}"
        )
        Log.d("CheckoutScreen", "ViewModel cartItems size: ${cartViewModel.cartItems.value.size}")
    }

    // Get purchased items from ViewModel using collectAsState for real-time updates
    val purchasedItems by cartViewModel.purchasedItems.collectAsState()
    val viewModelCartItems by cartViewModel.cartItems.collectAsState()

    // Determine effectiveCartItems
    val effectiveCartItems = remember(cartItems, purchasedItems, viewModelCartItems) {
        when {
            !cartItems.isNullOrEmpty() -> cartItems
            purchasedItems.isNotEmpty() -> purchasedItems
            else -> viewModelCartItems
        }
    }

    // Log the selected items for debugging
    LaunchedEffect(effectiveCartItems) {
        Log.d("CheckoutScreen", "Final effectiveCartItems size: ${effectiveCartItems.size}")
    }

    // Fetch seller information for the cart items
    LaunchedEffect(effectiveCartItems) {
        effectiveCartItems.forEach { item ->
            cartViewModel.getProductById(item.productId) { product ->
                product?.let { prod ->
                    if (prod.ownerId !in sellerNames) {
                        cartViewModel.getUserById(prod.ownerId) { user ->
                            user?.let {
                                sellerNames[item.productId] = "${it.firstname} ${it.lastname}"
                            }
                        }
                    }
                }
            }
        }
    }

    if (showReceipt) {
        ReceiptScreen(
            navController = navController,
            cartViewModel = cartViewModel,
            cartItems = effectiveCartItems,
            totalPrice = totalPrice,
            userType = userType,
            sellerNames = sellerNames,
            paymentMethod = paymentMethod,
            referenceId = referenceId,
            organization = null,
            isDonation = false,
            orderNumber = UUID.randomUUID().toString().substring(0, 8).uppercase()
        )
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
                            if (paymentMethod == "GCash") {
                                Icon(
                                    painter = painterResource(id = R.drawable.gcash_icon),
                                    contentDescription = null,
                                    tint = Color.Unspecified,
                                    modifier = Modifier.size(40.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = Color(0xFF0DA54B),
                                    modifier = Modifier.size(40.dp)
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                when (paymentMethod) {
                                    "GCash" -> "GCash Payment"
                                    else -> "Cash on Delivery"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )

                            if (paymentMethod == "GCash" && referenceId.isNotEmpty()) {
                                Text(
                                    "Reference ID: $referenceId",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                            }

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
                                        tint = Color(0xFF0DA54B),
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
                                        tint = Color(0xFF0DA54B),
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        user.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }

                                Row(
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.LocationOn,
                                        contentDescription = null,
                                        tint = Color(0xFF0DA54B),
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
                    SectionTitle(title = "Order Items (${effectiveCartItems.size})")

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                            if (effectiveCartItems.isEmpty()) {
                                Text(
                                    text = "No items to display",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray,
                                    modifier = Modifier.padding(16.dp),
                                    textAlign = TextAlign.Center
                                )
                            } else {
                                effectiveCartItems.forEachIndexed { index, item ->
                                    val sellerName = sellerNames[item.productId] ?: "Loading..."

                                    CheckoutItemCard(
                                        item = item,
                                        sellerName = sellerName,
                                        themeColor = Color(0xFF0DA54B)
                                    )

                                    if (index < effectiveCartItems.size - 1) {
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
                                themeColor = Color(0xFF0DA54B)
                            )

                            PriceLine(
                                title = "Shipping Fee",
                                value = "₱50.00",
                                themeColor = Color(0xFF0DA54B)
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
                                themeColor = Color(0xFF0DA54B)
                            )

                            PriceLine(
                                title = "Payment Method",
                                value = when (paymentMethod) {
                                    "GCash" -> "GCash"
                                    else -> "Cash on Delivery"
                                },
                                themeColor = Color(0xFF0DA54B)
                            )
                        }
                    }

                    // Confirmation Button
                    Button(
                        onClick = {
                            if (paymentMethod == "COD" && !shippingFeePaid) {
                                showShippingFeeDialog = true
                            } else {
                                cartViewModel.completePurchase(userType, paymentMethod, referenceId)
                                cartViewModel.clearPurchasedItems()
                                showReceipt = true
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                        enabled = effectiveCartItems.isNotEmpty()
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
                }

                if (showShippingFeeDialog) {
                    AlertDialog(
                        onDismissRequest = {
                            if (!isPayingShippingFee) showShippingFeeDialog = false
                        },
                        title = { Text(text = "Pay Shipping Fee") },
                        text = {
                            if (isPayingShippingFee) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    CircularProgressIndicator()
                                    Spacer(modifier = Modifier.width(16.dp))
                                    Text("Processing payment...")
                                }
                            } else {
                                Text("You need to pay the shipping fee of ₱50.00 before confirming your order.")
                            }
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    isPayingShippingFee = true
                                    coroutineScope.launch {
                                        kotlinx.coroutines.delay(2000) // simulate delay
                                        shippingFeePaid = true
                                        isPayingShippingFee = false
                                        showShippingFeeDialog = false
                                    }
                                },
                                enabled = !isPayingShippingFee,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
                            ) {
                                Text("Pay ₱50.00")
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = {
                                    if (!isPayingShippingFee) showShippingFeeDialog = false
                                },
                                enabled = !isPayingShippingFee
                            ) {
                                Text("Cancel")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun CheckoutItemCard(
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