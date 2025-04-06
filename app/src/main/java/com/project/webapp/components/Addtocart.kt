package com.project.webapp.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.project.webapp.datas.CartItem
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.project.webapp.R


@Composable
fun CartScreen(cartViewModel: CartViewModel, navController: NavController) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val totalPrice by cartViewModel.totalCartPrice.collectAsStateWithLifecycle()


    var showDialog by remember { mutableStateOf(false) } // Controls the success dialog
    var isLoading by remember { mutableStateOf(false) }  // Controls button loading state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Cart", color = Color.White) },
                navigationIcon = {
                    // Back Button
                    IconButton(
                        onClick = { navController.popBackStack() },
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.backbtn),
                            contentDescription = "Back",
                            tint = Color.Unspecified
                        )
                    }
                },
                backgroundColor = Color(0xFF0DA54B), // Green theme
                elevation = 4.dp
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                items(cartItems) { item ->
                    CartItemRow(item, cartViewModel)
                }
            }

            Text(
                text = "Total: ₱${"%.2f".format(totalPrice)}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            Button(
                onClick = {
                    navController.navigate("paymentScreen/null/{totalPrice}") // ✅ Navigate only for cart checkout
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                enabled = cartItems.isNotEmpty(), // ✅ Enable only if cart is not empty
                colors = ButtonDefaults.buttonColors(backgroundColor  = Color(0xFF0DA54B)) // ✅ Use correct color parameter
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                } else {
                    Text("Proceed to Checkout", color = Color.White)
                }
            }
        }
    }
}


@Composable
fun CartItemRow(cartItem: CartItem, cartViewModel: CartViewModel? = null) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        elevation = 4.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cartItem.imageUrl,
                contentDescription = "Product Image",
                modifier = Modifier.size(80.dp)
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp)
            ) {
                Text(text = cartItem.name, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(text = "Price: ₱${cartItem.price}0")
                Text(text = "Unit Weight: ${cartItem.weight.toInt()} ${cartItem.unit}")
                Text(text = "Quantity: ${cartItem.quantity}")
            }
            IconButton(
                onClick = { cartViewModel?.removeFromCart(cartItem.productId) }
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Cart",
                    tint = Color.Red // Red delete button
                )
            }
        }
    }
}



