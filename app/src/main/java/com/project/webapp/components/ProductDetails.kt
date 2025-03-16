package com.project.webapp.components

import android.util.Log
import androidx.compose.foundation.background
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
import com.project.webapp.dashboards.TopBar
import com.project.webapp.datas.Product

@Composable
fun ProductDetailsScreen(
    navController: NavController,
    productId: String?,
    firestore: FirebaseFirestore,
    authViewModel: AuthViewModel = viewModel()
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var ownerName by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val authState by authViewModel.authState.observeAsState()

    LaunchedEffect(productId) {
        if (productId.isNullOrEmpty()) {
            isLoading = false
            return@LaunchedEffect
        }

        firestore.collection("products").document(productId)
            .get()
            .addOnSuccessListener { document ->
                product = document.toObject(Product::class.java)
                product?.ownerId?.let { ownerId ->
                    fetchOwnerName(firestore, ownerId) { name ->
                        ownerName = name
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        product?.let { prod ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TopBar()
                AsyncImage(
                    model = prod.imageUrl, contentDescription = "Product Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(12.dp))
                )
                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(prod.name, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                        Text("Category: ${prod.category}", fontSize = 16.sp, color = Color.Gray)
                        Text("Location: ${prod.cityName}", fontSize = 16.sp, color = Color.Gray)
                        Text("Owner: ${ownerName ?: "Unknown"}", fontSize = 16.sp, color = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "₱${String.format("%,d", prod.price.toInt())}.00", // Ensures 30,000 format
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0DA54B)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    shape = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("Description:", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Text(prod.description, fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                when (val state = authState) {
                    is AuthState.Authenticated -> {
                        val userId = state.userId
                        val userType = state.userType.lowercase()

                        if (userId == prod.ownerId) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Button(
                                    onClick = { navController.navigate("editProduct/${prod.prodId}") },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
                                ) {
                                    Text("Edit Product")
                                }
                                Button(
                                    onClick = { deleteProduct(firestore, productId!!, navController) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                                ) {
                                    Text("Delete Product")
                                }
                            }
                        } else if (userType == "market") {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Button(
                                    onClick = { addToCart(productId!!, userId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                ) {
                                    Text("Add to Cart")
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Button(
                                    onClick = { buyProduct(productId!!, userId) },
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
                                ) {
                                    Text("Buy Now")
                                }
                            }
                        } else if (userType == "organization") {
                            Text("Organizations cannot buy or sell products.", fontSize = 16.sp, color = Color.Gray)
                        }
                    }
                    else -> {
                        Text("You must be logged in to interact with products.", fontSize = 16.sp, color = Color.Red)
                    }
                }
            }
        } ?: Text("Product not found", fontSize = 18.sp, modifier = Modifier.padding(16.dp))
    }
}

fun fetchOwnerName(firestore: FirebaseFirestore, ownerId: String, onResult: (String) -> Unit) {
    firestore.collection("users").document(ownerId)
        .get()
        .addOnSuccessListener { document ->
            val name = document.getString("name")
            val email = document.getString("email")
            onResult(name ?: email ?: "Unknown") // Prioritize name, fallback to email
        }
        .addOnFailureListener {
            onResult("Unknown")
        }
}

fun deleteProduct(firestore: FirebaseFirestore, productId: String, navController: NavController) {
    firestore.collection("products").document(productId)
        .delete()
        .addOnSuccessListener { navController.popBackStack() }
}

fun addToCart(productId: String, userId: String?) {
    if (userId == null) return
    val firestore = FirebaseFirestore.getInstance()
    val cartItem = hashMapOf("userId" to userId, "productId" to productId, "quantity" to 1)
    firestore.collection("cart").add(cartItem)
}

fun buyProduct(productId: String, userId: String?) {
    if (userId == null) return
    val firestore = FirebaseFirestore.getInstance()
    val orderItem = hashMapOf("userId" to userId, "productId" to productId, "status" to "Pending", "orderDate" to System.currentTimeMillis())
    firestore.collection("orders").add(orderItem)
}
