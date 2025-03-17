package com.project.webapp.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController


@Composable
fun CheckoutScreen(navController: NavController, cartViewModel: CartViewModel) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val totalPrice by cartViewModel.totalPrice.collectAsStateWithLifecycle()

    var showDialog by remember { mutableStateOf(false) }
    var isProcessing by remember { mutableStateOf(false) }

    Scaffold(
        topBar = { TopBar(navController, cartViewModel, userType = "market") }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Text(
                text = "Checkout",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            LazyColumn {
                items(cartItems) { item ->
                    CartItemRow(item, cartViewModel)
                }
            }

            Text(
                text = "Total: $${totalPrice}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Button(
                onClick = {
                    navController.navigate("payment/$totalPrice")
                    isProcessing = true
                    cartViewModel.completePurchase()
                    showDialog = true
                    isProcessing = false
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                enabled = cartItems.isNotEmpty()
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                } else {
                    Text("Confirm Purchase")
                }
            }
            AnimatedVisibility(visible = showDialog) {
                OrderSuccessDialog(onDismiss = { showDialog = false }, navController)
            }
            // Show success dialog
            if (showDialog) {
                OrderSuccessDialog(onDismiss = { showDialog = false }, navController)
            }
        }
    }
}
    @Composable
    fun OrderSuccessDialog(onDismiss: () -> Unit, navController: NavController) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("🎉 Order Successful!", fontWeight = FontWeight.Bold) },
            text = { Text("Your order has been placed successfully. Thank you for shopping with us!") },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(onClick = {
                        onDismiss()
                        navController.navigate("market") // Redirect to Market
                    }) {
                        Text("Back to Market")
                    }

                    Button(onClick = {
                        onDismiss()
                        navController.navigate("orderHistory") // Redirect to Order History
                    }) {
                        Text("View Orders")
                    }
                }
            }
        )
    }

