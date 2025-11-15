package com.project.webapp.components.delivery

import TimelineStep
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.Viewmodel.OrderStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun OrderStatusTimeline(
    currentStatus: String,
    paymentStatus: String? = null,
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

    // Unified status list: To Pay → To Ship → To Receive → Completed
    val orderStatuses = listOf(
        TimelineStep(
            label = "To Pay",
            isCompleted = paymentStatus == "Payment Received" ||
                    currentStatus.uppercase() !in listOf("TO PAY", "PAYMENT PENDING", "PENDING", "TOPAY"),
            icon = Icons.Default.AccountBalanceWallet,
            message = "Waiting for payment confirmation"
        ),
        TimelineStep(
            label = "To Ship",
            isCompleted = currentStatus.uppercase() !in listOf("TO PAY", "PAYMENT PENDING", "PENDING", "TOPAY", "TO SHIP", "TOSHIP"),
            icon = Icons.Default.Inventory,
            message = "Your order is being prepared"
        ),
        TimelineStep(
            label = "To Deliver",
            isCompleted = currentStatus.uppercase() in listOf("COMPLETED", "COMPLETE", "DELIVERED"),
            icon = Icons.Default.LocalShipping,
            message = "Package is out for delivery"
        ),
        TimelineStep(
            label = "Completed",
            isCompleted = currentStatus.uppercase() in listOf("COMPLETED", "COMPLETE", "DELIVERED"),
            icon = Icons.Default.CheckCircle,
            message = "Order delivered successfully"
        )
    )

    // Determine current step
    val currentStepIndex = when (currentStatus.uppercase()) {
        "TO PAY", "PAYMENT PENDING", "PENDING", "TOPAY" -> 0
        "TO SHIP", "TOSHIP", "PROCESSING", "PREPARING" -> 1
        "TO DELIVER", "TODELIVER" -> 2
        "COMPLETED", "COMPLETE", "DELIVERED" -> 3
        "TO RECEIVE", "TORECEIVE", "SHIPPED", "IN_TRANSIT", "OUT_FOR_DELIVERY" -> 2 // Map old "To Receive" to "To Deliver"
        else -> -1
    }

    // Action visibility
    val canCancel = currentStepIndex <= 1 && currentStatus.uppercase() !in listOf("CANCELLED", "COMPLETED")
    val canTrack = currentStepIndex >= 2 && currentStatus.uppercase() !in listOf("CANCELLED", "COMPLETED")

    Column(modifier = modifier.fillMaxWidth()) {
        // Action Buttons
        if (showActions && orderId != null && currentStatus.uppercase() !in listOf("CANCELLED", "COMPLETED")) {
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
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFF44336)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF44336)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Cancel, contentDescription = "Cancel", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
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
                        Icon(Icons.Default.LocalShipping, contentDescription = "Track", modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Track", fontSize = 13.sp)
                    }
                }

                Button(
                    onClick = { Toast.makeText(context, "Contact seller feature coming soon", Toast.LENGTH_SHORT).show() },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(Icons.Default.Chat, contentDescription = "Chat", modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Contact", fontSize = 13.sp)
                }
            }

            Divider(modifier = Modifier.padding(vertical = 16.dp), color = Color.LightGray)
        }

        // Timeline
        Column {
            orderStatuses.forEachIndexed { index, step ->
                val isActive = index == currentStepIndex
                val isPast = index < currentStepIndex

                StatusTimelineItem(
                    label = step.label,
                    icon = step.icon,
                    isCompleted = step.isCompleted || isPast,
                    isActive = isActive,
                    primaryColor = primaryColor,
                    message = if (isActive) step.message else null
                )

                // Connecting line between steps
                if (index < orderStatuses.size - 1) {
                    Box(
                        modifier = Modifier
                            .padding(start = 12.dp)
                            .width(2.dp)
                            .height(16.dp)
                            .background(
                                color = if (step.isCompleted || isPast)
                                    Color(0xFF4CAF50)
                                else
                                    Color.LightGray
                            )
                    )
                }
            }
        }

        // Cancelled state
        if (currentStatus.equals("CANCELLED", ignoreCase = true)) {
            Spacer(Modifier.height(16.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF44336).copy(alpha = 0.1f)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Cancel,
                        contentDescription = "Cancelled",
                        tint = Color(0xFFF44336),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(
                            "Order Cancelled",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color(0xFFF44336)
                        )
                        Text(
                            "This order has been cancelled",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCancelDialog) {
        CancelOrderDialog(
            orderId = orderId ?: "",
            isProcessing = isProcessing,
            onConfirm = { reason ->
                scope.launch {
                    isProcessing = true
                    try {
                        cancelOrder(
                            orderId = orderId ?: "",
                            reason = reason,
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
    label: String,
    isCompleted: Boolean,
    isActive: Boolean,
    primaryColor: Color,
    message: String? = null
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

    val bgColor = if (isActive) primaryColor.copy(alpha = 0.1f) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = bgColor, shape = RoundedCornerShape(8.dp))
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color = pointColor, shape = CircleShape)
                .then(if (!isCompleted && !isActive) Modifier.border(2.dp, Color.LightGray, CircleShape) else Modifier),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
            }
        }

        Spacer(Modifier.width(16.dp))

        Column {
            Text(
                text = label,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp,
                color = textColor
            )
            if (isActive && message != null) {
                Spacer(Modifier.height(4.dp))
                Text(text = message, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

@Composable
fun StatusTimelineItem(
    label: String,
    icon: ImageVector,
    isCompleted: Boolean,
    isActive: Boolean,
    primaryColor: Color,
    message: String? = null
) {
    val pointColor = when {
        isCompleted -> Color(0xFF4CAF50)  // Green for completed
        isActive -> primaryColor           // Primary color for active
        else -> Color.LightGray            // Gray for pending
    }

    val textColor = when {
        isActive -> primaryColor
        isCompleted -> Color.DarkGray
        else -> Color.Gray
    }

    val bgColor = if (isActive) primaryColor.copy(alpha = 0.1f) else Color.Transparent

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .background(color = bgColor, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 12.dp)
    ) {
        // Circle with icon or checkmark
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    color = if (isCompleted || isActive) pointColor else Color.Transparent,
                    shape = CircleShape
                )
                .then(
                    if (!isCompleted && !isActive)
                        Modifier.border(2.dp, Color.LightGray, CircleShape)
                    else
                        Modifier
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else if (isActive) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            } else {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = Color.LightGray,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                fontSize = 16.sp,
                color = textColor
            )
            if (isActive && message != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = message,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    lineHeight = 18.sp
                )
            }
        }

        // Status indicator
        if (isCompleted) {
            Surface(
                color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Done",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF4CAF50),
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        } else if (isActive) {
            Surface(
                color = primaryColor.copy(alpha = 0.15f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    "Current",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = primaryColor,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}

// === KEEP ALL EXISTING DIALOGS & FIREBASE FUNCTIONS BELOW UNCHANGED ===
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
        "Changed my mind", "Found a better price", "Ordered by mistake",
        "Delivery time too long", "Want to change delivery address", "Other"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Cancel, contentDescription = null, tint = Color(0xFFF44336), modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Text("Cancel Order", fontWeight = FontWeight.Bold, fontSize = 20.sp)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text("Please select a reason for cancellation:", fontSize = 14.sp, color = Color.Gray)
                Spacer(Modifier.height(16.dp))

                cancelReasons.forEach { reason ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = selectedReason == reason, onClick = { selectedReason = reason }, colors = RadioButtonDefaults.colors(selectedColor = Color(0xFFF44336)))
                        Spacer(Modifier.width(8.dp))
                        Text(reason, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    }
                }

                if (selectedReason == "Other") {
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = customReason, onValueChange = { customReason = it },
                        label = { Text("Please specify") }, modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp), maxLines = 3
                    )
                }

                Spacer(Modifier.height(16.dp))
                Text("Warning: This action cannot be undone", fontSize = 13.sp, color = Color(0xFFF44336), fontWeight = FontWeight.Medium)
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val reason = if (selectedReason == "Other") customReason else selectedReason
                    onConfirm(reason)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)),
                enabled = !isProcessing && (selectedReason != "Other" || customReason.isNotBlank()),
                shape = RoundedCornerShape(8.dp)
            ) {
                if (isProcessing) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                else Text("Cancel Order")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss, enabled = !isProcessing, shape = RoundedCornerShape(8.dp)) {
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
            val doc = FirebaseFirestore.getInstance().collection("orders").document(orderId).get().await()
            trackingInfo = doc.data
        } catch (e: Exception) {
            Log.e("TrackingDialog", "Error loading tracking info", e)
        } finally {
            isLoading = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.LocalShipping, contentDescription = null, tint = primaryColor, modifier = Modifier.size(28.dp))
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("Track Order", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                    Text("#${orderId.takeLast(8)}", fontSize = 14.sp, color = Color.Gray)
                }
            }
        },
        text = {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else {
                Column {
                    Card(colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)), shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(16.dp)) {
                            TrackingInfoRow(
                                icon = Icons.Default.QrCode,
                                label = "Tracking Number",
                                value = trackingInfo?.get("trackingNumber") as? String ?: "TRK${orderId.takeLast(10).uppercase()}",
                                primaryColor = primaryColor
                            )
                            Spacer(Modifier.height(12.dp))
                            TrackingInfoRow(
                                icon = Icons.Default.LocalShipping,
                                label = "Courier",
                                value = trackingInfo?.get("courier") as? String ?: "Standard Delivery",
                                primaryColor = primaryColor
                            )
                            Spacer(Modifier.height(12.dp))
                            TrackingInfoRow(
                                icon = Icons.Default.Schedule,
                                label = "Estimated Delivery",
                                value = trackingInfo?.get("estimatedDelivery") as? String ?: "3-5 business days",
                                primaryColor = primaryColor
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Tip: You can track your package using the tracking number on the courier's website",
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
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close")
            }
        }
    )
}

