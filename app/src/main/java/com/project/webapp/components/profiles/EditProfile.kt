import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.datas.UserData
import java.util.UUID
import com.project.webapp.R

@Composable
fun FarmerEditProfileScreen(navController: NavController) {
    val firestore = FirebaseFirestore.getInstance()
    val auth = FirebaseAuth.getInstance()
    val storage = FirebaseStorage.getInstance()
    val userData = remember { mutableStateOf<UserData?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasChanges by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            hasChanges = true
        }
    }

    // Primary and secondary colors
    val primaryColor = Color(0xFF0DA54B)
    val secondaryColor = Color(0xFFE8F5E9)
    val errorColor = Color(0xFFB00020)

    // Load user data
    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        document.toObject(UserData::class.java)?.let {
                            userData.value = it
                            firstName = it.firstname ?: ""
                            lastName = it.lastname ?: ""
                            email = it.email ?: ""
                            address = it.address ?: ""
                            contactNumber = it.phoneNumber ?: ""
                            profilePicture = it.profilePicture ?: ""
                            userType = it.userType ?: ""
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } ?: run { isLoading = false }
    }

    // Effect to track changes
    LaunchedEffect(firstName, lastName, address, contactNumber, selectedImageUri) {
        val originalData = userData.value
        hasChanges = selectedImageUri != null ||
                firstName != (originalData?.firstname ?: "") ||
                lastName != (originalData?.lastname ?: "") ||
                address != (originalData?.address ?: "") ||
                contactNumber != (originalData?.phoneNumber ?: "")
    }

    // Show loading screen
    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = primaryColor)
                Spacer(modifier = Modifier.height(16.dp))
                Text("Loading Profile...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        // Main content
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top app bar with back button and title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }

                    Text(
                        "Edit Profile",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Profile picture section
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "Profile Photo",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = rememberAsyncImagePainter(
                                    selectedImageUri ?:
                                    profilePicture.takeIf { it.isNotEmpty() } ?:
                                    R.drawable.profile_icon
                                ),
                                contentDescription = "Profile Image",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(120.dp)
                                    .shadow(4.dp, CircleShape)
                                    .clip(CircleShape)
                                    .border(2.dp, primaryColor, CircleShape)
                                    .background(secondaryColor)
                            )

                            IconButton(
                                onClick = { imagePickerLauncher.launch("image/*") },
                                modifier = Modifier
                                    .size(36.dp)
                                    .shadow(2.dp, CircleShape)
                                    .clip(CircleShape)
                                    .background(primaryColor)
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.addphoto),
                                    contentDescription = "Change Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Personal Information Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Personal Information",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ProfileTextField(
                            label = "First Name",
                            value = firstName,
                            onValueChange = { firstName = it },
                            primaryColor = primaryColor
                        )

                        ProfileTextField(
                            label = "Last Name",
                            value = lastName,
                            onValueChange = { lastName = it },
                            primaryColor = primaryColor
                        )

                        ProfileTextField(
                            label = "Email",
                            value = email,
                            readOnly = true,
                            onValueChange = {},
                            primaryColor = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Contact Information Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            "Contact Information",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(bottom = 16.dp)
                        )

                        ProfileTextField(
                            label = "Address",
                            value = address,
                            onValueChange = { address = it },
                            primaryColor = primaryColor
                        )

                        ProfileTextField(
                            label = "Contact Number",
                            value = contactNumber,
                            onValueChange = { contactNumber = it },
                            primaryColor = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // User Type Information
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    elevation = CardDefaults.cardElevation(4.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.profile_icon),
                            contentDescription = "User Type",
                            tint = primaryColor,
                            modifier = Modifier.size(24.dp)
                        )

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                "User Type",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.Gray
                            )

                            Text(
                                userType,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Button(
                    onClick = {
                        isSubmitting = true
                        val userId = auth.currentUser?.uid ?: return@Button

                        if (selectedImageUri != null) {
                            uploadImageToFirebaseStorage(userId, selectedImageUri!!) { downloadUrl ->
                                profilePicture = downloadUrl
                                updateProfile(userId, firstName, lastName, address, contactNumber, profilePicture, userType) {
                                    isSubmitting = false
                                    showDialog = true
                                    hasChanges = false
                                }
                            }
                        } else {
                            updateProfile(userId, firstName, lastName, address, contactNumber, profilePicture, userType) {
                                isSubmitting = false
                                showDialog = true
                                hasChanges = false
                            }
                        }
                    },
                    enabled = !isSubmitting && hasChanges,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = primaryColor,
                        disabledContainerColor = primaryColor.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .then(
                            if (!isSubmitting && hasChanges) {
                                Modifier.shadow(4.dp, RoundedCornerShape(8.dp))
                            } else {
                                Modifier
                            }
                        )
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp),
                            strokeWidth = 2.dp
                        )

                        Spacer(modifier = Modifier.width(12.dp))
                    }

                    Text(
                        "Save Changes",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = { navController.popBackStack() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
                    border = BorderStroke(1.dp, primaryColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        "Cancel",
                        style = MaterialTheme.typography.labelLarge
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Save confirmation dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = {
                        Text(
                            "Success",
                            style = MaterialTheme.typography.headlineSmall.copy(
                                fontWeight = FontWeight.Bold
                            )
                        )
                    },
                    text = {
                        Text("Your profile has been updated successfully!")
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                showDialog = false
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
                        ) {
                            Text("OK")
                        }
                    },
                    containerColor = Color.White,
                    shape = RoundedCornerShape(16.dp)
                )
            }
        }
    }
}

// Function to upload image to Firebase Storage
fun uploadImageToFirebaseStorage(userId: String, imageUri: Uri, onSuccess: (String) -> Unit) {
    val storageRef = FirebaseStorage.getInstance().reference.child("profile_pictures/$userId/${UUID.randomUUID()}.jpg")

    storageRef.putFile(imageUri)
        .addOnSuccessListener { taskSnapshot ->
            taskSnapshot.storage.downloadUrl.addOnSuccessListener { uri ->
                onSuccess(uri.toString())
            }
        }
}

// Function to update Firestore with new user details
fun updateProfile(
    userId: String,
    firstName: String,
    lastName: String,
    address: String,
    contactNumber: String,
    profilePicture: String,
    userType: String,
    onSuccess: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("users").document(userId)
        .update(
            mapOf(
                "firstname" to firstName,
                "lastname" to lastName,
                "address" to address,
                "phoneNumber" to contactNumber,
                "profilePicture" to profilePicture
                // userType is not updated here; it remains unchanged
            )
        )
        .addOnSuccessListener { onSuccess() }
        .addOnFailureListener { exception ->
            // Handle failure if needed
        }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit,
    primaryColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = { if (!readOnly) onValueChange(it) },
        label = { Text(label) },
        singleLine = true,
        readOnly = readOnly,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            disabledContainerColor = Color.White.copy(alpha = 0.9f),
            focusedIndicatorColor = primaryColor,
            unfocusedIndicatorColor = Color.Gray,
            focusedLabelColor = primaryColor,
            cursorColor = primaryColor
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}