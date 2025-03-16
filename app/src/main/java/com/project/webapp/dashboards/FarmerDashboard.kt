package com.project.webapp.dashboards

import WeatherSection
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.ui.draw.clip
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
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.api.AutoImageSlider
import com.project.webapp.datas.Post
import com.project.webapp.datas.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale



@Composable
fun FarmerDashboard(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(14.dp)
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
            item { HeroBanner() }
            item { FeaturedProductsSection(authViewModel, navController) }
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
fun HeroBanner() {
    val bannerImages = listOf(
        "https://www.healthyeating.org/images/default-source/home-0.0/nutrition-topics-2.0/general-nutrition-wellness/2-2-2-2foodgroups_vegetables_detailfeature.jpg?sfvrsn=226f1bc7_6",
        "https://ujamaaseeds.com/cdn/shop/collections/TUBERS_720x.jpg?v=1674322049",
        "https://domf5oio6qrcr.cloudfront.net/medialibrary/11499/3b360279-8b43-40f3-9b11-604749128187.jpg"
    )

    // 🔥 Hero Banner Section
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Color.Gray) // Fallback color
    ) {
        AutoImageSlider(images = bannerImages)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)) // Dark overlay
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.Start
        ) {

            Text(
                text = "🌿 Fresh & Organic",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Discover the best local products!",
                fontSize = 16.sp,
                color = Color.White
            )
        }
    }
}

@Composable
fun FeaturedProductsSection(authViewModel: AuthViewModel, navController: NavController) {
    val storage = FirebaseStorage.getInstance()
    val firestore = FirebaseFirestore.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
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
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {

        Text(
            text = "🌟 Featured Products",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF085F2F),
            modifier = Modifier.padding(bottom = 8.dp)
        )

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
                color = Color.Gray,
                modifier = Modifier.padding(16.dp)
            )
        } else {
            LazyRow(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(250.dp) // Increased height for better layout
            ) {
                items(products) { product ->
                    ElevatedCard(
                        modifier = Modifier
                            .width(180.dp)
                            .padding(end = 12.dp)
                            .clip(RoundedCornerShape(16.dp)),
                        elevation = CardDefaults.elevatedCardElevation(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)) // Soft green
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            AsyncImage(
                                model = product.imageUrl,
                                contentDescription = product.name,
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(RoundedCornerShape(12.dp)),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = product.name,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF085F2F)
                            )
                            Text(
                                text = "₱${product.price}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.Black
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            // View Details Button
                            Button(
                                onClick = {
                                    if (!product.prodId.isNullOrEmpty()) {
                                        navController.navigate("productDetails/${product.prodId}")
                                    } else {
                                        Log.e("Navigation", "Invalid productId: ${product.prodId}")
                                    }
                                },
                                modifier = Modifier
                                    .padding(top = 8.dp)
                                    .fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
                            ) {
                                Text("View here")
                            }
                        }
                    }
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
    val posts = remember { mutableStateListOf<Post>() }
    val userId = "user_123" // Replace with actual logged-in user ID

    // Firestore listener with LaunchedEffect
    LaunchedEffect(Unit) {
        postsCollection.orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.e("Firestore", "Listen failed.", e)
                    return@addSnapshotListener
                }
                posts.clear()
                snapshot?.documents?.mapNotNull { it.toObject(Post::class.java) }?.let { posts.addAll(it) }
            }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Community Feed") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                LazyColumn(modifier = Modifier.height(200.dp)) {
                    items(posts) { post ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = post.content, // Ensure Post class has a content field
                                fontSize = 14.sp,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(
                                onClick = { deletePost(postsCollection, post.id) }, // Ensure Post class has an id field
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
            Button(
                onClick = { addPost(postsCollection, newPost, userId) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)) // Button background color
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent) // No background color
            ) {
                Text("Close", color = Color.Black) // Black text
            }
        }

    )
}


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

fun deletePost(postsCollection: CollectionReference, postId: String) {
    postsCollection.document(postId).delete()
        .addOnSuccessListener { Log.d("Firestore", "Post deleted successfully") }
        .addOnFailureListener { e -> Log.e("Firestore", "Error deleting post", e) }
}

