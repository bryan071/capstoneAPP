package com.project.webapp.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Money
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.R
import com.project.webapp.datas.CartItem
import com.project.webapp.datas.Product

@Composable
fun PaymentScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    directBuyProductId: String? = null,
    directBuyPrice: String? = null
) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val cartTotalState by cartViewModel.totalCartPrice.collectAsState()
    val amount = directBuyPrice?.toDoubleOrNull() ?: cartTotalState

    var directBuyProduct by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(directBuyProductId) {
        directBuyProductId?.let { id ->
            cartViewModel.getProductById(id) { product ->
                directBuyProduct = product
            }
        }
    }

    val displayItems: List<CartItem> = remember(directBuyProduct, cartItems) {
        if (directBuyProduct != null) {
            listOf(
                CartItem(
                    productId = directBuyProduct!!.prodId,
                    name = directBuyProduct!!.name,
                    price = directBuyProduct!!.price,
                    quantity = 1,
                    imageUrl = directBuyProduct!!.imageUrl,
                    isDirectBuy = true
                )
            )
        } else {
            cartItems
        }
    }

    var showCheckout by remember { mutableStateOf(false) }

    if (showCheckout) {
        // Navigate to the CheckoutScreen with necessary details
        val userType = if (directBuyProductId != null) "direct_buying" else "cart_checkout"
        CheckoutScreen(navController, cartViewModel, amount, displayItems)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(
                    painter = painterResource(id = R.drawable.backbtn),
                    contentDescription = "Back",
                    tint = Color.Unspecified
                )
            }

            Text("Choose Payment Method", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Total Price: ₱${"%.2f".format(amount)}", style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.SemiBold)

            LazyColumn {
                items(displayItems.size) { index ->
                    CartItemRow(displayItems[index])
                }
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.elevatedCardElevation(4.dp),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Button(
                        onClick = { navController.navigate("gcashScreen/${"%.2f".format(amount)}") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
                    ) {
                        Text("Pay with GCash", color = Color.White)
                    }

                    Button(
                        onClick = {
                            // Navigate to CheckoutScreen with total price and display items
                            navController.navigate("checkoutScreen/${if (directBuyProductId != null) "direct_buying" else "cart_checkout"}/${amount.toFloat()}")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                        enabled = displayItems.isNotEmpty()
                    ) {
                        Text("Cash on Delivery (COD)", color = Color.White)
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    totalPrice: Double,
    cartItems: List<CartItem>
) {
    var showReceipt by remember { mutableStateOf(false) }

    if (showReceipt) {
        ReceiptScreen(navController, cartViewModel, cartItems, totalPrice)
    } else {
        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Checkout", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            LazyColumn {
                items(cartItems.size) { index ->  // ✅ Use cartItems instead of displayItems
                    CartItemRow(cartItems[index])
                }
            }

            Text("Total: ₱${"%.2f".format(totalPrice)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Button(
                onClick = {
                    cartViewModel.completePurchase("COD", "", "")
                    showReceipt = true
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                enabled = cartItems.isNotEmpty()
            ) {
                Text("Confirm Purchase")
            }
        }
    }
}




@Composable
fun ReceiptScreen(
    navController: NavController,cartViewModel: CartViewModel,
    cartItems: List<CartItem>,
    totalPrice: Double,
    userType: String?= "defaultUser" // Ensure userType is passed as a parameter
) {
    Scaffold(
        topBar = {
            userType?.let { type ->
                TopBar(navController, cartViewModel, userType = type)
            } // Fallback in case userType is null
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Text("Order Receipt", fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(cartItems.size) { index ->
                    CartItemRow(cartItems[index])
                }
            }

            Text("Total: ₱${"%.2f".format(totalPrice)}", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(onClick = { navController.navigate("market") }) {
                    Text("Back to Market")
                }
                Button(onClick = { navController.navigate("orderHistory") }) {
                    Text("View Orders")
                }
            }
        }
    }
}



@Composable
fun GcashScreen(navController: NavController, totalPrice: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        IconButton(onClick = { navController.popBackStack() }) {
            Icon(
                painter = painterResource(id = R.drawable.backbtn),
                contentDescription = "Back",
                tint = Color.Unspecified
            )
        }

        Text(
            text = "GCash Payment",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Amount to Pay: ₱${totalPrice}",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        Button(
            onClick = { /* Implement GCash Payment Logic */ },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
        ) {
            Icon(
                painter = painterResource(id = R.drawable.gcash_icon),
                contentDescription = "GCash Icon",
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Proceed with GCash")
        }
    }
}
