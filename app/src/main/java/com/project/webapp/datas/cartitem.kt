package com.project.webapp.datas

data class CartItem(
    val productId: String = "",
    val name: String = "",
    val price: Double = 0.0,
    val quantity: Int = 1,
    val imageUrl: String = "",
    val sellerId: String = "",
    val weight: Double = 0.0,
    val unit: String = "",
    val isDirectBuy: Boolean = false,
    val sellerName: String = ""
) {
    // Empty constructor needed for Firebase
    constructor() : this("", "", 0.0, 1, "", "", 0.0, "", false)
}

