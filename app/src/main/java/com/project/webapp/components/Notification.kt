package com.project.webapp.components

import CartViewModel
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.Surface
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.ripple.rememberRipple
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
import com.project.webapp.R
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.rememberLottieComposition
import com.google.firebase.Timestamp
import com.project.webapp.datas.Product
import androidx.compose.material.icons.filled.Chat
import com.google.firebase.firestore.SetOptions
import com.project.webapp.Viewmodel.ChatViewModel
import com.project.webapp.components.delivery.OrderStatusTimeline
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerNotificationScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    firestore: FirebaseFirestore,
    cartViewModel: CartViewModel,
    chatViewModel: ChatViewModel
) {
    val notifications = remember { mutableStateListOf<Map<String, Any>>() }
    var selectedNotification by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val currentUserId = FirebaseAuth.getInstance().currentUser?.uid
    val primaryColor = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF7FAF9)

    LaunchedEffect(Unit) {
        if (currentUserId != null) {
            firestore.collection("notifications")
                .whereEqualTo("userId", currentUserId)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener { snapshot, error ->
                    isLoading = false
                    if (error != null) {
                        Log.e("Firestore", "Error fetching notifications", error)
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        notifications.clear()
                        notifications.addAll(
                            snapshot.documents.mapNotNull { doc ->
                                doc.data?.plus("id" to doc.id)
                            }
                        )
                    }
                }
        } else {
            isLoading = false
        }
    }

    val context = LocalContext.current
    var userType by remember { mutableStateOf<String?>(null) }

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
                            onClick = { selectedNotification = notification },
                            onDelete = { id ->
                                notifications.removeAll { it["id"] == id }
                            },
                            onChatClick = { notification ->
                                val notificationType = notification["type"] as? String
                                val currentUserId = FirebaseAuth.getInstance().currentUser?.uid ?: ""

                                val otherUserId = when (notificationType) {
                                    "product_sold", "product_donated" -> notification["buyerId"] as? String
                                    "purchase_confirmed", "donation_made" -> notification["sellerId"] as? String
                                    else -> null
                                }

                                val transactionId = notification["transactionId"] as? String ?: ""

                                if (otherUserId != null && transactionId.isNotEmpty()) {
                                    chatViewModel.createOrGetTransactionChatRoom(
                                        user1Id = currentUserId,
                                        user2Id = otherUserId,
                                        notificationId = transactionId
                                    ) { chatRoomId ->
                                        navController.navigate("chat/$chatRoomId/false")
                                    }
                                }
                            },
                            chatViewModel = chatViewModel
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
            currentUserId = currentUserId ?: ""
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
    onChatClick: (Map<String, Any>) -> Unit,
    primaryColor: Color,
    chatViewModel: ChatViewModel
) {
    val notificationType = notification["type"] as? String ?: "product_added"
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val name = notification["name"] as? String ?: "Unnamed"
    val price = (notification["price"] as? Number)?.toDouble() ?: 0.0
    val quantity = (notification["quantity"] as? Number)?.toInt() ?: 0
    val quantityUnit = notification["quantityUnit"] as? String ?: "Unknown"
    val notificationId = notification["id"] as? String
    val organizationName = notification["organizationName"] as? String
    val buyerId = notification["buyerId"] as? String
    val sellerId = notification["sellerId"] as? String
    val orderStatus = notification["orderStatus"] as? String

    var otherPartyName by remember { mutableStateOf("Loading...") }
    var isPressed by remember { mutableStateOf(false) }

    val (notificationIcon, notificationIconTint) = when (orderStatus) {
        "To Pay" -> Icons.Default.CreditCard to Color(0xFFFFC107)
        "To Ship" -> Icons.Default.LocalShipping to Color(0xFFFF9800)
        "To Deliver" -> Icons.Default.LocalShipping to Color(0xFF9C27B0)
        "To Receive" -> Icons.Default.CheckCircle to Color(0xFF2196F3)
        "Completed" -> Icons.Default.Check to Color(0xFF4CAF50)
        else -> when (notificationType) {
            "product_sold" -> Icons.Default.ShoppingCart to Color(0xFF0DA54B)
            "product_donated" -> Icons.Default.Favorite to Color(0xFFE91E63)
            "purchase_confirmed" -> Icons.Default.ShoppingCart to Color(0xFF2196F3)
            "donation_made" -> Icons.Default.Favorite to Color(0xFFFF9800)
            else -> Icons.Default.Notifications to primaryColor
        }
    }

    LaunchedEffect(buyerId, sellerId, notificationType) {
        val otherPartyId = when (notificationType) {
            "product_sold", "product_donated" -> buyerId
            "purchase_confirmed", "donation_made" -> sellerId
            else -> null
        }

        if (notificationType == "donation_made" || notificationType == "product_donated") {
            otherPartyName = organizationName ?: "Unknown Organization"
        } else if (otherPartyId != null) {
            firestore.collection("users").document(otherPartyId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val firstName = document.getString("firstname") ?: ""
                        val lastName = document.getString("lastname") ?: ""
                        otherPartyName = if (firstName.isNotEmpty() || lastName.isNotEmpty()) {
                            "$firstName $lastName".trim()
                        } else {
                            document.getString("email") ?: "Unknown"
                        }
                    } else {
                        otherPartyName = "Unknown"
                    }
                }
                .addOnFailureListener {
                    otherPartyName = "Unknown"
                }
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                onClick = onClick,
                indication = rememberRipple(),
                interactionSource = remember { MutableInteractionSource() }
            )
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

                Text(
                    text = when (notificationType) {
                        "product_sold" -> "Sold to $otherPartyName"
                        "product_donated" -> "Donated to $otherPartyName"
                        "purchase_confirmed" -> "Purchased from $otherPartyName"
                        "donation_made" -> "Donated to $otherPartyName"
                        else -> "New notification"
                    },
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

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

                if (orderStatus != null && notificationType in listOf("product_sold", "purchase_confirmed")) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .background(
                                color = getOrderStatusColor(orderStatus).copy(alpha = 0.1f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = when (orderStatus) {
                                "To Pay" -> Icons.Default.CreditCard
                                "To Ship" -> Icons.Default.LocalShipping
                                "To Receive" -> Icons.Default.CheckCircle
                                "Completed" -> Icons.Default.Check
                                else -> Icons.Default.Schedule
                            },
                            contentDescription = "Status Icon",
                            tint = getOrderStatusColor(orderStatus),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = orderStatus,
                            fontSize = 13.sp,
                            color = getOrderStatusColor(orderStatus),
                            fontWeight = FontWeight.Medium
                        )
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = notificationIcon,
                            contentDescription = "Status",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (notificationType) {
                                "product_sold" -> "Sold"
                                "product_donated" -> "Donated"
                                "purchase_confirmed" -> "Purchased"
                                "donation_made" -> "Donated"
                                else -> "Notification"
                            },
                            fontSize = 13.sp,
                            color = notificationIconTint,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = "Time",
                            tint = Color.Gray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Just now",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }

            val transactionId = notification["transactionId"] as? String ?: ""
            val hasValidTransaction = transactionId.isNotEmpty()

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                IconButton(
                    onClick = {
                        if (hasValidTransaction) {
                            onChatClick(notification)
                        }
                    },
                    enabled = hasValidTransaction,
                    modifier = Modifier
                        .size(40.dp)
                        .background(
                            color = if (hasValidTransaction)
                                primaryColor.copy(alpha = 0.1f)
                            else
                                Color.Gray.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(10.dp)
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Chat,
                        contentDescription = "Chat",
                        tint = if (hasValidTransaction) primaryColor else Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )
                }

                if (!notificationId.isNullOrEmpty()) {
                    IconButton(
                        onClick = {
                            deleteNotification(firestore, notificationId) {
                                onDelete(notificationId)
                            }
                        },
                        modifier = Modifier.size(40.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color.Red.copy(alpha = 0.7f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
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
    val sellerId = notification["sellerId"] as? String
    val timestamp = notification["timestamp"] as? Timestamp ?: null
    val message = notification["message"] as? String ?: getDefaultMessage(notificationType)
    val paymentMethod = notification["paymentMethod"] as? String
    val deliveryAddress = notification["deliveryAddress"] as? String
    val organizationName = notification["organizationName"] as? String
    val transactionId = notification["transactionId"] as? String
    val notificationId = notification["id"] as? String
    val orderId = notification["orderId"] as? String

    var paymentStatus by remember { mutableStateOf(notification["paymentStatus"] as? String ?: "Payment Pending") }
    var orderStatus by remember { mutableStateOf(notification["orderStatus"] as? String ?: "To Pay") }
    var isUpdatingPayment by remember { mutableStateOf(false) }
    var isUpdatingShipping by remember { mutableStateOf(false) }

    val context = LocalContext.current

    var ownerName by remember { mutableStateOf("Loading...") }
    var buyerName by remember { mutableStateOf("Loading...") }
    var isImageLoading by remember { mutableStateOf(true) }

    // FIXED: Correct buyer detection
    val isSeller = userId == currentUserId && notificationType == "product_sold"
    val isBuyer = userId == currentUserId && notificationType == "purchase_confirmed"

    val showTimeline = notificationType in listOf("product_sold", "purchase_confirmed")

    val showPaymentConfirmButton = isSeller &&
            notificationType == "product_sold" &&
            paymentStatus != "Payment Received" &&
            orderStatus == "To Pay"

    val showShipButton = isSeller &&
            notificationType == "product_sold" &&
            orderStatus == "To Ship" &&
            paymentStatus == "Payment Received"

    val showReceivedButton = isBuyer &&
            notificationType == "purchase_confirmed" &&
            orderStatus == "To Deliver"

    val (dialogIcon, dialogTitle, dialogColor) = when (notificationType) {
        "product_sold", "purchase_confirmed" -> Triple(
            Icons.Default.ShoppingCart,
            "Order Details",
            Color(0xFF2196F3)
        )
        "product_donated", "donation_made" -> Triple(
            Icons.Default.Favorite,
            "Donation Details",
            Color(0xFFE91E63)
        )
        "product_added" -> Triple(
            Icons.Default.Notifications,
            "New Product",
            Color(0xFF0DA54B)
        )
        else -> Triple(Icons.Default.Notifications, "Notification", primaryColor)
    }

    LaunchedEffect(userId, sellerId) {
        val sellerIdToFetch = if (notificationType == "purchase_confirmed") sellerId else userId

        if (sellerIdToFetch != null) {
            firestore.collection("users").document(sellerIdToFetch)
                .get()
                .addOnSuccessListener { doc ->
                    ownerName = if (doc.exists()) {
                        val first = doc.getString("firstname") ?: ""
                        val last = doc.getString("lastname") ?: ""
                        if (first.isNotEmpty() || last.isNotEmpty()) "$first $last".trim()
                        else doc.getString("email") ?: "Unknown"
                    } else "Unknown"
                }
                .addOnFailureListener { ownerName = "Unknown" }
        }
    }

    LaunchedEffect(buyerId, notificationType) {
        if (buyerId != null && (notificationType == "product_sold" || notificationType == "product_donated")) {
            firestore.collection("users").document(buyerId)
                .get()
                .addOnSuccessListener { doc ->
                    buyerName = if (doc.exists()) {
                        val first = doc.getString("firstname") ?: ""
                        val last = doc.getString("lastname") ?: ""
                        if (first.isNotEmpty() || last.isNotEmpty()) "$first $last".trim()
                        else doc.getString("email") ?: "Unknown"
                    } else "Unknown"
                }
                .addOnFailureListener { buyerName = "Unknown" }
        }
    }

    // Real-time status listener
    LaunchedEffect(orderId, transactionId) {
        if (!orderId.isNullOrEmpty() && notificationType in listOf("product_sold", "purchase_confirmed")) {
            firestore.collection("notifications")
                .document(notificationId ?: return@LaunchedEffect)
                .addSnapshotListener { snapshot, error ->
                    if (error != null || snapshot == null) return@addSnapshotListener

                    val newOrderStatus = snapshot.getString("orderStatus") ?: "To Pay"
                    val newPaymentStatus = snapshot.getString("paymentStatus") ?: "Payment Pending"

                    orderStatus = newOrderStatus
                    paymentStatus = newPaymentStatus

                    Log.d("NotificationSync", "Status updated in dialog: $newOrderStatus, Payment: $newPaymentStatus")
                }
        }
    }

    fun confirmPaymentReceived() {
        if (notificationId.isNullOrEmpty() || orderId.isNullOrEmpty()) {
            Toast.makeText(context, "Missing notification or order ID", Toast.LENGTH_SHORT).show()
            return
        }
        isUpdatingPayment = true

        val updates = mapOf(
            "paymentStatus" to "Payment Received",
            "orderStatus" to "To Ship"
        )

        firestore.collection("notifications").document(notificationId!!)
            .update(updates)
            .addOnSuccessListener {
                paymentStatus = "Payment Received"
                orderStatus = "To Ship"

                firestore.collection("orders").document(orderId!!)
                    .update("status", "To Ship")
                    .addOnSuccessListener {
                        Log.d("NotificationSync", "✓ Order status updated to To Ship")

                        updateBuyerStatus(firestore, orderId!!, "To Ship", "Payment Received")

                        Toast.makeText(context, "Payment confirmed! Ready to ship.", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationSync", "✗ Failed to update order", e)
                    }

                if (!transactionId.isNullOrEmpty()) {
                    firestore.collection("transactions").document(transactionId!!)
                        .update(mapOf(
                            "Payment_received" to true,
                            "status" to "To Ship"
                        ))
                }

                isUpdatingPayment = false
            }
            .addOnFailureListener { e ->
                isUpdatingPayment = false
                Log.e("NotificationSync", "✗ Failed to confirm payment", e)
                Toast.makeText(context, "Failed to confirm payment", Toast.LENGTH_SHORT).show()
            }
    }

    fun confirmItemShipped() {
        if (notificationId.isNullOrEmpty() || orderId.isNullOrEmpty()) {
            Toast.makeText(context, "Missing notification or order ID", Toast.LENGTH_SHORT).show()
            return
        }
        isUpdatingShipping = true

        firestore.collection("notifications").document(notificationId!!)
            .update("orderStatus", "To Deliver")
            .addOnSuccessListener {
                orderStatus = "To Deliver"

                firestore.collection("orders").document(orderId!!)
                    .update("status", "To Deliver")
                    .addOnSuccessListener {
                        Log.d("NotificationSync", "✓ Order status updated to To Deliver")

                        updateBuyerStatus(firestore, orderId!!, "To Deliver", null)

                        Toast.makeText(context, "Item marked as shipped!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationSync", "✗ Failed to update order", e)
                    }

                if (!transactionId.isNullOrEmpty()) {
                    firestore.collection("transactions").document(transactionId!!)
                        .update("status", "To Deliver")
                }

                isUpdatingShipping = false
            }
            .addOnFailureListener { e ->
                isUpdatingShipping = false
                Log.e("NotificationSync", "✗ Failed to update shipping", e)
                Toast.makeText(context, "Failed to update shipping", Toast.LENGTH_SHORT).show()
            }
    }

    fun confirmItemReceived() {
        if (notificationId.isNullOrEmpty() || orderId.isNullOrEmpty()) {
            Toast.makeText(context, "Missing notification or order ID", Toast.LENGTH_SHORT).show()
            return
        }
        isUpdatingShipping = true

        firestore.collection("notifications").document(notificationId!!)
            .update("orderStatus", "Completed")
            .addOnSuccessListener {
                orderStatus = "Completed"

                firestore.collection("orders").document(orderId!!)
                    .update("status", "Completed")
                    .addOnSuccessListener {
                        Log.d("NotificationSync", "✓ Order completed")

                        updateSellerStatus(firestore, orderId!!, "Completed")

                        Toast.makeText(context, "Order completed successfully!", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationSync", "✗ Failed to update order", e)
                    }

                if (!transactionId.isNullOrEmpty()) {
                    firestore.collection("transactions").document(transactionId!!)
                        .update("status", "Completed")
                }

                isUpdatingShipping = false
                onDismiss()
            }
            .addOnFailureListener { e ->
                isUpdatingShipping = false
                Log.e("NotificationSync", "✗ Failed to confirm delivery", e)
                Toast.makeText(context, "Failed to confirm delivery", Toast.LENGTH_SHORT).show()
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
                    contentDescription = null,
                    tint = dialogColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = dialogTitle,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color.DarkGray
                    )
                    if (showTimeline) {
                        Surface(
                            color = getOrderStatusColor(orderStatus).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = orderStatus,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = getOrderStatusColor(orderStatus),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
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
                            CircularProgressIndicator(color = dialogColor, modifier = Modifier.size(40.dp))
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
                        Icon(imageVector = dialogIcon, contentDescription = null, tint = dialogColor, modifier = Modifier.size(90.dp))
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = name, fontWeight = FontWeight.Bold, fontSize = 24.sp, color = Color.DarkGray)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Category, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = category, fontSize = 15.sp, color = Color.Gray)
                        }
                    }
                    Card(colors = CardDefaults.cardColors(containerColor = dialogColor), shape = RoundedCornerShape(12.dp)) {
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

                if (showTimeline) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                        shape = RoundedCornerShape(16.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Order Status",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = primaryColor
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            OrderStatusTimeline(
                                currentStatus = orderStatus,
                                paymentStatus = paymentStatus,
                                orderId = orderId,
                                primaryColor = dialogColor,
                                showActions = false
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F8F8)),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = when (notificationType) {
                                "product_added" -> "Product Information"
                                "product_sold", "purchase_confirmed" -> "Purchase Information"
                                "product_donated", "donation_made" -> "Donation Information"
                                else -> "Notification Details"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = dialogColor
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        DetailRow(icon = Icons.Default.Category, label = "Category", value = category, primaryColor = dialogColor)
                        if (quantity != null && quantityUnit != null) {
                            DetailRow(icon = Icons.Default.ShoppingBasket, label = "Quantity", value = "$quantity $quantityUnit", primaryColor = dialogColor)
                        }
                        DetailRow(icon = Icons.Default.LocationOn, label = "Location", value = location, primaryColor = dialogColor)

                        val (labelText, nameValue) = when (notificationType) {
                            "product_added" -> "Posted by" to ownerName
                            "product_sold" -> "Seller" to ownerName
                            "purchase_confirmed" -> "Seller" to ownerName
                            "product_donated" -> "Donated by" to buyerName
                            "donation_made" -> "Donated to" to (organizationName ?: "Unknown Organization")
                            else -> "User" to ownerName
                        }
                        DetailRow(icon = Icons.Default.Person, label = labelText, value = nameValue, primaryColor = dialogColor)

                        when (notificationType) {
                            "product_sold" -> {
                                DetailRow(icon = Icons.Default.Person, label = "Buyer", value = buyerName, primaryColor = dialogColor)
                            }
                            "product_donated" -> {
                                DetailRow(icon = Icons.Default.Favorite, label = "Donated to", value = organizationName ?: "Unknown", primaryColor = dialogColor)
                            }
                            "donation_made" -> {
                                DetailRow(icon = Icons.Default.Person, label = "From", value = ownerName, primaryColor = dialogColor)
                            }
                        }

                        if (notificationType == "product_sold" || notificationType == "purchase_confirmed") {
                            DetailRow(icon = Icons.Default.CreditCard, label = "Payment", value = paymentMethod ?: "Not specified", primaryColor = dialogColor)

                            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = if (paymentStatus == "Payment Received") Color(0xFF4CAF50) else Color.Gray, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(text = "Status:", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.DarkGray, modifier = Modifier.width(80.dp))
                                Card(
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (paymentStatus == "Payment Received") Color(0xFF4CAF50).copy(alpha = 0.1f) else Color.Red.copy(alpha = 0.1f)
                                    ),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = paymentStatus,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (paymentStatus == "Payment Received") Color(0xFF4CAF50) else Color.Red,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            if (deliveryAddress != null) {
                                DetailRow(icon = Icons.Default.Home, label = "Delivery to", value = deliveryAddress, primaryColor = dialogColor)
                            }
                        }

                        DetailRow(
                            icon = Icons.Default.Schedule,
                            label = if (notificationType == "product_added") "Posted on" else "Date",
                            value = timestamp?.toDate()?.toString() ?: "Unknown",
                            primaryColor = dialogColor
                        )

                        if (message.isNotEmpty() && !message.contains("New product")) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color.LightGray)
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Message", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = dialogColor)
                            Card(colors = CardDefaults.cardColors(containerColor = Color.White), shape = RoundedCornerShape(8.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                Text(text = message, fontSize = 15.sp, modifier = Modifier.padding(12.dp), lineHeight = 24.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Column {
                if (showPaymentConfirmButton) {
                    Button(
                        onClick = { confirmPaymentReceived() },
                        enabled = !isUpdatingPayment,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50), disabledContainerColor = Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (isUpdatingPayment) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirming...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(imageVector = Icons.Default.CreditCard, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Payment Received", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (showShipButton) {
                    Button(
                        onClick = { confirmItemShipped() },
                        enabled = !isUpdatingShipping,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2196F3), disabledContainerColor = Color.Gray),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (isUpdatingShipping) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Processing...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(imageVector = Icons.Default.LocalShipping, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Mark as Shipped", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                if (showReceivedButton) {
                    Button(
                        onClick = { confirmItemReceived() },
                        enabled = !isUpdatingShipping,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = primaryColor,
                            disabledContainerColor = Color.Gray
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        if (isUpdatingShipping) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirming...", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Confirm Delivered", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Gray),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(vertical = 12.dp)
                ) {
                    Text("Close", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    )
}

fun getOrderStatusColor(status: String): Color {
    return when (status.uppercase()) {
        "TO PAY", "PAYMENT PENDING" -> Color(0xFFFFC107)
        "TO SHIP" -> Color(0xFFFF9800)
        "TO DELIVER" -> Color(0xFF9C27B0)
        "TO RECEIVE" -> Color(0xFF2196F3)
        "COMPLETED" -> Color(0xFF4CAF50)
        "CANCELLED" -> Color(0xFFF44336)
        else -> Color.Gray
    }
}

private fun updateBuyerStatus(
    firestore: FirebaseFirestore,
    orderId: String,
    newStatus: String,
    paymentStatus: String? = null
) {
    Log.d("NotificationSync", "Updating buyer notifications for orderId: $orderId to status: $newStatus")

    firestore.collection("notifications")
        .whereEqualTo("orderId", orderId)
        .whereEqualTo("type", "purchase_confirmed")
        .get()
        .addOnSuccessListener { docs ->
            if (docs.isEmpty) {
                Log.w("NotificationSync", "⚠ No buyer notifications found for orderId: $orderId")
            }

            docs.forEach { doc ->
                val updates = mutableMapOf<String, Any>("orderStatus" to newStatus)
                if (paymentStatus != null) {
                    updates["paymentStatus"] = paymentStatus
                }

                firestore.collection("notifications").document(doc.id)
                    .update(updates)
                    .addOnSuccessListener {
                        Log.d("NotificationSync", "✓ Buyer notification updated: ${doc.id} -> $newStatus")
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationSync", "✗ Failed to update buyer notification: ${doc.id}", e)
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("NotificationSync", "✗ Failed to query buyer notifications for orderId: $orderId", e)
        }
}

private fun updateSellerStatus(firestore: FirebaseFirestore, orderId: String, newStatus: String) {
    Log.d("NotificationSync", "Updating seller notifications for orderId: $orderId to status: $newStatus")

    firestore.collection("notifications")
        .whereEqualTo("orderId", orderId)
        .whereEqualTo("type", "product_sold")
        .get()
        .addOnSuccessListener { docs ->
            if (docs.isEmpty) {
                Log.w("NotificationSync", "⚠ No seller notifications found for orderId: $orderId")
            }

            docs.forEach { doc ->
                firestore.collection("notifications").document(doc.id)
                    .update("orderStatus", newStatus)
                    .addOnSuccessListener {
                        Log.d("NotificationSync", "✓ Seller notification updated: ${doc.id} -> $newStatus")
                    }
                    .addOnFailureListener { e ->
                        Log.e("NotificationSync", "✗ Failed to update seller notification: ${doc.id}", e)
                    }
            }
        }
        .addOnFailureListener { e ->
            Log.e("NotificationSync", "✗ Failed to query seller notifications for orderId: $orderId", e)
        }
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

fun createSaleNotification(
    firestore: FirebaseFirestore,
    product: Product,
    buyerId: String,
    paymentMethod: String? = null,
    deliveryAddress: String? = null,
    transactionId: String? = null,
    orderId: String? = null,
    quantity: Int = 1  // ✅ Add quantity parameter
) {
    val finalOrderId = orderId ?: firestore.collection("orders").document().id

    Log.d("NotificationCreate", "Creating order: $finalOrderId")

    val orderData = hashMapOf(
        "orderId" to finalOrderId,
        "buyerId" to buyerId,
        "sellerId" to product.ownerId,
        "paymentMethod" to (paymentMethod ?: "Not specified"),
        "deliveryAddress" to (deliveryAddress ?: ""),
        "transactionId" to (transactionId ?: ""),
        "status" to "To Pay",
        "paymentStatus" to "Payment Pending",
        "timestamp" to Timestamp.now()
    )

    firestore.collection("orders").document(finalOrderId)
        .set(orderData, SetOptions.merge())
        .addOnSuccessListener {
            Log.d("OrderCreate", "✓ Order created: $finalOrderId")

            // ✅ FIX: Create order_items subcollection
            val orderItem = hashMapOf(
                "productId" to product.prodId,
                "name" to product.name,
                "price" to product.price,
                "quantity" to quantity,
                "quantityUnit" to product.quantityUnit,
                "imageUrl" to product.imageUrl,
                "category" to product.category,
                "location" to product.cityName,
                "sellerId" to product.ownerId
            )

            firestore.collection("orders").document(finalOrderId)
                .collection("order_items")
                .add(orderItem)
                .addOnSuccessListener { itemDoc ->
                    Log.d("OrderCreate", "✓ Item added to subcollection: ${itemDoc.id}")
                }
                .addOnFailureListener { e ->
                    Log.e("OrderCreate", "✗ Failed to add item", e)
                }
        }
        .addOnFailureListener { e ->
            Log.e("OrderCreate", "✗ Failed to create order", e)
        }

    // Notifications (same as before)
    val sellerNotification = hashMapOf(
        "type" to "product_sold",
        "productId" to product.prodId,
        "name" to product.name,
        "price" to product.price,
        "quantity" to quantity,
        "quantityUnit" to product.quantityUnit,
        "imageUrl" to product.imageUrl,
        "category" to product.category,
        "location" to product.cityName,
        "timestamp" to Timestamp.now(),
        "userId" to product.ownerId,
        "buyerId" to buyerId,
        "message" to "Your product was sold!",
        "transactionType" to "sale",
        "paymentStatus" to "Payment Pending",
        "paymentMethod" to (paymentMethod ?: "Not specified"),
        "orderStatus" to "To Pay",
        "orderId" to finalOrderId,
        "deliveryAddress" to (deliveryAddress ?: "")
    )
    transactionId?.let { sellerNotification["transactionId"] = it }

    firestore.collection("notifications").add(sellerNotification)

    val buyerNotification = hashMapOf(
        "type" to "purchase_confirmed",
        "productId" to product.prodId,
        "name" to product.name,
        "price" to product.price,
        "quantity" to quantity,
        "quantityUnit" to product.quantityUnit,
        "imageUrl" to product.imageUrl,
        "category" to product.category,
        "location" to product.cityName,
        "timestamp" to Timestamp.now(),
        "userId" to buyerId,
        "sellerId" to product.ownerId,
        "message" to "Purchase successful!",
        "transactionType" to "purchase",
        "paymentStatus" to "Payment Pending",
        "paymentMethod" to (paymentMethod ?: "Not specified"),
        "orderStatus" to "To Pay",
        "orderId" to finalOrderId,
        "deliveryAddress" to (deliveryAddress ?: "")
    )
    transactionId?.let { buyerNotification["transactionId"] = it }

    firestore.collection("notifications").add(buyerNotification)
}

fun createDonationNotification(
    firestore: FirebaseFirestore,
    product: Product,
    donatorId: String,
    organizationName: String,
    transactionId: String? = null
) {
    val donorNotification = hashMapOf(
        "type" to "donation_made",
        "productId" to product.prodId,
        "name" to product.name,
        "price" to product.price,
        "quantity" to product.quantity,
        "quantityUnit" to product.quantityUnit,
        "imageUrl" to product.imageUrl,
        "category" to product.category,
        "location" to product.cityName,
        "timestamp" to Timestamp.now(),
        "userId" to donatorId,
        "sellerId" to product.ownerId,
        "message" to "Thank you for donating ${product.name} to $organizationName!",
        "transactionType" to "donation",
        "organizationName" to organizationName
    )
    transactionId?.let { donorNotification["transactionId"] = it }

    val sellerNotification = hashMapOf(
        "type" to "product_donated",
        "productId" to product.prodId,
        "name" to product.name,
        "price" to product.price,
        "quantity" to product.quantity,
        "quantityUnit" to product.quantityUnit,
        "imageUrl" to product.imageUrl,
        "category" to product.category,
        "location" to product.cityName,
        "timestamp" to Timestamp.now(),
        "userId" to product.ownerId,
        "buyerId" to donatorId,
        "message" to "${product.name} was donated to $organizationName.",
        "transactionType" to "donation",
        "organizationName" to organizationName
    )
    transactionId?.let { sellerNotification["transactionId"] = it }

    firestore.collection("notifications")
        .add(donorNotification)
        .addOnSuccessListener {
            Log.d("Firestore", "Donor notification created with ID: ${it.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error creating donor notification", e)
        }

    firestore.collection("notifications")
        .add(sellerNotification)
        .addOnSuccessListener {
            Log.d("Firestore", "Seller donation notification created with ID: ${it.id}")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error creating seller notification", e)
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
            onSuccess()
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error deleting notification: $notificationId", e)
        }
}

private fun getDefaultMessage(notificationType: String): String {
    return when (notificationType) {
        "product_sold" -> "Your product was sold!"
        "product_donated" -> "You donated a product!"
        "purchase_confirmed" -> "Purchase successful! Your order has been placed."
        else -> "New product added!"
    }
}