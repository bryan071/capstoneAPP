package com.project.webapp.Viewmodel

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Transaction
import java.util.Date
import java.util.UUID

enum class OrderStatus(val displayName: String) {
    PAYMENT_RECEIVED("Payment Received"),
    TO_SHIP("To Ship"),
    SHIPPING("Shipping"),
    TO_DELIVER("To Deliver"),
    DELIVERED("Delivered"),
    COMPLETED("Completed")
}

sealed class OrderItem {
    data class Purchase(val order: Order) : OrderItem()
    data class Donation(val transaction: Transaction) : OrderItem()
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

    // Add debug logs to trace the items being saved
    Log.d("OrderDebug", "Creating order for payment method: $paymentMethod")
    Log.d("OrderDebug", "Items count: ${items.size}")

    // Detailed logging of each item
    items.forEachIndexed { index, item ->
        Log.d("OrderDebug", "Item $index: ${item.name}, ID: ${item.productId}, Quantity: ${item.quantity}")
    }

    // Check if we have items to process
    if (items.isEmpty()) {
        Log.e("Order", "No items to purchase!")
        return
    }

    // Map items and ensure seller names are fetched correctly
    val orderItems = items.map { item ->
        mapOf(
            "productId" to item.productId,
            "name" to item.name,
            "price" to item.price,
            "quantity" to item.quantity,
            "imageUrl" to item.imageUrl,
            "sellerName" to item.sellerId
        )
    }

    // Log the mapped items to ensure they're correctly transformed
    Log.d("OrderDebug", "Mapped ${orderItems.size} items for order record")

    // Create the order data
    val orderData = hashMapOf(
        "orderId" to orderId,
        "buyerId" to userId,
        "items" to orderItems,
        "status" to OrderStatus.PAYMENT_RECEIVED.name,
        "paymentMethod" to paymentMethod,
        "totalAmount" to totalAmount,
        "deliveryAddress" to deliveryAddress,
        "createdAt" to currentTime,
        "updatedAt" to currentTime,
        "estimatedDelivery" to Timestamp(calculateEstimatedDelivery(currentTime))
    )

    // Add the order to Firestore
    firestore.collection("orders").document(orderId)
        .set(orderData)
        .addOnSuccessListener {
            Log.d("Order", "Order record created successfully with ID: $orderId")
            createOrderStatusHistory(orderId, OrderStatus.PAYMENT_RECEIVED.name, currentTime)
        }
        .addOnFailureListener { e ->
            Log.e("Order", "Error creating order record", e)
        }

    // IMPORTANT: The cleanup operation has been completely removed from this function
    // as it was potentially interfering with new orders
}
fun createDonationRecord(
    userId: String,
    productId: String,
    productName: String,
    organizationId: String,
    organizationName: String,
    quantity: Int,
    orderNumber: String,
    firestore: FirebaseFirestore
) {
    val donationId = "DON-$orderNumber"
    val donationData = hashMapOf(
        "userId" to userId,
        "productId" to productId,
        "productName" to productName,
        "organizationId" to organizationId,
        "organizationName" to organizationName,
        "quantity" to quantity,
        "timestamp" to FieldValue.serverTimestamp(),
        "status" to "completed"
    )

    firestore.collection("donations")
        .document(donationId)
        .set(donationData)
        .addOnSuccessListener {
            Log.d("DonationScreen", "Donation record created successfully: $donationId")
        }
        .addOnFailureListener { e ->
            Log.e("DonationScreen", "Error saving donation record: ${e.message}", e)
        }
}
// Calculate estimated delivery date (3 days from now)
fun calculateEstimatedDelivery(currentTime: Long): Date {
    val estimatedTimeMillis = currentTime + 3 * 24 * 60 * 60 * 1000 // 3 days
    return Date(estimatedTimeMillis)
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
        .addOnSuccessListener {
            Log.d("Order", "Status history created successfully for order: $orderId")
        }
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

