package com.project.webapp.components.delivery

import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import com.project.webapp.R
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.webapp.Viewmodel.OrderStatus
import com.project.webapp.components.DetailRow
import com.project.webapp.components.LoadingAnimation
import com.project.webapp.datas.Order
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true // Parameter to control scaffold visibility
) {
    val primaryColor = Color(0xFF0DA54B)

    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var orders by remember { mutableStateOf<List<Order>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedOrder by remember { mutableStateOf<Order?>(null) }

    // Fetch orders for the current user
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            firestore.collection("orders")
                .whereEqualTo("buyerId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        Log.e("Orders", "Error fetching orders", error)
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        orders = snapshot.documents.mapNotNull { doc ->
                            doc.toObject(Order::class.java)
                        }
                    }
                }
        } else {
            isLoading = false
        }
    }

    if (showScaffold) {
        Scaffold(
            topBar = {
                SmallTopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Orders",
                                tint = primaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "My Orders",
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.DarkGray
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.DarkGray
                            )
                        }
                    },
                    colors = TopAppBarDefaults.smallTopAppBarColors(
                        containerColor = Color.White,
                        titleContentColor = Color.DarkGray,
                    ),
                    modifier = Modifier.shadow(elevation = 4.dp)
                )
            },
            containerColor = Color(0xFFF7FAF9)
        ) { paddingValues ->
            OrdersContent(
                paddingValues = paddingValues,
                isLoading = isLoading,
                orders = orders,
                selectedOrder = selectedOrder,
                onOrderSelected = { selectedOrder = it },
                navController = navController,
                primaryColor = primaryColor,
                modifier = modifier
            )
        }
    } else {
        // When embedded in profile, show just the content without scaffold
        OrdersContent(
            paddingValues = PaddingValues(0.dp),
            isLoading = isLoading,
            orders = orders,
            selectedOrder = selectedOrder,
            onOrderSelected = { selectedOrder = it },
            navController = navController,
            primaryColor = primaryColor,
            modifier = modifier
        )
    }

    // Show order details dialog when an order is selected
    selectedOrder?.let { order ->
        OrderDetailsDialog(
            order = order,
            onDismiss = { selectedOrder = null },
            primaryColor = primaryColor
        )
    }
}

