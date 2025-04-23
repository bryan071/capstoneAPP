package com.project.webapp.datas

import com.project.webapp.Viewmodel.OrderStatus

data class Order(
    val orderId: String = "",
    val buyerId: String = "",
    val items: List<Map<String, Any>> = emptyList(),
    val totalAmount: Float = 0f,
    val paymentMethod: String = "",
    val deliveryAddress: String = "",
    val status: String = OrderStatus.PAYMENT_RECEIVED.name,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)