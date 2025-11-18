import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.project.webapp.datas.UserData
import com.project.webapp.R
import java.util.UUID

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
    var contactNumber by remember { mutableStateOf("+63") }
    var profilePicture by remember { mutableStateOf("") }
    var userType by remember { mutableStateOf("") }
    var selectedImageUri by remember { mutableStateOf<Uri?>(null) }
    var hasChanges by remember { mutableStateOf(false) }
    var showDialog by remember { mutableStateOf(false) }

    // === NEW: Address States ===
    var selectedProvince by remember { mutableStateOf("") }
    var selectedMunicipality by remember { mutableStateOf("") }
    var selectedBarangay by remember { mutableStateOf("") }
    var streetDetails by remember { mutableStateOf("") }

    // Final address
    val fullAddress = listOf(streetDetails, selectedBarangay, selectedMunicipality, selectedProvince)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    val primaryColor = Color(0xFF0DA54B)
    val secondaryColor = Color(0xFFE8F5E9)

    val imagePickerLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let {
            selectedImageUri = it
            hasChanges = true
        }
    }

    // Load user data + parse address
    LaunchedEffect(auth.currentUser) {
        auth.currentUser?.uid?.let { userId ->
            firestore.collection("users").document(userId).get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        document.toObject(UserData::class.java)?.let { data ->
                            userData.value = data
                            firstName = data.firstname ?: ""
                            lastName = data.lastname ?: ""
                            email = data.email ?: ""
                            contactNumber = data.phoneNumber ?: ""
                            profilePicture = data.profilePicture ?: ""
                            userType = data.userType ?: ""

                            // Parse saved address into dropdowns
                            data.address?.let { savedAddress ->
                                parseAddressToDropdowns(savedAddress) { prov, mun, bar, street ->
                                    selectedProvince = prov
                                    selectedMunicipality = mun
                                    selectedBarangay = bar
                                    streetDetails = street
                                }
                            }
                        }
                    }
                    isLoading = false
                }
                .addOnFailureListener { isLoading = false }
        } ?: run { isLoading = false }
    }

    // Track changes
    LaunchedEffect(firstName, lastName, contactNumber, selectedImageUri, fullAddress) {
        val original = userData.value
        hasChanges = selectedImageUri != null ||
                firstName != (original?.firstname ?: "") ||
                lastName != (original?.lastname ?: "") ||
                contactNumber != (original?.phoneNumber ?: "") ||
                fullAddress != (original?.address ?: "")
    }

    if (isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                CircularProgressIndicator(color = primaryColor)
                Spacer(Modifier.height(16.dp))
                Text("Loading Profile...", style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                    Text("Edit Profile", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), modifier = Modifier.padding(start = 8.dp))
                }

                Spacer(Modifier.height(16.dp))

                // Profile Picture
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(16.dp)) {
                        Text("Profile Photo", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                        Box(contentAlignment = Alignment.BottomEnd) {
                            Image(
                                painter = rememberAsyncImagePainter(selectedImageUri ?: profilePicture.takeIf { it.isNotEmpty() } ?: R.drawable.profile_icon),
                                contentDescription = "Profile",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(120.dp).clip(CircleShape).border(2.dp, primaryColor, CircleShape).background(secondaryColor)
                            )
                            IconButton(onClick = { imagePickerLauncher.launch("image/*") }, modifier = Modifier.size(36.dp).background(primaryColor, CircleShape)) {
                                Icon(painter = painterResource(R.drawable.addphoto), contentDescription = "Change", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Personal Info
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Personal Information", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                        ProfileTextField("First Name", firstName, onValueChange = { firstName = it; hasChanges = true })
                        ProfileTextField("Last Name", lastName, onValueChange = { lastName = it; hasChanges = true })
                        ProfileTextField("Email", email, readOnly = true, onValueChange = {})
                    }
                }

                Spacer(Modifier.height(16.dp))

                // Contact + Address Card
                Card(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(Color.White)) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Contact & Address", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))

                        // === PHILIPPINE ADDRESS PICKER ===
                        Text("Complete Address", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = primaryColor, modifier = Modifier.padding(top = 8.dp, bottom = 8.dp))

                        DropdownSelector("Province", philippineProvinces, selectedProvince) {
                            selectedProvince = it
                            selectedMunicipality = ""
                            selectedBarangay = ""
                            hasChanges = true
                        }

                        DropdownSelector("City / Municipality", if (selectedProvince.isEmpty()) emptyList() else municipalities[selectedProvince] ?: emptyList(), selectedMunicipality) {
                            selectedMunicipality = it
                            selectedBarangay = ""
                            hasChanges = true
                        }

                        DropdownSelector("Barangay", if (selectedMunicipality.isEmpty()) emptyList() else barangays["$selectedProvince|$selectedMunicipality"] ?: emptyList(), selectedBarangay) {
                            selectedBarangay = it
                            hasChanges = true
                        }

                        OutlinedTextField(
                            value = streetDetails,
                            onValueChange = { streetDetails = it; hasChanges = true },
                            label = { Text("Street, Purok, Sitio, House # (Optional)") },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                            shape = RoundedCornerShape(12.dp),
                            singleLine = true
                        )

                        if (fullAddress.isNotEmpty()) {
                            Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Final: $fullAddress", color = primaryColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                            }
                        }

                        PhoneNumberTextField("Contact Number", contactNumber, onValueChange = { contactNumber = it; hasChanges = true })
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Save Button
                Button(
                    onClick = {
                        isSubmitting = true
                        val userId = auth.currentUser?.uid ?: return@Button

                        if (selectedImageUri != null) {
                            uploadImageToFirebaseStorage(userId, selectedImageUri!!) { url ->
                                profilePicture = url
                                updateProfile(userId, firstName, lastName, fullAddress, contactNumber, profilePicture, userType) {
                                    isSubmitting = false
                                    showDialog = true
                                    hasChanges = false
                                }
                            }
                        } else {
                            updateProfile(userId, firstName, lastName, fullAddress, contactNumber, profilePicture, userType) {
                                isSubmitting = false
                                showDialog = true
                                hasChanges = false
                            }
                        }
                    },
                    enabled = !isSubmitting && hasChanges,
                    colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    if (isSubmitting) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                    else Text("Save Changes", style = MaterialTheme.typography.labelLarge)
                }

                Spacer(Modifier.height(12.dp))
                OutlinedButton(onClick = { navController.popBackStack() }, modifier = Modifier.fillMaxWidth().height(54.dp)) {
                    Text("Cancel")
                }

                Spacer(Modifier.height(16.dp))
            }

            // Success Dialog
            if (showDialog) {
                AlertDialog(
                    onDismissRequest = { showDialog = false },
                    title = { Text("Success") },
                    text = { Text("Profile updated successfully!") },
                    confirmButton = {
                        Button(onClick = { showDialog = false; navController.popBackStack() }) {
                            Text("OK")
                        }
                    }
                )
            }
        }
    }
}

