package com.project.webapp.pages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.Viewmodel.AuthState
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.popup.Privacy
import com.project.webapp.popup.Terms

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Register(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // Form states
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("+63") }
    var password by remember { mutableStateOf("") }
    var confirmpass by remember { mutableStateOf("") }
    var termsChecked by remember { mutableStateOf(false) }
    var privacyChecked by remember { mutableStateOf(false) }
    var userType by remember { mutableStateOf("") }
    var certificateUri by remember { mutableStateOf<Uri?>(null) }

    // Address states
    var selectedProvince by remember { mutableStateOf("") }
    var selectedMunicipality by remember { mutableStateOf("") }
    var selectedBarangay by remember { mutableStateOf("") }
    var streetDetails by remember { mutableStateOf("") }

    // Final address (for saving)
    val fullAddress = listOf(streetDetails, selectedBarangay, selectedMunicipality, selectedProvince)
        .filter { it.isNotBlank() }
        .joinToString(", ")

    val primaryColor = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White

    val authState by authViewModel.authState.observeAsState()
    val context = LocalContext.current

    val certificatePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? -> certificateUri = uri }

    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.RegistrationSuccess -> {
                Toast.makeText(context, "Registration successful! Please wait for admin approval.", Toast.LENGTH_LONG).show()
                authViewModel.logout()
                navController.navigate(Route.LOGIN) { popUpTo(Route.REGISTER) { inclusive = true } }
            }
            is AuthState.Error -> Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            else -> Unit
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(backgroundColor).padding(16.dp)) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(painter = painterResource(R.drawable.logo), contentDescription = "Logo", modifier = Modifier.size(80.dp))
            Text("Supporting farmers, reducing waste!", fontSize = 22.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, color = primaryColor)
            Spacer(Modifier.height(20.dp))

            Card(modifier = Modifier.fillMaxWidth().padding(8.dp), elevation = CardDefaults.cardElevation(4.dp), colors = CardDefaults.cardColors(cardColor), shape = RoundedCornerShape(16.dp)) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Register", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                    HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp).width(100.dp).height(2.dp), color = primaryColor)

                    // Name fields
                    OutlinedTextField(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), value = firstname, onValueChange = { firstname = it }, label = { Text("First Name") }, shape = RoundedCornerShape(8.dp), singleLine = true)
                    OutlinedTextField(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), value = lastname, onValueChange = { lastname = it }, label = { Text("Last Name") }, shape = RoundedCornerShape(8.dp), singleLine = true)
                    OutlinedTextField(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), value = email, onValueChange = { email = it }, label = { Text("Email Address") }, shape = RoundedCornerShape(8.dp), singleLine = true)

                    // === PHILIPPINE ADDRESS PICKER ===
                    Text("Complete Address", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = primaryColor, modifier = Modifier.align(Alignment.Start).padding(top = 12.dp, bottom = 8.dp))

                    DropdownSelector(
                        label = "Province",
                        options = philippineProvinces,
                        selected = selectedProvince,
                        onSelected = {
                            selectedProvince = it
                            selectedMunicipality = ""
                            selectedBarangay = ""
                        }
                    )

                    DropdownSelector(
                        label = "City / Municipality",
                        options = if (selectedProvince.isEmpty()) emptyList() else municipalities[selectedProvince] ?: emptyList(),
                        selected = selectedMunicipality,
                        onSelected = {
                            selectedMunicipality = it
                            selectedBarangay = ""
                        }
                    )

                    DropdownSelector(
                        label = "Barangay",
                        options = if (selectedMunicipality.isEmpty()) emptyList() else barangays["$selectedProvince|$selectedMunicipality"] ?: emptyList(),
                        selected = selectedBarangay,
                        onSelected = { selectedBarangay = it }
                    )

                    OutlinedTextField(
                        value = streetDetails,
                        onValueChange = { streetDetails = it },
                        label = { Text("Street, Purok, Sitio, House # (Optional)") },
                        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    // Show preview
                    if (fullAddress.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn, contentDescription = null, tint = primaryColor, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Address: $fullAddress", color = primaryColor, fontSize = 14.sp, modifier = Modifier.weight(1f))
                        }
                    }

                    // Phone, password, etc.
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = phoneNumber.removePrefix("+63"),
                        onValueChange = { input ->
                            // Keep only digits
                            var digits = input.filter { it.isDigit() }

                            // Convert pasted numbers like 09123456789 or +639123456789
                            digits = when {
                                digits.startsWith("63") -> digits.drop(2)   // 63XXXXXXXXXX
                                digits.startsWith("0") -> digits.drop(1)    // 09XXXXXXXXX
                                else -> digits
                            }

                            // Limit to 10 digits
                            digits = digits.take(10)

                            // Build final stored value
                            phoneNumber = "+63$digits"
                        },
                        label = { Text("Contact Number") },
                        prefix = { Text("+63") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )

                    OutlinedTextField(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), value = password, onValueChange = { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(8.dp), singleLine = true)
                    OutlinedTextField(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), value = confirmpass, onValueChange = { confirmpass = it }, label = { Text("Confirm Password") }, visualTransformation = PasswordVisualTransformation(), shape = RoundedCornerShape(8.dp), singleLine = true)

                    Spacer(Modifier.height(12.dp))
                    UserTypeSelector(selectedType = userType, onTypeSelected = { userType = it }, primaryColor = primaryColor)
                    Spacer(Modifier.height(12.dp))

                    Text("Upload Certificate or ID", fontWeight = FontWeight.Medium, fontSize = 16.sp, color = primaryColor, modifier = Modifier.align(Alignment.Start))
                    Button(onClick = { certificatePickerLauncher.launch("image/*") }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9D9D9)), modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                        Text(if (certificateUri != null) "Change File" else "Choose File", color = Color.Black)
                    }
                    certificateUri?.let { Text("Selected: ${it.lastPathSegment}", fontSize = 14.sp, color = primaryColor) }

                    Spacer(Modifier.height(12.dp))
                    Text("Please accept the terms and conditions and data privacy policy to proceed.", fontSize = 14.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(vertical = 8.dp))
                    TermsAndCondition(isChecked = termsChecked, onCheckedChange = { termsChecked = it }, primaryColor = primaryColor)
                    DataPrivacy(isChecked = privacyChecked, onCheckedChange = { privacyChecked = it }, primaryColor = primaryColor)

                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = {
                            when {
                                !termsChecked || !privacyChecked -> Toast.makeText(context, "Accept terms and privacy", Toast.LENGTH_SHORT).show()
                                userType.isEmpty() -> Toast.makeText(context, "Select user type", Toast.LENGTH_SHORT).show()
                                firstname.isBlank() || lastname.isBlank() || email.isBlank() || fullAddress.isBlank() -> Toast.makeText(context, "Complete all fields", Toast.LENGTH_SHORT).show()
                                password != confirmpass -> Toast.makeText(context, "Passwords don't match", Toast.LENGTH_SHORT).show()
                                password.length < 6 -> Toast.makeText(context, "Password too short", Toast.LENGTH_SHORT).show()
                                else -> {
                                    val formattedPhone = formatPhoneNumber(phoneNumber)
                                    authViewModel.signup(
                                        email = email,
                                        password = password,
                                        firstname = firstname,
                                        lastname = lastname,
                                        address = fullAddress,  // ← This is your complete PH address
                                        phoneNumber = formattedPhone,
                                        userType = userType,
                                        confirmpass = confirmpass,
                                        certificateUri = certificateUri,
                                        context = context
                                    )
                                }
                            }
                        },
                        enabled = authState != AuthState.Loading,
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (authState == AuthState.Loading) CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
                        else Text("Register", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    if (authState is AuthState.Error) Text((authState as AuthState.Error).message, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 8.dp))

                    TextButton(onClick = { navController.navigate(Route.LOGIN) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Already have an account? Login", color = primaryColor, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }
    }
}

