package com.project.webapp.api


import com.project.webapp.datas.GCashPaymentRequest
import com.project.webapp.datas.GCashPaymentResponse
import com.project.webapp.datas.GCashTokenResponse
import com.project.webapp.datas.GCashVerificationResponse
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface GCashApi {
    @POST("oauth/token")
    suspend fun getAccessToken(
        @Query("client_id") clientId: String,
        @Query("client_secret") clientSecret: String
    ): GCashTokenResponse

    @POST("payments/create")
    suspend fun createPayment(
        @Body paymentRequest: GCashPaymentRequest,
        @Header("Authorization") authorization: String
    ): GCashPaymentResponse

    @GET("payments/verify/{referenceId}")
    suspend fun verifyPayment(
        @Path("referenceId") referenceId: String,
        @Header("Authorization") authorization: String
    ): GCashVerificationResponse
}
