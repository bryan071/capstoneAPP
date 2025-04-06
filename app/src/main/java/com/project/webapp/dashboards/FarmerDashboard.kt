package com.project.webapp.dashboards

import WeatherSection
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.Chat
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
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
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.launch
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.Viewmodel.ChatViewModel
import com.project.webapp.api.AutoImageSlider
import com.project.webapp.components.TopBar
import com.project.webapp.datas.Post
import com.project.webapp.datas.Product
import java.text.NumberFormat
import java.util.Locale

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
    var loading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()

    // Animated loading scale
    val loadingScale by animateFloatAsState(
        targetValue = if (loading) 1.2f else 1f,
        animationSpec = tween(durationMillis = 1000),
        label = "loadingScale"
    )

    // Fetch userType from Firebase
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType")
                    loading = false
                }
                .addOnFailureListener {
                    Log.e("FirebaseError", "Failed to fetch user type: ${it.message}")
                    loading = false
                }
        } ?: run { loading = false }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = loading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.scale(loadingScale),
                    color = Color(0xFF0DA54B)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    "Loading Dashboard...",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color(0xFF0DA54B)
                )
            }
        }

        AnimatedVisibility(
            visible = !loading,
            enter = fadeIn(animationSpec = tween(durationMillis = 500)),
            exit = fadeOut()
        ) {
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
                    item { SearchBar(modifier) }
                    item { HeroBanner() }
                    item { FeaturedProductsSection(authViewModel, navController) }
                    item { DiscountsBanner() }
                    item { WeatherSection(context) }
                    item { CommunityFeed() }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }

        // Chat FAB with pulse animation effect
        ChatFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            unreadCount = unreadCount,
            onClick = { navController.navigate("chat") }
        )
    }
}

@Composable
fun ChatFab(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onClick: () -> Unit
) {
    // Pulse animation for when there are unread messages
    val pulseState = remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (pulseState.value && unreadCount > 0) 1.1f else 1f,
        animationSpec = tween(500),
        label = "pulseScale"
    )

    // Trigger pulse animation periodically when unread messages exist
    LaunchedEffect(unreadCount) {
        if (unreadCount > 0) {
            while (true) {
                pulseState.value = true
                kotlinx.coroutines.delay(1000)
                pulseState.value = false
                kotlinx.coroutines.delay(1000)
            }
        }
    }

    Box(modifier = modifier) {
        FloatingActionButton(
            onClick = onClick,
            modifier = Modifier.scale(scale),
            containerColor = Color(0xFF0DA54B),
            contentColor = Color.White
        ) {
            Icon(
                Icons.Outlined.Chat,
                contentDescription = "Chat",
                modifier = Modifier.size(24.dp)
            )
        }

        // Unread messages badge
        if (unreadCount > 0) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(Color.Red, shape = CircleShape)
                    .align(Alignment.TopEnd)
                    .offset(x = 4.dp, y = (-4).dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (unreadCount > 99) "99+" else unreadCount.toString(),
                    color = Color.White,
                    style = MaterialTheme.typography.bodySmall,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun SearchBar(modifier: Modifier) {
    var query by remember { mutableStateOf("") }

    OutlinedTextField(
        value = query,
        onValueChange = { query = it },
        placeholder = { Text("What are you looking for?") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search Icon",
                tint = Color(0xFF0DA54B)
            )
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .height(56.dp),
        shape = RoundedCornerShape(16.dp),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color(0xFF0DA54B),
            unfocusedIndicatorColor = Color.LightGray,
            cursorColor = Color(0xFF0DA54B)
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

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            // Image slider
            AutoImageSlider(images = bannerImages)

            // Gradient overlay for better text visibility
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.7f)
                            ),
                            startY = 0f,
                            endY = 450f
                        )
                    )
            )

            // Banner content
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "🌿 Fresh & Organic",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Discover the best local products!",
                    fontSize = 16.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
