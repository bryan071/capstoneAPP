package com.project.webapp.components.delivery

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.Viewmodel.OrderStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun OrderStatusTimeline(
    currentStatus: String,
    orderId: String? = null,
    primaryColor: Color,
    modifier: Modifier = Modifier,
    showActions: Boolean = true,
    onStatusUpdated: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showCancelDialog by remember { mutableStateOf(false) }
    var showTrackingDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    val orderStatuses = listOf(
        OrderStatus.PAYMENT_RECEIVED to "Payment Received",
        OrderStatus.TO_SHIP to "Processing",
        OrderStatus.SHIPPING to "Shipped",
        OrderStatus.TO_DELIVER to "Out for Delivery",
        OrderStatus.DELIVERED to "Delivered",
        OrderStatus.COMPLETED to "Completed"
    )

    val currentStatusIndex = orderStatuses.indexOfFirst {
        it.first.name.equals(currentStatus, ignoreCase = true)
    }

    // Check if order can be cancelled
    val canCancel = currentStatusIndex <= 1 &&
            !currentStatus.equals("CANCELLED", ignoreCase = true) &&
            !currentStatus.equals("COMPLETED", ignoreCase = true)

    val canTrack = currentStatusIndex >= 2 && currentStatusIndex <= 3

    Column(modifier = modifier.fillMaxWidth()) {
        // Action Buttons
        if (showActions && orderId != null) {
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
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            Color(0xFFF44336)
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Cancel",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Cancel Order")
                    }
                }

                if (canTrack) {
                    Button(
                        onClick = { showTrackingDialog = true },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = "Track",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Track Order")
                    }
                }

                Button(
                    onClick = {
                        Toast.makeText(context, "Contact seller feature coming soon", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Contact")
                }
            }

            Divider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = Color.LightGray
            )
        }

        // Status Timeline
        Box(modifier = Modifier.fillMaxWidth()) {
            // Timeline line
            Divider(
                color = Color.LightGray,
                thickness = 2.dp,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = 12.dp)
                    .height(((orderStatuses.size - 1) * 84).dp)
            )

            // Status points
            Column {
                orderStatuses.forEachIndexed { index, (status, label) ->
                    val isCompleted = index <= currentStatusIndex
                    val isActive = index == currentStatusIndex

                    StatusTimelineItem(
                        status = status.name,
                        label = label,
                        isCompleted = isCompleted,
                        isActive = isActive,
                        primaryColor = primaryColor
                    )

                    if (index < orderStatuses.size - 1) {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }

        // Show cancelled status if applicable
        if (currentStatus.equals("CANCELLED", ignoreCase = true)) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF44336).copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Cancel,
                        contentDescription = "Cancelled",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Order Cancelled",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            text = "This order has been cancelled",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Cancel Order Dialog
    if (showCancelDialog) {
        CancelOrderDialog(
            orderId = orderId ?: "",
            isProcessing = isProcessing,
            onConfirm = {
                scope.launch {
                    isProcessing = true
                    try {
                        cancelOrder(
                            orderId = orderId ?: "",
                            reason = it,
                            onSuccess = {
                                Toast.makeText(context, "Order cancelled successfully", Toast.LENGTH_SHORT).show()
                                showCancelDialog = false
                                onStatusUpdated?.invoke()
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
            orderId = orderId ?: "",
            onDismiss = { showTrackingDialog = false },
            primaryColor = primaryColor
        )
    }
}

@Composable
fun StatusTimelineItem(
    status: String,
    label: String,
    isCompleted: Boolean,
    isActive: Boolean,
    primaryColor: Color
) {
    val pointColor = when {
        isActive -> primaryColor
        isCompleted -> Color(0xFF4CAF50)
        else -> Color.LightGray
    }

    val textColor = when {
        isActive -> primaryColor
        isCompleted -> Color.DarkGray
        else -> Color.Gray
    }

    val bgColor = when {
        isActive -> primaryColor.copy(alpha = 0.1f)
        isCompleted -> Color(0xFFE8F5E9)
        else -> Color.Transparent
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isActive) bgColor else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(8.dp)
    ) {
        // Status point
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = pointColor,
                    shape = CircleShape
                )
                .then(
                    if (!isCompleted && !isActive) {
                        Modifier.border(2.dp, Color.LightGray, CircleShape)
                    } else Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp,
                color = textColor
            )

            // Show timeline specific message based on status
            if (isActive) {
                val statusMessage = when (status) {
                    OrderStatus.PAYMENT_RECEIVED.name -> "Payment has been confirmed"
                    OrderStatus.TO_SHIP.name -> "Your order is being prepared"
                    OrderStatus.SHIPPING.name -> "Your order is on the way"
                    OrderStatus.TO_DELIVER.name -> "Package will be delivered today"
                    OrderStatus.DELIVERED.name -> "Package has been delivered"
                    OrderStatus.COMPLETED.name -> "Order has been completed"
                    else -> ""
                }

                if (statusMessage.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = statusMessage,
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun CancelOrderDialog(
    orderId: String,
    isProcessing: Boolean,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedReason by remember { mutableStateOf("Changed my mind") }
    var customReason by remember { mutableStateOf("") }

    val cancelReasons = listOf(
        "Changed my mind",
        "Found a better price",
        "Ordered by mistake",
        "Delivery time too long",
        "Want to change delivery address",
        "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Cancel,
                    contentDescription = null,
                    tint = Color(0xFFF44336),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Cancel Order",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Please select a reason for cancellation:",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(16.dp))

                cancelReasons.forEach { reason ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedReason == reason,
                            onClick = { selectedReason = reason },
                            colors = RadioButtonDefaults.colors(
                                selectedColor = Color(0xFFF44336)
                            )
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = reason,
                            fontSize = 15.sp,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                if (selectedReason == "Other") {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customReason,
                        onValueChange = { customReason = it },
                        label = { Text("Please specify") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        maxLines = 3
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "⚠️ This action cannot be undone",
                    fontSize = 13.sp,
                    color = Color(0xFFF44336),
                    fontWeight = FontWeight.Medium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reason = if (selectedReason == "Other") customReason else selectedReason
                    onConfirm(reason)
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFF44336)
                ),
                enabled = !isProcessing && (selectedReason != "Other" || customReason.isNotBlank()),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    Text("Cancel Order")
                }
            }
        },
        dismissButton = {
            OutlinedButton(
                onClick = onDismiss,
                enabled = !isProcessing,
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Keep Order")
            }
        }
    )
}

@Composable
fun TrackingInfoDialog(
    orderId: String,
    onDismiss: () -> Unit,
    primaryColor: Color
) {
    var trackingInfo by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(orderId) {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val doc = firestore.collection("orders")
                .document(orderId)
                .get()
                .await()

            trackingInfo = doc.data
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = null,
                    tint = primaryColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Track Order",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                    Text(
                        text = "#${orderId.takeLast(8)}",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        fontWeight = FontWeight.Normal
                    )
                }
            }
        },
        text = {
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = primaryColor.copy(alpha = 0.1f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            TrackingInfoRow(
                                icon = Icons.Default.QrCode,
                                label = "Tracking Number",
                                value = trackingInfo?.get("trackingNumber") as? String
                                    ?: "TRK${orderId.takeLast(10).uppercase()}",
                                primaryColor = primaryColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TrackingInfoRow(
                                icon = Icons.Default.LocalShipping,
                                label = "Courier",
                                value = trackingInfo?.get("courier") as? String ?: "Standard Delivery",
                                primaryColor = primaryColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TrackingInfoRow(
                                icon = Icons.Default.Schedule,
                                label = "Estimated Delivery",
                                value = trackingInfo?.get("estimatedDelivery") as? String
                                    ?: "3-5 business days",
                                primaryColor = primaryColor
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "💡 Tip: You can track your package using the tracking number on the courier's website",
                        fontSize = 13.sp,
                        color = Color.Gray,
                        lineHeight = 18.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(
                    containerColor = primaryColor
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TrackingInfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    primaryColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = primaryColor,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontSize = 12.sp,
                color = Color.Gray
            )
            Text(
                text = value,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.DarkGray
            )
        }
    }
}

// Firebase Functions
fun cancelOrder(
    orderId: String,
    reason: String,
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentTime = System.currentTimeMillis()

    firestore.collection("orders").document(orderId)
        .update(
            mapOf(
                "status" to "CANCELLED",
                "cancelReason" to reason,
                "cancelledAt" to currentTime,
                "updatedAt" to currentTime
            )
        )
        .addOnSuccessListener {
            // Create cancellation history entry
            val historyEntry = hashMapOf(
                "status" to "CANCELLED",
                "timestamp" to currentTime,
                "notes" to "Order cancelled: $reason"
            )

            firestore.collection("orders").document(orderId)
                .collection("statusHistory")
                .add(historyEntry)
                .addOnSuccessListener {
                    notifyBuyerOfCancellation(orderId, reason)
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        }
        .addOnFailureListener { e ->
            onError(e)
        }
}

fun updateOrderStatus(
    orderId: String,
    newStatus: OrderStatus,
    notes: String = "",
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentTime = System.currentTimeMillis()

    firestore.collection("orders").document(orderId)
        .update(
            mapOf(
                "status" to newStatus.name,
                "updatedAt" to currentTime
            )
        )
        .addOnSuccessListener {
            val historyEntry = hashMapOf(
                "status" to newStatus.name,
                "timestamp" to currentTime,
                "notes" to if (notes.isNotEmpty()) notes else newStatus.displayName
            )

            firestore.collection("orders").document(orderId)
                .collection("statusHistory")
                .add(historyEntry)
                .addOnSuccessListener {
                    notifyBuyerOfStatusUpdate(orderId, newStatus)
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    onError(e)
                }
        }
        .addOnFailureListener { e ->
            onError(e)
        }
}

fun notifyBuyerOfStatusUpdate(orderId: String, status: OrderStatus) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("orders").document(orderId).get()
        .addOnSuccessListener { document ->
            val buyerId = document.getString("buyerId") ?: return@addOnSuccessListener

            val notification = hashMapOf(
                "userId" to buyerId,
                "title" to "Order Status Update",
                "message" to "Your order #${orderId.takeLast(6)} has been updated to: ${status.displayName}",
                "timestamp" to System.currentTimeMillis(),
                "type" to "order_update",
                "orderId" to orderId,
                "read" to false
            )

            firestore.collection("notifications").add(notification)
        }
}

fun notifyBuyerOfCancellation(orderId: String, reason: String) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("orders").document(orderId).get()
        .addOnSuccessListener { document ->
            val buyerId = document.getString("buyerId") ?: return@addOnSuccessListener
            val sellerId = document.getString("sellerId") ?: document.getString("ownerId")
            val productName = document.getString("productName") ?: "Order"
            val totalAmount = document.getDouble("totalAmount") ?: 0.0
            val imageUrl = document.getString("imageUrl") ?: ""

            // Get first item details if available
            val items = document.get("items") as? List<Map<String, Any>>
            val firstItem = items?.firstOrNull()
            val itemName = firstItem?.get("name") as? String ?: productName
            val quantity = (firstItem?.get("quantity") as? Number)?.toInt() ?: 1

            // Notification for buyer (who cancelled)
            val buyerNotification = hashMapOf(
                "userId" to buyerId,
                "type" to "order_cancelled",
                "title" to "Order Cancelled",
                "message" to "Your order has been cancelled. Reason: $reason",
                "timestamp" to System.currentTimeMillis(),
                "orderId" to orderId,
                "name" to itemName,
                "price" to totalAmount,
                "quantity" to quantity,
                "quantityUnit" to "item(s)",
                "imageUrl" to imageUrl,
                "cancelReason" to reason,
                "sellerId" to sellerId,
                "read" to false
            )

            firestore.collection("notifications").add(buyerNotification)
                .addOnSuccessListener {
                    Log.d("Notification", "Buyer cancellation notification created")
                }

            // Notification for seller (about buyer's cancellation)
            if (!sellerId.isNullOrEmpty()) {
                val sellerNotification = hashMapOf(
                    "userId" to sellerId,
                    "type" to "order_cancelled",
                    "title" to "Order Cancelled by Buyer",
                    "message" to "Order #${orderId.takeLast(6)} was cancelled by the buyer. Reason: $reason",
                    "timestamp" to System.currentTimeMillis(),
                    "orderId" to orderId,
                    "name" to itemName,
                    "price" to totalAmount,
                    "quantity" to quantity,
                    "quantityUnit" to "item(s)",
                    "imageUrl" to imageUrl,
                    "cancelReason" to reason,
                    "buyerId" to buyerId,
                    "read" to false
                )

                firestore.collection("notifications").add(sellerNotification)
                    .addOnSuccessListener {
                        Log.d("Notification", "Seller cancellation notification created")
                    }
            }
        }
}