// Reusable Dropdown
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownSelector(
    label: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit
) {
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

// Philippine Data — ONLY Valenzuela City + Bulacan Province
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

@Composable
fun UserTypeSelector(
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    primaryColor: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFEAF6EE)),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = "Select User Type:",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = primaryColor,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                UserTypeOption("Farmer", selectedType, onTypeSelected, primaryColor)
                Spacer(modifier = Modifier.width(8.dp))
                UserTypeOption("Business", selectedType, onTypeSelected, primaryColor)
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                UserTypeOption("Household", selectedType, onTypeSelected, primaryColor)
                Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun RowScope.UserTypeOption(
    type: String,
    selectedType: String,
    onTypeSelected: (String) -> Unit,
    primaryColor: Color
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (selectedType == type) primaryColor.copy(alpha = 0.2f)
                else Color.Transparent
            )
            .padding(8.dp)
            .clickable { onTypeSelected(type) }
    ) {
        RadioButton(
            selected = selectedType == type,
            onClick = { onTypeSelected(type) },
            colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
        )
        Text(type, fontSize = 14.sp)
    }
}

@Composable
fun TermsAndCondition(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryColor: Color
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        Terms(onDismiss = { showDialog = false })
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 8.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = primaryColor,
                uncheckedColor = Color.Gray
            )
        )

        Text(
            text = "I have read and agree to the Terms and Conditions",
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .clickable { showDialog = true }
        )
    }
}

@Composable
fun DataPrivacy(
    isChecked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    primaryColor: Color
) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        Privacy(onDismiss = { showDialog = false })
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.padding(end = 8.dp),
            colors = CheckboxDefaults.colors(
                checkedColor = primaryColor,
                uncheckedColor = Color.Gray
            )
        )

        Text(
            text = "I consent to the collection and processing of my data as described in the Privacy Policy",
            fontSize = 14.sp,
            modifier = Modifier
                .weight(1f)
                .clickable { showDialog = true }
        )
    }
}

fun formatPhoneNumber(input: String): String {
    val cleaned = input.replace("\\s".toRegex(), "").replace("-", "")

    return when {
        cleaned.startsWith("+63") -> cleaned
        cleaned.startsWith("09") -> "+63${cleaned.removePrefix("0")}"
        else -> "+63"
    }
}