@Composable
fun TrackingInfoRow(icon: ImageVector, label: String, value: String, primaryColor: Color) {
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(label, fontSize = 12.sp, color = Color.Gray)
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.DarkGray)
        }
    }
}

// === FIREBASE FUNCTIONS (UNCHANGED) ===
fun cancelOrder(orderId: String, reason: String, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
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
            val historyEntry = hashMapOf(
                "status" to "CANCELLED",
                "timestamp" to currentTime,
                "notes" to "Order cancelled: $reason"
            )
            firestore.collection("orders").document(orderId).collection("statusHistory").add(historyEntry)
                .addOnSuccessListener { notifyBuyerOfCancellation(orderId, reason); onSuccess() }
                .addOnFailureListener(onError)
        }
        .addOnFailureListener(onError)
}

fun updateOrderStatus(orderId: String, newStatus: OrderStatus, notes: String = "", onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    val firestore = FirebaseFirestore.getInstance()
    val currentTime = System.currentTimeMillis()

    firestore.collection("orders").document(orderId)
        .update(mapOf("status" to newStatus.name, "updatedAt" to currentTime))
        .addOnSuccessListener {
            val historyEntry = hashMapOf("status" to newStatus.name, "timestamp" to currentTime, "notes" to notes.ifEmpty { newStatus.displayName })
            firestore.collection("orders").document(orderId).collection("statusHistory").add(historyEntry)
                .addOnSuccessListener { notifyBuyerOfStatusUpdate(orderId, newStatus); onSuccess() }
                .addOnFailureListener(onError)
        }
        .addOnFailureListener(onError)
}

