package com.project.webapp.components.payment

import android.graphics.Bitmap
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
import com.project.webapp.Viewmodel.createOrderRecord
import com.project.webapp.components.createDonationNotification
import com.project.webapp.components.createSaleNotification
import com.project.webapp.components.fetchOwnerName
import com.project.webapp.components.generateQRCode
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









