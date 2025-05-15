package com.project.webapp.components

import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.webapp.Viewmodel.AuthViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.project.webapp.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.project.webapp.datas.Product

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerNotificationScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    firestore: FirebaseFirestore,
    cartViewModel: CartViewModel
) {
    val notifications = remember { mutableStateListOf<Map<String, Any>>() }
    var selectedNotification by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val primaryColor = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF7FAF9)
    val notificationList = remember { mutableStateListOf<Map<String, Any>>() }

    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            firestore.collection("notifications")
                .whereEqualTo("userId", currentUserId) // Filter notifications by current user ID
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        Log.e("Firestore", "Error fetching notifications", error)
                        Log.d("NotificationDebug", "Current logged-in user ID: $currentUserId")
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        notifications.clear()
                        notifications.addAll(
                            snapshot.documents.mapNotNull { doc ->
                                doc.data?.plus("id" to doc.id)
                            }
                        )

                        Log.d("Firestore", "Fetched notifications: $notifications")
                    }
                }
        } else {
            // Handle case when user is not logged in
            isLoading = false
        }
    }

    val context = LocalContext.current
    var userType by remember { mutableStateOf<String?>(null) }

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

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = primaryColor,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Notifications",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.DarkGray
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.White,
                    titleContentColor = Color.DarkGray,
                ),
                modifier = Modifier.shadow(elevation = 4.dp)
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (isLoading) {
                LoadingAnimation(primaryColor = primaryColor)
            } else if (notifications.isEmpty()) {
                EmptyNotificationScreen()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(notifications.size) { index ->
                        val notification = notifications[index]
                        NotificationItem(
                            notification = notification,
                            firestore = firestore,
                            primaryColor = primaryColor,
                            onClick = { /* Handle navigation */ },
                            onDelete = { id ->
                                notifications.removeAll { it["id"] == id }
                            }
                        )
                    }
                }
            }
        }
    }

    selectedNotification?.let { notification ->
        NotificationDetailsDialog(
            notification = notification,
            onDismiss = { selectedNotification = null },
            primaryColor = primaryColor,
            currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""
        )
    }
}

@Composable
fun LoadingAnimation(primaryColor: Color) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            val infiniteTransition = rememberInfiniteTransition(label = "loading_transition")
            val angle by infiniteTransition.animateFloat(
                initialValue = 0f,
                targetValue = 360f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1000, easing = LinearEasing),
                    repeatMode = RepeatMode.Restart
                ),
                label = "rotation_animation"
            )
            val scale by infiniteTransition.animateFloat(
                initialValue = 0.8f,
                targetValue = 1.2f,
                animationSpec = infiniteRepeatable(
                    animation = tween(800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale_animation"
            )

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .graphicsLayer {
                        rotationZ = angle
                        scaleX = scale
                        scaleY = scale
                    }
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.fillMaxSize(),
                    color = primaryColor,
                    strokeWidth = 6.dp
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Loading notifications...",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor
            )
        }
    }
}

