package com.project.webapp.components.delivery

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.Viewmodel.OrderStatus


@Composable
fun OrderStatusTimeline(
    currentStatus: String,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    val orderStatuses = listOf(
        OrderStatus.PAYMENT_RECEIVED to "Payment Received",
        OrderStatus.TO_SHIP to "Processing",
        OrderStatus.SHIPPING to "Shipped",
        OrderStatus.TO_DELIVER to "Out for Delivery",
        OrderStatus.DELIVERED to "Delivered",
        OrderStatus.COMPLETED to "Completed"
    )

    val currentStatusIndex = orderStatuses.indexOfFirst { it.first.name == currentStatus }

    Column(modifier = modifier.fillMaxWidth()) {
        // Status timeline
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
fun updateOrderStatus(
    orderId: String,
    newStatus: OrderStatus,
    notes: String = "",
    onSuccess: () -> Unit,
    onError: (Exception) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val currentTime = System.currentTimeMillis()

    // Update order document with new status
    firestore.collection("orders").document(orderId)
        .update(
            mapOf(
                "status" to newStatus.name,
                "updatedAt" to currentTime
            )
        )
        .addOnSuccessListener {
            // Create status history entry
            val historyEntry = hashMapOf(
                "status" to newStatus.name,
                "timestamp" to currentTime,
                "notes" to if (notes.isNotEmpty()) notes else newStatus.displayName
            )

            firestore.collection("orders").document(orderId)
                .collection("statusHistory")
                .add(historyEntry)
                .addOnSuccessListener {
                    // Notify buyer about status update
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

    // Get order info to access buyer ID
    firestore.collection("orders").document(orderId).get()
        .addOnSuccessListener { document ->
            val buyerId = document.getString("buyerId") ?: return@addOnSuccessListener

            // Create notification
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