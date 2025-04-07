package com.project.webapp.components

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Payments
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.project.webapp.R
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Product
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
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

                // Section: Payment Summary
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
                        val subtotalText = directBuyPrice?.toDoubleOrNull()?.let { "₱%.2f".format(it) }
                            ?: "₱%.2f".format(cartTotalState)

                        PriceLine(
                            title = "Subtotal (Items)",
                            value = subtotalText,
                            themeColor = themeColor
                        )

                        PriceLine(
                            title = "Shipping Fee",
                            value = "₱${"%.2f".format(shippingFee)}",
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
                            value = "₱$formattedAmount",
                            isBold = true,
                            themeColor = themeColor
                        )
                    }
                }

                // Section: Payment Methods
                SectionTitle(title = "Payment Methods")

                Button(
                    onClick = { navController.navigate("gcashScreen/$formattedAmount") },
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
                        navController.navigate("checkoutScreen/$userType/${amount.toFloat()}")
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                    enabled = displayItems.isNotEmpty()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Cash on Delivery (COD)",
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
                    text = "₱${item.price}",
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
                    text = "₱${item.price}",
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
                    text = "₱${item.price}",
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



@Composable
fun GcashScreen(navController: NavController, totalPrice: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                painter = painterResource(id = R.drawable.backbtn),
                contentDescription = "Back",
                tint = Color.Unspecified
            )
        }

        Text(
            text = "GCash Payment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Amount to Pay: ₱$totalPrice",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = { /* Implement GCash Payment Logic */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.gcash_icon),
                contentDescription = "GCash Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Proceed with GCash")
        }
    }
}
