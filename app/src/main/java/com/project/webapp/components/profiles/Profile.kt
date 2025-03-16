package com.project.webapp.components.profiles

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.dashboards.TopBar
import com.project.webapp.datas.UserData

@Composable
fun FarmerProfileScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userData = remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }


    // Fetch user data
    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData.value = document.toObject(UserData::class.java)
                    }
                    isLoading = false
                }
                .addOnFailureListener {
                    Log.e("ProfileScreen", "Error fetching user data: ${it.message}")
                    isLoading = false
                }
        } ?: run {
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar()

        Spacer(modifier = Modifier.height(20.dp))

        if (isLoading) {
            CircularProgressIndicator()
        } else {
            // Profile Image
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = userData.value?.profilePicture ?: "",
                    contentDescription = "Profile Image",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape),
                    error = painterResource(id = R.drawable.profile_icon),
                    placeholder = painterResource(id = R.drawable.profile_icon)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // User Details
            Text(
                userData.value?.firstname ?: "Guest",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(userData.value?.email ?: "No email", fontSize = 16.sp, color = Color.Gray)

            Spacer(modifier = Modifier.height(20.dp))

            // Edit Profile Button
            Button(
                onClick = {
                    userData.value?.let { user ->
                        navController.navigate(Route.EDIT_PROFILE)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B)),
                enabled = userData.value != null // Ensure button is only clickable when user data is available
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.edit),
                    contentDescription = "Edit",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile")
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Stats Cards
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                ProfileStatCard("Products", userData.value?.productsListed ?: 0)
                ProfileStatCard("Sales", userData.value?.salesCompleted ?: 0)
                ProfileStatCard("Rating", 4.8)
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Settings & Logout
            Column(modifier = Modifier.fillMaxWidth()) {
                ProfileOption("Settings", R.drawable.setting) { /* Navigate to settings */ }
                ProfileOption("Recent Activity", R.drawable.history) { /* Navigate to Recent Activity */ }
                ProfileOption("Logout", R.drawable.logout) {
                    showDialog = true
                }
            }

            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Confirm Logout") },
                    text = { Text("Are you sure you want to log out?") },
                    confirmButton = {
                        Button(
                            onClick = {
                                authViewModel.logout()
                                navController.navigate(Route.LOGIN) {
                                    popUpTo(Route.FARMER_DASHBOARD) { inclusive = true }
                                }
                                showDialog = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                        ) {
                            Text("Logout")
                        }
                    },
                    dismissButton = {
                        Button(
                            onClick = { showDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun ProfileStatCard(title: String, value: Any) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "$value", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = Color.Green)
            Text(text = title, fontSize = 14.sp, color = Color.Gray)
        }
    }
}

@Composable
fun ProfileOption(title: String, iconRes: Int, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconRes),
            contentDescription = title,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(title, fontSize = 18.sp)
    }
}
