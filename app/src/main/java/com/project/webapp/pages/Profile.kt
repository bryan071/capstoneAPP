package com.project.webapp.pages

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.userdata.UserData



@Composable
fun ProfileScreen(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val userData = remember { mutableStateOf<UserData?>(null) }

    // Fetch user data
    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        userData.value = document.toObject(UserData::class.java)
                    }
                }
                .addOnFailureListener {
                    Log.e("ProfileScreen", "Error fetching user data: ${it.message}")
                }
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

        // Profile Image
        Image(
            painter = painterResource(id = R.drawable.profile_icon), // Replace with actual image
            contentDescription = "Profile Image",
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // User Details
        Text(userData.value?.firstname ?: "Guest", fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(userData.value?.email ?: "No email", fontSize = 16.sp, color = Color.Gray)


        Spacer(modifier = Modifier.height(20.dp))

        // Edit Profile Button
        Button(onClick = { /* Navigate to Edit Profile */ }) {
            Text("Edit Profile")
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Settings & Logout
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            ProfileOption("Settings", R.drawable.setting) { /* Navigate to settings */ }
            ProfileOption("Order History", R.drawable.history) { /* Navigate to order history */ }
            ProfileOption("Logout", R.drawable.logout) {
                authViewModel.logout()
                navController.navigate(Route.login) {
                    popUpTo(Route.dashboard) { inclusive = true } // Clears back stack to prevent auto-login
                }
            }
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