@Composable
private fun OrdersContent(
    paddingValues: PaddingValues,
    isLoading: Boolean,
    orders: List<Order>,
    selectedOrder: Order?,
    onOrderSelected: (Order) -> Unit,
    navController: NavController,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        if (isLoading) {
            LoadingAnimation(primaryColor = primaryColor)
        } else if (orders.isEmpty()) {
            EmptyOrdersScreen(navController)
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(orders.size) { index ->
                    val order = orders[index]
                    OrderItem(
                        order = order,
                        onClick = { onOrderSelected(order) },
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun OrderItem(
    order: Order,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val firstItem = order.items.firstOrNull()
    val itemName = firstItem?.get("name") as? String ?: "Unknown Product"
    val itemPrice = (firstItem?.get("price") as? Number)?.toDouble() ?: 0.0
    val itemImageUrl = firstItem?.get("imageUrl") as? String ?: ""
    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(order.createdAt))
    var isPressed by remember { mutableStateOf(false) }

    // Get color and icon for status
    val (statusColor, statusIcon) = when (order.status) {
        OrderStatus.PAYMENT_RECEIVED.name -> Color(0xFF0DA54B) to Icons.Default.CheckCircle
        OrderStatus.TO_SHIP.name -> Color(0xFF0DA54B) to Icons.Default.Inventory
        OrderStatus.SHIPPING.name -> Color(0xFF0DA54B) to Icons.Default.LocalShipping
        OrderStatus.TO_DELIVER.name -> Color(0xFF0DA54B) to Icons.Default.LocalShipping
        OrderStatus.DELIVERED.name -> Color(0xFF0DA54B) to Icons.Default.CheckCircle
        OrderStatus.COMPLETED.name -> Color(0xFF0DA54B) to Icons.Default.CheckCircle
        else -> Color.Gray to Icons.Default.Info
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .graphicsLayer {
                scaleX = if (isPressed) 0.98f else 1f
                scaleY = if (isPressed) 0.98f else 1f
            }
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Status header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = statusColor.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = statusIcon,
                    contentDescription = order.status,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = OrderStatus.valueOf(order.status).displayName,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formattedDate,
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Order item preview
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product image
                if (itemImageUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(itemImageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Product Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFEDF7F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Order Icon",
                            modifier = Modifier.size(40.dp),
                            tint = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = itemName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(2.dp))

                    // Show number of items
                    Text(
                        text = "${order.items.size} item(s)",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "₱${String.format("%.2f", order.totalAmount)}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = primaryColor
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = Color.Gray
                )
            }
        }
    }
}

@Composable
fun OrderDetailsDialog(
    order: Order,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    val firestore = FirebaseFirestore.getInstance()
    val scrollState = rememberScrollState()
    val formattedDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))

    // Get color for status
    val statusColor = when (order.status) {
        OrderStatus.PAYMENT_RECEIVED.name -> Color(0xFF0DA54B)
        OrderStatus.TO_SHIP.name -> Color(0xFF0DA54B)
        OrderStatus.SHIPPING.name -> Color(0xFF0DA54B)
        OrderStatus.TO_DELIVER.name -> Color(0xFF0DA54B)
        OrderStatus.DELIVERED.name -> Color(0xFF4CAF50)
        OrderStatus.COMPLETED.name -> Color(0xFF0DA54B)
        else -> Color.Gray
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "Order Details",
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Order #${order.orderId.takeLast(6)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color.DarkGray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(scrollState)
            ) {
                // Add estimated delivery if applicable
                if (order.status !in listOf(OrderStatus.DELIVERED.name, OrderStatus.COMPLETED.name)) {
                    DeliveryEstimation(
                        order = order,
                        primaryColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))
                }

                // Status card with enhanced tracking
                Card(
                    colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.1f)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = OrderStatus.valueOf(order.status).displayName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = statusColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // New detailed timeline for better visualization
                        OrderStatusTimeline(
                            currentStatus = order.status,
                            primaryColor = statusColor
                        )
                    }
                }

                // Add tracking information for orders in transit
                if (order.status in listOf(
                        OrderStatus.SHIPPING.name,
                        OrderStatus.TO_DELIVER.name
                    )
                ) {
                    Spacer(modifier = Modifier.height(16.dp))

                    TrackingInformation(
                        order = order,
                        primaryColor = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Order items
                Text(
                    text = "Items (${order.items.size})",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        order.items.forEachIndexed { index, item ->
                            val name = item["name"] as? String ?: "Unknown Product"
                            val price = (item["price"] as? Number)?.toDouble() ?: 0.0
                            val quantity = (item["quantity"] as? Number)?.toInt() ?: 1
                            val imageUrl = item["imageUrl"] as? String ?: ""

                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(vertical = 8.dp)
                            ) {
                                // Product image
                                if (imageUrl.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                    ) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalContext.current)
                                                .data(imageUrl)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = "Product Image",
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(50.dp)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFEDF7F0)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ShoppingBasket,
                                            contentDescription = "Product Icon",
                                            tint = primaryColor,
                                            modifier = Modifier.size(28.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = name,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = "Qty: $quantity",
                                        fontSize = 13.sp,
                                        color = Color.Gray
                                    )
                                }

                                Text(
                                    text = "₱${String.format("%.2f", price * quantity)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = Color.DarkGray
                                )
                            }

                            if (index < order.items.size - 1) {
                                Divider(color = Color.LightGray)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Payment information
                Text(
                    text = "Payment Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        DetailRow(
                            label = "Payment Method",
                            value = order.paymentMethod,
                            icon = Icons.Default.CreditCard,
                            primaryColor = primaryColor
                        )

                        DetailRow(
                            label = "Order Date",
                            value = formattedDate,
                            icon = Icons.Default.Schedule,
                            primaryColor = primaryColor
                        )

                        DetailRow(
                            label = "Items Subtotal",
                            value = "₱${String.format("%.2f", order.totalAmount - 50.0f)}",
                            icon = Icons.Default.ShoppingBasket,
                            primaryColor = primaryColor
                        )

                        DetailRow(
                            label = "Shipping Fee",
                            value = "₱50.00",
                            icon = Icons.Default.LocalShipping,
                            primaryColor = primaryColor
                        )

                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Total",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.DarkGray
                            )

                            Text(
                                text = "₱${String.format("%.2f", order.totalAmount)}",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = primaryColor
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Shipping information
                Text(
                    text = "Shipping Information",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.DarkGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = order.deliveryAddress,
                            fontSize = 14.sp,
                            lineHeight = 20.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    "Close",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}


@Composable
fun OrderStatusPoint(
    status: String,
    currentStatus: String,
    label: String
) {
    val orderStatusList = OrderStatus.values().map { it.name }
    val currentStatusIndex = orderStatusList.indexOf(currentStatus)
    val thisStatusIndex = orderStatusList.indexOf(status)

    val isCompleted = thisStatusIndex <= currentStatusIndex
    val statusColor = if (isCompleted) Color(0xFF0DA54B) else Color.Gray

    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .background(
                    color = if (isCompleted) statusColor else Color.White,
                    shape = CircleShape
                )
                .border(1.dp, statusColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            fontSize = 12.sp,
            color = statusColor,
            fontWeight = if (isCompleted) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
fun EmptyOrdersScreen(navController: NavController) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.empty)
        )

        // If you don't have a Lottie animation, use this fallback
        if (composition == null) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = "No Orders",
                tint = Color(0xFF0DA54B),
                modifier = Modifier.size(100.dp)
            )
        } else {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "No orders yet!",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "When you place orders, they will appear here.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { navController.navigate("market") },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0DA54B)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingCart,
                contentDescription = "Shop Now",
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Start Shopping",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}