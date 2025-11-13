package com.project.webapp.pages

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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

@Composable
fun Register(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel = viewModel()
) {
    // State variables
    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("+63") }
    var password by remember { mutableStateOf("") }
    var confirmpass by remember { mutableStateOf("") }
    var termsChecked by remember { mutableStateOf(false) }
    var privacyChecked by remember { mutableStateOf(false) }
    var userType by remember { mutableStateOf("") }
    var certificateUri by remember { mutableStateOf<Uri?>(null) }

    // Colors
    val primaryColor = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White

    val authState by authViewModel.authState.observeAsState()
    val context = LocalContext.current

    val certificatePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        certificateUri = uri
    }

    // Simplified authentication state handling
    LaunchedEffect(authState) {
        when (val state = authState) {
            is AuthState.Authenticated -> {
                // Navigate immediately based on stored userType
                when (userType) {
                    "Farmer", "Market" -> navController.navigate(Route.FARMER_DASHBOARD) {
                        popUpTo(Route.REGISTER) { inclusive = true }
                    }
                    "Business", "Household" -> navController.navigate(Route.FARMER_DASHBOARD) {
                        popUpTo(Route.REGISTER) { inclusive = true }
                    }
                    else -> Toast.makeText(context, "Registration successful", Toast.LENGTH_SHORT).show()
                }
            }
            is AuthState.Error -> {
                Toast.makeText(context, state.message, Toast.LENGTH_SHORT).show()
            }
            else -> Unit
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and Tagline
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App logo",
                modifier = Modifier.size(80.dp)
            )

            Text(
                text = "Supporting farmers, reducing waste!",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = primaryColor
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Registration Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                elevation = CardDefaults.cardElevation(4.dp),
                colors = CardDefaults.cardColors(containerColor = cardColor),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Register",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryColor
                    )

                    HorizontalDivider(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(100.dp)
                            .height(2.dp),
                        color = primaryColor
                    )

                    // Form Fields
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = firstname,
                        onValueChange = { firstname = it },
                        label = { Text("First Name") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = lastname,
                        onValueChange = { lastname = it },
                        label = { Text("Last Name") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = address,
                        onValueChange = { address = it },
                        label = { Text("Address") },
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = phoneNumber,
                        onValueChange = { input ->
                            if (input.startsWith("+63")) {
                                val digitsOnly = input.substring(3).filter { it.isDigit() }
                                if (digitsOnly.length <= 10) {
                                    phoneNumber = "+63$digitsOnly"
                                }
                            } else {
                                phoneNumber = "+63"
                            }
                        },
                        label = { Text("Contact Number (e.g., +639123456789)") },
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = confirmpass,
                        onValueChange = { confirmpass = it },
                        label = { Text("Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(8.dp),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // User Type Selection
                    UserTypeSelector(
                        selectedType = userType,
                        onTypeSelected = { userType = it },
                        primaryColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Certificate Upload
                    Text(
                        text = "Upload Certificate or ID",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp,
                        color = primaryColor,
                        modifier = Modifier.align(Alignment.Start)
                    )

                    Button(
                        onClick = { certificatePickerLauncher.launch("image/*") },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD9D9D9)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Text(
                            text = if (certificateUri != null) "Change File" else "Choose File",
                            color = Color.Black
                        )
                    }

                    certificateUri?.let {
                        Text(
                            text = "File selected: ${it.lastPathSegment}",
                            fontSize = 14.sp,
                            color = primaryColor,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Terms and Privacy
                    Text(
                        text = "Please accept the terms and conditions and data privacy policy to proceed.",
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )

                    TermsAndCondition(
                        isChecked = termsChecked,
                        onCheckedChange = { termsChecked = it },
                        primaryColor = primaryColor
                    )

                    DataPrivacy(
                        isChecked = privacyChecked,
                        onCheckedChange = { privacyChecked = it },
                        primaryColor = primaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Register Button
                    Button(
                        onClick = {
                            when {
                                !termsChecked || !privacyChecked -> {
                                    Toast.makeText(
                                        context,
                                        "You must accept the terms and privacy policy",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                userType.isEmpty() -> {
                                    Toast.makeText(
                                        context,
                                        "Please select a user type",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                firstname.isBlank() || lastname.isBlank() ||
                                        email.isBlank() || address.isBlank() -> {
                                    Toast.makeText(
                                        context,
                                        "Please fill in all fields",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                password != confirmpass -> {
                                    Toast.makeText(
                                        context,
                                        "Passwords do not match",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                password.length < 6 -> {
                                    Toast.makeText(
                                        context,
                                        "Password must be at least 6 characters",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                }
                                else -> {
                                    val formattedPhone = formatPhoneNumber(phoneNumber)
                                    authViewModel.signup(
                                        email = email,
                                        password = password,
                                        firstname = firstname,
                                        lastname = lastname,
                                        address = address,
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        if (authState == AuthState.Loading) {
                            CircularProgressIndicator(
                                color = Color.White,
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text(
                                text = "Register",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    if (authState is AuthState.Error) {
                        Text(
                            text = (authState as AuthState.Error).message,
                            color = MaterialTheme.colorScheme.error,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center,
                            fontSize = 14.sp
                        )
                    }

                    TextButton(
                        onClick = { navController.navigate(Route.LOGIN) },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Already have an account? Login",
                            color = primaryColor,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

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