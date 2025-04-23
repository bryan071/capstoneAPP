package com.project.webapp.Viewmodel

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Order
import java.util.Calendar
import java.util.UUID

enum class OrderStatus(val displayName: String) {
    PAYMENT_RECEIVED("Payment Received"),
    TO_SHIP("To Ship"),
    SHIPPING("Shipping"),
    TO_DELIVER("To Deliver"),
    DELIVERED("Delivered"),
    COMPLETED("Completed")
}

fun createOrderRecord(
    userId: String,
    items: List<CartItem>,
    paymentMethod: String,
    totalAmount: Float,
    deliveryAddress: String
) {
    val firestore = FirebaseFirestore.getInstance()
    val orderId = UUID.randomUUID().toString()
    val currentTime = System.currentTimeMillis()

    // Create an order object with initial status
    val orderData = hashMapOf(
        "orderId" to orderId,
        "buyerId" to userId,
        "items" to items.map { item ->
            hashMapOf(
                "productId" to item.productId,
                "name" to item.name,
                "price" to item.price,
                "quantity" to item.quantity,
                "imageUrl" to item.imageUrl
            )
        },
        "status" to OrderStatus.PAYMENT_RECEIVED.name,
        "paymentMethod" to paymentMethod,
        "totalAmount" to totalAmount,
        "deliveryAddress" to deliveryAddress,
        "createdAt" to currentTime,
        "updatedAt" to currentTime,
        // Add estimated delivery timeframe (5-7 days from now)
        "estimatedDelivery" to calculateEstimatedDelivery(currentTime)
    )

    // Add the order to Firestore
    firestore.collection("orders").document(orderId)
        .set(orderData)
        .addOnSuccessListener {
            Log.d("Order", "Order record created successfully")
            // Create order status history for tracking
            createOrderStatusHistory(orderId, OrderStatus.PAYMENT_RECEIVED.name, currentTime)
        }
        .addOnFailureListener { e ->
            Log.e("Order", "Error creating order record", e)
        }
}

// Calculate estimated delivery date (5-7 days from now)
fun calculateEstimatedDelivery(currentTime: Long): Map<String, Long> {
    val calendar = Calendar.getInstance()
    // Min delivery date (5 days from now)
    calendar.timeInMillis = currentTime
    calendar.add(Calendar.DAY_OF_MONTH, 5)
    val minDelivery = calendar.timeInMillis

    // Max delivery date (7 days from now)
    calendar.timeInMillis = currentTime
    calendar.add(Calendar.DAY_OF_MONTH, 7)
    val maxDelivery = calendar.timeInMillis

    return mapOf(
        "minDate" to minDelivery,
        "maxDate" to maxDelivery
    )
}

// Create order status history entry
fun createOrderStatusHistory(orderId: String, status: String, timestamp: Long) {
    val firestore = FirebaseFirestore.getInstance()
    val historyEntry = hashMapOf(
        "status" to status,
        "timestamp" to timestamp,
        "notes" to when(status) {
            OrderStatus.PAYMENT_RECEIVED.name -> "Payment has been received and order is confirmed"
            OrderStatus.TO_SHIP.name -> "Order is being prepared for shipping"
            OrderStatus.SHIPPING.name -> "Order has been shipped and is in transit"
            OrderStatus.TO_DELIVER.name -> "Order is out for delivery"
            OrderStatus.DELIVERED.name -> "Order has been delivered successfully"
            OrderStatus.COMPLETED.name -> "Order has been completed"
            else -> "Status updated"
        }
    )

    firestore.collection("orders").document(orderId)
        .collection("statusHistory")
        .add(historyEntry)
        .addOnFailureListener { e ->
            Log.e("Order", "Error creating status history", e)
        }
}

// Helper function to get a message based on order status
private fun getStatusMessage(status: String, orderId: String): String {
    return when (status) {
        OrderStatus.PAYMENT_RECEIVED.name -> "Payment received for order #${orderId.takeLast(6)}. Thank you!"
        OrderStatus.TO_SHIP.name -> "Your order #${orderId.takeLast(6)} is being prepared for shipping."
        OrderStatus.SHIPPING.name -> "Your order #${orderId.takeLast(6)} has been shipped."
        OrderStatus.TO_DELIVER.name -> "Your order #${orderId.takeLast(6)} is out for delivery."
        OrderStatus.DELIVERED.name -> "Your order #${orderId.takeLast(6)} has been delivered."
        OrderStatus.COMPLETED.name -> "Your order #${orderId.takeLast(6)} is completed. Thank you!"
        else -> "Status updated for your order #${orderId.takeLast(6)}."
    }
}

