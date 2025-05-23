package com.project.webapp.datas

    data class CartItem(
        var productId: String = "",
        val name: String = "",
        val price: Double = 0.0,
        val quantity: Int = 1,
        val imageUrl: String = "",
        val sellerId: String = "",
        val weight: Double = 0.0,
        val unit: String = "",
        val isDirectBuy: Boolean = false

    )

