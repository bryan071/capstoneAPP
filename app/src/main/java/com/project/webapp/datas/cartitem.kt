package com.project.webapp.datas

import com.google.firebase.firestore.PropertyName

data class CartItem(
    @get:PropertyName("productId")
    @set:PropertyName("productId")
    var productId: String = "",

    @get:PropertyName("name")
    @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("price")
    @set:PropertyName("price")
    var price: Double = 0.0,

    @get:PropertyName("quantity")
    @set:PropertyName("quantity")
    var quantity: Int = 1,

    @get:PropertyName("imageUrl")
    @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("sellerId")
    @set:PropertyName("sellerId")
    var sellerId: String = "",

    @get:PropertyName("weight")
    @set:PropertyName("weight")
    var weight: Double = 0.0,

    @get:PropertyName("unit")
    @set:PropertyName("unit")
    var unit: String = "",

    @get:PropertyName("isDirectBuy")
    @set:PropertyName("isDirectBuy")
    var isDirectBuy: Boolean = false
)