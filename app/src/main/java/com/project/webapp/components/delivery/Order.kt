package com.project.webapp.components.payment

import DialogDetails
import Order
import OrderItemDetails
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.webapp.Viewmodel.OrderItem
import com.project.webapp.components.delivery.CancelOrderDialog
import com.project.webapp.components.delivery.OrderStatusTimeline
import com.project.webapp.components.delivery.TrackingInfoDialog
import com.project.webapp.datas.Transaction
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// Order Status Enum
enum class OrderStatus(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.ShoppingBag),
    TO_PAY("To Pay", Icons.Default.AccountBalanceWallet),
    TO_SHIP("To Ship", Icons.Default.Inventory),
    TO_RECEIVE("To Receive", Icons.Default.LocalShipping),
    COMPLETED("Completed", Icons.Default.CheckCircle),
    CANCELLED("Cancelled", Icons.Default.Cancel),
    RETURN_REFUND("Return/Refund", Icons.Default.Refresh)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true,
    chatViewModel: com.project.webapp.Viewmodel.ChatViewModel = remember { com.project.webapp.Viewmodel.ChatViewModel() }
) {
    val primaryColor = Color(0xFF0DA54B)
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid

    var orderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<OrderItem?>(null) }
    var selectedStatus by remember { mutableStateOf(OrderStatus.ALL) }

    // Fetch orders and donations
    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            firestore.collection("orders")
                .whereEqualTo("buyerId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Orders", "Error fetching orders", error)
                        return@addSnapshotListener
                    }

                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Order::class.java)
                    }?.map { OrderItem.Purchase(it) } ?: emptyList()

                    firestore.collection("transactions")
                        .whereEqualTo("buyerId", currentUserId)
                        .whereEqualTo("transactionType", "donation")
                        .addSnapshotListener { transactionSnapshot, transactionError ->
                            isLoading = false
                            if (transactionError != null) {
                                Log.e("Orders", "Error fetching donations", transactionError)
                                return@addSnapshotListener
                            }

                            val donations = transactionSnapshot?.documents?.mapNotNull { doc ->
                                val data = doc.data ?: return@mapNotNull null
                                Transaction(
                                    id = doc.id,
                                    buyerId = data["buyerId"] as? String ?: "",
                                    item = data["item"] as? String ?: "",
                                    quantity = (data["quantity"] as? Long)?.toInt() ?: 0,
                                    totalAmount = (data["totalAmount"] as? Double) ?: 0.0,
                                    organization = data["organization"] as? String,
                                    transactionType = data["transactionType"] as? String ?: "",
                                    status = data["status"] as? String ?: "",
                                    timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                                    paymentMethod = data["paymentMethod"] as? String ?: "",
                                    referenceId = data["referenceId"] as? String
                                )
                            }?.map { OrderItem.Donation(it) } ?: emptyList()

                            orderItems = (orders + donations).sortedByDescending {
                                when (it) {
                                    is OrderItem.Purchase -> it.order.timestamp
                                    is OrderItem.Donation -> it.transaction.timestamp
                                }
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
                TopAppBar(
                    title = {
                        Text(
                            text = "My Orders",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = primaryColor
                    )
                )
            },
            containerColor = Color(0xFFF5F5F5)
        ) { paddingValues ->
            OrdersContent(
                paddingValues = paddingValues,
                isLoading = isLoading,
                orderItems = orderItems,
                selectedItem = selectedItem,
                selectedStatus = selectedStatus,
                onStatusSelected = { selectedStatus = it },
                onItemSelected = { selectedItem = it },
                primaryColor = primaryColor,
                modifier = modifier
            )
        }
    } else {
        OrdersContent(
            paddingValues = PaddingValues(0.dp),
            isLoading = isLoading,
            orderItems = orderItems,
            selectedItem = selectedItem,
            selectedStatus = selectedStatus,
            onStatusSelected = { selectedStatus = it },
            onItemSelected = { selectedItem = it },
            primaryColor = primaryColor,
            modifier = modifier
        )
    }

    selectedItem?.let { item ->
        OrderDetailsDialog(
            item = item,
            onDismiss = { selectedItem = null },
            primaryColor = primaryColor,
            navController = navController,
            chatViewModel = chatViewModel
        )
    }
}

