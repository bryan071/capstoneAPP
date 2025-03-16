package com.project.webapp

import FarmerEditProfileScreen
import MarketDashboard
import OrganizationDashboard
import SplashScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.farmaid.ui.notifications.FarmerNotificationScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.components.EditProductScreen
import com.project.webapp.dashboards.BottomNavigationBar
import com.project.webapp.dashboards.FarmerDashboard
import com.project.webapp.components.FarmerMarketScreen
import com.project.webapp.components.ProductDetailsScreen
import com.project.webapp.pages.ForgotPass
import com.project.webapp.pages.Login
import com.project.webapp.components.profiles.FarmerProfileScreen
import com.project.webapp.pages.Register



@Composable
fun AppNav(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
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
                ForgotPass(modifier, navController, authViewModel)
            }
            composable(Route.FARMER_DASHBOARD) {
                FarmerDashboard(modifier, navController, authViewModel)
            }
            composable(Route.PROFILE) {
                FarmerProfileScreen(modifier, navController, authViewModel)
            }
            composable(Route.MARKET) {
                FarmerMarketScreen(modifier, navController, authViewModel)
            }
            composable(Route.NOTIFICATION) {
                FarmerNotificationScreen(
                    modifier,
                    navController,
                    authViewModel,
                    firestore = FirebaseFirestore.getInstance()
                )
            }
            composable(Route.EDIT_PROFILE) {
                FarmerEditProfileScreen(navController)
            }
            composable(Route.MARKET_DASHBOARD) {
                MarketDashboard(modifier, navController, authViewModel)
            }
            composable(Route.ORG_DASHBOARD) {
                OrganizationDashboard(modifier, navController, authViewModel)
            }
            composable("productDetails/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""
                ProductDetailsScreen(navController, productId, FirebaseFirestore.getInstance())
            }
            composable("editProduct/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                EditProductScreen(navController, productId, FirebaseFirestore.getInstance())
            }
        }
    }
}




