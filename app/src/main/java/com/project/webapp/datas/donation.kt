package com.project.webapp.datas

data class Donation(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val organizationId: String = "",
    val organizationName: String = "",
    val quantity: Int = 0,
    val timestamp: Long = 0,
    val status: String = ""
)