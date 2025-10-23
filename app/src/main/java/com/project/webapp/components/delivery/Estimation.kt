package com.project.webapp.components.delivery

import Order
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.project.webapp.Viewmodel.OrderStatus
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


@Composable
fun DeliveryEstimation(
    order: Order,
    primaryColor: Color,
    modifier: Modifier = Modifier
) {
    // Calculate estimated delivery based on order status
    val calendar = Calendar.getInstance()
    calendar.time = order.timestamp.toDate()

    // Add days based on status
    val daysToAdd = when(order.status) {
        OrderStatus.PAYMENT_RECEIVED.name -> 5 // 5 days from order
        OrderStatus.TO_SHIP.name -> 4 // 4 days from order
        OrderStatus.SHIPPING.name -> 3 // 3 days from order
        OrderStatus.TO_DELIVER.name -> 0 // Today
        else -> -1 // Already delivered
    }

    if (daysToAdd >= 0) {
        calendar.add(Calendar.DAY_OF_MONTH, daysToAdd)
        val deliveryDateFormat = SimpleDateFormat("EEE, MMM dd", Locale.getDefault())
        val estimatedDate = deliveryDateFormat.format(calendar.time)

        Card(
            colors = CardDefaults.cardColors(containerColor = primaryColor.copy(alpha = 0.1f)),
            shape = RoundedCornerShape(12.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.LocalShipping,
                    contentDescription = "Delivery",
                    tint = primaryColor,
                    modifier = Modifier.size(24.dp)
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    if (order.status == OrderStatus.TO_DELIVER.name) {
                        Text(
                            text = "Arriving Today!",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                    } else {
                        Text(
                            text = "Estimated Delivery",
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        Text(
                            text = estimatedDate,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = primaryColor
                        )
                    }
                }
            }
        }
    }
}