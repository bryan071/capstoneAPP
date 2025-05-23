    package com.project.webapp.datas

    import com.google.firebase.Timestamp

    data class Transaction(
        val id: String = "",
        val buyerId: String = "",
        val item: String = "",
        val quantity: Int = 0,
        val totalAmount: Double = 0.0,
        val organization: String? = null,
        val transactionType: String = "",
        val status: String = "",
        val timestamp: Timestamp = Timestamp.now(),
        val paymentMethod: String = "",
        val referenceId: String? = null
    )
