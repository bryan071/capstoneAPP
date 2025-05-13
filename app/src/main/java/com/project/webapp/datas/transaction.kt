package com.project.webapp.datas

data class Transaction(
    val id: String = "",
    val buyerId: String = "",
    val item: String = "",
    val quantity: Int = 0,
    val totalAmount: Double = 0.0,
    val organization: String? = null,
    val transactionType: String = "",
    val status: String = "",
    val timestamp: Long = 0,
    val paymentMethod: String = "",
    val referenceId: String? = null
)
