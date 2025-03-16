package com.project.webapp.components

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.AuthState
import com.project.webapp.AuthViewModel
import com.project.webapp.datas.Product


@Composable
fun ProductDetailsScreen(
    navController: NavController,
    productId: String?,
    firestore: FirebaseFirestore,
    authViewModel: AuthViewModel = viewModel()
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val authState by authViewModel.authState.observeAsState()

    // 🔥 Debugging: Check if productId is null
    LaunchedEffect(productId) {
        Log.d("ProductDetails", "Received productId: $productId")
        if (productId.isNullOrEmpty()) {
            Log.e("Firestore", "Invalid Product ID received!")
            isLoading = false
            return@LaunchedEffect
        }

        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    product = document.toObject(Product::class.java)
                    Log.d("Firestore", "Product loaded: ${product?.name}")
                } else {
                    Log.e("Firestore", "No product found for ID: $productId")
                }
                isLoading = false
            }
            .addOnFailureListener { e ->
                isLoading = false
                Log.e("Firestore", "Failed to fetch product details", e)
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        product?.let { prod ->
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                AsyncImage(
                    model = prod.imageUrl, contentDescription = "Product Image",
                    modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(8.dp))
                )
                Spacer(modifier = Modifier.height(16.dp))

                Text(prod.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Category: ${prod.category}", fontSize = 18.sp, color = Color.Gray)
                Text("Location: ${prod.cityName}", fontSize = 18.sp, color = Color.Gray)
                Text("Price: ₱${prod.price}", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0DA54B))
                Spacer(modifier = Modifier.height(8.dp))

                Text("Description:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Text(prod.description, fontSize = 16.sp)

                Spacer(modifier = Modifier.height(16.dp))

                // 🔥 Debugging: Check Auth State
                Log.d("AuthState", "Current Auth State: $authState")

                when (val state = authState) {
                    is AuthState.Authenticated -> {
                        val userId = state.userId
                        val userType = state.userType

                        // 🔥 Log to check userType
                        Log.d("DEBUG", "AuthState: userType=$userType, userId=$userId, productOwner=${prod.ownerId}")

                        if (userId == prod.ownerId) {
                            // Owner (Farmer) UI
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                                Button(onClick = { navController.navigate("editProduct/${prod.prodId}") }) {
                                    Text("Edit Product")
                                }
                                Button(
                                    onClick = { deleteProduct(firestore, productId!!, navController) },
                                    colors = ButtonDefaults.buttonColors(Color.Red)
                                ) {
                                    Text("Delete Product")
                                }
                            }
                        } else if (userType.lowercase() == "market") {  // 🔥 Fix: Convert to lowercase
                            Log.d("DEBUG", "Market user detected: Showing Buy and Add to Cart buttons.")
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { addToCart(productId!!, userId) },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text("Add to Cart")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { buyProduct(productId!!, userId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(Color(0xFF0DA54B))
                                ) {
                                    Text("Buy Now")
                                }
                            }

                    } else if (userType == "organization") {
                            Text("Organizations cannot buy or sell products.", fontSize = 16.sp, color = Color.Gray)
                        }
                    }
                    else -> {
                        Log.d("DEBUG", "User not authenticated. No buttons will be displayed.")
                        Text("You must be logged in to interact with products.", fontSize = 16.sp, color = Color.Red)
                    }
                }
            }
        } ?: Text("Product not found", fontSize = 18.sp, modifier = Modifier.padding(16.dp))
    }
}



fun deleteProduct(firestore: FirebaseFirestore, productId: String, navController: NavController) {
    firestore.collection("products").document(productId)
        .delete()
        .addOnSuccessListener {
            Log.d("Firestore", "Product deleted successfully")
            navController.popBackStack() // Go back after deletion
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error deleting product", e)
        }
}

fun addToCart(productId: String, userId: String?) {
    if (userId == null) {
        Log.e("Cart", "User not logged in")
        return
    }

    val firestore = FirebaseFirestore.getInstance()
    val cartItem = hashMapOf(
        "userId" to userId,
        "productId" to productId,
        "quantity" to 1
    )

    firestore.collection("cart").add(cartItem)
        .addOnSuccessListener { Log.d("Cart", "Product added to cart!") }
        .addOnFailureListener { e -> Log.e("Cart", "Failed to add to cart", e) }
}

fun buyProduct(productId: String, userId: String?) {
    if (userId == null) {
        Log.e("Order", "User not logged in")
        return
    }

    val firestore = FirebaseFirestore.getInstance()
    val orderItem = hashMapOf(
        "userId" to userId,
        "productId" to productId,
        "status" to "Pending",
        "orderDate" to System.currentTimeMillis()
    )

    firestore.collection("orders").add(orderItem)
        .addOnSuccessListener { Log.d("Order", "Order placed successfully!") }
        .addOnFailureListener { e -> Log.e("Order", "Failed to place order", e) }
}