// === REUSABLE DROPDOWN ===
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(label: String, options: List<String>, selected: String, onSelected: (String) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(expanded = expanded, onExpandedChange = { expanded = !expanded }, modifier = Modifier.padding(vertical = 4.dp)) {
        OutlinedTextField(
            value = selected.ifEmpty { "Select $label" },
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier.menuAnchor().fillMaxWidth()
        )
        ExposedDropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { option ->
                DropdownMenuItem(text = { Text(option) }, onClick = {
                    onSelected(option)
                    expanded = false
                })
            }
        }
    }
}

// === PARSE SAVED ADDRESS INTO DROPDOWNS ===
fun parseAddressToDropdowns(address: String, onParsed: (prov: String, mun: String, bar: String, street: String) -> Unit) {
    val parts = address.split(",").map { it.trim() }
    var province = ""
    var municipality = ""
    var barangay = ""
    var street = ""

    // Try to match from the end
    if (parts.isNotEmpty()) {
        val last = parts.last()
        if (philippineProvinces.any { it.equals(last, ignoreCase = true) }) {
            province = last
            if (parts.size >= 2) {
                val secondLast = parts[parts.size - 2]
                val key = "$province|$secondLast"
                if (barangays.containsKey(key) || municipalities[province]?.contains(secondLast) == true) {
                    municipality = secondLast
                    if (parts.size >= 3) barangay = parts[parts.size - 3]
                    street = parts.take(parts.size - 3).joinToString(", ")
                }
            }
        }
    }
    onParsed(province, municipality, barangay, street)
}

/// Philippine Data — ONLY Valenzuela City + Bulacan Province
val philippineProvinces = listOf(
    "Bulacan",           // Province
    "Metro Manila"    // Highly Urbanized City (treated as separate "province" in dropdown)
)

