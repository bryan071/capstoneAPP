package com.project.webapp.api

import com.project.webapp.datas.GCashPaymentRequest
import com.project.webapp.datas.GCashPaymentResponse
import com.project.webapp.datas.GCashTokenResponse
import com.project.webapp.datas.GCashVerificationResponse
import kotlinx.coroutines.delay

class MockGCashApi : GCashApi {
    override suspend fun getAccessToken(
        clientId: String,
        clientSecret: String
    ): GCashTokenResponse {
        // Simulate network delay
        delay(1000)
        return GCashTokenResponse(
            accessToken = "mock_access_token_${System.currentTimeMillis()}",
            expiresIn = 3600,
            tokenType = "Bearer"
        )
    }

    override suspend fun createPayment(
        paymentRequest: GCashPaymentRequest,
        authorization: String
    ): GCashPaymentResponse {
        // Simulate network delay
        delay(1500)

        val referenceId = "REF_${System.currentTimeMillis()}"
        // Create a deep link that will come back to the app with the referenceId
        val checkoutUrl = "gcash://payment?reference=$referenceId&amount=${paymentRequest.amount / 100.0}"

        return GCashPaymentResponse(
            isSuccessful = true,
            referenceId = referenceId,
            checkoutUrl = checkoutUrl,
            message = "Payment initiated successfully"
        )
    }

    override suspend fun verifyPayment(
        referenceId: String,
        authorization: String
    ): GCashVerificationResponse {
        // Simulate network delay
        delay(2000)

        // In a real app, we would query the status from GCash
        // Here we simulate a successful payment
        return GCashVerificationResponse(
            status = "COMPLETED", // Could be "PENDING", "FAILED", or "COMPLETED"
            referenceId = referenceId,
            transactionId = "TXN_${referenceId.substring(4)}",
            amount = 0 // In a real API, this would be the actual amount
        )
    }
}