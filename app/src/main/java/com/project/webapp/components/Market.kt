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
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.*
import androidx.compose.material.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.ui.text.style.TextAlign
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

// Define a consistent color scheme
private val primaryColor = Color(0xFF0DA54B)
private val secondaryColor = Color(0xFF81C784)
private val lightGreen = Color(0xFFE4F7ED)
private val textPrimaryColor = Color(0xFF212121)
private val textSecondaryColor = Color(0xFF757575)
private val surfaceColor = Color.White
private val dividerColor = Color(0xFFDDDDDD)

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
    var showDialog by remember { mutableStateOf(false) }
    var showFilters by remember { mutableStateOf(false) }

    val categories = listOf("All", "vegetable", "fruits", "rootcrops", "grains", "spices")
    val coroutineScope = rememberCoroutineScope()
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(navController.context) }
    val permissionState = rememberPermissionState(android.Manifest.permission.ACCESS_FINE_LOCATION)
    val context = LocalContext.current

    var minPrice by remember { mutableStateOf("") }
    var maxPrice by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf<String?>(null) }
    val authState by authViewModel.authState.observeAsState()

    // ✅ Fetch userType before rendering the UI
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            firestore.collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType")?.trim()?.lowercase()
                }
                .addOnFailureListener { e ->
                    Log.e("FarmerMarketScreen", "Error fetching userType", e)
                }
        }
    }

    val isFarmer = userType == "farmer"

    // ✅ Fetch products only once with Firestore listener
    DisposableEffect(Unit) {
        val listenerRegistration = fetchProducts(firestore) { fetchedProducts ->
            products = fetchedProducts
            filteredProducts = fetchedProducts.sortedBy { it.price }
            isLoading = false
        }

        onDispose {
            listenerRegistration.remove()  // 🔥 Cleanup listener when screen is destroyed
        }
    }

    // ✅ Smooth Filtering & Sorting
    LaunchedEffect(selectedCategory, minPrice, maxPrice, isAscending, products) {
        val category = selectedCategory?.trim()?.lowercase()
        val min = minPrice.toDoubleOrNull() ?: 0.0
        val max = maxPrice.toDoubleOrNull() ?: Double.MAX_VALUE

        filteredProducts = products.filter { product ->
            val isCategoryMatch = category == null || category == "all" || product.category.trim().lowercase() == category
            val isPriceInRange = product.price in min..max

            isCategoryMatch && isPriceInRange
        }.sortedBy { if (isAscending) it.price else -it.price }
    }

    // ✅ Full-Screen Loading while fetching data
    if (isLoading || userType == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = primaryColor)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading Market...", style = MaterialTheme.typography.bodyLarge, color = Color(0xFF0DA54B))
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize().background(surfaceColor)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TopBar(navController, cartViewModel, userType = userType ?: "market")

                // Enhanced header section
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Marketplace",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimaryColor
                    )

                    // Filter toggle button
                    IconButton(
                        onClick = { showFilters = !showFilters },
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (showFilters) primaryColor else lightGreen)
                    ) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter Options",
                            tint = if (showFilters) Color.White else primaryColor
                        )
                    }
                }

                // Improved search bar
                SearchBar(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                        .shadow(4.dp, RoundedCornerShape(12.dp))
                )

                // Categories section
                Text(
                    "Categories",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = textPrimaryColor,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                LazyRow(modifier = Modifier.padding(bottom = 16.dp)) {
                    items(categories) { category ->
                        val isSelected = (selectedCategory == category) ||
                                (category == "All" && selectedCategory == null)

                        Card(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .height(40.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) primaryColor else lightGreen
                            ),
                            elevation = CardDefaults.cardElevation(
                                defaultElevation = if (isSelected) 4.dp else 0.dp
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .clickable { selectedCategory = if (category == "All") null else category }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    category,
                                    color = if (isSelected) Color.White else primaryColor,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Animated filter section
                AnimatedVisibility(
                    visible = showFilters,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(300))
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = lightGreen),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Filter by Price",
                                fontWeight = FontWeight.SemiBold,
                                color = textPrimaryColor,
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            PriceFilterSection(
                                minPrice = minPrice,
                                maxPrice = maxPrice,
                                onMinPriceChange = { minPrice = it },
                                onMaxPriceChange = { maxPrice = it },
                                isAscending = isAscending,
                                onSortToggle = { isAscending = !isAscending }
                            )
                        }
                    }
                }

                // Products grid header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Available Products",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = textPrimaryColor
                    )

                    Text(
                        "${filteredProducts.size} items",
                        fontSize = 14.sp,
                        color = textSecondaryColor
                    )
                }

                if (filteredProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrowup), // Replace with appropriate icon
                                contentDescription = "No Products",
                                tint = textSecondaryColor,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "No products available",
                                fontSize = 18.sp,
                                color = textSecondaryColor,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filteredProducts.size) { index ->
                            val product = filteredProducts[index]
                            ProductCard(product, navController, firestore, storage)
                        }
                    }
                }
            }

            // ✅ Enhanced FloatingActionButton for Farmers
            if (isFarmer) {
                FloatingActionButton(
                    onClick = { showDialog = true },
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    shape = CircleShape,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp,
                        pressedElevation = 8.dp
                    ),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(24.dp)
                        .size(60.dp)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Product",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }

    // ✅ Enhanced Add Product Dialog
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
                        quantity = quantity,
                        unit = unit,
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
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAddProduct: (String, String, String, Double, String, Double, Uri?) -> Unit
) {
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
    val unitOptions = listOf("kg", "grams")
    var selectedUnit by remember { mutableStateOf(unitOptions[0]) }
    var expandedCategory by remember { mutableStateOf(false) }
    var expandedUnit by remember { mutableStateOf(false) }
    var textFieldSize by remember { mutableStateOf(Size.Zero) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = surfaceColor,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()) // ⬅️ make it scrollable
        )  {

            Text(
                "Add New Product",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = textPrimaryColor
                ),
                modifier = Modifier.padding(bottom = 20.dp)
            )

            // Category Dropdown
            Text(
                "Category",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = textPrimaryColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            ExposedDropdownMenuBox(
                expanded = expandedCategory,
                onExpandedChange = { expandedCategory = !expandedCategory }
            ) {
                OutlinedTextField(
                    value = selectedCategory,
                    onValueChange = {},
                    readOnly = true,
                    placeholder = { Text("Select category") },
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = dividerColor,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .onGloballyPositioned { coordinates -> textFieldSize = coordinates.size.toSize() },
                    trailingIcon = {
                        IconButton(onClick = { expandedCategory = !expandedCategory }) {
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Dropdown",
                                tint = primaryColor
                            )
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

            Spacer(modifier = Modifier.height(16.dp))

            // Product Name
            Text(
                "Product Name",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = textPrimaryColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Enter product name") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = dividerColor,
                    cursorColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Description Input
            Text(
                "Description",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = textPrimaryColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                placeholder = { Text("Enter product description") },
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = dividerColor,
                    cursorColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                maxLines = 5
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Price Input
            Text(
                "Price",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = textPrimaryColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            OutlinedTextField(
                value = price,
                onValueChange = { price = it.filter { char -> char.isDigit() || char == '.' } },
                placeholder = { Text("Enter price here...") },
                leadingIcon = {
                    Text("₱", fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 12.dp))
                },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = TextFieldDefaults.outlinedTextFieldColors(
                    focusedBorderColor = primaryColor,
                    unfocusedBorderColor = dividerColor,
                    cursorColor = primaryColor
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Quantity and Unit Row with labels
            Text(
                "Quantity & Unit",
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Medium),
                color = textPrimaryColor,
                modifier = Modifier.padding(bottom = 4.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp), // Adds bottom spacing for visibility
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Quantity Input
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it.filter { char -> char.isDigit() || char == '.' } },
                    placeholder = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = dividerColor,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(0.65f)
                        .height(56.dp) // Match height with dropdown
                )

                // Unit Dropdown
                ExposedDropdownMenuBox(
                    expanded = expandedUnit,
                    onExpandedChange = { expandedUnit = it },
                    modifier = Modifier
                        .weight(0.35f)
                ) {
                    OutlinedTextField(
                        value = selectedUnit,
                        onValueChange = {},
                        readOnly = true,
                        placeholder = { Text("Unit") },
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = dividerColor,
                            cursorColor = primaryColor
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .height(56.dp), // Match height with quantity input
                        trailingIcon = {
                            IconButton(onClick = { expandedUnit = !expandedUnit }) {
                                Icon(
                                    Icons.Default.ArrowDropDown,
                                    contentDescription = "Dropdown",
                                    tint = primaryColor
                                )
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
                                    expandedUnit = false
                                }
                            )
                        }
                    }
                }
            }


            Spacer(modifier = Modifier.height(20.dp))

            // Image Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .clickable { imagePickerLauncher.launch("image/*") },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = lightGreen),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    if (imageUri != null) {
                        AsyncImage(
                            model = imageUri,
                            contentDescription = "Product Image",
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrowup),
                                contentDescription = "Upload Image",
                                tint = primaryColor,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                "Tap to upload product image",
                                color = primaryColor,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ✅ Form Buttons Row (Cancel / Add)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = textPrimaryColor),
                    border = BorderStroke(1.dp, dividerColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        "Cancel",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.weight(1f),
                    onClick = {
                        val priceDouble = price.toDoubleOrNull() ?: 0.0
                        val finalQuantity = quantity.toDoubleOrNull() ?: 1.0
                        onAddProduct(
                            selectedCategory,
                            name,
                            description,
                            finalQuantity,
                            selectedUnit,
                            priceDouble,
                            imageUri
                        )
                    }
                ) {
                    Text(
                        "Add Product",
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
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
        "quantity" to product.quantity,
        "quantityUnit" to product.quantityUnit,
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



