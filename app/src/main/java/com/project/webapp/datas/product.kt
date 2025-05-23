package com.project.webapp.datas

import com.google.firebase.Timestamp

data class Product(
    var prodId: String = "",
    val ownerId: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val name: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val quantityUnit: String = "unit",
    val price: Double = 0.0,
    val cityName: String = "",
    val timestamp: Timestamp = Timestamp.now()
)

