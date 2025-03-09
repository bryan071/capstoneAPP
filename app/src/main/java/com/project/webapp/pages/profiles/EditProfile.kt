import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.userdata.UserData
import java.util.UUID
import com.project.webapp.R

@Composable
fun EditProfileScreen(navController: NavController) {
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
    var showDialog by remember { mutableStateOf(false) }

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
                        }
                    }
                }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Edit Profile", fontSize = MaterialTheme.typography.headlineMedium.fontSize)

        Spacer(modifier = Modifier.height(20.dp))

        // Profile Image (Uses Default if None Exists)
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
                .size(100.dp)
                .clip(MaterialTheme.shapes.medium)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // "Change Profile Picture" Button
        Button(onClick = { imagePickerLauncher.launch("image/*") }) {
            Text("Change Profile Picture")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Editable Fields
        TextField(value = firstName, onValueChange = { firstName = it }, label = { Text("First Name") })
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = lastName, onValueChange = { lastName = it }, label = { Text("Last Name") })
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = email, onValueChange = { email = it }, label = { Text("Email") }, readOnly = true)
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = address, onValueChange = { address = it }, label = { Text("Address") })
        Spacer(modifier = Modifier.height(12.dp))
        TextField(value = contactNumber, onValueChange = { contactNumber = it }, label = { Text("Contact Number") })

        Spacer(modifier = Modifier.height(20.dp))

        // Save Changes Button
        Button(onClick = {
            val userId = auth.currentUser?.uid ?: return@Button

            if (selectedImageUri != null) {
                uploadImageToFirebaseStorage(userId, selectedImageUri!!) { downloadUrl ->
                    profilePicture = downloadUrl
                    updateProfile(userId, firstName, lastName, address, contactNumber, profilePicture) {
                        showDialog = true
                    }
                }
            } else {
                updateProfile(userId, firstName, lastName, address, contactNumber, profilePicture) {
                    showDialog = true
                }
            }
        }) {
            Text("Save Changes")
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Back Button
        Button(onClick = { navController.popBackStack() }) {
            Text("Back")
        }
    }

    // Success Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Success") },
            text = { Text("Profile updated successfully!") },
            confirmButton = {
                Button(onClick = {
                    showDialog = false
                    navController.popBackStack()
                }) {
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
        )
        .addOnSuccessListener { onSuccess() }
}
