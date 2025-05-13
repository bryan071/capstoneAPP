package com.project.webapp.datas

data class ReceiptData(
    val cartItems: List<CartItem>?,
    val totalPrice: Double?,
    val userType: String?,
    val sellerNames: Map<String, String>?,
    val paymentMethod: String?,
    val referenceId: String?,
    val organization: Organization?,
    val isDonation: Boolean?
)