fun FeaturedProductsSection(authViewModel: AuthViewModel, navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val storage = FirebaseStorage.getInstance()
    var products by remember { mutableStateOf<List<Product>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Manage Firestore listener lifecycle
    DisposableEffect(Unit) {
        val listenerRegistration = fetchProducts(firestore) { fetchedProducts ->
            products = fetchedProducts
            isLoading = false
        }

        onDispose {
            listenerRegistration.remove()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "🌟 Featured Products",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF085F2F)
            )

            TextButton(
                onClick = { navController.navigate(Route.MARKET) },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = Color(0xFF0DA54B)
                )
            ) {
                Text(
                    "View All",
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

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
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFFF5F5F5)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "No Products",
                        tint = Color.Gray,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "No featured products available",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.Gray
                    )
                }
            }
        } else {
            LazyRow(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .height(250.dp),
                contentPadding = PaddingValues(horizontal = 8.dp)
            ) {
                items(products) { product ->
                    ProductCard(
                        product = product,
                        navController = navController,
                        firestore = firestore,
                        storage = storage,
                        onCardClick = {
                            if (!product.prodId.isNullOrEmpty()) {
                                navController.navigate("productDetails/${product.prodId}")
                            } else {
                                Log.e("Navigation", "Invalid productId: ${product.prodId}")
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun ProductCard(
    product: Product,
    navController: NavController,
    firestore: FirebaseFirestore,
    storage: FirebaseStorage,
    onCardClick: () -> Unit = {} // Optional parameter with default value
){
    val currencyFormatter = NumberFormat.getCurrencyInstance(Locale("en", "PH"))
    val formattedPrice = currencyFormatter.format(product.price)

    Card(
        modifier = Modifier
            .width(180.dp)
            .padding(end = 12.dp)
            .clickable { onCardClick() },
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(4.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFEAF5EA) // Lighter green for softer look
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Product Image with overlay for better aesthetics
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFDCEDC8))
            ) {
                AsyncImage(
                    model = product.imageUrl,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = product.name,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF085F2F),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = "Quantity: ${product.quantity.toInt()} ${product.quantityUnit}",
                fontSize = 12.sp,
                color = Color.DarkGray,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(top = 2.dp)
            )

            Text(
                text = formattedPrice,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black,
                modifier = Modifier.padding(vertical = 4.dp)
            )

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
                    .fillMaxWidth()
                    .height(36.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("View Details")
            }
        }
    }
}

@Composable
fun DiscountsBanner() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .height(80.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE) // Light red background
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color(0xFFD32F2F),
                            Color(0xFFFF5252)
                        )
                    )
                )
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.notification_icon), // Use appropriate icon
                contentDescription = "Discount Icon",
                tint = Color.White,
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    "Special Offers!",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    "Up to 50% off on select products",
                    fontSize = 14.sp,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun CommunityFeed() {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clickable { showDialog = true },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .height(IntrinsicSize.Min),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Community icon with background
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = "Community Icon",
                    tint = Color(0xFF0DA54B),
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Community Feed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3E3E3E)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Connect with local farmers and share your thoughts!",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Icon(
                painter = painterResource(id = R.drawable.search), // Replace with appropriate arrow icon
                contentDescription = "View Community",
                tint = Color(0xFF0DA54B),
                modifier = Modifier.size(24.dp)
            )
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
    val userId = FirebaseAuth.getInstance().currentUser?.uid ?: "user_123"
    val coroutineScope = rememberCoroutineScope()

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
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = Color(0xFF0DA54B),
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    "Community Feed",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = Color(0xFF0DA54B)
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // New Post Input
                OutlinedTextField(
                    value = newPost,
                    onValueChange = { newPost = it },
                    label = { Text("Share your thoughts...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedIndicatorColor = Color(0xFF0DA54B),
                        unfocusedIndicatorColor = Color.LightGray
                    ),
                    maxLines = 3
                )

                // Post button
                Button(
                    onClick = {
                        coroutineScope.launch {
                            if (newPost.isNotBlank()) {
                                addPost(postsCollection, newPost, userId)
                                newPost = ""
                            }
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.End)
                        .padding(bottom = 16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
                ) {
                    Text("Post")
                }

                Divider(thickness = 1.dp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))

                // Posts List with header
                Text(
                    "Recent Posts",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                if (posts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No posts yet. Be the first to share!",
                            color = Color.Gray
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(posts) { post ->
                            PostItem(post, postsCollection, userId)
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF0DA54B))
            }
        }
    )
}

