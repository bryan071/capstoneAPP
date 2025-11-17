package com.project.webapp.components

import android.app.DatePickerDialog
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.datas.Product
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
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
    var isCategoryExpanded by remember { mutableStateOf(false) }
    val categories = listOf("Vegetable", "Fruits", "Rootcrops", "Grains", "Spices")

    var quantity by remember { mutableStateOf("") }
    var quantityUnit by remember { mutableStateOf("") }
    var isUnitExpanded by remember { mutableStateOf(false) }
    val units = listOf("kilo", "grams")

    var imageUrl by remember { mutableStateOf<String?>(null) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }

    var isLoading by remember { mutableStateOf(true) }
    var isUpdating by remember { mutableStateOf(false) }

    // Harvest Date
    var harvestDate by remember { mutableStateOf<Timestamp?>(product?.harvestDate) }
    var harvestDateText by remember { mutableStateOf("") }

    // Image picker
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri -> selectedImageUri = uri }

    // Decimal input helpers
    fun normalizeDecimalInput(raw: String): String {
        var cleaned = raw.replace(",", "").filter { it.isDigit() || it == '.' }
        val parts = cleaned.split('.')
        if (parts.size > 2) return cleaned.dropLast(1)
        cleaned = if (parts.size == 2) parts[0] + "." + parts[1].take(2) else cleaned
        return cleaned
    }

    fun formatWithCommas(value: String): String {
        if (value.isBlank()) return ""
        return try {
            val numeric = value.toDouble()
            if (value.contains(".")) "%,.2f".format(numeric) else "%,d".format(numeric.toInt())
        } catch (e: Exception) { value }
    }

    LaunchedEffect(productId) {
        if (!productId.isNullOrEmpty()) {
            firestore.collection("products").document(productId)
                .get()
                .addOnSuccessListener { doc ->
                    doc.toObject(Product::class.java)?.let {
                        product = it
                        name = it.name
                        description = it.description
                        price = formatWithCommas(it.price.toString())
                        category = it.category
                        quantity = formatWithCommas(it.quantity.toString())
                        quantityUnit = it.quantityUnit
                        imageUrl = it.imageUrl
                        it.harvestDate?.toDate()?.let { date ->
                            harvestDate = it.harvestDate
                            harvestDateText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } else isLoading = false
    }

    // DatePickerDialog
    val datePickerDialog = remember {
        val calendar = Calendar.getInstance()
        harvestDate?.toDate()?.let { calendar.time = it }

        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                harvestDate = Timestamp(calendar.time)
                harvestDateText = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(calendar.time)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edit Product") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0DA54B),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0DA54B))
            }
        } else {
            product?.let {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Image picker
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.LightGray)
                            .border(2.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                            .clickable { imagePicker.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        if (selectedImageUri != null || imageUrl != null) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri ?: imageUrl),
                                contentDescription = "Product Image",
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Camera,
                                contentDescription = "Add Image",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(8.dp)
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0DA54B))
                                .padding(6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit Image", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Name
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Product Name") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF0DA54B),
                            focusedLabelColor = Color(0xFF0DA54B)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth().height(100.dp),
                        shape = RoundedCornerShape(8.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = Color(0xFF0DA54B),
                            focusedLabelColor = Color(0xFF0DA54B)
                        )
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price and Category Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = price,
                            onValueChange = {
                                val normalized = normalizeDecimalInput(it)
                                price = formatWithCommas(normalized)
                            },
                            label = { Text("Price") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF0DA54B),
                                focusedLabelColor = Color(0xFF0DA54B)
                            ),
                            leadingIcon = { Text("₱", fontWeight = FontWeight.Bold) }
                        )

                        // Category Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isCategoryExpanded,
                            onExpandedChange = { isCategoryExpanded = !isCategoryExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = category,
                                onValueChange = { },
                                label = { Text("Category") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isCategoryExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF0DA54B),
                                    focusedLabelColor = Color(0xFF0DA54B)
                                )
                            )
                            ExposedDropdownMenu(expanded = isCategoryExpanded, onDismissRequest = { isCategoryExpanded = false }) {
                                categories.forEach { selection ->
                                    DropdownMenuItem(
                                        text = { Text(selection) },
                                        onClick = { category = selection; isCategoryExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Quantity & Unit Row
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                        OutlinedTextField(
                            value = quantity,
                            onValueChange = {
                                val normalized = normalizeDecimalInput(it)
                                quantity = formatWithCommas(normalized)
                            },
                            label = { Text("Quantity") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            colors = TextFieldDefaults.outlinedTextFieldColors(
                                focusedBorderColor = Color(0xFF0DA54B),
                                focusedLabelColor = Color(0xFF0DA54B)
                            )
                        )

                        // Unit Dropdown
                        ExposedDropdownMenuBox(
                            expanded = isUnitExpanded,
                            onExpandedChange = { isUnitExpanded = !isUnitExpanded },
                            modifier = Modifier.weight(1f)
                        ) {
                            OutlinedTextField(
                                value = quantityUnit,
                                onValueChange = { },
                                label = { Text("Unit") },
                                readOnly = true,
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = isUnitExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                colors = TextFieldDefaults.outlinedTextFieldColors(
                                    focusedBorderColor = Color(0xFF0DA54B),
                                    focusedLabelColor = Color(0xFF0DA54B)
                                )
                            )
                            ExposedDropdownMenu(expanded = isUnitExpanded, onDismissRequest = { isUnitExpanded = false }) {
                                units.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unit) },
                                        onClick = { quantityUnit = unit; isUnitExpanded = false }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Harvest Date Field
                    OutlinedTextField(
                        value = harvestDate?.toDate()?.let {
                            SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(it)
                        } ?: "",
                        onValueChange = {},
                        label = { Text("Harvest Date") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                enabled = true,
                                onClick = { datePickerDialog.show() }
                            ),
                        readOnly = true,
                        enabled = false, // Add this to prevent keyboard from showing
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            focusedBorderColor = Color(0xFF0DA54B),
                            focusedLabelColor = Color(0xFF0DA54B)
                        ),
                        trailingIcon = {
                            IconButton(onClick = { datePickerDialog.show() }) {
                                Icon(
                                    imageVector = Icons.Default.CalendarMonth,
                                    contentDescription = "Select date"
                                )
                            }
                        }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Update Button
                    Button(
                        onClick = {
                            isUpdating = true
                            if (selectedImageUri != null) {
                                uploadImageAndSaveData(
                                    selectedImageUri!!, productId!!, firestore, storage, name, description, price,
                                    category, quantity, quantityUnit, harvestDate!!, navController
                                )
                            } else {
                                updateProductData(
                                    productId!!, name, description, price, category, quantity, quantityUnit,
                                    imageUrl!!, harvestDate!!, firestore, navController
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                        shape = RoundedCornerShape(16.dp),
                        enabled = !isUpdating
                    ) {
                        if (isUpdating) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Update Product", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

// Update functions with harvestDate parameter
fun uploadImageAndSaveData(
    imageUri: Uri,
    productId: String,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    name: String,
    description: String,
    price: String,
    category: String,
    quantity: String,
    quantityUnit: String,
    harvestDate: Timestamp,
    navController: NavController
) {
    val storageRef = storage.reference.child("product_images/$productId.jpg")
    storageRef.putFile(imageUri)
        .addOnSuccessListener {
            storageRef.downloadUrl.addOnSuccessListener { uri ->
                updateProductData(
                    productId, name, description, price, category, quantity, quantityUnit,
                    uri.toString(), harvestDate, firestore, navController
                )
            }
        }
        .addOnFailureListener { Log.e("EditProduct", "Image upload failed", it) }
}

fun updateProductData(
    productId: String,
    name: String,
    description: String,
    price: String,
    category: String,
    quantity: String,
    quantityUnit: String,
    imageUrl: String,
    harvestDate: Timestamp,
    firestore: FirebaseFirestore,
    navController: NavController
) {
    val updatedProduct = mapOf(
        "name" to name,
        "description" to description,
        "price" to price.toDouble(),
        "category" to category,
        "quantity" to quantity.toDouble(),
        "quantityUnit" to quantityUnit,
        "imageUrl" to imageUrl,
        "harvestDate" to harvestDate
    )

    firestore.collection("products").document(productId)
        .update(updatedProduct)
        .addOnSuccessListener { navController.popBackStack() }
        .addOnFailureListener { Log.e("EditProduct", "Error updating product", it) }
}
