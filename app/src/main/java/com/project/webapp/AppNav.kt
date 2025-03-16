package com.project.webapp


import FarmerEditProfileScreen
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
import com.project.webapp.farmers.BottomNavigationBar
import com.project.webapp.farmers.FarmerDashboard
import com.project.webapp.farmers.FarmerMarketScreen
import com.project.webapp.pages.ForgotPass
import com.project.webapp.pages.Login
import com.project.webapp.farmers.profiles.FarmerProfileScreen
import com.project.webapp.market.MarketDashboard
import com.project.webapp.org.OrgDashboard
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
                Route.editprofile -> false
                Route.profile -> true // Corrected typo here
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
            startDestination = Route.login,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(Route.splash) {
                SplashScreen(navController)
            }
            composable(Route.login) {
                Login(modifier, navController, authViewModel)
            }
            composable(Route.register) {
                Register(modifier, navController, authViewModel)
            }
            composable(Route.forgot) {
                ForgotPass(modifier, navController, authViewModel)
            }
            composable(Route.farmerdashboard) {
                FarmerDashboard(modifier, navController, authViewModel)
            }
            composable(Route.profile) {
                FarmerProfileScreen(modifier, navController, authViewModel)
            }
            composable(Route.market) {
                FarmerMarketScreen(modifier, navController, authViewModel)
            }
            composable(Route.notification) {
                FarmerNotificationScreen(modifier, navController, authViewModel, firestore = FirebaseFirestore.getInstance())
            }
            composable(Route.editprofile) {
                FarmerEditProfileScreen(navController)
            }
            composable(Route.marketdashboard) {
                MarketDashboard(modifier, navController, authViewModel)
            }
            composable(Route.orgdashboard) {
                OrgDashboard(modifier, navController, authViewModel)
            }
        }
    }
}



