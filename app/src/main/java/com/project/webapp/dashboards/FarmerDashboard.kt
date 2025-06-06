package com.project.webapp.dashboards

import WeatherSection
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.android.gms.tasks.Tasks
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.QuerySnapshot
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
import com.project.webapp.components.SearchBar
import com.project.webapp.datas.Announcement
import com.project.webapp.datas.UserData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
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
    var unreadCount by remember { mutableStateOf(0) }
    var loading by remember { mutableStateOf(true) }
    val coroutineScope = rememberCoroutineScope()
    val firestore = FirebaseFirestore.getInstance()

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

    // Listen for unread admin messages
    LaunchedEffect(Unit) {
        chatViewModel.getAdminChatUnreadCount { count ->
            unreadCount = count
        }
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
                    item { SearchBar(modifier = Modifier.fillMaxWidth(),
                        navController = navController,
                        firestore = firestore) }
                    item { HeroBanner() }
                    item { FeaturedProductsSection(authViewModel, navController) }
                    item { DiscountsBanner() }
                    item { WeatherSection(context) }
                    item { CommunityFeed() }

                    item { Spacer(modifier = Modifier.height(72.dp)) }
                }
            }
        }

        ChatFab(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp),
            unreadCount = unreadCount,
            onClick = {
                chatViewModel.createOrGetAdminChatRoom { chatRoomId ->
                    if (chatRoomId.isNotEmpty()) {
                        val encodedChatRoomId = Uri.encode(chatRoomId)
                        navController.navigate("chat/$encodedChatRoomId/false")
                        Log.d("NavDebug", "Navigating to chat/$chatRoomId/false")
                    } else {
                        Log.e("FarmerDashboard", "ChatRoomId is empty!")
                    }
                }
            }
        )
    }
}

