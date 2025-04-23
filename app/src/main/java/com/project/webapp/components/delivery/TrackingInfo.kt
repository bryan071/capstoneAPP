package com.project.webapp.components.delivery

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.Viewmodel.OrderStatus
import com.project.webapp.datas.Order


@Composable
fun TrackingInformation(
    order: Order,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    // Only show tracking info for orders that are being shipped
    if (order.status in listOf(
            OrderStatus.SHIPPING.name,
            OrderStatus.TO_DELIVER.name
        )
    ) {
        var trackingDetails by remember { mutableStateOf<Map<String, Any>?>(null) }
        var isTrackingLoading by remember { mutableStateOf(true) }
        val firestore = FirebaseFirestore.getInstance()

        // Fetch tracking details
        LaunchedEffect(order.orderId) {
            firestore.collection("tracking")
                .document(order.orderId)
                .get()
                .addOnSuccessListener { document ->
                    isTrackingLoading = false
                    if (document.exists()) {
                        trackingDetails = document.data
                    }
                }
                .addOnFailureListener {
                    isTrackingLoading = false
                }
        }

        Card(
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocalShipping,
                        contentDescription = "Tracking",
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "Tracking Information",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                if (isTrackingLoading) {
                    // Show loading indicator
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        CircularProgressIndicator(
                            color = primaryColor,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                } else if (trackingDetails != null) {
                    // Show tracking carrier and number
                    val carrier = trackingDetails?.get("carrier") as? String ?: "Standard Delivery"
                    val trackingNumber = trackingDetails?.get("trackingNumber") as? String ?: "N/A"

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Carrier",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )

                            Text(
                                text = carrier,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = Color.DarkGray
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Tracking #",
                                fontSize = 14.sp,
                                color = Color.Gray
                            )

                            Text(
                                text = trackingNumber,
                                fontWeight = FontWeight.Medium,
                                fontSize = 16.sp,
                                color = Color.DarkGray
                            )
                        }
                    }

                    // Add button to track package if applicable
                    if (trackingNumber != "N/A") {
                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { /* Open tracking URL or dialog */ },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Inventory,
                                contentDescription = "Track Package"
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            Text(
                                text = "Track Package",
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    // No tracking details available
                    Text(
                        text = "Tracking information will be updated soon",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}