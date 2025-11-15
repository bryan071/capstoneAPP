package com.project.webapp.components.payment

import DialogDetails
import Order
import OrderItemDetails
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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

/* -------------------------------------------------------------------------
   ENUMS & HELPERS
   ------------------------------------------------------------------------- */
enum class OrderStatus(val displayName: String, val icon: ImageVector) {
    ALL("All", Icons.Default.ShoppingBag),
    COMPLETED("Completed", Icons.Default.CheckCircle),
    CANCELLED("Cancelled", Icons.Default.Cancel),
    RETURN_REFUND("Return/Refund", Icons.Default.Refresh)
}

private fun getOrderStatusColor(status: String): Color = when (status.uppercase()) {
    "TO PAY", "PAYMENT PENDING", "PENDING", "TOPAY" -> Color(0xFFFFC107)
    "TO SHIP", "TOSHIP", "PROCESSING", "PREPARING" -> Color(0xFFFF9800)
    "TO DELIVER", "TODELIVER" -> Color(0xFF9C27B0)
    "TO RECEIVE", "TORECEIVE", "SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY" -> Color(0xFF2196F3)
    "COMPLETED", "COMPLETE", "DELIVERED" -> Color(0xFF4CAF50)
    "CANCELLED", "CANCELED" -> Color(0xFFF44336)
    else -> Color.Gray
}

private fun getStatusDisplayText(status: String): String = when (status.uppercase()) {
    "TO PAY", "PAYMENT PENDING", "PENDING", "TOPAY" -> "To Pay"
    "TO SHIP", "TOSHIP", "PROCESSING", "PREPARING" -> "To Ship"
    "TO DELIVER", "TODELIVER" -> "To Deliver"
    "TO RECEIVE", "TORECEIVE", "SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY" -> "To Receive"
    "COMPLETED", "COMPLETE", "DELIVERED" -> "Completed"
    "CANCELLED", "CANCELED" -> "Cancelled"
    else -> status.replaceFirstChar { it.uppercase() }
}

private fun getStatusIcon(status: String): ImageVector = when (status.uppercase()) {
    "TO PAY", "PAYMENT PENDING" -> Icons.Default.AccountBalanceWallet
    "TO SHIP" -> Icons.Default.Inventory
    "TO DELIVER" -> Icons.Default.LocalShipping
    "TO RECEIVE" -> Icons.Default.CheckCircle
    "COMPLETED" -> Icons.Default.CheckCircle
    "CANCELLED" -> Icons.Default.Cancel
    else -> Icons.Default.Schedule
}

