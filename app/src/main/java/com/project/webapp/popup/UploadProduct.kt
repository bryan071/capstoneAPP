package com.project.webapp.popup

import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadProductDialog(
    onDismiss: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance().reference
    val coroutineScope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var isUploading by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Upload Product", style = MaterialTheme.typography.headlineSmall)
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Product Name") })
            OutlinedTextField(value = category, onValueChange = { category = it }, label = { Text("Category") })
            OutlinedTextField(value = price, onValueChange = { price = it }, label = { Text("Price") })
            OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text("Description") })
            Spacer(modifier = Modifier.height(8.dp))

            imageUri?.let { uri ->
                AsyncImage(model = uri, contentDescription = "Product Image", modifier = Modifier.height(150.dp))
            }

            Button(onClick = { /* Implement Image Picker */ }) {
                Text("Select Image")
            }

            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    if (name.isNotBlank() && category.isNotBlank() && price.isNotBlank() && imageUri != null) {
                        isUploading = true
                        val imageRef = storage.child("products/${UUID.randomUUID()}")
                        imageUri?.let { uri ->
                            coroutineScope.launch {
                                imageRef.putFile(uri).continueWithTask { task ->
                                    if (!task.isSuccessful) task.exception?.let { throw it }
                                    imageRef.downloadUrl
                                }.addOnSuccessListener { url ->
                                    val product = hashMapOf(
                                        "name" to name,
                                        "category" to category,
                                        "price" to price,
                                        "description" to description,
                                        "imageUrl" to url.toString()
                                    )
                                    firestore.collection("products").add(product)
                                        .addOnSuccessListener { onDismiss() }
                                }
                            }
                        }
                    }
                },
                enabled = !isUploading
            ) {
                if (isUploading) CircularProgressIndicator() else Text("Upload Product")
            }
        }
    }
}