@Composable
fun NotificationItem(
    notification: Map<String, Any>,
    firestore: FirebaseFirestore,
    onClick: () -> Unit,
    onDelete: (String) -> Unit,
    primaryColor: Color
) {
    val notificationType = notification["type"] as? String ?: "product_added"
    val message = notification["message"] as? String ?: getDefaultMessage(notificationType)
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val category = notification["category"] as? String ?: "Unknown"
    val name = notification["name"] as? String ?: "Unnamed"
    val price = (notification["price"] as? Number)?.toDouble() ?: 0.0
    val quantity = (notification["quantity"] as? Number)?.toInt() ?: 0
    val quantityUnit = notification["quantityUnit"] as? String ?: "Unknown"
    val timestamp = notification["timestamp"] as? Long ?: 0L
    val userId = notification["userId"] as? String
    val buyerId = notification["buyerId"] as? String
    val notificationId = notification["id"] as? String
    val transactionType = notification["transactionType"] as? String ?: notificationType
    val organizationName = notification["organizationName"] as? String // Added for donation notifications

    val formattedDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    var ownerName by remember { mutableStateOf("Loading...") }
    var buyerName by remember { mutableStateOf("") }
    var isPressed by remember { mutableStateOf(false) }

    // Determine notification icon and color based on type
    val (notificationIcon, notificationIconTint) = when (notificationType) {
        "product_sold" -> Icons.Default.ShoppingCart to Color(0xFF0DA54B) // Blue for sales
        "product_donated" -> Icons.Default.Favorite to Color(0xFFE91E63) // Pink for donations
        else -> Icons.Default.Notifications to primaryColor // Default green for new products
    }

    // Fetch owner name
    LaunchedEffect(userId) {
        userId?.let { id ->
            firestore.collection("users").document(id)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstname") ?: ""
                        val lastName = document.getString("lastname") ?: ""

                        ownerName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            "$firstName $lastName".trim()
                        } else {
                            document.getString("email") ?: "Unknown"
                        }
                    } else {
                        ownerName = "Unknown"
                    }
                }
                .addOnFailureListener {
                    ownerName = "Unknown"
                    Log.e("Firestore", "Error fetching owner name for userId: $id")
                }
        } ?: run {
            ownerName = "Unknown"
        }
    }

    // Fetch buyer name if applicable (for sales only)
    LaunchedEffect(buyerId, notificationType) {
        if (notificationType == "product_sold" && buyerId != null) {
            firestore.collection("users").document(buyerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstname") ?: ""
                        val lastName = document.getString("lastname") ?: ""

                        buyerName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            "$firstName $lastName".trim()
                        } else {
                            document.getString("email") ?: "Unknown"
                        }
                    } else {
                        buyerName = "Unknown"
                    }
                }
                .addOnFailureListener {
                    buyerName = "Unknown"
                    Log.e("Firestore", "Error fetching buyer name for buyerId: $buyerId")
                }
        } else if (notificationType == "product_donated") {
            buyerName = organizationName ?: "Unknown Organization" // Use organizationName for donations
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        tryAwaitRelease()
                        isPressed = false
                    },
                    onTap = { onClick() }
                )
            }
            .graphicsLayer {
                scaleX = if (isPressed) 0.98f else 1f
                scaleY = if (isPressed) 0.98f else 1f
            }
            .animateContentSize(
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 2.dp,
            pressedElevation = 4.dp
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            if (imageUrl.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(imageUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Product Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFEDF7F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = notificationIcon,
                        contentDescription = "Notification Icon",
                        modifier = Modifier.size(50.dp),
                        tint = notificationIconTint
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 8.dp)
            ) {
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Display different message content based on notification type
                when (notificationType) {
                    "product_sold" -> {
                        Text(
                            text = if (buyerName.isNotEmpty()) "Sold to $buyerName" else "Your product was sold!",
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                    "product_donated" -> {
                        Text(
                            text = if (buyerName.isNotEmpty()) "Donated to $buyerName" else "You donated $name!",
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                    else -> {
                        Text(
                            text = message,
                            fontSize = 14.sp,
                            color = Color.DarkGray,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            lineHeight = 20.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .background(
                            color = notificationIconTint.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "₱${String.format("%.2f", price)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = notificationIconTint
                    )

                    Text(
                        text = " • ",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "$quantity $quantityUnit",
                        fontSize = 14.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    when (notificationType) {
                        "product_sold", "product_donated" -> {
                            Icon(
                                imageVector = if (notificationType == "product_sold")
                                    Icons.Default.ShoppingCart else Icons.Default.Favorite,
                                contentDescription = if (notificationType == "product_sold") "Sale" else "Donation",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (notificationType == "product_sold") "Sold" else "Donated",
                                fontSize = 13.sp,
                                color = notificationIconTint,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        else -> {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "User",
                                tint = Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = ownerName,
                                fontSize = 13.sp,
                                color = Color.DarkGray,
                                fontWeight = FontWeight.Medium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = "Time",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$formattedDate at $formattedTime",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (!notificationId.isNullOrEmpty()) {
                IconButton(
                    onClick = {
                        Log.d("DeleteNotification", "Trying to delete ID: $notificationId")
                        deleteNotification(firestore, notificationId!!) {
                            onDelete(notificationId)
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

// Helper function to get default message based on notification type
private fun getDefaultMessage(notificationType: String): String {
    return when (notificationType) {
        "product_sold" -> "Your product was sold!"
        "product_donated" -> "You donated a product!"
        else -> "New product added!"
    }
}

@Composable
fun NotificationDetailsDialog(
    notification: Map<String, Any>,
    onDismiss: () -> Unit,
    primaryColor: Color,
    currentUserId: String
) {
    val firestore = FirebaseFirestore.getInstance()
    val notificationType = notification["type"] as? String ?: "product_added"
    val category = notification["category"] as? String ?: "Unknown"
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val name = notification["name"] as? String ?: "Unnamed"
    val price = (notification["price"] as? Number)?.toDouble() ?: 0.0
    val quantity = (notification["quantity"] as? Number)?.toInt()
    val quantityUnit = notification["quantityUnit"] as? String
    val location = notification["location"] as? String ?: "Location not available"
    val userId = notification["userId"] as? String ?: "Unknown"
    val buyerId = notification["buyerId"] as? String
    val timestamp = notification["timestamp"] as? Long ?: 0L
    val message = notification["message"] as? String ?: getDefaultMessage(notificationType)
    val paymentMethod = notification["paymentMethod"] as? String
    val deliveryAddress = notification["deliveryAddress"] as? String
    val organizationName = notification["organizationName"] as? String // Added for donation notifications

    Log.d("PaymentDebug", "Retrieved payment method from notification: $paymentMethod")

    val formattedTime = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    var ownerName by remember { mutableStateOf("Loading...") }
    var buyerName by remember { mutableStateOf("Loading...") }
    var isImageLoading by remember { mutableStateOf(true) }

    // Determine notification icon and color based on type
    val (dialogIcon, dialogTitle, dialogColor) = when (notificationType) {
        "product_sold" -> Triple(
            Icons.Default.ShoppingCart,
            "Sale Details",
            Color(0xFF0DA54B) // Blue for sales
        )
        "product_donated" -> Triple(
            Icons.Default.Favorite,
            "Donation Details",
            Color(0xFFE91E63) // Pink for donations
        )
        else -> Triple(
            Icons.Default.ShoppingCart,
            "Product Details",
            primaryColor // Default green for new products
        )
    }

    // Fetch owner name (seller or poster)
    LaunchedEffect(userId) {
        firestore.collection("users").document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val firstName = document.getString("firstname") ?: ""
                    val lastName = document.getString("lastname") ?: ""

                    ownerName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                        "$firstName $lastName".trim()
                    } else {
                        document.getString("email") ?: "Unknown"
                    }
                } else {
                    ownerName = "Unknown"
                }
            }
            .addOnFailureListener {
                ownerName = "Unknown"
                Log.e("Firestore", "Error fetching owner name for userId: $userId")
            }
    }

    // Fetch buyer name for sales or set organization name for donations
    LaunchedEffect(buyerId, notificationType) {
        if (notificationType == "product_sold" && buyerId != null) {
            firestore.collection("users").document(buyerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstname") ?: ""
                        val lastName = document.getString("lastname") ?: ""

                        buyerName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            "$firstName $lastName".trim()
                        } else {
                            document.getString("email") ?: "Unknown"
                        }
                    } else {
                        buyerName = "Unknown"
                    }
                }
                .addOnFailureListener {
                    buyerName = "Unknown"
                    Log.e("Firestore", "Error fetching buyer name for buyerId: $buyerId")
                }
        } else if (notificationType == "product_donated" && buyerId != null) {
            firestore.collection("users").document(buyerId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstname") ?: ""
                        val lastName = document.getString("lastname") ?: ""

                        buyerName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            "$firstName $lastName".trim()
                        } else {
                            document.getString("email") ?: "Unknown"
                        }
                    } else {
                        buyerName = "Unknown"
                    }
                }
                .addOnFailureListener {
                    buyerName = "Unknown"
                    Log.e("Firestore", "Error fetching buyer name for buyerId: $buyerId")
                }
        }
    }

        AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(20.dp),
        containerColor = Color.White,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = dialogIcon,
                    contentDescription = dialogTitle,
                    tint = dialogColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    dialogTitle,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = dialogColor
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                if (imageUrl.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .border(1.dp, Color(0xFFE0E0E0), RoundedCornerShape(16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isImageLoading) {
                            CircularProgressIndicator(
                                color = dialogColor,
                                modifier = Modifier.size(40.dp)
                            )
                        }

                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(imageUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Product Image",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onLoading = { isImageLoading = true },
                            onSuccess = { isImageLoading = false },
                            onError = { isImageLoading = false }
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFFEDF7F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = dialogIcon,
                            contentDescription = "Default Icon",
                            modifier = Modifier.size(90.dp),
                            tint = dialogColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Product name and price section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = name,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = Color.DarkGray
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Category,
                                contentDescription = "Category",
                                tint = Color.Gray,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = category,
                                fontSize = 15.sp,
                                color = Color.Gray
                            )
                        }
                    }

                    Card(
                        colors = CardDefaults.cardColors(containerColor = dialogColor),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "₱${String.format("%.2f", price)}",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Details section
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Text(
                            text = when (notificationType) {
                                "product_added" -> "Product Information"
                                "product_sold" -> "Sale Information"
                                else -> "Donation Information"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = dialogColor
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(
                            icon = Icons.Default.Category,
                            label = "Category",
                            value = category,
                            primaryColor = dialogColor
                        )

                        if (quantity != null && quantityUnit != null) {
                            DetailRow(
                                icon = Icons.Default.ShoppingBasket,
                                label = "Quantity",
                                value = "$quantity $quantityUnit",
                                primaryColor = dialogColor
                            )
                        }

                        DetailRow(
                            icon = Icons.Default.LocationOn,
                            label = "Location",
                            value = location,
                            primaryColor = dialogColor
                        )


                        val (labelText, nameValue) = when (notificationType) {
                            "product_added" -> "Posted by" to ownerName
                            "product_sold" -> "Seller" to ownerName
                            "product_donated" -> "Donated by" to (buyerName.ifEmpty { "Unknown Donor" })
                            else -> "Posted by" to ownerName
                        }

                        DetailRow(
                            icon = Icons.Default.Person,
                            label = labelText,
                            value = nameValue,
                            primaryColor = dialogColor
                        )


                        when (notificationType) {
                            "product_sold" -> {
                            DetailRow(
                                icon = Icons.Default.ShoppingCart,
                                label = "Buyer",
                                value = buyerName,
                                primaryColor = dialogColor
                            )
                        }
                            "product_donated" -> {
                            DetailRow(
                                icon = Icons.Default.Favorite,
                                label = "Donated to",
                                value = organizationName ?: "Unknown Organization",
                                primaryColor = dialogColor
                            )
                        }
                            }

                        if (notificationType == "product_sold" || notificationType == "product_donated") {
                            if (notificationType == "product_sold") {
                                DetailRow(
                                    icon = Icons.Default.CreditCard,
                                    label = "Payment",
                                    value = paymentMethod ?: "Not specified",
                                    primaryColor = dialogColor
                                )

                                if (paymentMethod?.equals("GCash", ignoreCase = true) == true) {
                                    DetailRow(
                                        icon = Icons.Default.CreditCard,
                                        label = "GCash Info",
                                        value = "Paid electronically",
                                        primaryColor = dialogColor
                                    )
                                }

                                // Show delivery address if available
                                if (deliveryAddress != null) {
                                    DetailRow(
                                        icon = Icons.Default.Home,
                                        label = "Delivery to",
                                        value = deliveryAddress,
                                        primaryColor = dialogColor
                                    )
                                }
                            }
                        }

                        DetailRow(
                            icon = Icons.Default.Schedule,
                            label = if (notificationType == "product_added") "Posted on" else "Date",
                            value = formattedTime,
                            primaryColor = dialogColor
                        )

                        if (message.isNotEmpty() && !message.contains("New product")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))

                            Text(
                                text = "Message",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = dialogColor
                            )

                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color.White),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp)
                            ) {
                                Text(
                                    text = message,
                                    fontSize = 15.sp,
                                    modifier = Modifier.padding(12.dp),
                                    lineHeight = 24.sp
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = dialogColor),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth(),
                contentPadding = PaddingValues(vertical = 12.dp)
            ) {
                Text(
                    "Close",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
 fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String,
    primaryColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = primaryColor,
            modifier = Modifier.size(20.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            color = Color.DarkGray,
            modifier = Modifier.width(80.dp)
        )

        Text(
            text = value,
            fontSize = 15.sp,
            color = Color.DarkGray
        )
    }
}

@Composable
fun EmptyNotificationScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.empty)
        )

        // If you don't have a Lottie animation, use this fallback
        if (composition == null) {
            Icon(
                imageVector = Icons.Outlined.Notifications,
                contentDescription = "No Notifications",
                tint = Color(0xFF0DA54B),
                modifier = Modifier.size(100.dp)
            )
        } else {
            LottieAnimation(
                composition = composition,
                iterations = LottieConstants.IterateForever,
                modifier = Modifier.size(200.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            "No notifications yet",
            fontSize = 22.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.DarkGray
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            "You'll see updates about new products here.",
            fontSize = 16.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center,
            lineHeight = 24.sp
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Add a refresh button with bounce animation
        var isRefreshing by remember { mutableStateOf(false) }
        val rotation by animateFloatAsState(
            targetValue = if (isRefreshing) 360f else 0f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = LinearEasing
            ),
            label = "refresh_rotation"
        )

        Button(
            onClick = {
                isRefreshing = true
                // Simulate refresh
                Handler(Looper.getMainLooper()).postDelayed({
                    isRefreshing = false
                }, 1500)
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0DA54B)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = "Refresh",
                modifier = Modifier
                    .size(20.dp)
                    .graphicsLayer {
                        rotationZ = rotation
                    },
                tint = Color.White
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isRefreshing) "Refreshing..." else "Refresh",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
// Function to create sale notification in Firestore
fun createSaleNotification(
    firestore: FirebaseFirestore,
    product: Product,
    buyerId: String,
    quantity: Int = 1,
    paymentMethod: String? = null,
    deliveryAddress: String? = null,
    message: String? = null
) {
    val notificationData = hashMapOf(
        "type" to "product_sold",
        "productId" to product.prodId,
        "name" to product.name,
        "price" to product.price,
        "quantity" to product.quantity,
        "quantityUnit" to product.quantityUnit,
        "imageUrl" to product.imageUrl,
        "category" to product.category,
        "location" to product.cityName,
        "timestamp" to System.currentTimeMillis(),
        "userId" to product.ownerId, // The seller who will receive this notification
        "buyerId" to buyerId,
        "message" to "Your product was sold!",
        "transactionType" to "sale"
    )

    notificationData["paymentMethod"] = paymentMethod ?: "Not specified"
    deliveryAddress?.let { notificationData["deliveryAddress"] = it }

    firestore.collection("notifications")
        .add(notificationData)
        .addOnSuccessListener {
            Log.d("Firestore", "Sale notification created with ID: ${it.id}")
            Log.d("PaymentDebug", "Notification saved with payment: ${notificationData["paymentMethod"]}")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error creating sale notification", e)
        }
}

fun createDonationNotification(
    firestore: FirebaseFirestore,
    product: Product,
    donatorId: String,
    organizationName: String,
    quantity: Int,
    message: String
) {
    val timestamp = System.currentTimeMillis()

    // Notification for donator (buyer)
    val buyerNotification = hashMapOf(
        "type" to "product_donated",
        "productId" to product.prodId,
        "name" to product.name,
        "price" to product.price,
        "quantity" to product.quantity,
        "quantityUnit" to product.quantityUnit,
        "imageUrl" to product.imageUrl,
        "category" to product.category,
        "location" to product.cityName,
        "timestamp" to timestamp,
        "userId" to donatorId, // recipient
        "buyerId" to donatorId,
        "message" to "You donated ${product.name} to $organizationName!",
        "transactionType" to "donation",
        "organizationName" to organizationName
    )

    // Notification for seller (product owner)
    val sellerNotification = buyerNotification.toMutableMap().apply {
        this["userId"] = product.ownerId // now the seller is the recipient
        this["message"] = "${product.name} was donated to $organizationName."
    }
    Log.d("DonationDebug", "Creating buyer notification for UID: $donatorId")
    Log.d("DonationDebug", "Buyer Notification: $buyerNotification")



    // Send buyer's notification
    firestore.collection("notifications")
        .add(buyerNotification)
        .addOnSuccessListener {
            Log.d("DonationScreen", "Buyer donation notification created with ID: ${it.id}")
        }
        .addOnFailureListener { e ->
            Log.e("DonationScreen", "Error creating buyer donation notification: ${e.message}", e)
        }

    // Send seller's notification
    firestore.collection("notifications")
        .add(sellerNotification)
        .addOnSuccessListener {
            Log.d("DonationScreen", "Seller donation notification created with ID: ${it.id}")
        }
        .addOnFailureListener { e ->
            Log.e("DonationScreen", "Error creating seller donation notification: ${e.message}", e)
        }
}


fun deleteNotification(
    firestore: FirebaseFirestore,
    notificationId: String,
    onSuccess: () -> Unit
) {
    if (notificationId.isEmpty()) {
        Log.e("Firestore", "Invalid notificationId for deletion")
        return
    }

    firestore.collection("notifications").document(notificationId)
        .delete()
        .addOnSuccessListener {
            Log.d("Firestore", "Notification deleted successfully: $notificationId")
            onSuccess() // Call the callback to remove from UI
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error deleting notification: $notificationId", e)
        }
}