@Composable
fun ProductCard(product: Product, navController: NavController, firestore: FirebaseFirestore, storage: FirebaseStorage) {

    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val formattedPrice = currencyFormatter.format(product.price)

    Card(
        modifier = Modifier
            .padding(end = 8.dp)
            .width(150.dp)
            .height(220.dp),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFC8E6C9)),
    ) {
        Column(
            modifier = Modifier.padding(12.dp).fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.FillBounds, // or ContentScale.Fit
                modifier = Modifier
                    .size(80.dp)
                    .clip(RoundedCornerShape(4.dp)) // Prevents awkward image edges
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
                onClick = {
                    if (!product.prodId.isNullOrEmpty()) {
                        navController.navigate("productDetails/${product.prodId}")
                    } else {
                        Log.e("Navigation", "Invalid productId: ${product.prodId}")
                    }
                },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
            ) {
                Text("View here")
            }
        }
    }
}

suspend fun fetchProducts(firestore: FirebaseFirestore): List<Product> {
    return try {
        val snapshot = firestore.collection("products").get().await()
        Log.d("FirestoreDebug", "Fetched ${snapshot.documents.size} products")

        val productList = snapshot.documents.mapNotNull { doc ->
            val id = doc.id
            val imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null
            val category = doc.getString("category") ?: return@mapNotNull null
            val name = doc.getString("name") ?: return@mapNotNull null
            val description = doc.getString("description") ?: "" // Default empty if missing
            val price = doc.getDouble("price") ?: return@mapNotNull null
            val cityName = doc.getString("cityName") ?: "Unknown"
            val ownerId = doc.getString("ownerId") ?: return@mapNotNull null
            val listedAt = doc.getLong("listedAt") ?: System.currentTimeMillis()

            Log.d(
                "FirestoreDebug",
                "Product Loaded: ID=$id, Name=$name, Category=$category, Price=$price, " +
                        "CityName=$cityName, OwnerID=$ownerId, ListedAt=$listedAt"
            )

            // Assign values correctly
            Product(
                prodId = id,
                ownerId = ownerId,
                category = category,
                imageUrl = imageUrl,
                name = name,
                description = description,
                price = price,
                cityName = cityName,
                listedAt = listedAt
            )
        }

        Log.d("FirestoreDebug", "Returning ${productList.size} products")
        productList
    } catch (e: Exception) {
        Log.e("FirestoreDebug", "Error fetching products", e)
        emptyList()
    }
}


@Composable
fun BottomNavigationBar(navController: NavController) {
    val currentRoute = navController.currentDestination?.route

    NavigationBar(
        modifier = Modifier
            .fillMaxWidth()
            .height(70.dp), // Adjust height for a modern look
        containerColor = Color.White,
        tonalElevation = 8.dp // Soft shadow effect
    ) {
        NavigationItem(
            iconId = R.drawable.home_icon,
            label = "Home",
            isSelected = currentRoute == Route.FARMER_DASHBOARD,
            onClick = {
                // Only navigate if not already on the screen
                if (currentRoute != Route.FARMER_DASHBOARD) {
                    navController.navigate(Route.FARMER_DASHBOARD)
                }
            }
        )

        NavigationItem(
            iconId = R.drawable.stall_icon,
            label = "Market",
            isSelected = currentRoute == Route.MARKET,
            onClick = {
                if (currentRoute != Route.MARKET) {
                    navController.navigate(Route.MARKET)
                }
            }
        )

        NavigationItem(
            iconId = R.drawable.notification_icon,
            label = "Notifications",
            isSelected = currentRoute == Route.NOTIFICATION,
            onClick = {
                if (currentRoute != Route.NOTIFICATION) {
                    navController.navigate(Route.NOTIFICATION)
                }
            }
        )

        NavigationItem(
            iconId = R.drawable.profile_icon,
            label = "Profile",
            isSelected = currentRoute == Route.PROFILE,
            onClick = {
                if (currentRoute != Route.PROFILE) {
                    navController.navigate(Route.PROFILE)
                }
            }
        )
    }
}

@Composable
fun RowScope.NavigationItem(iconId: Int, label: String, isSelected: Boolean, onClick: () -> Unit) {
    NavigationBarItem(
        icon = {
            Icon(
                painter = painterResource(id = iconId),
                contentDescription = label,
                modifier = Modifier.size(28.dp),
                tint = Color.Unspecified // 🔹 Keeps the original icon color
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = if (isSelected) Color(0xFF4CAF50) else Color.Gray
            )
        },
        selected = isSelected,
        onClick = onClick,
        alwaysShowLabel = true,
        colors = NavigationBarItemDefaults.colors(
            selectedIconColor = Color.Unspecified, // 🔹 Keeps original color for selected icon
            unselectedIconColor = Color.Unspecified, // 🔹 Keeps original color for unselected icon
            indicatorColor = Color(0xFFDCEDC8) // Light green background for selected tab
        )
    )
}

