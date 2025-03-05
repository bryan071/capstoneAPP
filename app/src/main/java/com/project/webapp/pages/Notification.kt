import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.Date
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.compose.rememberAsyncImagePainter
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.pages.TopBar

@Composable
fun NotificationScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel, firestore: FirebaseFirestore) {
    var notifications by remember { mutableStateOf<List<Map<String, Any>>>(emptyList()) }
    var selectedNotification by remember { mutableStateOf<Map<String, Any>?>(null) }

    LaunchedEffect(Unit) {
        firestore.collection("notifications")
            .orderBy("timestamp")
            .addSnapshotListener { snapshot, _ ->
                if (snapshot != null) {
                    notifications = snapshot.documents.mapNotNull { it.data }
                    Log.d("Firestore", "Fetched notifications: $notifications")
                }
            }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Notifications", fontSize = 24.sp, fontWeight = FontWeight.Bold)

        LazyColumn(modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
            items(notifications.size) { index ->
                val notification = notifications[index]
                NotificationItem(notification) { selectedNotification = notification }
            }
        }
    }

    selectedNotification?.let { notification ->
        NotificationDetailsDialog(notification, onDismiss = { selectedNotification = null })
    }
}

@Composable
fun NotificationItem(notification: Map<String, Any>, onClick: () -> Unit) {
    val message = notification["message"] as? String ?: "New product added"
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val category = notification["category"] as? String ?: "Unknown"
    val name = notification["name"] as? String ?: "Unnamed"
    val price = notification["price"] as? Double ?: 0.0
    val timestamp = notification["timestamp"] as? Long ?: 0L
    val formattedTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(Color.White)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(16.dp)) {
            // FarmAID Logo
            Image(
                painter = painterResource(id = R.drawable.iconlogo), // Ensure you have this logo in res/drawable
                contentDescription = "FarmAID Logo",
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))

            Column {
                Text(message, fontWeight = FontWeight.Bold)
                Text("Category: $category")
                Text("Name: $name")
                Text("Price: $$price")
                Text("Added on: $formattedTime", fontSize = 12.sp, color = Color.Gray)
            }
        }
    }
}


@Composable
fun NotificationDetailsDialog(notification: Map<String, Any>, onDismiss: () -> Unit) {
    val category = notification["category"] as? String ?: "Unknown"
    val imageUrl = notification["imageUrl"] as? String ?: ""
    val name = notification["name"] as? String ?: "Unnamed"
    val price = notification["price"] as? Double ?: 0.0
    val location = notification["location"] as? String ?: "Unknown"
    val timestamp = notification["timestamp"] as? Long ?: 0L
    val formattedTime = SimpleDateFormat("EEE, dd MMM yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Product Details") },
        text = {
            Column {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = "Product Image",
                    modifier = Modifier.fillMaxWidth().height(200.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("Category: $category", fontWeight = FontWeight.Bold)
                Text("Name: $name", fontWeight = FontWeight.Bold)
                Text("Price: $$price", fontWeight = FontWeight.Bold)
                Text("Location: $location")
                Text("Added on: $formattedTime")
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}


