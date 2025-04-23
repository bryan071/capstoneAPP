package com.project.webapp

import FarmerEditProfileScreen
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
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.project.webapp.components.FarmerNotificationScreen
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.Viewmodel.AuthState
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.Viewmodel.ChatViewModel
import com.project.webapp.components.CartScreen
import com.project.webapp.components.ChatScreen
import com.project.webapp.components.CheckoutScreen
import com.project.webapp.components.payment.DonationScreen
import com.project.webapp.components.EditProductScreen
import com.project.webapp.dashboards.BottomNavigationBar
import com.project.webapp.dashboards.FarmerDashboard
import com.project.webapp.components.FarmerMarketScreen
import com.project.webapp.components.payment.GcashScreen
import com.project.webapp.components.payment.PaymentScreen
import com.project.webapp.components.ProductDetailsScreen
import com.project.webapp.components.delivery.OrdersScreen
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
                val chatViewModel: ChatViewModel = viewModel()
                FarmerDashboard(modifier, navController, authViewModel, cartViewModel, chatViewModel)
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

            composable(Route.ORDERS) {
                OrdersScreen(
                    navController = navController
                )
            }

            composable("productDetails/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId") ?: ""

                // You can get ViewModels either this way or via hiltViewModel() if using Hilt
                val cartViewModel: CartViewModel = viewModel()
                val authViewModel: AuthViewModel = viewModel()

                ProductDetailsScreen(
                    navController = navController,
                    productId = productId,
                    firestore = FirebaseFirestore.getInstance(),
                    authViewModel = authViewModel,
                    cartViewModel = cartViewModel
                )
            }


            composable(
                route = "checkoutScreen/{userType}/{totalPrice}",
                arguments = listOf(
                    navArgument("userType") { type = NavType.StringType },
                    navArgument("totalPrice") { type = NavType.FloatType } // Use FloatType for now
                )
            ) { backStackEntry ->
                val userType = backStackEntry.arguments?.getString("userType") ?: "cart_checkout"
                val totalPrice = backStackEntry.arguments?.getFloat("totalPrice")?.toDouble()
                    ?: 0.0 // Convert to Double here

                CheckoutScreen(
                    navController = navController,
                    cartViewModel = cartViewModel,
                    totalPrice = totalPrice,
                    cartItems = cartViewModel.checkoutItems,
                    userType = userType
                )
            }

            composable("editProduct/{productId}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                EditProductScreen(navController, productId, FirebaseFirestore.getInstance())
            }

            composable("cart") { CartScreen(cartViewModel, navController)
            }

            composable("paymentScreen/{productId}/{totalPrice}") { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                val totalPrice = backStackEntry.arguments?.getString("totalPrice")

                PaymentScreen(
                    navController = navController,
                    cartViewModel = cartViewModel,
                    directBuyProductId = productId,
                    directBuyPrice = totalPrice
                )
            }
            composable(
                route = "donationScreen/{productId}/{price}",
                arguments = listOf(
                    navArgument("productId") { type = NavType.StringType },
                    navArgument("price") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val productId = backStackEntry.arguments?.getString("productId")
                val price = backStackEntry.arguments?.getString("price")

                DonationScreen(
                    navController = navController,
                    cartViewModel = cartViewModel,
                    directBuyProductId = productId,
                    directBuyPrice = price
                )
            }
            composable(
                "gcashScreen/{totalPrice}/{ownerId}",
                arguments = listOf(
                    navArgument("totalPrice") { type = NavType.StringType },
                    navArgument("ownerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val totalPrice = backStackEntry.arguments?.getString("totalPrice") ?: "0.00"
                val ownerId = backStackEntry.arguments?.getString("ownerId") ?: ""

                GcashScreen(navController, totalPrice, FirebaseFirestore.getInstance(), ownerId)
            }


            composable("chat") {
                val chatViewModel = viewModel<ChatViewModel>()
                val defaultChatRoomId = "default_room" // Define a default chat room
                ChatScreen(navController = navController, viewModel = chatViewModel, chatRoomId = defaultChatRoomId, isAdmin = false)
            }

            composable("chat/{chatRoomId}") { backStackEntry ->
                val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: "default_room"
                val chatViewModel = viewModel<ChatViewModel>()  // Ensure ViewModel is provided
                ChatScreen(navController = navController, viewModel = chatViewModel, chatRoomId = chatRoomId, isAdmin = false)
            }
        }
    }
}