@Composable
fun PostItem(post: Post, postsCollection: CollectionReference, userId: String) {
    val isCurrentUserPost = post.userId == userId
    val backgroundColor = if (isCurrentUserPost) Color(0xFFE3F2FD) else Color.White

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                // User icon/avatar
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (isCurrentUserPost) Color(0xFF0DA54B) else Color.Gray),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = "User Icon",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(8.dp))

                // User identification and timestamp
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCurrentUserPost) "You" else "Community Member",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = post.timestamp?.toDate()?.toString() ?: "Unknown time",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Delete button (only for user's own posts)
                if (isCurrentUserPost) {
                    IconButton(onClick = { deletePost(postsCollection, post.id) }) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete Post",
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            // Post content
            Text(
                text = post.content,
                fontSize = 14.sp,
                modifier = Modifier.padding(top = 8.dp, start = 40.dp)
            )
        }
    }
}

fun addPost(postsCollection: CollectionReference, content: String, userId: String) {
    if (content.isBlank()) return

    val postId = postsCollection.document().id // Generate ID first

    val newPost = hashMapOf(
        "id" to postId, // Store ID in the document itself
        "userId" to userId,
        "content" to content,
        "timestamp" to FieldValue.serverTimestamp()
    )

    postsCollection.document(postId).set(newPost)
        .addOnSuccessListener { Log.d("Firestore", "Post added successfully") }
        .addOnFailureListener { e -> Log.e("Firestore", "Error adding post", e) }
}

fun deletePost(postsCollection: CollectionReference, postId: String) {
    postsCollection.document(postId).delete()
        .addOnSuccessListener { Log.d("Firestore", "Post deleted successfully") }
        .addOnFailureListener { e -> Log.e("Firestore", "Error deleting post", e) }
}


fun fetchProducts(firestore: FirebaseFirestore, onProductsFetched: (List<Product>) -> Unit): ListenerRegistration {
    return firestore.collection("products")
        .addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e("FirestoreDebug", "Error fetching products: ${error.message}", error)
                return@addSnapshotListener
            }

            if (snapshot == null || snapshot.isEmpty) {
                Log.w("FirestoreDebug", "No products found")
                onProductsFetched(emptyList())
                return@addSnapshotListener
            }

            val fetchedProducts = snapshot.documents.mapNotNull { doc ->
                try {
                    Product(
                        prodId = doc.id,
                        ownerId = doc.getString("ownerId") ?: return@mapNotNull null,
                        category = doc.getString("category") ?: return@mapNotNull null,
                        imageUrl = doc.getString("imageUrl") ?: return@mapNotNull null,
                        name = doc.getString("name") ?: return@mapNotNull null,
                        description = doc.getString("description") ?: "",
                        price = doc.getDouble("price") ?: 0.0,
                        quantity = doc.getDouble("quantity") ?: 0.0,
                        quantityUnit = doc.getString("quantityUnit") ?: "unit",
                        cityName = doc.getString("cityName") ?: "Unknown",
                        listedAt = doc.getLong("listedAt") ?: System.currentTimeMillis()
                    )
                } catch (e: Exception) {
                    Log.e("FirestoreDebug", "Error parsing product data: ${e.message}", e)
                    null
                }
            }

            onProductsFetched(fetchedProducts)
            Log.d("FirestoreDebug", "Updated UI with ${fetchedProducts.size} products")
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

