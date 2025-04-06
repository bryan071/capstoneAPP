package com.farmaid.ui.notifications

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.R
import com.project.webapp.components.fetchOwnerName
import com.project.webapp.datas.Product


@Composable
fun FarmerNotificationScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    firestore: FirebaseFirestore, cartViewModel: CartViewModel
) {
    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedNotification by remember { mutableStateOf<Map<String, Any>?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        firestore.collection("notifications")
            .orderBy("timestamp")
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
    Scaffold() { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // 🔹 Add "Notification" title
            Text(
                text = "Notification",
                fontSize = 30.sp, // Adjust size as needed
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp) // Space between title and content
            )

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFF0DA54B))
                }
            } else if (notifications.isEmpty()) {
                EmptyNotificationScreen()
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
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
    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    var ownerName by remember { mutableStateOf("Loading...") }

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
            .padding(8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(16.dp)
        ) {
            if (imageUrl.isNotEmpty()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Product Image",
                    modifier = Modifier.size(50.dp)
                )
            } else {
                Icon(
                    painter = painterResource(id = R.drawable.iconlogo),
                    contentDescription = "Default Icon",
                    modifier = Modifier.size(50.dp),
                    tint = Color.Unspecified
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(message, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Text("Category: $category", fontSize = 14.sp, color = Color.Gray)
                Text("Name: $name", fontSize = 14.sp)
                Text("Price: ₱${String.format("%.2f", price)}", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text("Quantity: $quantity $quantityUnit", fontSize = 14.sp, color = Color(0xFF0DA54B))
                Text("Added on: $formattedTime", fontSize = 12.sp, color = Color.Gray)
                Text("Posted by: $ownerName", fontSize = 12.sp, color = Color.DarkGray)
            }

            if (notificationId != null) {
                IconButton(onClick = { deleteNotification(firestore, notificationId) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
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

    val category = notification["category"] as? String ?: "Unknown"
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val name = notification["name"] as? String ?: "Unnamed"
    val price = (notification["price"] as? Number)?.toDouble() ?: 0.0
    val quantity = (notification["quantity"] as? Number)?.toInt()
    val quantityUnit = notification["quantityUnit"] as? String
    val location = notification["location"] as? String ?: "Location not available"
    val userId = notification["userId"] as? String ?: "Unknown"
    val timestamp = notification["timestamp"] as? Long ?: 0L

    val formattedTime = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    var ownerName by remember { mutableStateOf("Loading...") }


    LaunchedEffect(userId) {
        fetchOwnerName(firestore, userId) { name ->
            ownerName = name
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Details") },
        text = {
            Column {
                if (imageUrl.isNotEmpty()) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Product Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(id = R.drawable.iconlogo),
                        contentDescription = "Default Icon",
                        modifier = Modifier.size(100.dp),
                        tint = Color.Unspecified
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Category: $category", fontWeight = FontWeight.Bold)
                Text("Name: $name", fontWeight = FontWeight.Bold)
                Text("Price: ₱${String.format("%.2f", price)}", fontWeight = FontWeight.Bold)

                if (quantity != null && quantityUnit != null) {
                    Text(
                        "Quantity: $quantity $quantityUnit",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0DA54B)
                    )
                }


                Text("Location: $location")
                Text("Posted by: $ownerName", fontWeight = FontWeight.Medium)
                Text("Added on: $formattedTime", fontSize = 12.sp, color = Color.Gray)
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}




@Composable
fun EmptyNotificationScreen() {
    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Icon(painterResource(id = R.drawable.iconlogo), contentDescription = "No Notifications", tint = Color.Unspecified)
        Text("No notifications yet", fontSize = 18.sp, fontWeight = FontWeight.Medium)
        Text("You'll see updates about new products here.", fontSize = 14.sp, color = Color.Gray)
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