// Municipalities / Cities under Bulacan + Valenzuela as standalone
val municipalities = mapOf(
    "Bulacan" to listOf(
        "Baliuag", "Malolos City", "Meycauayan City", "San Jose del Monte City",
        "Bocaue", "Marilao", "Santa Maria", "Norzagaray", "Obando", "Pandi",
        "Plaridel", "Pulilan", "Guiguinto", "Bulacan", "Bustos", "Calumpit",
        "Hagonoy", "Paombong", "Angat", "Doña Remedios Trinidad", "San Ildefonso",
        "San Miguel", "San Rafael"
    ),
    "Metro Manila" to listOf("Valenzuela City") // It's a single city, no sub-muni
)

// Real Barangays (sample from popular areas — you can expand later)
val barangays = mapOf(
    // === VALENZUELA CITY (33 official barangays - showing most used) ===
    "Metro Manila|Valenzuela City" to listOf(
        "Malinta", "Karuhatan", "Gen. T. de Leon", "Dalandanan", "Maysan",
        "Paso de Blas", "Mapulang Lupa", "Bagbaguin", "Arkong Bato",
        "Punturin", "Canumay West", "Canumay East", "Coloong", "Lingunan",
        "Parada", "Mabolo", "Poblacion", "Tagalag", "Rincon", "Palasan",
        "Wawang Pulo", "Bignay", "Viente Reales", "Marulas", "Ugong"
    ),

    // === BULACAN MUNICIPALITIES (sample barangays) ===
    "Bulacan|Baliuag" to listOf("Poblacion", "Sabang", "Tiaong", "Pagala", "Bagong Nayon", "Tangos", "Hinukay", "Matangtubig"),
    "Bulacan|Malolos City" to listOf("Sumapang Matanda", "Santo Niño", "Catmon", "San Agustin", "Barihan", "Pulong Buhangin", "Mojon"),
    "Bulacan|Meycauayan City" to listOf("Calvario", "Perez", "Camalig", "Hulo", "Malhacan", "Poblacion", "Bangkal"),
    "Bulacan|San Jose del Monte City" to listOf("Muzon", "Graceville", "Tungkong Mangga", "Kaypian", "San Pedro", "Citrus"),
    "Bulacan|Bocaue" to listOf("Batia", "Bundukan", "Tambobong", "Taal", "Biñang 1st", "Lolomboy"),
    "Bulacan|Marilao" to listOf("Loma de Gato", "Saog", "Lambakin", "Ibayo", "Poblacion I", "Tabing Ilog"),
    "Bulacan|Santa Maria" to listOf("Cay Pombo", "Pulong Buhangin", "Guyong", "Mag-asawang Sapa", "Catmon"),
    "Bulacan|Plaridel" to listOf("Bangkal", "Tabang", "Sipat", "Bulihan", "Parulan", "Agnaya"),
    "Bulacan|Guiguinto" to listOf("Poblacion", "Santa Rita", "Tuktukan", "Ilang-Ilang", "Daungan")
    // Add more as needed — this covers the most common areas
)

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
    onValueChange: (String) -> Unit
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
            focusedIndicatorColor = Color(0xFF0DA54B),
            unfocusedIndicatorColor = Color.Gray,
            cursorColor = Color(0xFF0DA54B)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)
    )
}

@Composable
fun PhoneNumberTextField(
    label: String = "Contact Number",
    value: String,
    onValueChange: (String) -> Unit
) {
    // Strip +63 prefix from the value for display purposes
    val rawNumber = value.removePrefix("+63")

    OutlinedTextField(
        value = rawNumber,
        onValueChange = { input ->
            // Keep digits only
            var digits = input.filter { it.isDigit() }

            // Handle pasted or typed formats
            digits = when {
                digits.startsWith("63") -> digits.drop(2)   // +639XXXXXXXXX
                digits.startsWith("0") -> digits.drop(1)    // 09XXXXXXXXX
                else -> digits
            }

            // Max 10 digits
            digits = digits.take(10)

            // Send back full number with +63 prefix
            onValueChange("+63$digits")
        },
        label = { Text(label) },
        placeholder = { Text("9123456789") },
        prefix = { Text("+63") },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.White,
            unfocusedContainerColor = Color.White,
            focusedIndicatorColor = Color(0xFF0DA54B),
            unfocusedIndicatorColor = Color.Gray,
            cursorColor = Color(0xFF0DA54B)
        ),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    )
}
