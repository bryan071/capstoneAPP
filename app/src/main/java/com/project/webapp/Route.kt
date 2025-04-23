package com.project.webapp

object Route {
    // Authentication Routes
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT_PASSWORD = "forgot"

    // Farmer User Routes
    const val FARMER_DASHBOARD = "farmerdashboard"
    const val PROFILE = "profile"
    const val EDIT_PROFILE = "editprofile"
    const val MARKET = "market"
    const val NOTIFICATION = "notification"
    const val EDIT_PRODUCT = "editProduct/{productId}"

    const val ORDERS = "order"
    // Organization Routes
    const val ORG_DASHBOARD = "orgdashboard"

    const val PRODUCT_DETAILS = "product_details/{productId}"
}
