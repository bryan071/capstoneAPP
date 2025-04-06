package com.project.webapp.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
    val formattedAmount = "%.2f".format(amount)

    // 👇 Define userType based on whether this is direct buy or not
    val userType = if (directBuyProductId != null) "direct_buying" else "cart_checkout"

    var directBuyProduct by remember { mutableStateOf<Product?>(null) }

    // Fetch the product for direct buy
    LaunchedEffect(directBuyProductId) {
        directBuyProductId?.let { id ->
            cartViewModel.getProductById(id) { product ->
                directBuyProduct = product
            }
        }
    }

    // Determine what items to show: full cart or 1-item direct buy
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
            "Choose Payment Method",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Text(
            "Total Price: ₱$formattedAmount",
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.SemiBold
        )

        LazyColumn {
            itemsIndexed(displayItems) { _, item ->
                CartItemRow(item)
            }
        }

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.elevatedCardElevation(4.dp),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = {
                        // Navigate to GCash with amount
                        navController.navigate("gcashScreen/$formattedAmount")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF))
                ) {
                    Text("Pay with GCash", color = Color.White)
                }

                Button(
                    onClick = {
                        // ✅ Set correct items and go to checkout
                        cartViewModel.setCheckoutItems(displayItems)
                        navController.navigate("checkoutScreen/$userType/${amount.toFloat()}")
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


@Composable
fun CheckoutScreen(
    navController: NavController,
    cartViewModel: CartViewModel,
    totalPrice: Double,
    cartItems: List<CartItem>,
    userType: String
) {
    var showReceipt by remember { mutableStateOf(false) }

    if (showReceipt) {
        ReceiptScreen(navController, cartViewModel, cartItems, totalPrice, userType)
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Checkout", fontSize = 24.sp, fontWeight = FontWeight.Bold)

            LazyColumn {
                itemsIndexed(cartItems) { _, item ->
                    CartItemRow(item)
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
    navController: NavController,
    cartViewModel: CartViewModel,
    cartItems: List<CartItem>,
    totalPrice: Double,
    userType: String = "defaultUser"
) {
    Scaffold(
        topBar = {
            TopBar(navController, cartViewModel, userType = userType)
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
                itemsIndexed(cartItems) { _, item ->
                    CartItemRow(item)
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
            text = "Amount to Pay: ₱$totalPrice",
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
