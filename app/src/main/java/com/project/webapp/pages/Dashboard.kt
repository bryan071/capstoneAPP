package com.project.webapp.pages

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.tasks.await
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.productdata.Product
import fetchWeather
import getNearestCity
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale


@Composable
fun Dashboard(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        TopBar()
        SearchBar()
        FeaturedProductsSection()
        DiscountsBanner()
        WeatherSection(context)
        CommunityFeed()

    }
}

// 🔹 Top Bar
@Composable
fun TopBar() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Expanding the logo size while keeping space constraints
        Image(
            painter = painterResource(id = R.drawable.logo),
            contentDescription = "logo image",
            modifier = Modifier
                .size(100.dp) // Enlarged size
                .weight(1f) // Prevents extra spacing on the sides
        )

        Row(
            modifier = Modifier.weight(1f), // Ensures icons don't move too much
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = { /* Cart Action */ }) {
                Icon(
                    painter = painterResource(id = R.drawable.cart),
                    contentDescription = "Cart",
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
fun SearchBar() {
    var query by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("What you want to buy?") },
        leadingIcon = {
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_search),
                contentDescription = "Search Icon",
                tint = Color.Gray
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color(0xFF0DA54B),
            unfocusedIndicatorColor = Color.LightGray
        ),
        singleLine = true
    )
}



// 🔹 Featured Products Section
@Composable
fun FeaturedProductsSection() {
    val firestore = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val fetchedProducts = fetchProducts(firestore)
            Log.d("FirestoreDebug", "Setting state with ${fetchedProducts.size} products")
            products = fetchedProducts
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Featured Products", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        LazyRow(
            modifier = Modifier
                .padding(top = 8.dp)
                .height(240.dp)
        ) {
            items(products) { product ->
                ProductCard(product)
            }
        }
    }
}

@Composable
fun DiscountsBanner() {
    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color(0xFFD32F2F))
    ) {
        Text(
            "Exclusive Discounts! Up to 50% off!",
            fontSize = 18.sp,
            color = Color.White,
            modifier = Modifier.padding(16.dp)
        )
    }
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun WeatherSection(context: Context) {
    val fusedLocationProviderClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    val scope = rememberCoroutineScope()
    val apiKey = "865c1bf771394c12ad1122044250303"

    var cityName by remember { mutableStateOf("Fetching location...") }
    var temperature by remember { mutableStateOf("Fetching...") }
    var weatherCondition by remember { mutableStateOf("Please wait...") }
    var weatherIcon by remember { mutableStateOf(R.drawable.sun) }

    val locationPermissionState = rememberPermissionState(permission = android.Manifest.permission.ACCESS_FINE_LOCATION)

    LaunchedEffect(locationPermissionState.status) {
        if (locationPermissionState.status.isGranted) {
            getUserLocation(fusedLocationProviderClient) { city, lat, lon ->
                cityName = city // ✅ Update city name in UI
                scope.launch {
                    fetchWeather(city, apiKey) { temp, condition, icon ->
                        temperature = "$temp°C"
                        weatherCondition = condition
                        weatherIcon = icon
                    }
                }
            }
        } else {
            locationPermissionState.launchPermissionRequest()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth().padding(8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color(0xFF4CAF50))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = weatherIcon),
                    contentDescription = "Weather Icon",
                    tint = Color.White,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text("Weather Update", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(cityName, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = Color.White) // ✅ Show city name
                    Text("Temperature: $temperature | $weatherCondition", fontSize = 14.sp, color = Color.White)
                }
            }
        }
    }
}


