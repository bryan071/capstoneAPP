package com.project.webapp.components

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.material.Icon
import androidx.compose.material.icons.filled.ArrowDropDown
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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.toSize
import androidx.navigation.NavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.AuthViewModel
import com.project.webapp.datas.Product
import androidx.compose.runtime.Composable
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.project.webapp.R
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.project.webapp.dashboards.ProductCard
import com.project.webapp.dashboards.SearchBar
import com.project.webapp.dashboards.TopBar
import com.project.webapp.dashboards.fetchProducts
import kotlinx.coroutines.launch
import java.util.Locale


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FarmerMarketScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var filteredProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isAscending by remember { mutableStateOf(true) } // Sorting state
    val categories = listOf("All", "vegetable", "fruits", "rootcrops", "grains", "spices")
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    val fusedLocationClient =
        remember { LocationServices.getFusedLocationProviderClient(navController.context) }
    val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }

    // Fetch products on first load
    LaunchedEffect(Unit) {
        coroutineScope.launch {
            products = fetchProducts(firestore)
            filteredProducts = products.sortedBy { it.price }
            isLoading = false
        }
    }

    // Apply category filtering and price sorting
    LaunchedEffect(selectedCategory, minPrice, maxPrice, isAscending, products) {
        val category = selectedCategory?.trim()?.lowercase()
        val min = minPrice.toDoubleOrNull() ?: 0.0
        val max = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE

        filteredProducts = products.filter { product ->
            val productPrice = product.price
            val isCategoryMatch = category == null || category == "all" || product.category.trim().lowercase() == category
            val isPriceInRange = productPrice in min..max

            isCategoryMatch && isPriceInRange
        }.sortedBy { if (isAscending) it.price else -it.price }
    }
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TopBar()
        SearchBar()

        Text("Market", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        // Category Filter Buttons
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

        // Price Filtering Inputs - Modernized UI
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            PriceFilterSection(
                minPrice = minPrice,
                maxPrice = maxPrice,
                onMinPriceChange = { minPrice = it },
                onMaxPriceChange = { maxPrice = it },
                isAscending = isAscending,
                onSortToggle = { isAscending = !isAscending }
            )
        }


        Spacer(modifier = Modifier.height(8.dp))

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
                    ProductCard(product, navController, firestore, storage)
                }
            }
        }
    }

    // Floating Action Button for Adding a Product
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        contentAlignment = Alignment.BottomEnd
    ) {
        FloatingActionButton(
            onClick = { showDialog = true },
            containerColor = Color(0xFF0DA54B),
            contentColor = Color.White
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add Product")
        }
    }

    // Add Product Dialog
    if (showDialog) {
        AddProductDialog(
            onDismiss = { showDialog = false },
            onAddProduct = { category, name, description, price, imageUri ->
                if (permissionState.status.isGranted) {
                    uploadProduct(
                        name = name,
                        description = description,
                        category = category,
                        price = price,
                        imageUri = imageUri,
                        firestore = firestore,
                        storage = storage,
                        authViewModel = authViewModel,
                        fusedLocationClient = fusedLocationClient,
                        context = context
                    )
                } else {
                    permissionState.launchPermissionRequest() // Request permission
                }
                showDialog = false
            }
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(onDismiss: () -> Unit, onAddProduct: (String, String, String, Double, Uri?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") } // New description field
    var selectedCategory by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    val categories = listOf("vegetable", "fruits", "rootcrops", "grains", "spices")
    var expanded by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }
    var pressedCategory by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add Product", style = MaterialTheme.typography.headlineSmall)

            // Category Dropdown
            Text("Category", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .onGloballyPositioned { coordinates ->
                            textFieldSize = coordinates.size.toSize()
                        },
                    trailingIcon = {
                        IconButton(onClick = { expanded = !expanded }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                )

                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                    modifier = Modifier
                        .width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    category,
                                    textDecoration = if (pressedCategory == category) TextDecoration.Underline else TextDecoration.None
                                )
                            },
                            onClick = {
                                pressedCategory = category
                                selectedCategory = category
                                expanded = false
                            }
                        )
                    }
                }
            }

            // Product Name
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            // Description Input
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )

            // Price Input
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Price (₱)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Image Preview
            imageUri?.let {
                AsyncImage(
                    model = it,
                    contentDescription = "Product Image",
                    modifier = Modifier
                        .height(150.dp)
                        .fillMaxWidth()
                )
            }

            // Image Picker Button
            Button(
                onClick = { imagePickerLauncher.launch("image/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Select Image")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Form Actions
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, colors = ButtonDefaults.textButtonColors(contentColor = Color.Black)) {
                    Text("Cancel")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                    onClick = {
                        val priceDouble = price.toDoubleOrNull() ?: 0.0
                        onAddProduct(selectedCategory, name, description, priceDouble, imageUri)
                        onDismiss()
                    }
                ) {
                    Text("Add Product")
                }
            }
        }
    }
}


