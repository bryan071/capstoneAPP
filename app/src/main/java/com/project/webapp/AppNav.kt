package com.project.webapp

import FarmerEditProfileScreen
import MarketDashboard
import OrganizationDashboard
import SplashScreen
import android.annotation.SuppressLint
import android.app.Activity
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.farmaid.ui.notifications.FarmerNotificationScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.Viewmodel.AuthState
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.components.CartScreen
import com.project.webapp.components.CheckoutScreen
import com.project.webapp.components.EditProductScreen
import com.project.webapp.dashboards.BottomNavigationBar
import com.project.webapp.dashboards.FarmerDashboard
import com.project.webapp.components.FarmerMarketScreen
import com.project.webapp.components.GcashScreen
import com.project.webapp.components.PaymentScreen
import com.project.webapp.components.ProductDetailsScreen
import com.project.webapp.pages.ForgotPass
import com.project.webapp.pages.Login
import com.project.webapp.components.profiles.FarmerProfileScreen
import com.project.webapp.pages.Register

@SuppressLint("ContextCastToActivity")
@Composable
fun AppNav(modifier: Modifier = Modifier, authViewModel: AuthViewModel, cartViewModel: CartViewModel) {
    val navController = rememberNavController()
    val authState = authViewModel.authState.observeAsState().value

    // State to control visibility of the bottom navigation
    val isBottomNavVisible = remember { mutableStateOf(true) }

    // Listen for navigation changes to show/hide the bottom navigation bar
    LaunchedEffect(navController) {
        navController.addOnDestinationChangedListener { _, destination, _ ->
            isBottomNavVisible.value = when (destination.route) {
                Route.EDIT_PROFILE -> false
                Route.PROFILE -> true
                else -> true
            }
        }
    }

    Scaffold(
        bottomBar = {
            if (authState is AuthState.Authenticated) {
                BottomNavigationBar(navController)
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.LOGIN, // Updated reference
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Route.SPLASH) {
                SplashScreen(navController)
            }
            composable(Route.LOGIN) {
                Login(modifier, navController, authViewModel)
            }
            composable(Route.REGISTER) {
                Register(modifier, navController, authViewModel)
            }
            composable(Route.FORGOT_PASSWORD) {
                val activity = LocalContext.current as Activity // Get the current activity
                ForgotPass(modifier, navController, authViewModel, activity, cartViewModel)
            }
            composable(Route.FARMER_DASHBOARD) {
                FarmerDashboard(modifier, navController, authViewModel, cartViewModel)
            }
            composable(Route.PROFILE) {
                FarmerProfileScreen(modifier, navController, authViewModel)
            }
            composable(Route.MARKET) {
                FarmerMarketScreen(modifier, navController, authViewModel, cartViewModel)
            }
            composable(Route.NOTIFICATION) {
                FarmerNotificationScreen(
                    modifier,
                    navController,
                    authViewModel,
                    firestore = FirebaseFirestore.getInstance(),cartViewModel
                )
            }
            composable(Route.EDIT_PROFILE) {
                FarmerEditProfileScreen(navController)
            }
            composable(Route.MARKET_DASHBOARD) {
                MarketDashboard(modifier, navController, authViewModel, cartViewModel)
            }
            composable(Route.ORG_DASHBOARD) {
                OrganizationDashboard(modifier, navController, authViewModel, cartViewModel)
            }
            composable("productDetails/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""

                ProductDetailsScreen(
                    navController = navController,
                    productId = productId,
                    firestore = FirebaseFirestore.getInstance()
                )
            }

            composable("checkout/{productId}/{productName}/{productPrice}/{productImageUrl}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                val productName = backStackEntry.arguments?.getString("productName")
                val productPrice = backStackEntry.arguments?.getString("productPrice")
                val productImageUrl = backStackEntry.arguments?.getString("productImageUrl")

                val cartViewModel: CartViewModel = viewModel()

                CheckoutScreen(
                    navController = navController,
                    cartViewModel = cartViewModel
                )
            }
            composable("editProduct/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                EditProductScreen(navController, productId, FirebaseFirestore.getInstance())
            }
            composable("cart") { CartScreen(cartViewModel, navController)
            }
            composable("payment/{totalPrice}") { backStackEntry ->
                val totalPrice = backStackEntry.arguments?.getString("totalPrice") ?: "0"

                PaymentScreen(navController, totalPrice, cartViewModel
                )
            }
            composable("gcashScreen/{totalPrice}") { backStackEntry ->
                val totalPrice = backStackEntry.arguments?.getString("totalPrice") ?: "0.00"

                GcashScreen(navController, totalPrice)
            }




        }

    }
}




