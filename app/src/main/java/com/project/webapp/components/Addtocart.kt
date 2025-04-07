package com.project.webapp.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.project.webapp.R
import com.project.webapp.datas.CartItem

@Composable
fun CartScreen(cartViewModel: CartViewModel, navController: NavController) {
    val cartItems by cartViewModel.cartItems.collectAsStateWithLifecycle()
    val subtotalPrice by cartViewModel.totalCartPrice.collectAsStateWithLifecycle()
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val primaryGreen = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardBackground = Color.White

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "My Cart",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            painter = painterResource(id = R.drawable.backbtn),
                            contentDescription = "Back",
                            tint = Color.Unspecified
                        )
                    }
                },
                backgroundColor = primaryGreen,
                elevation = 0.dp
            )
        },
        backgroundColor = backgroundColor
    ) { paddingValues ->
        if (cartItems.isEmpty()) {
            // Empty cart state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.backbtn), // Replace with an appropriate cart icon
                    contentDescription = "Empty Cart",
                    modifier = Modifier.size(100.dp),
                    tint = primaryGreen.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Your cart is empty",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Add items to start shopping",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .background(backgroundColor)
            ) {
                Text(
                    text = "${cartItems.size} item${if (cartItems.size > 1) "s" else ""} in your cart",
                    modifier = Modifier.padding(16.dp, 12.dp),
                    color = Color.Gray,
                    fontSize = 14.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    items(cartItems) { item ->
                        CartItemRow(cartItem = item, cartViewModel = cartViewModel)
                    }
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth(),
                    backgroundColor = cardBackground,
                    elevation = 8.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Subtotal",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            Text(
                                text = "₱${"%.2f".format(subtotalPrice)}",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        // Delivery fee row (optional)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Delivery Fee",
                                fontSize = 16.sp,
                                color = Color.Gray
                            )
                            val shippingFee = 50.0

                            Text(
                                text = "₱${"%.2f".format(shippingFee)}",
                                fontSize = 16.sp
                            )
                        }

                        Divider()

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Total",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            val shippingFee = 50.0
                            Text(
                                text = "₱${"%.2f".format(subtotalPrice + shippingFee)}",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = primaryGreen
                            )
                        }

                        Button(
                            onClick = {
                                navController.navigate("paymentScreen/null/${cartViewModel.getTotalCartPrice()}")
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp),
                            enabled = cartItems.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(backgroundColor = primaryGreen),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    "Proceed to Checkout",
                                    color = Color.White,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CartItemRow(
    cartItem: CartItem,
    sellerName: String = "",
    cartViewModel: CartViewModel? = null
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = 2.dp,
        shape = RoundedCornerShape(12.dp),
        backgroundColor = Color.White
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            AsyncImage(
                model = cartItem.imageUrl,
                contentDescription = "Product Image",
                modifier = Modifier
                    .size(90.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
            ) {
                Text(
                    text = cartItem.name,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "₱${cartItem.price}0",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0DA54B)
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${cartItem.weight.toInt()} ${cartItem.unit}",
                    fontSize = 14.sp,
                    color = Color.Gray
                )

                if (sellerName.isNotBlank()) {
                    Text(
                        text = "Seller: $sellerName",
                        fontSize = 14.sp,
                        fontStyle = FontStyle.Italic,
                        color = Color.Gray
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Quantity selector with increment/decrement (would need implementation)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(Color(0xFFF0F0F0), RoundedCornerShape(8.dp))
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color.LightGray)
                                .clickable {
                                    if (cartItem.quantity > 1) {
                                        cartViewModel?.updateCartItemQuantity(cartItem.productId, cartItem.quantity - 1)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "−",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "${cartItem.quantity}",
                            modifier = Modifier.padding(horizontal = 12.dp),
                            fontWeight = FontWeight.Medium
                        )

                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF0DA54B))
                                .clickable {
                                    cartViewModel?.updateCartItemQuantity(cartItem.productId, cartItem.quantity + 1)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "+",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            IconButton(
                onClick = { cartViewModel?.removeFromCart(cartItem.productId) },
                modifier = Modifier
                    .size(32.dp)
                    .background(Color(0xFFFFEBEE), CircleShape)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove from Cart",
                    tint = Color(0xFFE53935),
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}



