package com.project.webapp.components

import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
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
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.datas.Product
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
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
import com.google.firebase.auth.FirebaseAuth
import com.project.webapp.dashboards.ProductCard
import com.project.webapp.dashboards.SearchBar
import com.project.webapp.dashboards.fetchProducts
import kotlinx.coroutines.launch
import java.util.Locale


@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FarmerMarketScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    cartViewModel: CartViewModel
) {
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var filteredProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var selectedCategory by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isAscending by remember { mutableStateOf(true) }
    val categories = listOf("All", "vegetable", "fruits", "rootcrops", "grains", "spices")
    val coroutineScope = rememberCoroutineScope()
    var showDialog by remember { mutableStateOf(false) }
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(navController.context) }
    val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current
    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    val authState by authViewModel.authState.observeAsState()
    var userType by remember { mutableStateOf<String?>(null) }

    // ✅ Fetch user type only once
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        Log.d("FarmerMarketScreen", "Fetching userType for userId: $userId")

        userId?.let {
            firestore.collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    val fetchedUserType = document.getString("userType")?.trim()?.lowercase()
                    userType = fetchedUserType
                    Log.d("FarmerMarketScreen", "Fetched userType: $userType")
                }
                .addOnFailureListener { e ->
                    Log.e("FarmerMarketScreen", "Error fetching userType", e)
                }
        }
    }

    val isFarmer = userType == "farmer"

    // ✅ Fetch products on first load using Firestore listener
    DisposableEffect(Unit) {
        val listenerRegistration = fetchProducts(firestore) { fetchedProducts ->
            products = fetchedProducts
            filteredProducts = fetchedProducts.sortedBy { it.price }
            isLoading = false
        }

        onDispose {
            listenerRegistration.remove()  // 🔥 Properly remove the Firestore listener when screen is destroyed
        }
    }


    // Apply category filtering and price sorting
    LaunchedEffect(selectedCategory, minPrice, maxPrice, isAscending, products) {
        val category = selectedCategory?.trim()?.lowercase()
        val min = minPrice.toDoubleOrNull() ?: 0.0
        val max = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE

        filteredProducts = products.filter { product ->
            val productPrice = product.price
            val isCategoryMatch = category == null || category == "all" || product.category.trim()
                .lowercase() == category
            val isPriceInRange = productPrice in min..max

            isCategoryMatch && isPriceInRange
        }.sortedBy { if (isAscending) it.price else -it.price }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(14.dp)
        ) {
            userType?.let { type -> TopBar(navController, cartViewModel, userType = type) }
            SearchBar()
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

            // Price Filtering Inputs
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

        if (isFarmer) {
            Log.d("FarmerMarketScreen", "Displaying FloatingActionButton for farmer userType")

            FloatingActionButton(
                onClick = { showDialog = true },
                containerColor = Color(0xFF0DA54B),
                contentColor = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Product")
            }
        }
    }

    // Add Product Dialog
    if (showDialog) {
        AddProductDialog(
            onDismiss = { showDialog = false },
            onAddProduct = { category, name, description, quantity, unit, price, imageUri ->
                if (permissionState.status.isGranted) {
                    uploadProduct(
                        name = name,
                        description = description,
                        category = category,
                        price = price,
                        quantity = quantity,  // ✅ Now passed as a separate numeric value
                        unit = unit,  // ✅ Now passed as a separate string
                        imageUri = imageUri,
                        firestore = firestore,
                        storage = storage,
                        authViewModel = authViewModel,
                        fusedLocationClient = fusedLocationClient,
                        context = context
                    )
                } else {
                    permissionState.launchPermissionRequest()
                }
                showDialog = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductDialog(onDismiss: () -> Unit, onAddProduct: (String, String, String, Double, String, Double, Uri?) -> Unit) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) {
        imageUri = it
    }

    val categories = listOf("vegetable", "fruits", "rootcrops", "grains", "spices")
    var expandedCategory by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    // Unit Selection
    val unitOptions = listOf("kg", "grams")  // ✅ Updated with more common units
    var selectedUnit by remember { mutableStateOf(unitOptions[0]) }
    var expandedUnit by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Add Product", style = MaterialTheme.typography.headlineSmall)
            Spacer(modifier = Modifier.height(12.dp))

            // Category Dropdown
            Text("Category", style = MaterialTheme.typography.labelLarge)
            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .onGloballyPositioned { coordinates -> textFieldSize = coordinates.size.toSize() },
                    trailingIcon = {
                        IconButton(onClick = { expandedCategory = !expandedCategory }) {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                        }
                    }
                )

                DropdownMenu(
                    expanded = expandedCategory,
                    onDismissRequest = { expandedCategory = false },
                    modifier = Modifier.width(with(LocalDensity.current) { textFieldSize.width.toDp() })
                ) {
                    categories.forEach { category ->
                        DropdownMenuItem(
                            text = { Text(category) },
                            onClick = {
                                selectedCategory = category
                                expandedCategory = false
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
                onValueChange = { price = it.filter { char -> char.isDigit() || char == '.' } },
                label = { Text("Price (₱)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Quantity and Unit Row
            Row(modifier = Modifier.fillMaxWidth()) {
                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { char -> char.isDigit() || char == '.' } },
                    label = { Text("Quantity") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(190.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))

                // Unit Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedUnit,
                    onExpandedChange = { expandedUnit = it } // Correctly toggling state
                ) {
                    OutlinedTextField(
                        value = selectedUnit,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Unit") },
                        modifier = Modifier
                            .width(130.dp)
                            .menuAnchor(),
                        trailingIcon = {
                            IconButton(onClick = { expandedUnit = !expandedUnit }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Dropdown")
                            }
                        }
                    )

                    DropdownMenu(
                        expanded = expandedUnit,
                        onDismissRequest = { expandedUnit = false }
                    ) {
                        unitOptions.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(unit) },
                                onClick = {
                                    selectedUnit = unit
                                    expandedUnit = false // Correctly closing dropdown
                                }
                            )
                        }
                    }
                }
            }

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
            Spacer(modifier = Modifier.height(8.dp))

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
                        val finalQuantity = quantity.toDoubleOrNull() ?: 1.0  // ✅ Convert quantity to Double safely
                        onAddProduct(selectedCategory, name, description, finalQuantity, selectedUnit, priceDouble, imageUri)
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
    quantity: Double,  // ✅ Ensuring it's passed as a Double
    unit: String,  // ✅ Ensuring unit is separate
    price: Double,
    imageUri: Uri?,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    authViewModel: AuthViewModel,
    fusedLocationClient: FusedLocationProviderClient,
    context: Context
) {
    if (name.isBlank() || description.isBlank() || category.isBlank() || quantity <= 0 || unit.isBlank()) {
        Toast.makeText(context, "All fields are required, and quantity must be greater than 0!", Toast.LENGTH_SHORT).show()
        return
    }

    val productRef = firestore.collection("products").document()
    val userId = authViewModel.currentUser?.uid ?: return

    if (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
        ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
        Toast.makeText(context, "Location permission is required to add a product", Toast.LENGTH_LONG).show()
        return
    }

    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
        val cityName = try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                val addressList = geocoder.getFromLocation(location.latitude, location.longitude, 1)
                addressList?.firstOrNull()?.locality ?: "Unknown City"
            } else {
                geocoder.getFromLocation(location.latitude, location.longitude, 1)?.firstOrNull()?.locality ?: "Unknown City"
            }
        } catch (e: Exception) {
            Log.e("Geocoder", "Failed to retrieve city name", e)
            "Unknown City"
        }

        if (imageUri != null) {
            val imageRef = storage.reference.child("product_images/${productRef.id}.jpg")
            imageRef.putFile(imageUri)
                .addOnSuccessListener {
                    imageRef.downloadUrl.addOnSuccessListener { uri ->
                        saveProductToFirestore(firestore, productRef.id, userId, category, uri.toString(), name, description, price, quantity, unit, cityName, context)
                    }.addOnFailureListener { e ->
                        Log.e("Firebase", "Failed to get download URL", e)
                        Toast.makeText(context, "Image upload failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("Firebase", "Image upload failed", e)
                    Toast.makeText(context, "Failed to upload image. Check your internet connection.", Toast.LENGTH_SHORT).show()
                }
        } else {
            saveProductToFirestore(firestore, productRef.id, userId, category, "", name, description, price, quantity, unit, cityName, context)
        }
    }.addOnFailureListener {
        Log.e("Location", "Failed to get location", it)
        Toast.makeText(context, "Could not retrieve location. Please try again.", Toast.LENGTH_SHORT).show()
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
    quantity: Double,
    unit: String,
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
        quantity = quantity,  // ✅ Ensure separate numeric quantity
        quantityUnit = unit,  // ✅ Ensure unit is passed correctly
        cityName = cityName
    )

    firestore.collection("products").document(productId).set(product)
        .addOnSuccessListener {
            Log.d("Firebase", "Product added successfully!")
            addNotification(firestore, product, userId, cityName)
            Toast.makeText(context, "Product added successfully!", Toast.LENGTH_SHORT).show()
        }
        .addOnFailureListener { e ->
            Log.e("Firebase", "Error adding product", e)
            Toast.makeText(context, "Failed to add product. Please try again.", Toast.LENGTH_SHORT).show()
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



