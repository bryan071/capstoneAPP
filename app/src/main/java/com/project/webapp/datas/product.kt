package com.project.webapp.datas

data class Product(
    val prodId: String = "",
    val ownerId: String = "",
    val category: String = "",
    val imageUrl: String = "",
    val name: String = "",
    val description: String = "",
    val quantity: Double = 0.0,
    val quantityUnit: String = "",
    val price: Double = 0.0,
    val cityName: String = "",
    val listedAt: Long = System.currentTimeMillis()
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", "", "", "", "",0.0, "", 0.0, "", 0L)
}