fun notifyBuyerOfStatusUpdate(orderId: String, status: OrderStatus) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("orders").document(orderId).get()
        .addOnSuccessListener { doc ->
            val buyerId = doc.getString("buyerId") ?: return@addOnSuccessListener
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
        .addOnSuccessListener { doc ->
            val buyerId = doc.getString("buyerId") ?: return@addOnSuccessListener
            val sellerId = doc.getString("sellerId") ?: doc.getString("ownerId")
            val items = doc.get("items") as? List<Map<String, Any>>
            val firstItem = items?.firstOrNull()
            val itemName = firstItem?.get("name") as? String ?: "Order"
            val quantity = (firstItem?.get("quantity") as? Number)?.toInt() ?: 1
            val totalAmount = doc.getDouble("totalAmount") ?: 0.0
            val imageUrl = doc.getString("imageUrl") ?: ""

            val buyerNotification = hashMapOf(
                "userId" to buyerId, "type" to "order_cancelled", "title" to "Order Cancelled",
                "message" to "Your order has been cancelled. Reason: $reason", "timestamp" to System.currentTimeMillis(),
                "orderId" to orderId, "name" to itemName, "price" to totalAmount, "quantity" to quantity,
                "quantityUnit" to "item(s)", "imageUrl" to imageUrl, "cancelReason" to reason, "sellerId" to sellerId, "read" to false
            )
            firestore.collection("notifications").add(buyerNotification)

            if (!sellerId.isNullOrEmpty()) {
                val sellerNotification = hashMapOf(
                    "userId" to sellerId, "type" to "order_cancelled", "title" to "Order Cancelled by Buyer",
                    "message" to "Order #${orderId.takeLast(6)} was cancelled by the buyer. Reason: $reason",
                    "timestamp" to System.currentTimeMillis(), "orderId" to orderId, "name" to itemName,
                    "price" to totalAmount, "quantity" to quantity, "quantityUnit" to "item(s)",
                    "imageUrl" to imageUrl, "cancelReason" to reason, "buyerId" to buyerId, "read" to false
                )
                firestore.collection("notifications").add(sellerNotification)
            }
        }
}