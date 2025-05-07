package com.project.webapp.components.profiles

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.Route.ORDERS
import com.project.webapp.datas.Post
import com.project.webapp.datas.UserData
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.*

private val primaryColor = Color(0xFF0DA54B)
private val lightGreen = Color(0xFFE8F5E9)


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FarmerProfileScreen(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    userId: String? = null // Allow viewing other profiles by passing a userId
) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    var userData by remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var showDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Profile", "Timeline")
    var isCurrentUserProfile by remember { mutableStateOf(false) }

    // State for posts
    var posts by remember { mutableStateOf<List<Post>>(emptyList()) }
    var isPostsLoading by remember { mutableStateOf(true) }
    var newPostText by remember { mutableStateOf("") }
    var isPostSubmitting by remember { mutableStateOf(false) }
    var showPostSuccess by remember { mutableStateOf(false) }

    // Add a refreshTrigger to force recomposition when needed
    var refreshTrigger by remember { mutableStateOf(0) }

    val currentUserId = auth.currentUser?.uid ?: ""
    val profileUserId = userId ?: currentUserId

    isCurrentUserProfile = currentUserId == profileUserId

    // Function to fetch posts from Firestore - Now with proper error handling and collection listener
    LaunchedEffect(profileUserId, refreshTrigger) {
        profileUserId?.let { uid ->
            isPostsLoading = true

            try {
                // Use snapshotListener instead of get() to listen for real-time updates
                val postListener = firestore.collection("posts")
                    .whereEqualTo("userId", uid)
                    .orderBy("timestamp", Query.Direction.DESCENDING)
                    .addSnapshotListener { snapshot, e ->
                        if (e != null) {
                            Log.e("FarmerProfileScreen", "Listen failed", e)
                            isPostsLoading = false
                            return@addSnapshotListener
                        }

                        if (snapshot != null) {
                            posts = snapshot.documents.mapNotNull { document ->
                                try {
                                    document.toObject(Post::class.java)?.copy(postId = document.id)
                                } catch (e: Exception) {
                                    Log.e("FarmerProfileScreen", "Error parsing post", e)
                                    null
                                }
                            }
                        }

                        isPostsLoading = false
                    }

                // No need to remove the listener as it will be automatically cleaned up
                // when the composable is disposed
            } catch (e: Exception) {
                Log.e("FarmerProfileScreen", "Error setting up post listener", e)
                isPostsLoading = false
            }
        } ?: run {
            isPostsLoading = false
        }
    }

    // Fetch user data only once
    LaunchedEffect(profileUserId) {
        profileUserId?.let { uid ->
            try {
                val document = firestore.collection("users").document(uid).get().await()
                userData = document.toObject(UserData::class.java)
            } catch (e: Exception) {
                Log.e("FarmerProfileScreen", "Error fetching user data", e)
            } finally {
                isLoading = false
            }
        } ?: run { isLoading = false }
    }

    // Function to create a new post
    fun createNewPost() {
        if (newPostText.isNotBlank() && !isPostSubmitting) {
            isPostSubmitting = true

            val newPost = Post(
                userId = auth.currentUser?.uid ?: "",
                userName = userData?.firstname ?: "Anonymous",
                userImage = userData?.profilePicture ?: "",
                content = newPostText,
                imageUrl = null,
                timestamp = System.currentTimeMillis(),
                likes = 0,
                comments = 0
            )

            // Add to Firestore
            firestore.collection("posts").add(newPost)
                .addOnSuccessListener { documentRef ->
                    // Clear the input field
                    newPostText = ""
                    isPostSubmitting = false
                    showPostSuccess = true

                    // Hide success message after some time
                    MainScope().launch {
                        delay(3000)
                        showPostSuccess = false
                    }
                }
                .addOnFailureListener { e ->
                    Log.e("FarmerProfileScreen", "Error creating post", e)
                    isPostSubmitting = false
                }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Header gradient background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor, primaryColor.copy(alpha = 0.7f))
                    )
                )
        )

        // Show centered loading indicator when loading
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center)  {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = primaryColor)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("Loading Profile...", style = MaterialTheme.typography.bodyLarge, color = primaryColor)
                }
            }
        } else {
            // Content when loaded
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(16.dp))

                // Profile Card with improved elevation and design
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .shadow(12.dp, RoundedCornerShape(16.dp)),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        // Profile Image - improved with animation
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            // Outer circle for decoration
                            Box(
                                modifier = Modifier
                                    .size(130.dp)
                                    .background(
                                        brush = Brush.radialGradient(
                                            colors = listOf(
                                                primaryColor.copy(alpha = 0.2f),
                                                primaryColor.copy(alpha = 0.05f)
                                            )
                                        ),
                                        shape = CircleShape
                                    )
                            )

                            // Profile image
                            AsyncImage(
                                model = userData?.profilePicture ?: "",
                                contentDescription = "Profile Image",
                                modifier = Modifier
                                    .size(120.dp)
                                    .clip(CircleShape)
                                    .border(3.dp, primaryColor, CircleShape),
                                error = painterResource(id = R.drawable.profile_icon),
                                placeholder = painterResource(id = R.drawable.profile_icon),
                                contentScale = ContentScale.Crop
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // User Details with improved typography
                        Text(
                            userData?.firstname ?: "Guest",
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        Text(
                            userData?.email ?: "No email",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Edit Profile Button with improved design
                        if (isCurrentUserProfile) {
                            Button(
                                onClick = { navController.navigate(Route.EDIT_PROFILE) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(0.6f),
                                enabled = userData != null,
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.edit),
                                    contentDescription = "Edit",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Edit Profile", fontWeight = FontWeight.Medium)
                            }
                        } else {
                            // Follow Button for other users
                            Button(
                                onClick = { /* Implement follow functionality */ },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = primaryColor,
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier.fillMaxWidth(0.6f),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 4.dp,
                                    pressedElevation = 2.dp
                                )
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.add),
                                    contentDescription = "Follow",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Follow", fontWeight = FontWeight.Medium)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Tab Layout for Profile/Timeline - improved contrast and design
                TabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = Color.White,
                    contentColor = primaryColor,
                    indicator = { tabPositions ->
                        TabRowDefaults.Indicator(
                            modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                            height = 3.dp,
                            color = primaryColor
                        )
                    },
                    divider = {
                        Divider(
                            thickness = 1.dp,
                            color = Color.LightGray.copy(alpha = 0.5f)
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .shadow(elevation = 2.dp, shape = RoundedCornerShape(8.dp))
                ) {
                    tabs.forEachIndexed { index, title ->
                        Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = {
                                Text(
                                    title,
                                    fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal,
                                    fontSize = 16.sp
                                )
                            },
                            selectedContentColor = primaryColor,
                            unselectedContentColor = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Content based on selected tab
                when (selectedTabIndex) {
                    0 -> {
                        // Profile Tab Content
                        ProfileTabContent(
                            userData = userData,
                            isCurrentUserProfile = isCurrentUserProfile,
                            navController = navController,
                            primaryColor = primaryColor,
                            lightGreen = lightGreen,
                            onShowLogoutDialog = { showDialog = true },
                            profileUserId = profileUserId
                        )
                    }
                    1 -> {
                        // Timeline Tab Content
                        TimelineTabContent(
                            userData = userData,
                            isCurrentUserProfile = isCurrentUserProfile,
                            posts = posts,
                            isPostsLoading = isPostsLoading,
                            newPostText = newPostText,
                            onNewPostTextChange = { newPostText = it },
                            onPostSubmit = { createNewPost() },
                            isPostSubmitting = isPostSubmitting,
                            showPostSuccess = showPostSuccess,
                            currentUserId = currentUserId,
                            firestore = firestore,
                            onCommentClick = { postId ->
                                navController.navigate("postDetails/$postId")
                            },
                            onLikeUpdated = {
                                // Let the snapshot listener handle updates
                            },
                            primaryColor = primaryColor,
                            lightGreen = lightGreen
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Confirm Logout", fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to log out?") },
            confirmButton = {
                Button(
                    onClick = {
                        authViewModel.logout()
                        // Small delay to ensure auth state is cleared before navigation
                        MainScope().launch {
                            delay(100)
                            navController.navigate(Route.LOGIN) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Logout", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Gray)
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileTabContent(
    userData: UserData?,
    isCurrentUserProfile: Boolean,
    navController: NavController,
    primaryColor: Color,
    lightGreen: Color,
    onShowLogoutDialog: () -> Unit,
    profileUserId: String?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
    ) {
        if (userData?.userType?.trim()?.lowercase() == "market") {

            Button(
                onClick = { navController.navigate(ORDERS) },
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("View your order here", color = Color.White)

            }
        } else {
            // Farmer Statistics
            Text(
                "Farmer Statistics",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                ProfileStatCard("Products", userData?.productsListed ?: 0, primaryColor, lightGreen)
                ProfileStatCard("Sales", userData?.salesCompleted ?: 0, primaryColor, lightGreen)
                ProfileStatCard("Rating", 4.8, primaryColor, lightGreen)
            }

            Spacer(modifier = Modifier.height(24.dp))
        }

        // Settings & Logout (only for current user)
        if (isCurrentUserProfile) {
            Text(
                "Account",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    ProfileOption("Settings", R.drawable.setting, primaryColor) {}
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    ProfileOption("Recent Activity", R.drawable.history, primaryColor) {}
                    Divider(color = Color.LightGray.copy(alpha = 0.5f))
                    ProfileOption("Logout", R.drawable.logout, Color.Red) { onShowLogoutDialog() }
                }
            }
        } else {
            Text(
                "Top Products",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .padding(start = 8.dp, bottom = 12.dp)
            )

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .shadow(4.dp, RoundedCornerShape(12.dp)),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        "View this farmer's products in the marketplace",
                        fontSize = 16.sp,
                        color = Color.DarkGray
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { navController.navigate("farmerProducts/${profileUserId}") },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.align(Alignment.End),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Text("Browse Products")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimelineTabContent(
    userData: UserData?,
    isCurrentUserProfile: Boolean,
    posts: List<Post>,
    isPostsLoading: Boolean,
    newPostText: String,
    onNewPostTextChange: (String) -> Unit,
    onPostSubmit: () -> Unit,
    isPostSubmitting: Boolean,
    showPostSuccess: Boolean,
    currentUserId: String?,
    firestore: FirebaseFirestore,
    onCommentClick: (String) -> Unit,
    onLikeUpdated: () -> Unit,
    primaryColor: Color,
    lightGreen: Color
) {
    // State for controlling post dialog visibility
    var showPostDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxWidth()) {
        // Post creation trigger UI (only for the profile owner)
        if (isCurrentUserProfile) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .clickable { showPostDialog = true }
                ) {
                    // Profile picture
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

                    // Clickable text field look-alike
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                width = 1.dp,
                                color = Color.LightGray,
                                shape = RoundedCornerShape(24.dp)
                            )
                            .padding(vertical = 12.dp, horizontal = 16.dp)
                    ) {
                        Text(
                            "What's on your mind?",
                            color = Color.Gray,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            // Show success message when post is added
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
                        .padding(bottom = 16.dp)
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
        }

        // Posts feed
        if (isPostsLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = primaryColor)
            }
        } else if (posts.isEmpty()) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = lightGreen.copy(alpha = 0.7f)),
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.post),
                        contentDescription = "No Posts",
                        modifier = Modifier.size(48.dp),
                        tint = primaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        if (isCurrentUserProfile)
                            "Share your first update!"
                        else
                            "${userData?.firstname} hasn't posted any updates yet.",
                        fontWeight = FontWeight.Medium,
                        color = Color.DarkGray
                    )

                    if (isCurrentUserProfile) {
                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            "Your updates help other farmers learn from your experience.",
                            fontSize = 14.sp,
                            color = Color.DarkGray.copy(alpha = 0.7f),
                            modifier = Modifier.padding(horizontal = 24.dp)
                        )
                    }
                }
            }
        } else {
            LazyColumn {
                items(posts) { post ->
                    PostItem(
                        post = post,
                        currentUserId = currentUserId,
                        firestore = firestore,
                        onCommentClick = onCommentClick,
                        onLikeUpdated = onLikeUpdated,
                        primaryColor = primaryColor
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }
    }

    // Post creation dialog
    if (showPostDialog) {
        Dialog(
            onDismissRequest = { showPostDialog = false },
            properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
        ) {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    // Dialog header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Create Post",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )

                        // Close button
                        IconButton(
                            onClick = { showPostDialog = false },
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
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Divider(
                        color = Color.LightGray.copy(alpha = 0.5f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )

                    // User info
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

                    // Post text field with improved design
                    OutlinedTextField(
                        value = newPostText,
                        onValueChange = onNewPostTextChange,
                        placeholder = { Text("Share an update about your farm...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(150.dp),
                        colors = TextFieldDefaults.outlinedTextFieldColors(
                            focusedBorderColor = primaryColor,
                            unfocusedBorderColor = Color.LightGray,
                            cursorColor = primaryColor
                        ),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Add photo button
                    OutlinedButton(
                        onClick = { /* Add photo functionality */ },
                        border = BorderStroke(1.dp, primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
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

                    Spacer(modifier = Modifier.height(16.dp))

                    // Post button
                    Button(
                        onClick = {
                            onPostSubmit()
                            showPostDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth(),
                        enabled = newPostText.isNotBlank() && !isPostSubmitting,
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        if (isPostSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        Text("Post", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun PostItem(
    post: Post,
    currentUserId: String?,
    firestore: FirebaseFirestore,
    onCommentClick: (String) -> Unit,
    onLikeUpdated: () -> Unit,
    primaryColor: Color
) {
    val timeFormat = SimpleDateFormat("MMM dd, yyyy 'at' h:mm a", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(post.timestamp))
    var isLiked by remember { mutableStateOf(false) }
    var likeCount by remember { mutableStateOf(post.likes) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }

    // Check if the post belongs to the current user
    val isOwnPost = currentUserId == post.userId

    // Check if post is liked by current user
    LaunchedEffect(currentUserId, post.postId) {
        if (currentUserId != null && post.postId != null) {
            try {
                val documents = firestore.collection("postLikes")
                    .whereEqualTo("postId", post.postId)
                    .whereEqualTo("userId", currentUserId)
                    .get()
                    .await()

                isLiked = !documents.isEmpty
            } catch (e: Exception) {
                Log.e("PostItem", "Error checking likes", e)
            }
        }
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, shape = RoundedCornerShape(12.dp)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // User info row with more options button for post owner
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AsyncImage(
                        model = post.userImage,
                        contentDescription = "User Image",
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
                            post.userName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        Text(
                            formattedTime,
                            color = Color.Gray,
                            fontSize = 12.sp
                        )
                    }
                }

                // More options menu for the post owner
                if (isOwnPost) {
                    IconButton(
                        onClick = { showDeleteConfirmDialog = true },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.more), // Use a more/kebab menu icon
                            contentDescription = "More Options",
                            tint = Color.Gray,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Post content
            Text(
                post.content,
                fontSize = 16.sp,
                lineHeight = 24.sp
            )

            // Post image if any
            post.imageUrl?.let { imageUrl ->
                Spacer(modifier = Modifier.height(12.dp))

                // Image with loading indicator
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.LightGray.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = primaryColor,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )

                    AsyncImage(
                        model = imageUrl,
                        contentDescription = "Post Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Interaction buttons with improved UI
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Like button with animation
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            if (currentUserId != null && post.postId != null) {
                                val newLikeState = !isLiked

                                // Optimistic UI update
                                isLiked = newLikeState
                                likeCount = if (newLikeState) likeCount + 1 else likeCount - 1

                                if (newLikeState) {
                                    // Add like to database
                                    firestore.collection("postLikes").add(
                                        hashMapOf(
                                            "postId" to post.postId,
                                            "userId" to currentUserId,
                                            "timestamp" to System.currentTimeMillis()
                                        )
                                    ).addOnSuccessListener {
                                        // Update post like count
                                        firestore.collection("posts").document(post.postId)
                                            .update("likes", likeCount)
                                            .addOnSuccessListener {
                                                onLikeUpdated()
                                            }
                                            .addOnFailureListener { e ->
                                                Log.e("PostItem", "Error updating like count", e)
                                                // Revert optimistic update on failure
                                                isLiked = !newLikeState
                                                likeCount = if (!newLikeState) likeCount + 1 else likeCount - 1
                                            }
                                    }
                                } else {
                                    // Remove like from database
                                    firestore.collection("postLikes")
                                        .whereEqualTo("postId", post.postId)
                                        .whereEqualTo("userId", currentUserId)
                                        .get()
                                        .addOnSuccessListener { documents ->
                                            for (document in documents) {
                                                firestore.collection("postLikes").document(document.id).delete()
                                            }
                                            // Update post like count
                                            firestore.collection("posts").document(post.postId)
                                                .update("likes", likeCount)
                                                .addOnSuccessListener {
                                                    onLikeUpdated()
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("PostItem", "Error updating like count", e)
                                                    // Revert optimistic update on failure
                                                    isLiked = !newLikeState
                                                    likeCount = if (!newLikeState) likeCount + 1 else likeCount - 1
                                                }
                                        }
                                }
                            }
                        }
                        .background(
                            color = if (isLiked) Color.Red.copy(alpha = 0.1f) else Color.Transparent,
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = if (isLiked) R.drawable.like else R.drawable.likedef),
                        contentDescription = "Like",
                        tint = if (isLiked) Color.Unspecified else Color.Unspecified,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        "$likeCount Likes",
                        color = if (isLiked) Color.Red else Color.Gray,
                        fontSize = 14.sp
                    )
                }

                // Comment button with improved UI
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .clickable {
                            post.postId?.let { onCommentClick(it) }
                        }
                        .background(
                            color = Color.Gray.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        )
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.comment),
                        contentDescription = "Comment",
                        tint = Color.Gray,
                        modifier = Modifier.size(20.dp)
                    )

                    Spacer(modifier = Modifier.width(4.dp))

                    Text(
                        "${post.comments} Comments",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
        }
    }

    // Delete confirmation dialog
    if (showDeleteConfirmDialog) {
        val context = LocalContext.current

        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            shape = RoundedCornerShape(16.dp),
            title = { Text("Delete Post", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Are you sure you want to delete this post?")
                    Text("This action cannot be undone.",
                        fontSize = 14.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 8.dp)
                    )

                    if (isDeleting) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = primaryColor,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!isDeleting && post.postId != null) {
                            isDeleting = true

                            // First delete all likes related to this post
                            firestore.collection("postLikes")
                                .whereEqualTo("postId", post.postId)
                                .get()
                                .addOnSuccessListener { likeDocuments ->
                                    // Create a batch to delete all likes efficiently
                                    val batch = firestore.batch()
                                    for (document in likeDocuments) {
                                        batch.delete(document.reference)
                                    }

                                    // Check for comments
                                    firestore.collection("postComments")
                                        .whereEqualTo("postId", post.postId)
                                        .get()
                                        .addOnSuccessListener { commentDocuments ->
                                            // Add comment deletions to the batch
                                            for (document in commentDocuments) {
                                                batch.delete(document.reference)
                                            }

                                            // Add post deletion to the batch
                                            batch.delete(firestore.collection("posts").document(post.postId))

                                            // Execute the batch
                                            batch.commit()
                                                .addOnSuccessListener {
                                                    Log.d("PostItem", "Post and related data deleted successfully")
                                                    isDeleting = false
                                                    showDeleteConfirmDialog = false

                                                    // Show a toast to confirm deletion
                                                    Toast.makeText(
                                                        context,
                                                        "Post deleted successfully",
                                                        Toast.LENGTH_SHORT
                                                    ).show()

                                                    // No need to manually update UI since the snapshot listener will handle it
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.e("PostItem", "Error in batch delete", e)
                                                    isDeleting = false

                                                    // Show error message
                                                    Toast.makeText(
                                                        context,
                                                        "Failed to delete post: ${e.message}",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                        }
                                        .addOnFailureListener { e ->
                                            Log.e("PostItem", "Error querying comments", e)
                                            isDeleting = false

                                            Toast.makeText(
                                                context,
                                                "Failed to delete post: ${e.message}",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                }
                                .addOnFailureListener { e ->
                                    Log.e("PostItem", "Error querying likes", e)
                                    isDeleting = false

                                    Toast.makeText(
                                        context,
                                        "Failed to delete post: ${e.message}",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                    shape = RoundedCornerShape(8.dp),
                    enabled = !isDeleting
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { showDeleteConfirmDialog = false },
                    shape = RoundedCornerShape(8.dp),
                    border = BorderStroke(1.dp, Color.Gray),
                    enabled = !isDeleting
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun ProfileStatCard(label: String, value: Number, primaryColor: Color, backgroundColor: Color) {
    Card(
        modifier = Modifier
            .width(100.dp)
            .height(100.dp)
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                value.toString(),
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = primaryColor
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                label,
                fontSize = 14.sp,
                color = Color.DarkGray
            )
        }
    }
}

@Composable
fun ProfileOption(title: String, iconResId: Int, iconTint: Color, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 16.dp, horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            painter = painterResource(id = iconResId),
            contentDescription = title,
            tint = iconTint,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            title,
            fontSize = 16.sp,
            color = Color.DarkGray
        )
        Spacer(modifier = Modifier.weight(1f))
        Icon(
            painter = painterResource(id = R.drawable.arrow),
            contentDescription = "Go",
            tint = Color.Gray,
            modifier = Modifier.size(16.dp)
        )
    }
}

// Import needed for AnimatedVisibility
@Composable
fun AnimatedVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    enter: EnterTransition = EnterTransition.None,
    exit: ExitTransition = ExitTransition.None,
    content: @Composable () -> Unit
) {
    val isVisibleState = remember { mutableStateOf(visible) }

    LaunchedEffect(visible) {
        isVisibleState.value = visible
    }

    if (isVisibleState.value) {
        content()
    }
}