fun uploadProduct(
    name: String,
    description: String,
    category: String,
    price: Double,
    imageUri: Uri?,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    authViewModel: AuthViewModel,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    val productRef = firestore.collection("products").document()
    val userId = authViewModel.currentUser?.uid ?: "Unknown"

    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Log.e("Location", "Permission not granted")
        return
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        val cityName = if (location != null) {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
            addressList?.firstOrNull()?.locality ?: "Unknown City"
        } else {
            Log.e("Location", "Failed to get location")
            "Unknown City"
        }

        // ✅ Image Upload Process
        if (imageUri != null) {
            val imageRef = storage.reference.child("product_images/${productRef.id}.jpg")
            Log.d("Firebase", "Uploading image to: ${imageRef.path}")

            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        Log.d("Firebase", "Image uploaded successfully: $uri")
                        saveProductToFirestore(firestore, productRef.id, userId, category, uri.toString(), name, description, price, cityName, context)
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Image upload failed", e)
                }
        } else {
            Log.e("Firebase", "Image URI is null")
            saveProductToFirestore(firestore, productRef.id, userId, category, "", name, description, price, cityName, context)
        }
    }.addOnFailureListener {
        Log.e("Location", "Failed to get location", it)
    }
}

fun saveProductToFirestore(
    firestore: FirebaseFirestore,
    productId: String,
    userId: String,
    category: String,
    imageUrl: String,
    name: String,
    description: String,
    price: Double,
    cityName: String,
    context: Context
) {
    val product = Product(
        prodId = productId,
        ownerId = userId,
        category = category,
        imageUrl = imageUrl,
        name = name,
        description = description,
        price = price,
        cityName = cityName
    )

    firestore.collection("products").document(productId).set(product)
        .addOnSuccessListener {
            Log.d("Firebase", "Product added successfully!")
            addNotification(firestore, product, userId, cityName)
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error adding product", e)
        }
}

// Function to create a notification
fun addNotification(firestore: FirebaseFirestore, product: Product, userId: String, location: String) {
    val notificationRef = firestore.collection("notifications").document()
    val notificationId = notificationRef.id

    val notification = mapOf(
        "id" to notificationId, // ✅ Store ID for deletion
        "message" to "New product added: ${product.name}",
        "timestamp" to System.currentTimeMillis(),
        "userId" to userId,
        "name" to product.name,
        "category" to product.category,
        "price" to product.price,
        "imageUrl" to product.imageUrl,
        "location" to location,
        "isRead" to false
    )

    notificationRef.set(notification)
        .addOnSuccessListener {
            Log.d("Firestore", "Notification added successfully: ${product.name}")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error adding notification", e)
        }
}

@OptIn(ExperimentalMaterial3Api::class) // Suppress warning
@Composable
fun PriceFilterSection(
    minPrice: String,
    maxPrice: String,
    onMinPriceChange: (String) -> Unit,
    onMaxPriceChange: (String) -> Unit,
    isAscending: Boolean,
    onSortToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        OutlinedTextField(
            value = minPrice,
            onValueChange = onMinPriceChange,
            label = { Text("Min Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF0DA54B),
                cursorColor = Color(0xFF0DA54B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )

        OutlinedTextField(
            value = maxPrice,
            onValueChange = onMaxPriceChange,
            label = { Text("Max Price") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = Color(0xFF0DA54B),
                cursorColor = Color(0xFF0DA54B)
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        )

        ElevatedButton(
            onClick = onSortToggle,
            colors = ButtonDefaults.elevatedButtonColors(containerColor = Color(0xFF0DA54B)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .height(56.dp)
                .width(64.dp)
        ) {
            Icon(
                painter = painterResource(id = if (isAscending) R.drawable.arrowup else R.drawable.arrowdown),
                contentDescription = "Sort Order",
                tint = Color.White
            )
        }
    }
}



