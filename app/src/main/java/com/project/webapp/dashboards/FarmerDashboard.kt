package com.project.webapp.dashboards

import WeatherSection
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.Viewmodel.ChatViewModel
import com.project.webapp.api.AutoImageSlider
import com.project.webapp.components.TopBar
import com.project.webapp.datas.Post
import com.project.webapp.datas.Product
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.material3.Button
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.ButtonDefaults




@Composable
fun FarmerDashboard(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    cartViewModel: CartViewModel,
    chatViewModel: ChatViewModel
) {
    val context = LocalContext.current
    var userType by remember { mutableStateOf<String?>(null) }
    val unreadCount by chatViewModel.unreadMessagesCount.collectAsState(initial = 0)

    // Fetch userType from Firebase
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType")
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            userType?.let { type ->
                TopBar(navController, cartViewModel, userType = type)
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item { SearchBar() }
                item { HeroBanner() }
                item { FeaturedProductsSection(authViewModel, navController) }
                item { DiscountsBanner() }
                item { WeatherSection(context) }
                item { CommunityFeed() }
            }
        }

        // Floating Action Button (FAB) for Chat
        FloatingActionButton(
            onClick = { navController.navigate("chat") },
            modifier = Modifier
                .align(Alignment.BottomEnd)  // Ensures placement at bottom right
                .padding(16.dp),
            containerColor = Color(0xFF0DA54B)
        ) {
            Icon(Icons.Outlined.Chat, contentDescription = "Chat")
        }

        // Unread messages badge
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Red, shape = CircleShape)
                    .align(Alignment.BottomEnd)
                    .offset(x = (-4).dp, y = (-4).dp), // Adjusts position relative to FAB
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = unreadCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall
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
                                text ="₱${String.format("%,d", product.price.toInt())}.00",
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
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(6.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Outlined.Groups, // Correct for Material 3
                contentDescription = "Community Icon",
                tint = Color(0xFF0DA54B),
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text("Community Feed", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Tap to see the latest posts or share your own thoughts!",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
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
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "user_123" // Get actual user ID

    // Firestore listener
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
        title = { Text("Community Feed", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Posts List
                LazyColumn(
                    modifier = Modifier.height(300.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(posts) { post ->
                        PostItem(post, postsCollection, userId)
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))

                // New Post Input
                OutlinedTextField(
                    value = newPost,
                    onValueChange = { newPost = it },
                    label = { Text("Write something...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { addPost(postsCollection, newPost, userId); newPost = "" },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
            ) {
                Text("Post")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color.Gray)
            }
        }
    )
}

@Composable
fun PostItem(post: Post, postsCollection: CollectionReference, userId: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp, horizontal = 8.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "User Icon",
                tint = Color(0xFF0DA54B),
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = post.content, fontSize = 14.sp)
                Text(
                    text = post.timestamp?.toDate()?.toString() ?: "Unknown time", // Converts to readable date
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            if (post.userId == userId) {
                IconButton(onClick = { deletePost(postsCollection, post.id) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete Post", tint = Color.Red)
                }
            }
        }
    }
}



fun addPost(postsCollection: CollectionReference, content: String, userId: String) {
    if (content.isBlank()) return

    val newPost = hashMapOf(
        "userId" to userId,
        "content" to content,
        "timestamp" to FieldValue.serverTimestamp() // This ensures Firestore saves a proper timestamp
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

