package com.farmaid.ui.notifications

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.R
import com.project.webapp.components.fetchOwnerName
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
    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedNotification by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    val primaryColor = Color(0xFF0DA54B)

    LaunchedEffect(Unit) {
        firestore.collection("notifications")
            .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, _ ->
                isLoading = false
                if (snapshot != null) {
                    notifications = snapshot.documents.mapNotNull { doc ->
                        doc.data?.plus("id" to doc.id)
                    }
                    Log.d("Firestore", "Fetched notifications: $notifications")
                }
            }
    }

    val context = LocalContext.current
    var userType by remember { mutableStateOf<String?>(null) }

    // Simulate fetching userType from Firebase or ViewModel
    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType") // Ensure field exists in Firestore
                }
        }
    }

    Scaffold(
        topBar = {
            SmallTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = primaryColor)
                }
            } else if (notifications.isEmpty()) {
                EmptyNotificationScreen()
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(notifications.size) { index ->
                        val notification = notifications[index]
                        NotificationItem(notification, firestore) { selectedNotification = notification }
                    }
                }
            }
        }
    }

    selectedNotification?.let { notification ->
        NotificationDetailsDialog(notification, onDismiss = { selectedNotification = null })
    }
}

@Composable
fun NotificationItem(notification: Map<String, Any>, firestore: FirebaseFirestore, onClick: () -> Unit) {
    val message = notification["message"] as? String ?: "New product added"
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val category = notification["category"] as? String ?: "Unknown"
    val name = notification["name"] as? String ?: "Unnamed"
    val price = (notification["price"] as? Number)?.toDouble() ?: 0.0
    val quantity = (notification["quantity"] as? Number)?.toInt() ?: 0
    val quantityUnit = notification["quantityUnit"] as? String ?: "Unknown"
    val timestamp = notification["timestamp"] as? Long ?: 0L
    val userId = notification["userId"] as? String
    val notificationId = notification["id"] as? String
    val formattedDate = SimpleDateFormat("MMM dd", Locale.getDefault()).format(Date(timestamp))
    val formattedTime = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

    var ownerName by remember { mutableStateOf("Loading...") }
    val primaryColor = Color(0xFF0DA54B)

    LaunchedEffect(userId) {
        if (!userId.isNullOrEmpty()) {
            fetchOwnerName(firestore, userId) { name ->
                ownerName = name
            }
        } else {
            ownerName = "Unknown"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Product Image",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(70.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFEDF7F0)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.iconlogo),
                        contentDescription = "Default Icon",
                        modifier = Modifier.size(40.dp),
                        tint = Color.Unspecified
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
                    fontSize = 16.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = message,
                    fontSize = 14.sp,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "₱${String.format("%.2f", price)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    Text(
                        text = " • ",
                        fontSize = 15.sp,
                        color = Color.Gray
                    )

                    Text(
                        text = "$quantity $quantityUnit",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }

                Row(
                    modifier = Modifier.padding(top = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "By: $ownerName",
                        fontSize = 13.sp,
                        color = Color.DarkGray,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        text = "$formattedDate at $formattedTime",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }

            if (notificationId != null) {
                IconButton(
                    onClick = { deleteNotification(firestore, notificationId) }
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Color.Red.copy(alpha = 0.7f),
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun NotificationDetailsDialog(
    notification: Map<String, Any>,
    onDismiss: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    val primaryColor = Color(0xFF0DA54B)

    val category = notification["category"] as? String ?: "Unknown"
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val name = notification["name"] as? String ?: "Unnamed"
    val price = (notification["price"] as? Number)?.toDouble() ?: 0.0
    val quantity = (notification["quantity"] as? Number)?.toInt()
    val quantityUnit = notification["quantityUnit"] as? String
    val location = notification["location"] as? String ?: "Location not available"
    val userId = notification["userId"] as? String ?: "Unknown"
    val timestamp = notification["timestamp"] as? Long ?: 0L
    val message = notification["message"] as? String ?: "Product details"

    val formattedTime = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    var ownerName by remember { mutableStateOf("Loading...") }

    LaunchedEffect(userId) {
        fetchOwnerName(firestore, userId) { name ->
            ownerName = name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = Color.White,
        title = {
            Text(
                "Product Details",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = primaryColor
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Product Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFEDF7F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.iconlogo),
                            contentDescription = "Default Icon",
                            modifier = Modifier.size(80.dp),
                            tint = Color.Unspecified
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Product name in large font
                Text(
                    text = name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Price with currency symbol
                Text(
                    text = "₱${String.format("%.2f", price)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = primaryColor
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Details section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFF8F8F8), RoundedCornerShape(12.dp))
                        .padding(16.dp)
                ) {
                    DetailRow("Category", category)

                    if (quantity != null && quantityUnit != null) {
                        DetailRow("Quantity", "$quantity $quantityUnit")
                    }

                    DetailRow("Location", location)
                    DetailRow("Posted by", ownerName)
                    DetailRow("Posted on", formattedTime)

                    if (message.isNotEmpty() && message != "New product added") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Message:",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp
                        )
                        Text(
                            text = message,
                            fontSize = 15.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Close", fontSize = 16.sp)
            }
        }
    )
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "$label:",
            fontWeight = FontWeight.Bold,
            fontSize = 15.sp,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            fontSize = 15.sp
        )
    }
}

@Composable
fun EmptyNotificationScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "No Notifications",
            tint = Color(0xFF0DA54B),
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "No notifications yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            "You'll see updates about new products here.",
            fontSize = 16.sp,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 32.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

fun deleteNotification(firestore: FirebaseFirestore, notificationId: String) {
    firestore.collection("notifications").document(notificationId)
        .delete()
        .addOnSuccessListener {
            Log.d("Firestore", "Notification deleted successfully")
        }
        .addOnFailureListener { e ->
            Log.e("Firestore", "Error deleting notification", e)
        }
}