package com.project.webapp


import FarmerEditProfileScreen
import SplashScreen
import android.annotation.SuppressLint
import android.app.Activity
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
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
import com.project.webapp.components.EditProductScreen
import com.project.webapp.dashboards.BottomNavigationBar
import com.project.webapp.dashboards.FarmerDashboard
import com.project.webapp.components.FarmerMarketScreen
import com.project.webapp.components.payment.GcashScreen
import com.project.webapp.components.payment.PaymentScreen
import com.project.webapp.components.ProductDetailsScreen
import com.project.webapp.pages.ForgotPass
import com.project.webapp.pages.Login
import com.project.webapp.components.profiles.FarmerProfileScreen
import com.project.webapp.pages.Register
import androidx.navigation.navDeepLink
import com.project.webapp.components.payment.DonationScreen
import com.project.webapp.components.payment.OrdersScreen
import com.project.webapp.components.payment.ReceiptScreen
import com.project.webapp.components.profiles.RecentActivityScreen

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
                route = "checkoutScreen/{userType}/{totalPrice}?paymentMethod={paymentMethod}&referenceId={referenceId}&items={items}",
                arguments = listOf(
                    navArgument("userType") { type = NavType.StringType },
                    navArgument("totalPrice") { type = NavType.FloatType }
                ),
                deepLinks = listOf(navDeepLink { uriPattern = "com.project.webapp://checkout/{userType}/{totalPrice}?paymentMethod={paymentMethod}&referenceId={referenceId}&items={items}" })
            ) { backStackEntry ->
                CheckoutScreen(
                    navController = navController,
                    cartViewModel = viewModel(),
                    totalPrice = backStackEntry.arguments?.getFloat("totalPrice")?.toDouble() ?: 0.0,
                    userType = backStackEntry.arguments?.getString("userType") ?: "",
                    paymentMethod = backStackEntry.arguments?.getString("paymentMethod") ?: "COD",
                    referenceId = backStackEntry.arguments?.getString("referenceId") ?: "",
                    itemsJson = backStackEntry.arguments?.getString("items")
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
                route = "receiptScreen/{orderNumber}",
                arguments = listOf(
                    navArgument("orderNumber") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                val orderNumber = backStackEntry.arguments?.getString("orderNumber")

                Log.d("ReceiptScreen", "Received orderNumber: $orderNumber")

                if (orderNumber == null) {
                    Log.e("ReceiptScreen", "Missing orderNumber for receiptScreen")
                    navController.navigate("errorScreen") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                    return@composable
                }

                val receiptData = cartViewModel.getReceiptData(orderNumber)

                if (receiptData == null) {
                    Log.e("ReceiptScreen", "No receipt data found for orderNumber: $orderNumber")
                    navController.navigate("errorScreen") {
                        popUpTo(navController.graph.startDestinationId)
                        launchSingleTop = true
                    }
                    return@composable
                }
                ReceiptScreen(
                    navController = navController,
                    cartViewModel = cartViewModel,
                    cartItems = receiptData.cartItems ?: emptyList(),
                    totalPrice = receiptData.totalPrice ?: 0.0,
                    userType = receiptData.userType ?: "Unknown",
                    sellerNames = receiptData.sellerNames ?: emptyMap(),
                    paymentMethod = receiptData.paymentMethod ?: "COD",
                    referenceId = receiptData.referenceId ?: "",
                    organization = receiptData.organization,
                    isDonation = receiptData.isDonation ?: false,
                    orderNumber = orderNumber
                )
            }

            composable("errorScreen") {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Something went wrong. Please try again.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.titleLarge
                    )
                    Button(onClick = { navController.popBackStack() }) {
                        Text("Go Back")
                    }
                }
            }
            composable(
                route = "gcashScreen/{totalPrice}/{ownerId}",
                arguments = listOf(
                    navArgument("totalPrice") { type = NavType.FloatType },
                    navArgument("ownerId") { type = NavType.StringType }
                )
            ) { backStackEntry ->
                GcashScreen(
                    navController = navController,
                    totalPrice = backStackEntry.arguments?.getFloat("totalPrice")?.toDouble() ?: 0.0,
                    ownerId = backStackEntry.arguments?.getString("ownerId") ?: "",
                    cartViewModel = viewModel()
                )
            }

            composable(
                route = "chat/{chatRoomId}/{isAdmin}",
                arguments = listOf(
                    navArgument("chatRoomId") { type = NavType.StringType },
                    navArgument("isAdmin") { type = NavType.BoolType }
                )
            ) { backStackEntry ->
                Log.d("NavDebug", "Entered ChatScreen with chatRoomId=${backStackEntry.arguments?.getString("chatRoomId")}")
                val chatRoomId = backStackEntry.arguments?.getString("chatRoomId") ?: ""
                val isAdmin = backStackEntry.arguments?.getBoolean("isAdmin") ?: false

                val chatViewModel: ChatViewModel = viewModel()

                ChatScreen(
                    navController = navController,
                    viewModel = chatViewModel,
                    chatRoomId = chatRoomId,
                    isAdmin = isAdmin
                )
            }



            composable("recent_activity_screen") {
                val state = authViewModel.authState.observeAsState().value

                if (state is AuthState.Authenticated) {
                    RecentActivityScreen(
                        userType = state.userType,
                        userId = state.userId
                    )
                } else {
                    // Show fallback or error
                    Text("User not authenticated")
                }
            }

        }
    }
}




