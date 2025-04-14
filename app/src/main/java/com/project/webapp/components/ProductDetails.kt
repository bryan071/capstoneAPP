package com.project.webapp.components


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.R
import com.project.webapp.Viewmodel.AuthState
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.datas.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailsScreen(
    navController: NavController,
    productId: String?,
    firestore: FirebaseFirestore,
    authViewModel: AuthViewModel = viewModel(),
    cartViewModel: CartViewModel = viewModel()
) {
    var product by remember { mutableStateOf<Product?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val authState by authViewModel.authState.observeAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val primaryColor = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF5F9F6)
    val cardColor = Color(0xFFE8F5E9)

    // Use mutableStateMapOf to store seller names by their IDs
    val sellerNames = remember { mutableStateMapOf<String, String>() }

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
                    // Fetch seller info and store in the map
                    cartViewModel.getUserById(ownerId) { user ->
                        user?.let {
                            sellerNames[ownerId] = "${it.firstname} ${it.lastname}"
                        } ?: run {
                            sellerNames[ownerId] = "Unknown Seller"
                        }
                    }
                }
                isLoading = false
            }
            .addOnFailureListener {
                isLoading = false
            }
    }

    var userType by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType")
                }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = backgroundColor
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Main Content Column with proper spacing
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp) // Consistent padding
            ) {
                // Top Row for Back Button and Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Back Button
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Title properly aligned with back button
                    Text(
                        text = "Product Details",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        lineHeight = 32.sp,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                // Loading state
                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = primaryColor)
                    }
                } else {
                    product?.let { prod ->
                        // Product Card
                        Card(
                            shape = RoundedCornerShape(16.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.White)
                            ) {
                                AsyncImage(
                                    model = prod.imageUrl,
                                    contentDescription = "Product Image",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(16.dp),
                                    contentScale = ContentScale.Fit
                                )

                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(12.dp)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(primaryColor.copy(alpha = 0.8f))
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Text(
                                        prod.category,
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Product Info - Title and Price
                        Text(
                            prod.name,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            lineHeight = 32.sp
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            "₱${String.format("%,d", prod.price.toInt())}.00",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = primaryColor
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Info Cards
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            InfoCard(
                                title = "Location",
                                value = prod.cityName,
                                modifier = Modifier.weight(1f),
                                color = cardColor
                            )

                            Spacer(modifier = Modifier.width(12.dp))

                            InfoCard(
                                title = "Quantity",
                                value = "${prod.quantity.toInt()} ${prod.quantityUnit}",
                                modifier = Modifier.weight(1f),
                                color = cardColor
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Enhanced Seller Card with better error handling
                        InfoCard(
                            title = "Seller",
                            value = product?.ownerId?.let { sellerNames[it] ?: "Loading..." } ?: "Unknown",
                            modifier = Modifier.fillMaxWidth(),
                            color = cardColor
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Description Section
                        Text(
                            "Description",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = cardColor),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                prod.description,
                                fontSize = 16.sp,
                                lineHeight = 24.sp,
                                modifier = Modifier.padding(16.dp)
                            )
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // Action Buttons
                        when (val state = authState) {
                            is AuthState.Authenticated -> {
                                val userId = state.userId
                                val userType = state.userType.lowercase()

                                if (userId == prod.ownerId) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Button(
                                            onClick = { navController.navigate("editProduct/${prod.prodId}") },
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "Edit Product",
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }

                                        Button(
                                            onClick = { deleteProduct(firestore, productId!!, navController) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                            modifier = Modifier.weight(1f),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "Delete Product",
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(vertical = 4.dp)
                                            )
                                        }
                                    }
                                } else if (userType == "market") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Button(
                                            onClick = {
                                                product?.let { p ->
                                                    navController.navigate("paymentScreen/${p.prodId}/${p.price}")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "Buy Now",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        OutlinedButton(
                                            onClick = { cartViewModel.addToCart(prod, userType, navController) },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            shape = RoundedCornerShape(8.dp),
                                            border = ButtonDefaults.outlinedButtonBorder.copy(width = 2.dp)
                                        ) {
                                            Text(
                                                "Add to Cart",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = primaryColor
                                            )
                                        }

                                        Spacer(modifier = Modifier.height(12.dp))

                                        // 💚 Donate Now button
                                        Button(
                                            onClick = {
                                                product?.let { p ->
                                                    navController.navigate("donationScreen/${p.prodId}/${p.price}")
                                                }
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(56.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0077FF)),
                                            shape = RoundedCornerShape(8.dp)
                                        ) {
                                            Text(
                                                "Donate Now",
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color.White
                                            )
                                        }
                                    }
                                } else if (userType == "organization") {
                                    Card(
                                        shape = RoundedCornerShape(8.dp),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFECB3)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            "Organizations cannot buy or sell products.",
                                            fontSize = 16.sp,
                                            color = Color(0xFF7D6608),
                                            modifier = Modifier.padding(16.dp),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                        )
                                    }
                                }
                            }
                            else -> {
                                Card(
                                    shape = RoundedCornerShape(8.dp),
                                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        "You must be logged in to interact with products.",
                                        fontSize = 16.sp,
                                        color = Color(0xFF832729),
                                        modifier = Modifier.padding(16.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } ?: run {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFCDD2)),
                            modifier = Modifier.width(300.dp)
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(24.dp)
                            ) {
                                Text(
                                    "Product Not Found",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF832729)
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                Text(
                                    "The product you're looking for might have been removed or is no longer available.",
                                    fontSize = 16.sp,
                                    color = Color(0xFF832729),
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(16.dp))

                                Button(
                                    onClick = { navController.popBackStack() },
                                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                                ) {
                                    Text("Go Back")
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    val snackbarMessage by cartViewModel.snackbarMessage.collectAsState()
    LaunchedEffect(snackbarMessage) {
        snackbarMessage?.let {
            snackbarHostState.showSnackbar(it)
            cartViewModel.clearSnackbarMessage()
        }
    }
}

@Composable
fun InfoCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    color: Color
) {
    Card(
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = color),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                title,
                fontSize = 14.sp,
                color = Color.Gray,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                value,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

fun deleteProduct(firestore: FirebaseFirestore, productId: String, navController: NavController) {
    firestore.collection("products").document(productId)
        .delete()
        .addOnSuccessListener { navController.popBackStack() }
}