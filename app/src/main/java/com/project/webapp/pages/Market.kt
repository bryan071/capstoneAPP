package com.project.webapp.pages

import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.AuthViewModel
import com.project.webapp.productdata.Product
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun MarketScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var filteredProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val categories = listOf("All", "vegetable", "fruits", "rootcrops", "grains", "spices")
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            products = fetchProducts(firestore)
            filteredProducts = products
            isLoading = false
        }
    }

    LaunchedEffect(selectedCategory) {
        filteredProducts = if (selectedCategory == null || selectedCategory.equals("All", ignoreCase = true)) {
            products
        } else {
            selectedCategory?.let { category ->
                products.filter { it.category.trim().lowercase() == category.trim().lowercase() }
            } ?: products
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar()
        Text("Market", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        LazyRow(modifier = Modifier.padding(vertical = 8.dp)) {
            items(categories) { category ->
                Button(
                    onClick = { selectedCategory = if (category == "All") null else category },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedCategory == category) Color(0xFF0DA54B) else Color.LightGray
                    ),
                    modifier = Modifier.padding(end = 8.dp)
                ) {
                    Text(category)
                }
            }
        }

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier.fillMaxSize().padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredProducts.size) { index ->
                    val product = filteredProducts[index]
                    ProductCard(product)
                }
            }
        }
    }

    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showDialog = true },
            modifier = Modifier.padding(16.dp),
            backgroundColor = Color(0xFF0DA54B) // Correct for Material2
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product", tint = Color.White)
        }
    }


    if (showDialog) {
        AddProductDialog(
            onDismiss = { showDialog = false },
            onAddProduct = { category, imageUrl, name, price ->
                uploadProduct(category, imageUrl, name, price, firestore, storage, authViewModel)
                showDialog = false
            }
        )
    }
}

@Composable
fun AddProductDialog(onDismiss: () -> Unit, onAddProduct: (String, String, Double, Uri?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Product") },
        text = {
            Column {
                TextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
                TextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                TextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
                Button(onClick = { imagePickerLauncher.launch("image/*") }) {
                    Text("Select Image")
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val priceDouble = price.toDoubleOrNull() ?: 0.0
                onAddProduct(category, name, priceDouble, imageUri)
            }) {
                Text("Add")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

fun uploadProduct(
    name: String,
    category: String,
    price: Double,
    imageUri: Uri?,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    authViewModel: AuthViewModel
) {
    val productRef = firestore.collection("products").document()
    val userId = authViewModel.currentUser?.uid ?: "Unknown"

    val uploadImageAndSaveProduct: (String) -> Unit = { imageUrl ->
        val product = Product(category, imageUrl, name, price)
        productRef.set(product).addOnSuccessListener {
            addNotification(firestore, product, userId) // Pass the full product object
        }
    }

    if (imageUri != null) {
        val imageRef = storage.reference.child("product_images/${productRef.id}.jpg")
        imageRef.putFile(imageUri).addOnSuccessListener {
            imageRef.downloadUrl.addOnSuccessListener { uri ->
                uploadImageAndSaveProduct(uri.toString())
            }
        }.addOnFailureListener {
            Log.e("Firebase", "Image upload failed", it)
        }
    } else {
        uploadImageAndSaveProduct("") // No image case
    }
}


fun addNotification(firestore: FirebaseFirestore, product: Product, userId: String) {
    val notificationRef = firestore.collection("notifications").document()
    val notification = mapOf(
        "message" to "New product added: ${product.name}",
        "timestamp" to System.currentTimeMillis(),
        "userId" to userId,
        "name" to product.name,
        "category" to product.category,
        "price" to product.price,
        "imageUrl" to product.imageUrl, // Store image URL for display
        // "location" to "Bulacan",  Add location (modify based on actual data)
        "isRead" to false
    )

    notificationRef.set(notification).addOnSuccessListener {
        Log.d("Firestore", "Notification added successfully: ${product.name}")
    }.addOnFailureListener { e ->
        Log.e("Firestore", "Error adding notification", e)
    }
}


