package com.project.webapp.components

import CartViewModel
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import kotlinx.coroutines.delay
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.scale
import com.project.webapp.R

@Composable
fun TopBar(navController: NavController, cartViewModel: CartViewModel, userType: String) {
    val cartItems by cartViewModel.cartItems.collectAsState()
    val cartIconShake by cartViewModel.cartIconShake.collectAsState()

    var animate by remember { mutableStateOf(false) }

    LaunchedEffect(cartIconShake) {
        if (cartIconShake) {
            animate = true
            delay(500)
            animate = false
            cartViewModel.resetCartIconShake()
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "Logo Image",
            modifier = Modifier.size(150.dp)
        )

        // Show cart icon for "business" and "household" users
        if (userType == "business" || userType == "household") {
            Row(modifier = Modifier, horizontalArrangement = Arrangement.End) {
                Box(modifier = Modifier.wrapContentSize()) {
                    IconButton(onClick = { navController.navigate("cart") }) {
                        val scale by animateFloatAsState(if (animate) 1.3f else 1f)
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Cart",
                            tint = Color.Black,
                            modifier = Modifier.size(28.dp).scale(scale)
                        )
                    }
                    if (cartItems.isNotEmpty()) {
                        Text(
                            text = cartItems.size.toString(),
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier
                                .background(Color.Red, shape = CircleShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                .align(Alignment.TopEnd)
                        )
                    }
                }
            }
        }
    }
}

