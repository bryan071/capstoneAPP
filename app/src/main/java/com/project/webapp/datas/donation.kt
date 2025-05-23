package com.project.webapp.datas

import com.google.firebase.Timestamp

data class Donation(
    val id: String = "",
    val userId: String = "",
    val productId: String = "",
    val productName: String = "",
    val organizationId: String = "",
    val organizationName: String = "",
    val quantity: Int = 0,
    val timestamp: Timestamp = Timestamp.now(),
    val status: String = ""
)