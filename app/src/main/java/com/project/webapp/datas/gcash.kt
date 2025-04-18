package com.project.webapp.datas

import com.google.gson.annotations.SerializedName

// API Models and Service classes
data class GCashApiConfig(
    val clientId: String,
    val clientSecret: String,
    val merchantId: String,
    val redirectUrl: String
)

data class GCashPaymentRequest(
    val amount: Int,
    val currency: String,
    val description: String,
    val transactionId: String,
    val merchantId: String
)

data class GCashPaymentResponse(
    val isSuccessful: Boolean,
    val referenceId: String,
    val checkoutUrl: String,
    val message: String
)

data class GCashVerificationResponse(
    val status: String,
    val referenceId: String,
    val transactionId: String,
    val amount: Int
)

data class GCashTokenResponse(
    val accessToken: String,
    val expiresIn: Int,
    val tokenType: String
)