/* -------------------------------------------------------------------------
   MAIN SCREEN
   ------------------------------------------------------------------------- */
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

    /* --------------------------------------------------------------
       REAL-TIME SYNC:
       1. orders (buyer side)
       2. notifications (purchase_confirmed) → latest status
       3. donations
       -------------------------------------------------------------- */
    LaunchedEffect(currentUserId) {
        if (currentUserId == null) {
            isLoading = false
            return@LaunchedEffect
        }

        // ---- 1. orders -------------------------------------------------
        firestore.collection("orders")
            .whereEqualTo("buyerId", currentUserId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snap, err ->
                if (err != null) {
                    Log.e("Orders", "orders error", err)
                    return@addSnapshotListener
                }

                val purchases = snap?.documents?.mapNotNull { doc ->
                    try {
                        val status = doc.getString("status") ?: "To Pay"

                        // CRITICAL FIX: Skip orders with "PAYMENT_RECEIVED" status
                        // These are old incomplete orders that should not be shown
                        if (status.equals("PAYMENT_RECEIVED", ignoreCase = true)) {
                            Log.d("Orders", "⚠️ Skipping old PAYMENT_RECEIVED order: ${doc.id}")
                            return@mapNotNull null
                        }

                        doc.toObject(Order::class.java)?.copy(
                            orderId = doc.id,
                            status = status
                        )?.let { OrderItem.Purchase(it) }
                    } catch (e: Exception) {
                        Log.e("Orders", "parse order ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                // ---- 2. notifications (buyer) ---------------------------------
                firestore.collection("notifications")
                    .whereEqualTo("userId", currentUserId)
                    .whereEqualTo("type", "purchase_confirmed")
                    .addSnapshotListener { nSnap, nErr ->
                        if (nErr != null) {
                            Log.e("Orders", "notif error", nErr)
                            return@addSnapshotListener
                        }

                        val notifMap = nSnap?.documents?.associateBy { it.getString("orderId") ?: "" } ?: emptyMap()

                        // Merge latest status from notification
                        val merged = purchases.map { purchase ->
                            val notif = notifMap[purchase.order.orderId]
                            val latestStatus = notif?.getString("orderStatus") ?: purchase.order.status
                            val latestPayment = notif?.getString("paymentStatus") ?: "Payment Pending"

                            purchase.copy(
                                order = purchase.order.copy(
                                    status = latestStatus,
                                    paymentStatus = latestPayment
                                )
                            )
                        }

                        // ---- 3. donations -----------------------------------------
                        firestore.collection("transactions")
                            .whereEqualTo("buyerId", currentUserId)
                            .whereEqualTo("transactionType", "donation")
                            .addSnapshotListener { tSnap, tErr ->
                                isLoading = false
                                if (tErr != null) {
                                    Log.e("Orders", "donations error", tErr)
                                    return@addSnapshotListener
                                }

                                val donations = tSnap?.documents?.mapNotNull { doc ->
                                    val d = doc.data ?: return@mapNotNull null
                                    Transaction(
                                        id = doc.id,
                                        buyerId = d["buyerId"] as? String ?: "",
                                        item = d["item"] as? String ?: "",
                                        quantity = (d["quantity"] as? Long)?.toInt() ?: 0,
                                        totalAmount = (d["totalAmount"] as? Double) ?: 0.0,
                                        organization = d["organization"] as? String,
                                        transactionType = d["transactionType"] as? String ?: "",
                                        status = d["status"] as? String ?: "",
                                        timestamp = doc.getTimestamp("timestamp") ?: Timestamp.now(),
                                        paymentMethod = d["paymentMethod"] as? String ?: "",
                                        referenceId = d["referenceId"] as? String
                                    )
                                }?.map { OrderItem.Donation(it) } ?: emptyList()

                                // Final list (PAYMENT_RECEIVED orders are excluded)
                                orderItems = (merged + donations).sortedByDescending {
                                    when (it) {
                                        is OrderItem.Purchase -> it.order.timestamp
                                        is OrderItem.Donation -> it.transaction.timestamp
                                    }
                                }

                                Log.d("OrdersScreen", "✓ Synced ${orderItems.size} orders (PAYMENT_RECEIVED excluded)")
                            }
                    }
            }
    }


    // -----------------------------------------------------------------
    // UI
    // -----------------------------------------------------------------
    val content: @Composable (PaddingValues) -> Unit = { pv ->
        OrdersContent(
            paddingValues = pv,
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

    if (showScaffold) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "My Orders",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.Default.ArrowBack, "Back", tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = primaryColor)
                )
            },
            containerColor = Color(0xFFF5F5F5),
            content = { content(it) }
        )
    } else {
        content(PaddingValues())
    }

    // -----------------------------------------------------------------
    // Dialog
    // -----------------------------------------------------------------
    selectedItem?.let {
        OrderDetailsDialog(
            item = it,
            onDismiss = { selectedItem = null },
            primaryColor = primaryColor,
            navController = navController,
            chatViewModel = chatViewModel
        )
    }
}