@Composable
fun ChatFab(
    modifier: Modifier = Modifier,
    unreadCount: Int,
    onClick: () -> Unit,
    chatViewModel: ChatViewModel? = null
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
                contentDescription = "Chat with Admin",
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
                        timestamp = doc.getTimestamp("listedAt") ?: Timestamp.now()
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CommunityFeedDialog(onDismiss: () -> Unit) {
    val db = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val postsCollection = db.collection("community_posts")
    val context = LocalContext.current

    // States
    var newPost by remember { mutableStateOf("") }
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isPostsLoading by remember { mutableStateOf(true) }
    var isPostSubmitting by remember { mutableStateOf(false) }
    var showPostSuccess by remember { mutableStateOf(false) }
    var userData by remember { mutableStateOf<UserData?>(null) }
    var announcements by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var isAnnouncementsLoading by remember { mutableStateOf(true) }


    val userId = auth.currentUser?.uid ?: ""
    val currentUserId = userId
    val coroutineScope = rememberCoroutineScope()

    // Colors
    val primaryColor = Color(0xFF0DA54B)
    val lightGreen = Color(0xFFE8F5E9)

    // Fetch current user data for post creation
    LaunchedEffect(userId) {
        try {
            val document = db.collection("users").document(userId).get().await()
            userData = document.toObject(UserData::class.java)
        } catch (e: Exception) {
            Log.e("CommunityFeedDialog", "Error fetching user data", e)
        }
    }

    // Firestore listener for posts
    LaunchedEffect(Unit) {
        try {
            val postListener = postsCollection
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        Log.e("CommunityFeedDialog", "Listen failed.", e)
                        isPostsLoading = false
                        return@addSnapshotListener
                    }

                    if (snapshot != null) {
                        posts = snapshot.documents.mapNotNull { document ->
                            try {
                                document.toObject(Post::class.java)?.copy(postId = document.id)
                            } catch (e: Exception) {
                                Log.e("CommunityFeedDialog", "Error parsing post", e)
                                null
                            }
                        }
                    }
                    isPostsLoading = false
                }
        } catch (e: Exception) {
            Log.e("CommunityFeedDialog", "Error setting up post listener", e)
            isPostsLoading = false
        }
    }

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid ?: return@LaunchedEffect
        val userDocRef = db.collection("users").document(userId)

        try {
            userDocRef.get()
                .addOnSuccessListener { userSnapshot ->
                    val userType = userSnapshot.getString("userType")?.replaceFirstChar { it.uppercase() }
                    if (userType != null) {
                        val queries = listOf("All Users", userType).map { audience ->
                            db.collection("announcements")
                                .whereEqualTo("audience", audience)
                                .orderBy("date", Query.Direction.DESCENDING)
                                .get()
                        }

                        Tasks.whenAllSuccess<QuerySnapshot>(queries)
                            .addOnSuccessListener { results ->
                                announcements = results
                                    .flatMap { it.documents }
                                    .mapNotNull { it.toObject(Announcement::class.java) }
                                    .sortedByDescending { it.date } // merge + sort
                                isAnnouncementsLoading = false
                            }
                            .addOnFailureListener { e ->
                                Log.e("CommunityFeedDialog", "Error fetching announcements", e)
                                isAnnouncementsLoading = false
                            }
                    } else {
                        Log.e("CommunityFeedDialog", "User type is null")
                        isAnnouncementsLoading = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CommunityFeedDialog", "Error fetching user data", e)
                    isAnnouncementsLoading = false
                }
        } catch (e: Exception) {
            Log.e("CommunityFeedDialog", "Unexpected error", e)
            isAnnouncementsLoading = false
        }
    }



    // Function to create a new post
    fun createNewPost() {
        if (newPost.isNotBlank() && !isPostSubmitting && userData != null) {
            isPostSubmitting = true

            val newPostData = Post(
                userId = auth.currentUser?.uid ?: "",
                userName = userData?.firstname ?: "Anonymous",
                userImage = userData?.profilePicture ?: "",
                content = newPost,
                imageUrl = null,
                timestamp = Timestamp.now(),
                likes = 0,
                comments = 0
            )

            // Add to Firestore
            postsCollection.add(newPostData)
                .addOnSuccessListener {
                    newPost = ""
                    isPostSubmitting = false
                    showPostSuccess = true

                    // Hide success message after some time
                    MainScope().launch {
                        delay(3000)
                        showPostSuccess = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("CommunityFeedDialog", "Error creating post", e)
                    Toast.makeText(context, "Failed to create post", Toast.LENGTH_SHORT).show()
                    isPostSubmitting = false
                }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f)
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Dialog Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Title with icon
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(id = R.drawable.post),
                            contentDescription = null,
                            tint = primaryColor,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Community Feed",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = primaryColor
                        )
                    }

                    // Close button
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(
                                color = Color.LightGray.copy(alpha = 0.3f),
                                shape = CircleShape
                            )
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.close),
                            contentDescription = "Close",
                            tint = Color.Unspecified,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Divider(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Current user info for post creation
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = userData?.profilePicture ?: "",
                        contentDescription = "Profile Image",
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, primaryColor, CircleShape),
                        error = painterResource(id = R.drawable.profile_icon),
                        placeholder = painterResource(id = R.drawable.profile_icon),
                        contentScale = ContentScale.Crop
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            userData?.firstname ?: "User",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            "Farmer",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Post creation area
                OutlinedTextField(
                    value = newPost,
                    onValueChange = { newPost = it },
                    placeholder = { Text("Share updates with the farming community...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = primaryColor,
                        unfocusedBorderColor = Color.LightGray,
                        cursorColor = primaryColor
                    ),
                    shape = RoundedCornerShape(8.dp),
                    maxLines = 5
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Success message when post is added
                AnimatedVisibility(
                    visible = showPostSuccess,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(containerColor = lightGreen),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.check),
                                contentDescription = "Success",
                                tint = primaryColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                "Post shared successfully!",
                                color = primaryColor,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Add photo button
                    OutlinedButton(
                        onClick = { /* Add photo functionality */ },
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.addphoto),
                            contentDescription = "Add Photo",
                            tint = primaryColor,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Photo", color = primaryColor)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Post button
                    Button(
                        onClick = { createNewPost() },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        enabled = newPost.isNotBlank() && !isPostSubmitting,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isPostSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                painter = painterResource(id = R.drawable.post),
                                contentDescription = "Post",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }
                Text(
                    "Admin Announcements",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                if (isAnnouncementsLoading) {
                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.padding(8.dp))
                } else if (announcements.isEmpty()) {
                    Text(
                        "No announcements at the moment.",
                        color = Color.Gray,
                        fontStyle = FontStyle.Italic,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        announcements.forEach { announcement ->
                            Card(
                                shape = RoundedCornerShape(8.dp),
                                colors = CardDefaults.cardColors(containerColor = lightGreen.copy(alpha = 0.3f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(announcement.title, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(announcement.message)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    val formattedDate = SimpleDateFormat("MMM dd, yyyy hh:mm a", Locale.getDefault())
                                        .format(announcement.date!!.toDate())

                                    Text(
                                        "Sent on: $formattedDate",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )

                                }
                            }
                        }
                    }
                }

                Divider(
                    color = Color.LightGray.copy(alpha = 0.5f),
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Community Feed Section Title
                Text(
                    "Community Updates",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = primaryColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

// Scrollable Posts Feed
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 100.dp, max = 400.dp) // adjust height as needed
                        .verticalScroll(rememberScrollState())
                ) {
                    when {
                        isPostsLoading -> {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = primaryColor)
                            }
                        }
                        posts.isEmpty() -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(lightGreen.copy(alpha = 0.5f))
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.post),
                                    contentDescription = "No Posts",
                                    tint = primaryColor,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "No posts yet",
                                    fontWeight = FontWeight.Medium,
                                    color = Color.DarkGray
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "Be the first to share with the community!",
                                    fontSize = 14.sp,
                                    color = Color.Gray,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        else -> {
                            Column(
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                posts.forEach { post ->
                                    PostItem(
                                        post = post,
                                        currentUserId = currentUserId,
                                        firestore = db,
                                        onCommentClick = { /* Navigate to post details */ },
                                        onLikeUpdated = { /* Let snapshot listener handle updates */ },
                                        primaryColor = primaryColor
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    currentUserId: String,
    firestore: FirebaseFirestore,
    onCommentClick: () -> Unit,
    onLikeUpdated: () -> Unit,
    primaryColor: Color
) {
    val isCurrentUserPost = post.userId == currentUserId
    val backgroundColor = if (isCurrentUserPost) Color(0xFFE3F2FD) else Color.White
    val postsCollection = firestore.collection("community_posts")

    // Track if current user has liked this post
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likes) }
    val context = LocalContext.current

    // Check if user has already liked this post
    LaunchedEffect(post.postId) {
        if (currentUserId.isNotEmpty() && post.postId.isNotEmpty()) {
            try {
                val likeDocument = firestore
                    .collection("post_likes")
                    .document("${post.postId}_$currentUserId")
                    .get()
                    .await()

                isLiked = likeDocument.exists()
            } catch (e: Exception) {
                Log.e("PostItem", "Error checking like status", e)
            }
        }
    }

    // Function to handle liking/unliking posts
    fun toggleLike() {
        if (currentUserId.isEmpty() || post.postId.isEmpty()) return

        val likeDocRef = firestore
            .collection("post_likes")
            .document("${post.postId}_$currentUserId")

        val postDocRef = postsCollection.document(post.postId)

        firestore.runTransaction { transaction ->
            // Get the current post data
            val postSnapshot = transaction.get(postDocRef)
            val currentLikes = postSnapshot.getLong("likes") ?: 0

            if (isLiked) {
                // User is unliking the post
                transaction.delete(likeDocRef)
                transaction.update(postDocRef, "likes", currentLikes - 1)
                likeCount--
            } else {
                // User is liking the post
                val likeData = hashMapOf(
                    "userId" to currentUserId,
                    "postId" to post.postId,
                    "timestamp" to System.currentTimeMillis()
                )
                transaction.set(likeDocRef, likeData)
                transaction.update(postDocRef, "likes", currentLikes + 1)
                likeCount++
            }
        }.addOnSuccessListener {
            isLiked = !isLiked
            // No need to update likeCount here as we already updated it above
        }.addOnFailureListener { e ->
            Log.e("PostItem", "Error updating like", e)
            Toast.makeText(context, "Failed to update like", Toast.LENGTH_SHORT).show()
        }
    }

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
                // User profile image
                AsyncImage(
                    model = post.userImage,
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .border(1.dp, if (isCurrentUserPost) primaryColor else Color.Gray, CircleShape),
                    error = painterResource(id = R.drawable.profile_icon),
                    placeholder = painterResource(id = R.drawable.profile_icon),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.width(8.dp))

                // User identification and timestamp
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isCurrentUserPost) "You (${post.userName})" else post.userName,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )

                    Text(
                        text = formatTimestamp(post.timestamp),
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Delete button (only for user's own posts)
                if (isCurrentUserPost) {
                    IconButton(onClick = {
                        deletePost(postsCollection, post.postId)
                        onLikeUpdated()
                    }) {
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
                modifier = Modifier.padding(top = 8.dp, start = 4.dp, end = 4.dp)
            )

            // Post image if available
            if (!post.imageUrl.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                AsyncImage(
                    model = post.imageUrl,
                    contentDescription = "Post Image",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }

            // Like and comment buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { toggleLike() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = if (isLiked) R.drawable.like else R.drawable.like),
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Unspecified else Color.Blue,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$likeCount",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }

                // Comment button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { onCommentClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.comment),
                        contentDescription = "Comment",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${post.comments}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

fun deletePost(postsCollection: CollectionReference, postId: String) {
    postsCollection.document(postId).delete()
        .addOnSuccessListener { Log.d("Firestore", "Post deleted successfully") }
        .addOnFailureListener { e -> Log.e("Firestore", "Error deleting post", e) }
}

fun formatTimestamp(timestamp: Timestamp): String {
    return try {
        val sdf = SimpleDateFormat("MMM dd, yyyy • h:mm a", Locale.getDefault())
        sdf.format(timestamp.toDate())
    } catch (e: Exception) {
        "Unknown time"
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

