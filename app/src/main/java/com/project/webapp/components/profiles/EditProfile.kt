import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
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

    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }
    var profilePicture by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }  // userType added here
    var showDialog by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }

    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri = it }
    }

    // Fetch user data
    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        val user = document.toObject(UserData::class.java)
                        userData.value = user

                        user?.let {
                            firstName = it.firstname ?: ""
                            lastName = it.lastname ?: ""
                            email = it.email ?: ""
                            address = it.address ?: ""
                            contactNumber = it.phoneNumber ?: ""
                            profilePicture = it.profilePicture ?: ""
                            userType = it.userType ?: ""  // Populate userType here
                        }
                    }
                }
        }
    }

    val primaryColor = Color(0xFF0DA54B)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Back Button
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.align(Alignment.Start)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.backbtn),
                contentDescription = "Back",
                tint = Color.Unspecified
            )
        }

        Text(
            text = "Edit Profile",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(vertical = 12.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Image
        Box(contentAlignment = Alignment.BottomEnd) {
            Image(
                painter = if (selectedImageUri != null) {
                    rememberAsyncImagePainter(selectedImageUri)
                } else if (profilePicture.isNotEmpty()) {
                    rememberAsyncImagePainter(profilePicture)
                } else {
                    painterResource(id = R.drawable.profile_icon)
                },
                contentDescription = "Profile Image",
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .border(2.dp, primaryColor, CircleShape)
                    .background(Color.LightGray)
            )
            IconButton(
                onClick = { imagePickerLauncher.launch("image/*") },
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(primaryColor)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.addphoto),
                    contentDescription = "Change Avatar",
                    tint = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editable Fields
        ProfileTextField("First Name", firstName) { firstName = it }
        ProfileTextField("Last Name", lastName) { lastName = it }
        ProfileTextField("Email", email, readOnly = true) { }
        ProfileTextField("Address", address) { address = it }
        ProfileTextField("Contact Number", contactNumber) { contactNumber = it }

        Spacer(modifier = Modifier.height(16.dp))

        // Display userType (not editable)
        Text(
            text = "User Type: $userType",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Save Changes Button
        ElevatedButton(
            onClick = {
                isSubmitting = true
                val userId = auth.currentUser?.uid ?: return@ElevatedButton

                if (selectedImageUri != null) {
                    uploadImageToFirebaseStorage(userId, selectedImageUri!!) { downloadUrl ->
                        profilePicture = downloadUrl
                        updateProfile(
                            userId,
                            firstName,
                            lastName,
                            address,
                            contactNumber,
                            profilePicture,
                            userType  // userType remains unchanged
                        ) {
                            isSubmitting = false
                            showDialog = true
                        }
                    }
                } else {
                    updateProfile(
                        userId,
                        firstName,
                        lastName,
                        address,
                        contactNumber,
                        profilePicture,
                        userType  // userType remains unchanged
                    ) {
                        isSubmitting = false
                        showDialog = true
                    }
                }
            },
            enabled = !isSubmitting,
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                Spacer(modifier = Modifier.width(8.dp))
            }
            Text("Save Changes", style = MaterialTheme.typography.labelLarge)
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Cancel Button
        OutlinedButton(
            onClick = { navController.popBackStack() },
            colors = ButtonDefaults.outlinedButtonColors(contentColor = primaryColor),
            border = BorderStroke(1.dp, primaryColor),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
        ) {
            Text("Cancel", style = MaterialTheme.typography.labelLarge)
        }
    }

    // Success Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Success") },
            text = { Text("Profile updated successfully!") },
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
            }
        )
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
    userType: String,  // userType is passed but not updated
    onSuccess: () -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()
    firestore.collection("users").document(userId)
        .update(
            "firstname", firstName,
            "lastname", lastName,
            "address", address,
            "phoneNumber", contactNumber,
            "profilePicture", profilePicture
            // userType is not updated here; it remains unchanged
        )
        .addOnSuccessListener { onSuccess() }
}

@Composable
fun ProfileTextField(
    label: String,
    value: String,
    readOnly: Boolean = false,
    onValueChange: (String) -> Unit
) {
    val primaryColor = Color(0xFF0DA54B)

    OutlinedTextField(
        value = value,
        onValueChange = { if (!readOnly) onValueChange(it) },
        label = { Text(label, color = Color.Gray) },
        singleLine = true,
        readOnly = readOnly,
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            disabledContainerColor = Color.Transparent,
            focusedIndicatorColor = primaryColor,
            unfocusedIndicatorColor = Color.Gray,
            disabledIndicatorColor = Color.LightGray,
            cursorColor = primaryColor
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clip(RoundedCornerShape(10.dp))
    )
}