/* -------------------------------------------------------------------------
   CONTENT (tabs + list)
   ------------------------------------------------------------------------- */
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
    Column(modifier = modifier.padding(paddingValues).fillMaxSize()) {
        OrderStatusTabs(
            selectedStatus = selectedStatus,
            onStatusSelected = onStatusSelected,
            orderItems = orderItems,
            primaryColor = primaryColor
        )

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading -> CircularProgressIndicator(
                    color = primaryColor,
                    modifier = Modifier.size(50.dp).align(Alignment.Center)
                )
                filterOrdersByStatus(orderItems, selectedStatus).isEmpty() -> EmptyOrdersState(
                    status = selectedStatus,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> {
                    val filtered = filterOrdersByStatus(orderItems, selectedStatus)
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered.size) { idx ->
                            OrderItemCard(
                                orderItem = filtered[idx],
                                onClick = { onItemSelected(filtered[idx]) },
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
private fun EmptyOrdersState(status: OrderStatus, modifier: Modifier = Modifier) {
    Column(modifier = modifier.padding(32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            status.icon,
            contentDescription = null,
            tint = Color.LightGray,
            modifier = Modifier.size(80.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No ${status.displayName} Orders",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )
        Spacer(Modifier.height(8.dp))
        Text(
            getEmptyStateMessage(status),
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
private fun getEmptyStateMessage(status: OrderStatus) = when (status) {
    OrderStatus.ALL -> "Start shopping or donating to see your orders here!"
    OrderStatus.COMPLETED -> "You haven't completed any orders yet."
    OrderStatus.CANCELLED -> "You don't have any cancelled orders."
    OrderStatus.RETURN_REFUND -> "No returns or refunds have been processed."
}
/* -------------------------------------------------------------------------
   TABS
   ------------------------------------------------------------------------- */
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
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(OrderStatus.values().size) { i ->
                val s = OrderStatus.values()[i]
                val cnt = getOrderCountByStatus(orderItems, s)
                OrderStatusChip(
                    status = s,
                    count = cnt,
                    isSelected = selectedStatus == s,
                    onClick = { onStatusSelected(s) },
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
    val bg = if (isSelected) primaryColor else Color(0xFFF5F5F5)
    val fg = if (isSelected) Color.White else Color.DarkGray

    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = bg),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(if (isSelected) 4.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(status.icon, null, tint = fg, modifier = Modifier.size(18.dp))
            Text(status.displayName, color = fg, fontSize = 14.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            if (count > 0 && status != OrderStatus.ALL) {
                Text("($count)", color = fg, fontSize = 12.sp)
            }
        }
    }
}

/* -------------------------------------------------------------------------
   CARD (list item)
   ------------------------------------------------------------------------- */
private data class OrderCardInfo(
    val title: String,
    val subtitle: String,
    val status: String,
    val timestamp: Timestamp,
    val quantity: Int,
    val totalAmount: Double
)

@Composable
private fun OrderItemCard(
    orderItem: OrderItem,
    onClick: () -> Unit,
    primaryColor: Color
) {
    var previewName by remember { mutableStateOf("Loading...") }
    var previewQty by remember { mutableStateOf(0) }
    var previewTotal by remember { mutableStateOf(0.0) }
    var itemCount by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }

    if (orderItem is OrderItem.Purchase) {
        val orderId = orderItem.order.orderId

        Log.d("OrderItemCard", "========================================")
        Log.d("OrderItemCard", "Loading order: $orderId")

        LaunchedEffect(orderId) {
            FirebaseFirestore.getInstance()
                .collection("orders").document(orderId)
                .collection("order_items")
                .addSnapshotListener { snap, err ->
                    if (err != null) {
                        Log.e("OrderItemCard", "❌ Error loading subcollection", err)
                        isLoading = false
                        return@addSnapshotListener
                    }

                    // ✅ NEW SYSTEM: Has order_items subcollection
                    if (snap != null && !snap.isEmpty) {
                        val allItems = snap.documents.mapNotNull { it.data }
                        itemCount = allItems.size

                        val firstItem = allItems.firstOrNull()
                        if (firstItem != null) {
                            previewName = firstItem["name"] as? String ?: "Order"
                            previewQty = (firstItem["quantity"] as? Number)?.toInt() ?: 1

                            val itemsTotal = allItems.sumOf {
                                val qty = (it["quantity"] as? Number)?.toInt() ?: 0
                                val price = (it["price"] as? Number)?.toDouble() ?: 0.0
                                price * qty
                            }
                            previewTotal = itemsTotal + 50.0
                        }

                        Log.d("OrderItemCard", "✓ Loaded $itemCount items")
                        isLoading = false
                        return@addSnapshotListener
                    }

                    // ⚠️ Empty subcollection - Try old system
                    Log.w("OrderItemCard", "⚠️ Empty subcollection, trying fallback...")

                    FirebaseFirestore.getInstance()
                        .collection("orders").document(orderId)
                        .get()
                        .addOnSuccessListener { doc ->
                            isLoading = false

                            if (!doc.exists()) {
                                previewName = "Order not found"
                                return@addOnSuccessListener
                            }

                            // Try items array field
                            val itemsArray = doc.get("items") as? List<Map<String, Any>>

                            if (!itemsArray.isNullOrEmpty()) {
                                itemCount = itemsArray.size
                                val firstItem = itemsArray.firstOrNull()

                                if (firstItem != null) {
                                    previewName = firstItem["name"] as? String ?: "Order"
                                    previewQty = (firstItem["quantity"] as? Number)?.toInt() ?: 1
                                    previewTotal = doc.getDouble("totalAmount") ?: 0.0
                                }

                                Log.d("OrderItemCard", "✓ Loaded from items field")
                            } else {
                                // This order has no items - it shouldn't be displayed
                                previewName = "Invalid order"
                                itemCount = 0
                                Log.e("OrderItemCard", "✗ Order has no items!")
                            }
                        }
                        .addOnFailureListener { e ->
                            isLoading = false
                            Log.e("OrderItemCard", "✗ Failed to load order", e)
                            previewName = "Error loading"
                        }
                }
        }
    }

    val info = when (orderItem) {
        is OrderItem.Purchase -> {
            val extra = if (itemCount > 1) "+${itemCount - 1} more items" else ""
            OrderCardInfo(
                title = if (isLoading) "Loading..." else previewName,
                subtitle = extra,
                status = orderItem.order.status,
                timestamp = orderItem.order.timestamp,
                quantity = previewQty,
                totalAmount = previewTotal
            )
        }
        is OrderItem.Donation -> {
            val t = orderItem.transaction
            OrderCardInfo(
                t.item,
                "Donation to ${t.organization ?: "Organization"}",
                t.status,
                t.timestamp,
                t.quantity,
                t.totalAmount
            )
        }
    }

    val statusColor = getOrderStatusColor(info.status)

    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = if (orderItem is OrderItem.Purchase) Icons.Default.ShoppingBag else Icons.Default.Favorite,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (orderItem is OrderItem.Purchase) "Purchase Order" else "Donation",
                            fontSize = 13.sp,
                            color = Color.Gray,
                            fontWeight = FontWeight.Medium
                        )
                        if (orderItem is OrderItem.Purchase) {
                            Text(
                                text = "#${orderItem.order.orderId.takeLast(8)}",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }
                    }
                }
                Surface(color = statusColor, shape = RoundedCornerShape(8.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = getStatusIcon(info.status),
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            getStatusDisplayText(info.status),
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier.size(70.dp).background(Color(0xFFF8F8F8), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            color = primaryColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Icon(Icons.Default.ShoppingBag, null, tint = primaryColor, modifier = Modifier.size(32.dp))
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        info.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (info.subtitle.isNotEmpty()) {
                        Spacer(Modifier.height(4.dp))
                        Text(info.subtitle, fontSize = 13.sp, color = Color.Gray)
                    }
                    Spacer(Modifier.height(4.dp))
                    Text("Items: $itemCount", fontSize = 13.sp, color = Color.Gray)
                }
                Icon(Icons.Default.KeyboardArrowRight, "Details", tint = Color.Gray, modifier = Modifier.size(24.dp))
            }

            Spacer(Modifier.height(12.dp))
            Divider(color = Color(0xFFEEEEEE))
            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(formatDate(info.timestamp.toDate()), fontSize = 12.sp, color = Color.Gray)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Total: ", fontSize = 13.sp, color = Color.Gray)
                    if (isLoading) {
                        Text("...", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    } else {
                        Text(
                            "₱${String.format("%.2f", info.totalAmount)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }
}

/* -------------------------------------------------------------------------
   DIALOG – LIVE STATUS FROM NOTIFICATION
   ------------------------------------------------------------------------- */
@Composable
fun OrderDetailsDialog(
    item: OrderItem,
    onDismiss: () -> Unit,
    primaryColor: Color,
    navController: NavController,
    chatViewModel: com.project.webapp.Viewmodel.ChatViewModel
) {
    // Live status from notification (same as before)
    var liveStatus by remember { mutableStateOf("") }
    var livePayment by remember { mutableStateOf("Payment Pending") }

    if (item is OrderItem.Purchase) {
        val orderId = item.order.orderId
        val firestore = FirebaseFirestore.getInstance()

        LaunchedEffect(orderId) {
            firestore.collection("notifications")
                .whereEqualTo("orderId", orderId)
                .whereEqualTo("type", "purchase_confirmed")
                .addSnapshotListener { snap, err ->
                    if (err != null || snap == null) return@addSnapshotListener
                    val doc = snap.documents.firstOrNull() ?: return@addSnapshotListener
                    liveStatus = doc.getString("orderStatus") ?: "To Pay"
                    livePayment = doc.getString("paymentStatus") ?: "Payment Pending"
                }
        }
    } else {
        liveStatus = (item as OrderItem.Donation).transaction.status
    }

    val statusToShow = if (item is OrderItem.Purchase) liveStatus else (item as OrderItem.Donation).transaction.status
    val statusColor = getOrderStatusColor(statusToShow)

    // -------------------------------------------------------------
    // NEW: Load real order items from sub-collection
    // -------------------------------------------------------------
    var realItems by remember { mutableStateOf<List<Map<String, Any>>?>(null) }
    var itemsLoading by remember { mutableStateOf(true) }

    if (item is OrderItem.Purchase) {
        val orderId = item.order.orderId
        LaunchedEffect(orderId) {
            FirebaseFirestore.getInstance()
                .collection("orders").document(orderId)
                .collection("order_items")
                .orderBy("name")
                .addSnapshotListener { snap, err ->
                    itemsLoading = false
                    if (err != null) {
                        Log.e("OrderDialog", "order_items error", err)
                        realItems = emptyList()
                        return@addSnapshotListener
                    }
                    realItems = snap?.documents?.mapNotNull { it.data } ?: emptyList()
                }
        }
    }

    val details = when (item) {
        is OrderItem.Purchase -> purchaseDialogDetails(
            order = item.order,
            liveStatus = liveStatus,
            livePayment = livePayment,
            realItems = realItems ?: emptyList(),
            itemsLoading = itemsLoading,
            primaryColor = primaryColor,
            onDismiss = onDismiss,
            navController = navController,
            chatViewModel = chatViewModel
        )
        is OrderItem.Donation -> donationDialogDetails(item.transaction)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (item is OrderItem.Purchase) Icons.Default.ShoppingBag else Icons.Default.Favorite,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(details.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.DarkGray)
                    Surface(color = statusColor, shape = RoundedCornerShape(8.dp)) {
                        Text(
                            getStatusDisplayText(statusToShow),
                            fontSize = 13.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().heightIn(max = 500.dp).verticalScroll(rememberScrollState())
            ) {
                // Timeline (unchanged)
                if (item is OrderItem.Purchase) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Order Status Timeline",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = primaryColor
                            )
                            Spacer(Modifier.height(12.dp))
                            OrderStatusTimeline(
                                currentStatus = liveStatus,
                                paymentStatus = livePayment,
                                primaryColor = statusColor,
                                showActions = true
                            )
                        }
                    }
                }

                details.detailsContent()
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) { Text("Close", fontSize = 16.sp, fontWeight = FontWeight.SemiBold) }
        }
    )
}

/* -------------------------------------------------------------------------
   PURCHASE DIALOG (uses live status)
   ------------------------------------------------------------------------- */
@Composable
private fun purchaseDialogDetails(
    order: Order,
    liveStatus: String,
    livePayment: String,
    realItems: List<Map<String, Any>>,
    itemsLoading: Boolean,
    primaryColor: Color,
    onDismiss: () -> Unit,
    navController: NavController,
    chatViewModel: com.project.webapp.Viewmodel.ChatViewModel
): DialogDetails {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCancel by remember { mutableStateOf(false) }
    var showTrack by remember { mutableStateOf(false) }
    var processing by remember { mutableStateOf(false) }

    val canCancel = liveStatus.uppercase() in listOf(
        "PENDING", "TO_PAY", "TOPAY", "PAYMENT_RECEIVED",
        "TO_SHIP", "TOSHIP", "PROCESSING"
    ) && !liveStatus.equals("CANCELLED", ignoreCase = true)
            && !liveStatus.equals("COMPLETED", ignoreCase = true)

    val canTrack = liveStatus.uppercase() in listOf(
        "SHIPPED", "SHIPPING",
        "TO_DELIVER", "TODELIVER",
        "TO_RECEIVE", "TORECEIVE",
        "IN_TRANSIT", "OUT_FOR_DELIVERY"
    )

    // Compute totals from REAL items (fallback to order.totalAmount if empty)
    val itemsTotal = realItems.sumOf { (it["price"] as? Number)?.toDouble()?.times((it["quantity"] as? Number)?.toInt() ?: 1) ?: 0.0 }
    val shippingFee = 50.0
    val displayTotal = if (realItems.isEmpty()) order.totalAmount else itemsTotal + shippingFee

    return DialogDetails(
        title = "Order #${order.orderId.takeLast(8)}",
        status = liveStatus,
        timestamp = order.timestamp.toDate().toString(),
        detailsContent = {
            Column(Modifier.fillMaxWidth()) {
                OrderActions(
                    canCancel = canCancel,
                    canTrack = canTrack,
                    onCancel = { showCancel = true },
                    onTrack = { showTrack = true },
                    primaryColor = primaryColor
                )

                Divider(color = Color(0xFFEEEEEE))
                Spacer(Modifier.height(16.dp))

                SectionHeader("Order Items (${realItems.size})")
                if (itemsLoading) {
                    Box(Modifier.fillMaxWidth().height(80.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = primaryColor, strokeWidth = 2.dp)
                    }
                } else if (realItems.isEmpty()) {
                    Text("No items found", color = Color.Gray, modifier = Modifier.padding(16.dp))
                } else {
                    Card(colors = CardDefaults.cardColors(Color(0xFFF8F8F8)), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(12.dp)) {
                            realItems.forEachIndexed { i, it ->
                                OrderItemRow(it, primaryColor)
                                if (i < realItems.size - 1) Divider(color = Color.LightGray, modifier = Modifier.padding(vertical = 8.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionHeader("Payment Information")
                InfoCard {
                    InfoRow("Payment Method", order.paymentMethod)
                    InfoRow("Order Date", formatDate(order.timestamp.toDate()))
                    InfoRow("Items Subtotal", "₱${String.format("%.2f", itemsTotal)}")
                    InfoRow("Shipping Fee", "₱${String.format("%.2f", shippingFee)}")
                    Spacer(Modifier.height(8.dp))
                    Divider()
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        Text(
                            "₱${String.format("%.2f", displayTotal)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionHeader("Shipping Address")
                InfoCard { Text(order.deliveryAddress, fontSize = 14.sp, lineHeight = 20.sp) }

                // Cancel / Track dialogs (unchanged)
                if (showCancel) {
                    CancelOrderDialog(
                        orderId = order.orderId,
                        isProcessing = processing,
                        onConfirm = { reason ->
                            scope.launch {
                                processing = true
                                try {
                                    com.project.webapp.components.delivery.cancelOrder(
                                        orderId = order.orderId,
                                        reason = reason,
                                        onSuccess = {
                                            Toast.makeText(context, "Order cancelled", Toast.LENGTH_SHORT).show()
                                            showCancel = false
                                            onDismiss()
                                        },
                                        onError = { Toast.makeText(context, "Cancel failed: ${it.message}", Toast.LENGTH_SHORT).show() }
                                    )
                                } finally { processing = false }
                            }
                        },
                        onDismiss = { showCancel = false }
                    )
                }

                if (showTrack) {
                    TrackingInfoDialog(
                        orderId = order.orderId,
                        onDismiss = { showTrack = false },
                        primaryColor = primaryColor
                    )
                }
            }
        }
    )
}

/* -------------------------------------------------------------------------
   DONATION DIALOG (unchanged)
   ------------------------------------------------------------------------- */
@Composable
private fun donationDialogDetails(t: Transaction): DialogDetails = DialogDetails(
    title = "Donation",
    status = t.status,
    timestamp = t.timestamp.toDate().toString(),
    detailsContent = {
        Column(Modifier.fillMaxWidth()) {
            SectionHeader("Donation Details")
            InfoCard {
                InfoRow("Item", t.item)
                InfoRow("Quantity", t.quantity.toString())
                InfoRow("Organization", t.organization ?: "Unknown")
                InfoRow("Date", formatDate(t.timestamp.toDate()))
                InfoRow("Payment Method", t.paymentMethod)
                if (!t.referenceId.isNullOrEmpty()) InfoRow("Reference ID", t.referenceId)

                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
                InfoRow(
                    "Total Amount",
                    "₱${String.format("%.2f", t.totalAmount)}",
                    valueColor = Color(0xFF0DA54B),
                    valueFontWeight = FontWeight.Bold
                )
            }
        }
    }
)

/* -------------------------------------------------------------------------
   RE-USABLE SMALL COMPOSABLES
   ------------------------------------------------------------------------- */
@Composable
private fun OrderActions(
    canCancel: Boolean,
    canTrack: Boolean,
    onCancel: () -> Unit,
    onTrack: () -> Unit,
    primaryColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (canCancel) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                border = BorderStroke(1.dp, Color(0xFFF44336)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Cancel, "Cancel", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Cancel", fontSize = 13.sp)
            }
        }
        if (canTrack) {
            Button(
                onClick = onTrack,
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.LocalShipping, "Track", modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Track", fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(text, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.DarkGray)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun InfoCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        colors = CardDefaults.cardColors(Color(0xFFF8F8F8)),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) { Column(Modifier.padding(16.dp), content = content) }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    valueColor: Color = Color.DarkGray,
    valueFontWeight: FontWeight = FontWeight.Normal
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.Gray, modifier = Modifier.weight(1f))
        Text(
            value,
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
    val name = item["name"] as? String ?: "Unnamed"
    val price = (item["price"] as? Number)?.toDouble() ?: 0.0
    val qty = (item["quantity"] as? Number)?.toInt() ?: 1

    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
        Box(
            modifier = Modifier.size(50.dp).background(Color(0xFFEDF7F0), RoundedCornerShape(6.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.ShoppingBag, null, tint = primaryColor, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Qty: $qty", fontSize = 13.sp, color = Color.Gray)
        }
        Text("₱${String.format("%.2f", price * qty)}", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.DarkGray)
    }
}

/* -------------------------------------------------------------------------
   FILTERING / COUNTING
   ------------------------------------------------------------------------- */
private fun filterOrdersByStatus(orders: List<OrderItem>, status: OrderStatus): List<OrderItem> {
    if (status == OrderStatus.ALL) return orders
    return orders.filter { matchesStatus(statusOf(it), status) }
}

private fun statusOf(item: OrderItem) = when (item) {
    is OrderItem.Purchase -> item.order.status
    is OrderItem.Donation -> item.transaction.status
}

private fun matchesStatus(itemStatus: String, filter: OrderStatus): Boolean = when (filter) {
    OrderStatus.COMPLETED -> itemStatus.uppercase() in listOf("COMPLETED", "COMPLETE", "DELIVERED")
    OrderStatus.CANCELLED -> itemStatus.uppercase() in listOf("CANCELLED", "CANCELED")
    OrderStatus.RETURN_REFUND -> itemStatus.uppercase() in listOf("RETURNED", "REFUNDED", "RETURN", "REFUND")
    OrderStatus.ALL -> true
}

private fun getOrderCountByStatus(orders: List<OrderItem>, status: OrderStatus) =
    filterOrdersByStatus(orders, status).size

/* -------------------------------------------------------------------------
   DATE FORMATTER
   ------------------------------------------------------------------------- */
private fun formatDate(date: Date): String =
    SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault()).format(date)