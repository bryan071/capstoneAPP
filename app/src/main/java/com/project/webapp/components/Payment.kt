package com.project.webapp.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.project.webapp.R

@Composable
fun PaymentScreen(navController: NavController, totalPrice: String?, cartViewModel: CartViewModel) {
    val cartTotalState = cartViewModel.totalCartPrice.collectAsState()
    val amount = totalPrice?.toDoubleOrNull() ?: cartTotalState.value

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Back Button using Drawable
        IconButton(onClick = { navController.popBackStack() }) {
            Image(
                painter = painterResource(id = R.drawable.backbtn),
                contentDescription = "Back"
            )
        }

        Text(text = "Choose Payment Method", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Total Price: ₱${"%.2f".format(amount)}", style = MaterialTheme.typography.bodyLarge)

        Button(
            onClick = { navController.navigate("gcashScreen/$amount") },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Pay with GCash")
        }

        Button(
            onClick = { /* Implement COD logic */ },
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
        ) {
            Text("Cash on Delivery (COD)")
        }
    }
}

@Composable
fun GcashScreen(navController: NavController, totalPrice: String) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {

        // Back Button using Drawable
        IconButton(onClick = { navController.popBackStack() }) {
            Image(
                painter = painterResource(id = R.drawable.backbtn),
                contentDescription = "Back"
            )
        }

        Text(text = "GCash Payment", style = MaterialTheme.typography.headlineMedium)
        Text(text = "Amount to Pay: ₱$totalPrice"+"0", style = MaterialTheme.typography.bodyLarge)

        Button(
            onClick = { /* Implement GCash Payment Logic */ },
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp)
        ) {
            Text("Proceed with GCash")
        }
    }
}