@Composable
private fun OrdersContent(
    paddingValues: PaddingValues,
    isLoading: Boolean,
    orderItems: List<OrderItem>,
    selectedItem: OrderItem?,
    selectedStatus: OrderStatus,
    onStatusSelected: (OrderStatus) -> Unit,
    onItemSelected: (OrderItem) -> Unit,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(paddingValues)
            .fillMaxSize()
    ) {
        // Status Tabs
        OrderStatusTabs(
            selectedStatus = selectedStatus,
            onStatusSelected = onStatusSelected,
            orderItems = orderItems,
            primaryColor = primaryColor
        )

        // Content
        Box(modifier = Modifier.fillMaxSize()) {
            if (isLoading) {
                CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier
                        .size(50.dp)
                        .align(Alignment.Center)
                )
            } else {
                val filteredItems = filterOrdersByStatus(orderItems, selectedStatus)

                if (filteredItems.isEmpty()) {
                    EmptyOrdersState(
                        status = selectedStatus,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredItems.size) { index ->
                            OrderItemCard(
                                orderItem = filteredItems[index],
                                onClick = { onItemSelected(filteredItems[index]) },
                                primaryColor = primaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OrderStatusTabs(
    selectedStatus: OrderStatus,
    onStatusSelected: (OrderStatus) -> Unit,
    orderItems: List<OrderItem>,
    primaryColor: Color
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(0.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(OrderStatus.values().size) { index ->
                val status = OrderStatus.values()[index]
                val count = getOrderCountByStatus(orderItems, status)

                OrderStatusChip(
                    status = status,
                    count = count,
                    isSelected = selectedStatus == status,
                    onClick = { onStatusSelected(status) },
                    primaryColor = primaryColor
                )
            }
        }
    }
}

@Composable
private fun OrderStatusChip(
    status: OrderStatus,
    count: Int,
    isSelected: Boolean,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val backgroundColor = if (isSelected) primaryColor else Color(0xFFF5F5F5)
    val contentColor = if (isSelected) Color.White else Color.DarkGray

    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = status.icon,
                contentDescription = status.displayName,
                tint = contentColor,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = status.displayName,
                color = contentColor,
                fontSize = 14.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
            if (count > 0 && status != OrderStatus.ALL) {
                Text(
                    text = "($count)",
                    color = contentColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun OrderItemCard(
    orderItem: OrderItem,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val (title, subtitle, status, timestamp, quantity, totalAmount) = when (orderItem) {
        is OrderItem.Purchase -> {
            val order = orderItem.order
            val firstItem = order.items.firstOrNull()
            val itemName = firstItem?.get("name") as? String ?: "Order"
            val itemCount = order.items.size
            val qty = order.items.sumOf { (it["quantity"] as? Number)?.toInt() ?: 0 }

            Tuple6(
                itemName,
                if (itemCount > 1) "+${itemCount - 1} more items" else "",
                order.status,
                order.timestamp,
                qty,
                order.totalAmount
            )
        }
        is OrderItem.Donation -> {
            val transaction = orderItem.transaction
            Tuple6(
                transaction.item,
                "Donation to ${transaction.organization ?: "Organization"}",
                transaction.status,
                transaction.timestamp,
                transaction.quantity,
                transaction.totalAmount
            )
        }
    }

    val statusColor = getStatusColor(status, primaryColor)
    val statusText = getStatusDisplayText(status)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header with status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = when (orderItem) {
                            is OrderItem.Purchase -> Icons.Default.ShoppingBag
                            is OrderItem.Donation -> Icons.Default.Favorite
                        },
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = when (orderItem) {
                            is OrderItem.Purchase -> "Purchase"
                            is OrderItem.Donation -> "Donation"
                        },
                        fontSize = 13.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Surface(
                    color = statusColor.copy(alpha = 0.1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = statusText,
                        color = statusColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Order item details
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (orderItem) {
                            is OrderItem.Purchase -> Icons.Default.ShoppingBag
                            is OrderItem.Donation -> Icons.Default.Favorite
                        },
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.DarkGray
                    )
                    if (subtitle.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = subtitle,
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Qty: $quantity",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                }

                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "View Details",
                    tint = Color.Gray,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(modifier = Modifier.height(12.dp))

            // Footer with total and date
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formatDate(timestamp.toDate()),
                    fontSize = 12.sp,
                    color = Color.Gray
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Total: ",
                        fontSize = 13.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "₱${String.format("%.2f", totalAmount)}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyOrdersState(
    status: OrderStatus,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = status.icon,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No ${status.displayName} Orders",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = getEmptyStateMessage(status),
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun OrderDetailsDialog(
    item: OrderItem,
    onDismiss: () -> Unit,
    primaryColor: Color,
    navController: NavController,
    chatViewModel: com.project.webapp.Viewmodel.ChatViewModel
) {
    val dialogDetails = when (item) {
        is OrderItem.Purchase -> createPurchaseDialogDetails(item.order, primaryColor, onDismiss, navController, chatViewModel)
        is OrderItem.Donation -> createDonationDialogDetails(item.transaction)
    }

    val statusColor = getStatusColor(dialogDetails.status, primaryColor)

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
                    imageVector = when (item) {
                        is OrderItem.Purchase -> Icons.Default.ShoppingBag
                        is OrderItem.Donation -> Icons.Default.Favorite
                    },
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = dialogDetails.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                    Text(
                        text = getStatusDisplayText(dialogDetails.status),
                        fontSize = 14.sp,
                        color = statusColor,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 500.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                dialogDetails.detailsContent()
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}

@Composable
private fun createPurchaseDialogDetails(
    order: Order,
    primaryColor: Color,
    onDismiss: () -> Unit = {},
    navController: NavController,
    chatViewModel: com.project.webapp.Viewmodel.ChatViewModel
): DialogDetails {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showTrackingDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }
    var orderStatus by remember { mutableStateOf(order.status) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

    // Check if order can be cancelled
    val canCancel = orderStatus.uppercase() in listOf("PENDING", "TO_PAY", "TOPAY", "PAYMENT_RECEIVED", "TO_SHIP", "TOSHIP", "PROCESSING") &&
            !orderStatus.equals("CANCELLED", ignoreCase = true) &&
            !orderStatus.equals("COMPLETED", ignoreCase = true)

    val canTrack = orderStatus.uppercase() in listOf("SHIPPED", "SHIPPING", "TO_RECEIVE", "TORECEIVE", "IN_TRANSIT", "OUT_FOR_DELIVERY", "TO_DELIVER")

    return DialogDetails(
        title = "Order #${order.orderId.takeLast(8)}",
        status = orderStatus,
        timestamp = order.timestamp.toDate().toString(),
        detailsContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Action Buttons at the top
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (canCancel) {
                        OutlinedButton(
                            onClick = { showCancelDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color(0xFFF44336)
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336)),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Cancel,
                                contentDescription = "Cancel",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Cancel", fontSize = 13.sp)
                        }
                    }

                    if (canTrack) {
                        Button(
                            onClick = { showTrackingDialog = true },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocalShipping,
                                contentDescription = "Track",
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Track", fontSize = 13.sp)
                        }
                    }
                }

                Divider(color = Color(0xFFEEEEEE))
                Spacer(modifier = Modifier.height(16.dp))

                // Order Status Timeline
                SectionHeader("Order Status")
                OrderStatusTimeline(
                    currentStatus = orderStatus,
                    orderId = order.orderId,
                    primaryColor = primaryColor,
                    modifier = Modifier.fillMaxWidth(),
                    showActions = false,
                    onStatusUpdated = {
                        orderStatus = "CANCELLED"
                        onDismiss()
                    }
                )


                // Cancel Order Dialog
                if (showCancelDialog) {
                    CancelOrderDialog(
                        orderId = order.orderId,
                        isProcessing = isProcessing,
                        onConfirm = { reason ->
                            scope.launch {
                                isProcessing = true
                                try {
                                    com.project.webapp.components.delivery.cancelOrder(
                                        orderId = order.orderId,
                                        reason = reason,
                                        onSuccess = {
                                            Toast.makeText(context, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
                                            showCancelDialog = false
                                            orderStatus = "CANCELLED"
                                            onDismiss()
                                        },
                                        onError = { error ->
                                            Toast.makeText(context, "Failed to cancel: ${error.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                } finally {
                                    isProcessing = false
                                }
                            }
                        },
                        onDismiss = { showCancelDialog = false }
                    )
                }

                // Tracking Dialog
                if (showTrackingDialog) {
                    TrackingInfoDialog(
                        orderId = order.orderId,
                        onDismiss = { showTrackingDialog = false },
                        primaryColor = primaryColor
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Order Items
                SectionHeader("Order Items (${order.items.size})")
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        order.items.forEachIndexed { index, item ->
                            OrderItemRow(item, primaryColor)
                            if (index < order.items.size - 1) {
                                Divider(
                                    color = Color.LightGray,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Payment Information
                SectionHeader("Payment Information")
                InfoCard {
                    val shippingFee = 50.0
                    val subtotal = order.totalAmount - shippingFee

                    InfoRow("Payment Method", order.paymentMethod)
                    InfoRow("Order Date", formatDate(order.timestamp.toDate()))
                    InfoRow("Items Subtotal", "₱${String.format("%.2f", subtotal)}")
                    InfoRow("Shipping Fee", "₱${String.format("%.2f", shippingFee)}")

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "₱${String.format("%.2f", order.totalAmount)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Shipping Information
                SectionHeader("Shipping Address")
                InfoCard {
                    Text(
                        text = order.deliveryAddress,
                        fontSize = 14.sp,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    )
}

@Composable
private fun createDonationDialogDetails(transaction: Transaction): DialogDetails {
    return DialogDetails(
        title = "Donation",
        status = transaction.status,
        timestamp = transaction.timestamp.toDate().toString(),
        detailsContent = {
            Column(modifier = Modifier.fillMaxWidth()) {
                SectionHeader("Donation Details")
                InfoCard {
                    InfoRow("Item", transaction.item)
                    InfoRow("Quantity", transaction.quantity.toString())
                    InfoRow("Organization", transaction.organization ?: "Unknown")
                    InfoRow("Date", formatDate(transaction.timestamp.toDate()))
                    InfoRow("Payment Method", transaction.paymentMethod)
                    if (!transaction.referenceId.isNullOrEmpty()) {
                        InfoRow("Reference ID", transaction.referenceId)
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(8.dp))

                    InfoRow(
                        "Total Amount",
                        "₱${String.format("%.2f", transaction.totalAmount)}",
                        valueColor = Color(0xFF0DA54B),
                        valueFontWeight = FontWeight.Bold
                    )
                }
            }
        }
    )
}

// Helper Composables
@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        color = Color.DarkGray
    )
    Spacer(modifier = Modifier.height(8.dp))
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.DarkGray,
    valueFontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontSize = 14.sp,
            color = valueColor,
            fontWeight = valueFontWeight,
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.End
        )
    }
}

@Composable
private fun OrderItemRow(item: Map<String, Any>, primaryColor: Color) {
    val name = item["name"] as? String ?: "Unnamed Product"
    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
    val quantity = (item["quantity"] as? Number)?.toInt() ?: 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(50.dp)
                .background(Color(0xFFEDF7F0), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ShoppingBag,
                contentDescription = null,
                tint = primaryColor,
                modifier = Modifier.size(28.dp)
            )
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
}

// Utility Functions
private fun filterOrdersByStatus(orders: List<OrderItem>, status: OrderStatus): List<OrderItem> {
    if (status == OrderStatus.ALL) return orders

    return orders.filter { item ->
        val itemStatus = when (item) {
            is OrderItem.Purchase -> item.order.status
            is OrderItem.Donation -> item.transaction.status
        }
        matchesStatus(itemStatus, status)
    }
}

private fun matchesStatus(itemStatus: String, filterStatus: OrderStatus): Boolean {
    return when (filterStatus) {
        OrderStatus.TO_PAY -> itemStatus.uppercase() in listOf("PENDING", "TO_PAY", "TOPAY")
        OrderStatus.TO_SHIP -> itemStatus.uppercase() in listOf("PROCESSING", "TO_SHIP", "TOSHIP", "PREPARING")
        OrderStatus.TO_RECEIVE -> itemStatus.uppercase() in listOf("SHIPPED", "TO_RECEIVE", "TORECEIVE", "IN_TRANSIT", "OUT_FOR_DELIVERY")
        OrderStatus.COMPLETED -> itemStatus.uppercase() in listOf("COMPLETED", "DELIVERED", "COMPLETE")
        OrderStatus.CANCELLED -> itemStatus.uppercase() in listOf("CANCELLED", "CANCELED")
        OrderStatus.RETURN_REFUND -> itemStatus.uppercase() in listOf("RETURNED", "REFUNDED", "RETURN", "REFUND")
        OrderStatus.ALL -> true
    }
}

private fun getOrderCountByStatus(orders: List<OrderItem>, status: OrderStatus): Int {
    return filterOrdersByStatus(orders, status).size
}

private fun getStatusColor(status: String, primaryColor: Color): Color {
    return when (status.uppercase()) {
        "COMPLETED", "DELIVERED", "COMPLETE" -> primaryColor
        "PROCESSING", "PREPARING", "TO_SHIP", "TOSHIP" -> Color(0xFFFF9800)
        "SHIPPED", "TO_RECEIVE", "TORECEIVE", "IN_TRANSIT", "OUT_FOR_DELIVERY" -> Color(0xFF2196F3)
        "PENDING", "TO_PAY", "TOPAY" -> Color(0xFFFFC107)
        "CANCELLED", "CANCELED" -> Color(0xFFF44336)
        "RETURNED", "REFUNDED", "RETURN", "REFUND" -> Color(0xFF9C27B0)
        else -> Color.Gray
    }
}

private fun getStatusDisplayText(status: String): String {
    return when (status.uppercase()) {
        "COMPLETED", "COMPLETE" -> "Completed"
        "DELIVERED" -> "Delivered"
        "PROCESSING" -> "Processing"
        "PREPARING" -> "Preparing"
        "TO_SHIP", "TOSHIP" -> "To Ship"
        "SHIPPED" -> "Shipped"
        "TO_RECEIVE", "TORECEIVE" -> "To Receive"
        "IN_TRANSIT" -> "In Transit"
        "OUT_FOR_DELIVERY" -> "Out for Delivery"
        "PENDING" -> "Pending Payment"
        "TO_PAY", "TOPAY" -> "To Pay"
        "CANCELLED", "CANCELED" -> "Cancelled"
        "RETURNED" -> "Returned"
        "REFUNDED" -> "Refunded"
        "RETURN" -> "Return"
        "REFUND" -> "Refund"
        else -> status.replaceFirstChar { it.uppercase() }
    }
}

private fun getEmptyStateMessage(status: OrderStatus): String {
    return when (status) {
        OrderStatus.ALL -> "Start shopping or donating to see your orders here!"
        OrderStatus.TO_PAY -> "You don't have any orders waiting for payment."
        OrderStatus.TO_SHIP -> "No orders are being prepared for shipment."
        OrderStatus.TO_RECEIVE -> "No orders are currently on the way to you."
        OrderStatus.COMPLETED -> "You haven't completed any orders yet."
        OrderStatus.CANCELLED -> "You don't have any cancelled orders."
        OrderStatus.RETURN_REFUND -> "No returns or refunds have been processed."
    }
}

private fun formatDate(date: Date): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    return sdf.format(date)
}

// Data class for tuple
private data class Tuple6<A, B, C, D, E, F>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D,
    val fifth: E,
    val sixth: F
)

// Deprecated: Keep for backward compatibility
@Composable
private fun ReceiptRow(label: String, value: String) {
    InfoRow(label, value)
}