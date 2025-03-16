package com.project.webapp.components

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberImagePainter
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.datas.Product
import kotlinx.coroutines.tasks.await

@Composable
fun EditProductScreen(
    navController: NavController,
    productId: String?,
    firestore: FirebaseFirestore
) {
    val storage = FirebaseStorage.getInstance()
    val context = LocalContext.current

    var product by remember { mutableStateOf<Product?>(null) }
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var imageUrl by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    // Image Picker
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedImageUri = uri
    }

    // Fetch Product Data
    LaunchedEffect(productId) {
        if (!productId.isNullOrEmpty()) {
            firestore.collection("products").document(productId)
                .get()
                .addOnSuccessListener { document ->
                    document.toObject(Product::class.java)?.let {
                        product = it
                        name = it.name
                        description = it.description
                        price = it.price.toString()
                        category = it.category
                        imageUrl = it.imageUrl
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    isLoading = false
                }
        } else {
            isLoading = false
        }
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
    } else {
        product?.let {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Back Button
                Button(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                ) {
                    Text("Back")
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text("Edit Product", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(16.dp))

                // Show Current or Selected Image
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = rememberImagePainter(selectedImageUri ?: imageUrl),
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .height(200.dp)
                            .fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Button to Pick Image (Green Color)
                Button(
                    onClick = { imagePickerLauncher.launch("image/*") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
                ) {
                    Text("Change Image")
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Product Name") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = price,
                    onValueChange = { price = it },
                    label = { Text("Price") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = category,
                    onValueChange = { category = it },
                    label = { Text("Category") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))

                // Save Button (Green Color)
                Button(
                    onClick = {
                        if (selectedImageUri != null) {
                            uploadImageAndSaveData(
                                selectedImageUri!!, productId!!, firestore, storage, name, description, price, category, navController
                            )
                        } else {
                            updateProductData(
                                productId!!, name, description, price, category, imageUrl!!, firestore, navController
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
                ) {
                    Text("Update Product")
                }
            }
        }
    }
}

// Upload Image and Save Product Data
fun uploadImageAndSaveData(
    imageUri: Uri,
    productId: String,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    name: String,
    description: String,
    price: String,
    category: String,
    navController: NavController
) {
    val storageRef = storage.reference.child("product_images/$productId.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                updateProductData(productId, name, description, price, category, uri.toString(), firestore, navController)
            }
        }
        .addOnFailureListener {
            Log.e("EditProduct", "Image upload failed", it)
        }
}

// Update Product in Firestore
fun updateProductData(
    productId: String,
    name: String,
    description: String,
    price: String,
    category: String,
    imageUrl: String,
    firestore: FirebaseFirestore,
    navController: NavController
) {
    val updatedProduct = mapOf(
        "name" to name,
        "description" to description,
        "price" to price.toDouble(),
        "category" to category,
        "imageUrl" to imageUrl
    )

    firestore.collection("products").document(productId)
        .update(updatedProduct)
        .addOnSuccessListener {
            navController.popBackStack()
        }
        .addOnFailureListener {
            Log.e("EditProduct", "Error updating product", it)
        }
}
