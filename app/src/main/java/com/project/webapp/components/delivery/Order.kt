package com.project.webapp.components.payment

import DialogDetails
import OrderItemDetails
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingBag
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.webapp.Viewmodel.OrderItem
import com.project.webapp.datas.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    showScaffold: Boolean = true
) {
    val primaryColor = Color(0xFF0DA54B)
    val firestore = FirebaseFirestore.getInstance()
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    var orderItems by remember { mutableStateOf<List<OrderItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedItem by remember { mutableStateOf<OrderItem?>(null) }

    LaunchedEffect(currentUserId) {
        if (currentUserId != null) {
            // Fetch purchases from orders
            firestore.collection("orders")
                .whereEqualTo("buyerId", currentUserId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    if (error != null) {
                        Log.e("Orders", "Error fetching orders", error)
                        return@addSnapshotListener
                    }

                    val orders = snapshot?.documents?.mapNotNull { doc ->
                        doc.toObject(Order::class.java)
                    }?.map { OrderItem.Purchase(it) } ?: emptyList()

                    // Fetch donations from transactions
                    firestore.collection("transactions")
                        .whereEqualTo("buyerId", currentUserId)
                        .whereEqualTo("transactionType", "donation")
                        .orderBy("timestamp", Query.Direction.DESCENDING)
                        .addSnapshotListener { transactionSnapshot, transactionError ->
                            isLoading = false
                            if (transactionError != null) {
                                Log.e("Orders", "Error fetching donation transactions", transactionError)
                                return@addSnapshotListener
                            }

                            val donationTransactions = transactionSnapshot?.documents?.mapNotNull { doc ->
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
                                    timestamp = data["timestamp"] as? Long ?: 0,
                                    paymentMethod = data["paymentMethod"] as? String ?: "",
                                    referenceId = data["referenceId"] as? String
                                )
                            }?.map { OrderItem.Donation(it) } ?: emptyList()

                            orderItems = (orders + donationTransactions).sortedByDescending {
                                when (it) {
                                    is OrderItem.Purchase -> it.order.createdAt
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
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingBag,
                                contentDescription = "Orders",
                                tint = primaryColor,
                                modifier = Modifier.size(32.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "My Orders & Donations",
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
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.White
                    )
                )
            },
            containerColor = Color(0xFFF7FAF9)
        ) { paddingValues ->
            OrdersContent(
                paddingValues = paddingValues,
                isLoading = isLoading,
                orderItems = orderItems,
                selectedItem = selectedItem,
                onItemSelected = { selectedItem = it },
                navController = navController,
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
            onItemSelected = { selectedItem = it },
            navController = navController,
            primaryColor = primaryColor,
            modifier = modifier
        )
    }

    selectedItem?.let { item ->
        OrderDetailsDialog(
            item = item,
            onDismiss = { selectedItem = null },
            primaryColor = primaryColor
        )
    }
}

@Composable
private fun OrdersContent(
    paddingValues: PaddingValues,
    isLoading: Boolean,
    orderItems: List<OrderItem>,
    selectedItem: OrderItem?,
    onItemSelected: (OrderItem) -> Unit,
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
            CircularProgressIndicator(
                color = primaryColor,
                modifier = Modifier
                    .size(50.dp)
                    .align(Alignment.Center)
            )
        } else if (orderItems.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ShoppingBag,
                    contentDescription = "No Orders",
                    tint = Color.Gray,
                    modifier = Modifier.size(80.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "No Orders or Donations Yet",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Start shopping or donating to see your history here!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(orderItems.size) { index ->
                    val item = orderItems[index]
                    OrderItem(
                        orderItem = item,
                        onClick = { onItemSelected(item) },
                        primaryColor = primaryColor
                    )
                }
            }
        }
    }
}

@Composable
fun OrderItem(
    orderItem: OrderItem,
    onClick: () -> Unit,
    primaryColor: Color
) {
    val itemDetails = when (orderItem) {
        is OrderItem.Purchase -> {
            val order = orderItem.order
            val firstItem = order.items.firstOrNull()
            val itemName = firstItem?.get("name") as? String ?: "Order #${order.orderId.takeLast(6)}"
            val qty = order.items.sumOf { item ->
                when (val quantity = item["quantity"]) {
                    is Number -> quantity.toInt()
                    else -> {
                        Log.w("OrderItem", "Invalid quantity for item: $item")
                        0
                    }
                }
            }
            Log.d("OrderItem", "Purchase items: ${order.items}, Calculated qty: $qty")
            OrderItemDetails(
                title = itemName,
                icon = Icons.Default.ShoppingBag,
                status = order.status,
                timestamp = order.createdAt,
                quantity = qty,
                details = "${order.items.size} item(s)"
            )
        }
        is OrderItem.Donation -> {
            val transaction = orderItem.transaction
            OrderItemDetails(
                title = transaction.item,
                icon = Icons.Default.Favorite,
                status = transaction.status,
                timestamp = transaction.timestamp,
                quantity = transaction.quantity,
                details = "To: ${transaction.organization ?: "Unknown"}"
            )
        }
    }

    val formattedDate = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(itemDetails.timestamp))
    val statusColor = when (itemDetails.status) {
        "completed", "COMPLETED", "DELIVERED" -> primaryColor
        else -> Color.Gray
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
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
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = itemDetails.status,
                    tint = statusColor,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = itemDetails.status.replaceFirstChar { it.uppercase() },
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
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(8.dp))
                        .background(Color(0xFFEDF7F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = itemDetails.icon,
                        contentDescription = "Item Icon",
                        modifier = Modifier.size(40.dp),
                        tint = primaryColor
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = itemDetails.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = itemDetails.details,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Qty: ${itemDetails.quantity}",
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
    item: OrderItem,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    val dialogDetails = when (item) {
        is OrderItem.Purchase -> {
            val order = item.order
            val formattedDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(order.createdAt))
            DialogDetails(
                title = "Order #${order.orderId.takeLast(6)}",
                status = order.status,
                timestamp = formattedDate,
                detailsContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Order Items (${order.items.size})",
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
                                                contentDescription = "Product Icon",
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
                                    if (index < order.items.size - 1) {
                                        Divider(color = Color.LightGray)
                                    }
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                                ReceiptRow(
                                    label = "Payment Method",
                                    value = order.paymentMethod
                                )
                                ReceiptRow(
                                    label = "Order Date",
                                    value = formattedDate
                                )
                                val shippingFee = 50.0
                                val subtotal = order.totalAmount - shippingFee
                                ReceiptRow(
                                    label = "Items Subtotal",
                                    value = "₱${String.format("%.2f", subtotal)}"
                                )
                                ReceiptRow(
                                    label = "Shipping Fee",
                                    value = "₱${String.format("%.2f", shippingFee)}"
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Total",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp
                                    )
                                    Text(
                                        text = "₱${String.format("%.2f", order.totalAmount)}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = primaryColor
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
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
                }
            )
        }
        is OrderItem.Donation -> {
            val transaction = item.transaction
            val formattedDate = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(transaction.timestamp))
            DialogDetails(
                title = "Donation to ${transaction.organization ?: "Unknown"}",
                status = transaction.status,
                timestamp = formattedDate,
                detailsContent = {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Donation Details",
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
                                ReceiptRow(
                                    label = "Item",
                                    value = transaction.item
                                )
                                ReceiptRow(
                                    label = "Quantity",
                                    value = transaction.quantity.toString()
                                )
                                ReceiptRow(
                                    label = "Organization",
                                    value = transaction.organization ?: "Unknown"
                                )
                                ReceiptRow(
                                    label = "Date",
                                    value = formattedDate
                                )
                                ReceiptRow(
                                    label = "Payment Method",
                                    value = transaction.paymentMethod
                                )
                                if (transaction.referenceId?.isNotEmpty() == true) {
                                    ReceiptRow(
                                        label = "Reference ID",
                                        value = transaction.referenceId
                                    )
                                }
                                ReceiptRow(
                                    label = "Total",
                                    value = "₱${String.format("%.2f", transaction.totalAmount)}"
                                )
                            }
                        }
                    }
                }
            )
        }
    }

    val statusColor = when (dialogDetails.status) {
        "completed", "COMPLETED", "DELIVERED" -> primaryColor
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
                    imageVector = when (item) {
                        is OrderItem.Purchase -> Icons.Default.ShoppingBag
                        is OrderItem.Donation -> Icons.Default.Favorite
                    },
                    contentDescription = "Details",
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = dialogDetails.title,
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
                    .verticalScroll(rememberScrollState())
            ) {
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
                            text = dialogDetails.status.replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = statusColor
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                dialogDetails.detailsContent()
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
