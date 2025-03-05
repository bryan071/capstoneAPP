package com.project.webapp

import NotificationScreen
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.pages.BottomNavigationBar
import com.project.webapp.pages.Dashboard
import com.project.webapp.pages.ForgotPass
import com.project.webapp.pages.Login
import com.project.webapp.pages.MarketScreen
import com.project.webapp.pages.ProfileScreen
import com.project.webapp.pages.Register



@Composable
fun AppNav(modifier: Modifier = Modifier, authViewModel: AuthViewModel) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            if (authViewModel.authState.observeAsState().value == AuthState.Authenticated) {
                BottomNavigationBar(navController) // Show bottom bar only when logged in
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Route.login,
            modifier = Modifier.padding(paddingValues) // This ensures content does not overlap bottom bar
        ) {
            composable(Route.login) {
                Login(modifier, navController, authViewModel)
            }
            composable(Route.register) {
                Register(modifier, navController, authViewModel)
            }
            composable(Route.forgot) {
                ForgotPass(modifier, navController, authViewModel)
            }
            composable(Route.dashboard) {
                Dashboard(modifier, navController, authViewModel)
            }
            composable(Route.profiie) {
                ProfileScreen(modifier, navController, authViewModel)
            }
            composable(Route.market) {
                MarketScreen(modifier, navController, authViewModel)
            }
            composable(Route.notification) {
                NotificationScreen(modifier, navController, authViewModel, firestore = FirebaseFirestore.getInstance())
            }
        }
    }
}