// Function to determine the nearest city based on coordinates
fun getNearestCity(lat: Double, lon: Double): String {
    return when {
        // Metro Manila
        lat in 14.50..14.70 && lon in 120.90..121.00 -> "Manila"
        lat in 14.55..14.60 && lon in 121.00..121.10 -> "Makati"
        lat in 14.66..14.74 && lon in 121.00..121.12 -> "Quezon City"
        lat in 14.52..14.60 && lon in 121.00..121.08 -> "Pasig"
        lat in 14.52..14.58 && lon in 120.98..121.02 -> "Mandaluyong"
        lat in 14.43..14.55 && lon in 120.98..121.02 -> "Parañaque"
        lat in 14.47..14.55 && lon in 120.97..121.03 -> "Taguig"
        lat in 14.68..14.75 && lon in 120.98..121.06 -> "Caloocan"
        lat in 14.63..14.72 && lon in 120.98..121.06 -> "Valenzuela"
        lat in 14.57..14.65 && lon in 121.10..121.20 -> "Marikina"
        lat in 14.38..14.50 && lon in 120.88..121.00 -> "Las Piñas"
        lat in 14.47..14.55 && lon in 120.88..121.00 -> "Muntinlupa"

        // Bulacan
        lat in 14.40..14.50 && lon in 120.85..120.95 -> "Malolos"
        lat in 14.83..14.88 && lon in 120.80..120.90 -> "Meycauayan"
        lat in 14.75..14.80 && lon in 120.90..121.00 -> "San Jose del Monte"
        lat in 14.90..14.95 && lon in 120.85..120.95 -> "Marilao"
        lat in 14.88..14.93 && lon in 120.90..121.00 -> "Santa Maria"
        lat in 14.92..14.97 && lon in 120.95..121.05 -> "Norzagaray"
        lat in 14.85..14.90 && lon in 120.90..121.00 -> "Bocaue"
        lat in 14.82..14.87 && lon in 120.90..121.00 -> "Balagtas"
        lat in 14.85..14.90 && lon in 120.85..120.95 -> "Pandi"
        lat in 14.80..14.85 && lon in 120.85..120.95 -> "Plaridel"
        lat in 14.65..14.70 && lon in 120.90..121.00 -> "Baliuag"
        lat in 14.72..14.77 && lon in 120.93..121.03 -> "San Rafael"
        lat in 14.75..14.80 && lon in 120.85..120.95 -> "San Ildefonso"
        lat in 14.78..14.83 && lon in 120.92..121.02 -> "Hagonoy"
        lat in 14.87..14.92 && lon in 120.88..120.98 -> "Angat"
        lat in 14.70..14.75 && lon in 120.85..120.95 -> "Guiguinto"
        lat in 14.72..14.77 && lon in 120.87..120.97 -> "Pulilan"
        lat in 14.68..14.73 && lon in 120.80..120.90 -> "Calumpit"
        lat in 14.80..14.85 && lon in 120.78..120.88 -> "Doña Remedios Trinidad"

        // Pampanga (keeping your original)
        lat in 15.00..15.30 && lon in 120.60..120.80 -> "San Fernando"

        // Baguio (keeping your original)
        lat in 16.40..16.50 && lon in 120.30..120.40 -> "Baguio"

        else -> "Unknown Location"
    }
}


// 🌍 Function to Get User Location
@SuppressLint("MissingPermission")
private fun getUserLocation(
    fusedLocationProviderClient: FusedLocationProviderClient,
    callback: (String, Double, Double) -> Unit
) {
    fusedLocationProviderClient.lastLocation
        .addOnSuccessListener { location: Location? ->
            location?.let {
                val nearestCity = getNearestCity(it.latitude, it.longitude) // Get the nearest city
                Log.d("LocationDebug", "Detected City: $nearestCity") // ✅ Log the city
                Log.d("LocationDebug", "Latitude: ${it.latitude}, Longitude: ${it.longitude}") // ✅ Log the coordinates
                callback(nearestCity, it.latitude, it.longitude)
            } ?: run {
                Log.d("LocationDebug", "Failed to fetch location, using default Manila") // ✅ Log default location
                callback("Manila", 14.5995, 120.9842)
            }
        }
        .addOnFailureListener { exception ->
            Log.e("LocationError", "Error fetching location: ${exception.message}") // ✅ Log errors
        }
}

@Composable
fun CommunityFeed() {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Community Feed", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Person, contentDescription = "Community Icon", tint = Color(0xFF0DA54B))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Click to see the latest posts or share your own!", fontSize = 14.sp)
            }
        }
    }

    if (showDialog) {
        CommunityFeedDialog(onDismiss = { showDialog = false })
    }
}



