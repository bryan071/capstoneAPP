package com.project.webapp.pages

import WeatherSection
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
import androidx.compose.material.icons.filled.Edit
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.productdata.Product
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

        // Scrollable content
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 8.dp), // Add slight top padding
            verticalArrangement = Arrangement.spacedBy(16.dp) // Space between sections
        ) {
            item { SearchBar() }
            item { FeaturedProductsSection(authViewModel) }
            item { DiscountsBanner() }
            item { WeatherSection(context) }
            item { CommunityFeed() }
        }
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

        }
    }
}

@Composable
fun SearchBar() {
    var query by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("What you want to see?") },
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

@Composable
fun FeaturedProductsSection(authViewModel: AuthViewModel) {
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }  // Loading state
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        coroutineScope.launch {
            val fetchedProducts = fetchProducts(firestore)
            if (fetchedProducts.isNotEmpty()) {
                Log.d("FirestoreDebug", "Updating UI with ${fetchedProducts.size} products")
                products = fetchedProducts
            } else {
                Log.w("FirestoreDebug", "No products available")
            }
            isLoading = false  // Stop loading when data is fetched
        }
    }

    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text("Featured Products", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(240.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF0DA54B))
            }
        } else if (products.isEmpty()) {
            Text(
                "No featured products available",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(240.dp)
            ) {
                items(products) { product ->
                    ProductCard(
                        product = product,
                        currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: "",
                        firestore = firestore,
                        storage = storage
                    )
                }
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
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = post,
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { deletePost(postsCollection, postId) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete Post",
                                    tint = Color.Red
                                )
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
fun ProductCard(product: Product, currentUserId: String, firestore: FirebaseFirestore, storage: FirebaseStorage) {
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH")) // Change to desired locale
    val formattedPrice = currencyFormatter.format(product.price)

    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(150.dp)
            .clickable{}
            .height(220.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(Color.LightGray),
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
                Text("View here")
            }
            if (currentUserId == product.prodId) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    IconButton(onClick = { editProduct(product, firestore) }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { deleteProduct(product, firestore, storage) }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                    }
                }
            }
        }
    }
}

fun deleteProduct(product: Product, firestore: FirebaseFirestore, storage: FirebaseStorage) {
    // 🔹 Delete image from Firebase Storage
    val imageRef = storage.getReferenceFromUrl(product.imageUrl)
    imageRef.delete()
        .addOnSuccessListener {
            Log.d("FirestoreDebug", "Image deleted successfully")

            // 🔹 Delete product document from Firestore
            firestore.collection("products").document(product.prodId)
                .delete()
                .addOnSuccessListener {
                    Log.d("FirestoreDebug", "Product deleted successfully")
                }
                .addOnFailureListener { e -> Log.e("FirestoreDebug", "Error deleting product", e) }
        }
        .addOnFailureListener { e -> Log.e("FirestoreDebug", "Error deleting image", e) }
}

fun editProduct(product: Product, firestore: FirebaseFirestore) {
    val newPrice = 200.0 // Example new price (you should get input from user)

    firestore.collection("products").document(product.prodId)
        .update("price", newPrice)
        .addOnSuccessListener { Log.d("FirestoreDebug", "Product updated successfully") }
        .addOnFailureListener { e -> Log.e("FirestoreDebug", "Error updating product", e) }
}

// 🔹 Fetch Products from Firestore
suspend fun fetchProducts(firestore: FirebaseFirestore): List<Product> {
    return try {
        val snapshot = firestore.collection("products").get().await()
        Log.d("FirestoreDebug", "Fetched ${snapshot.documents.size} products")

        val productList = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
            val category = doc.getString("category") ?: return@mapNotNull null
            val name = doc.getString("name") ?: return@mapNotNull null
            val price = doc.getDouble("price") ?: return@mapNotNull null
            val cityName = doc.getString("cityName") ?: "Unknown" // Ensure default if missing

            Log.d("FirestoreDebug", "Product Loaded: ID=$id, Name=$name, Category=$category, Price=$price,  CityName=$cityName")

            // Assign values correctly
            Product(
                prodId = id,
                category = category,
                imageUrl = imageUrl,
                name = name,
                price = price,
                cityName = cityName


            )
        }

        Log.d("FirestoreDebug", "Returning ${productList.size} products")
        productList
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