@Composable
fun CommunityFeedDialog(onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val postsCollection = db.collection("community_posts")

    var newPost by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    val userId = "user_123" // Replace with actual logged-in user ID

    // Firestore listener with DisposableEffect
    DisposableEffect(Unit) {
        val listener: ListenerRegistration = postsCollection
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }

                val updatedPosts = snapshot?.documents?.map { doc ->
                    Pair(doc.id, doc.getString("content") ?: "")
                } ?: emptyList()
                posts = updatedPosts
            }

        onDispose { listener.remove() }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Community Feed") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(posts) { (postId, post) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(post, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            IconButton(onClick = { deletePost(postsCollection, postId) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete Post", tint = Color.Red)
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = newPost,
                    onValueChange = { newPost = it },
                    label = { Text("Write a post...") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = { addPost(postsCollection, newPost, userId) }) {
                Text("Post")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


// Function to add a new post to Firestore
fun addPost(postsCollection: CollectionReference, content: String, userId: String) {
    if (content.isBlank()) return

    val newPost = hashMapOf(
        "userId" to userId,
        "content" to content,
        "timestamp" to FieldValue.serverTimestamp()
    )

    postsCollection.add(newPost)
        .addOnSuccessListener { Log.d("Firestore", "Post added successfully") }
        .addOnFailureListener { e -> Log.e("Firestore", "Error adding post", e) }
}


// Function to delete a post from Firestore
fun deletePost(postsCollection: CollectionReference, postId: String) {
    postsCollection.document(postId).delete()
        .addOnSuccessListener { Log.d("Firestore", "Post deleted successfully") }
        .addOnFailureListener { e -> Log.e("Firestore", "Error deleting post", e) }
}




// 🔹 Product Card Component
@Composable
fun ProductCard(product: Product) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH")) // Change to desired locale
    val formattedPrice = currencyFormatter.format(product.price)

    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(150.dp)
            .height(220.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.LightGray)
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = product.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = product.category,
                fontSize = 12.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = formattedPrice,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(top = 4.dp)
            )
            Button(
                onClick = { /* Add to Cart */ },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
            ) {
                Text("Add to Cart")
            }
        }
    }
}


// 🔹 Fetch Products from Firestore
suspend fun fetchProducts(firestore: FirebaseFirestore): List<Product> {
    return try {
        val snapshot = firestore.collection("products").get().await()
        Log.d("FirestoreDebug", "Fetched ${snapshot.documents.size} products")

        snapshot.documents.mapNotNull { doc ->
            val imageUrl = doc.getString("imageUrl") ?: run {
                Log.e("FirestoreDebug", "Missing 'imageUrl' in ${doc.id}")
                return@mapNotNull null
            }
            val category = doc.getString("category") ?: run {
                Log.e("FirestoreDebug", "Missing 'category' in ${doc.id}")
                return@mapNotNull null
            }
            val name = doc.getString("name") ?: run {
                Log.e("FirestoreDebug", "Missing 'name' in ${doc.id}")
                return@mapNotNull null
            }
            val price = doc.getDouble("price") ?: run {
                Log.e("FirestoreDebug", "Missing 'price' in ${doc.id}")
                return@mapNotNull null
            }

            Log.d("FirestoreDebug", "Product Loaded: $name, $category, $$price")

            Product(category, imageUrl, name, price)
        }
    } catch (e: Exception) {
        Log.e("FirestoreDebug", "Error fetching products", e)
        emptyList()
    }
}


// 🔹 Bottom Navigation Bar
@Composable
fun BottomNavigationBar(navController: NavController) {
    NavigationBar {
        val currentRoute = navController.currentDestination?.route

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.home_icon), contentDescription = "Home", modifier = Modifier.size(24.dp)) },
            label = { Text("Home") },
            selected = currentRoute == Route.dashboard,
            onClick = { navController.navigate(Route.dashboard) }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.stall_icon), contentDescription = "Market", modifier = Modifier.size(24.dp)) },
            label = { Text("Market") },
            selected = currentRoute == Route.market,
            onClick = { navController.navigate(Route.market) }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.notification_icon), contentDescription = "Notifications", modifier = Modifier.size(24.dp)) },
            label = { Text("Notifications") },
            selected = currentRoute == Route.notification,
            onClick = { navController.navigate(Route.notification) }
        )

        NavigationBarItem(
            icon = { Icon(painterResource(id = R.drawable.profile_icon), contentDescription = "Profile", modifier = Modifier.size(24.dp)) },
            label = { Text("Profile") },
            selected = currentRoute == Route.profiie, // Fixed typo (was Route.profiie)
            onClick = { navController.navigate(Route.profiie) }
        )
    }
}
