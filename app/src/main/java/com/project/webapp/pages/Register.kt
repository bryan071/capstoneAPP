package com.project.webapp.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
fun Register(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel = viewModel()) {
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

    // Define colors
    val primaryColor = Color(0xFF0DA54B)
    val backgroundColor = Color(0xFFF5F5F5)
    val cardColor = Color.White

    var authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                val userId = authViewModel.auth.currentUser?.uid
                userId?.let {
                    authViewModel.firestore.collection("users").document(it).get()
                        .addOnSuccessListener { document ->
                            val userType = document.getString("userType")
                            if (userType.isNullOrEmpty()) {
                                Toast.makeText(context, "User type not found", Toast.LENGTH_SHORT).show()
                            } else {
                                when (userType) {
                                    "Farmer" -> navController.navigate(Route.FARMER_DASHBOARD)
                                    "Market" -> navController.navigate(Route.MARKET_DASHBOARD)
                                    else -> Toast.makeText(context, "Invalid user type", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(context, "Failed to fetch user data", Toast.LENGTH_SHORT).show()
                        }
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 8.dp),
            verticalArrangement = Arrangement.Top,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Logo and Tagline
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "App Logo",
                modifier = Modifier
                    .size(200.dp)
                    .fillMaxWidth().size(80.dp)
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

                    Divider(
                        modifier = Modifier
                            .padding(vertical = 12.dp)
                            .width(100.dp)
                            .height(2.dp)
                            .background(primaryColor)
                    )

                    // Form Fields
                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = firstname,
                        onValueChange = { firstname = it },
                        label = { Text(text = "First Name") },
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = lastname,
                        onValueChange = { lastname = it },
                        label = { Text(text = "Last Name") },
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(text = "Email Address") },
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = address,
                        onValueChange = { address = it },
                        label = { Text(text = "Address") },
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = phoneNumber,
                        onValueChange = { input ->
                            // Ensure it starts with +63
                            if (input.length >= 3 && input.startsWith("+63")) {
                                val digitsOnly = input.substring(3).filter { it.isDigit() } // Remove non-numeric characters
                                if (digitsOnly.length <= 10) { // Limit to 10 digits after +63
                                    phoneNumber = "+63$digitsOnly"
                                }
                            } else {
                                phoneNumber = "+63" // Reset if invalid
                            }
                        },
                        label = { Text(text = "Contact Number (e.g., +639123456789)") },
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = password,
                        onValueChange = { password = it },
                        label = { Text(text = "Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    OutlinedTextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        value = confirmpass,
                        onValueChange = { confirmpass = it },
                        label = { Text(text = "Confirm Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        shape = RoundedCornerShape(8.dp)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // User Type Selection Card
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
                                // Farmer option
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (userType == "Farmer") primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(8.dp)
                                        .clickable { userType = "Farmer" }
                                ) {
                                    RadioButton(
                                        selected = userType == "Farmer",
                                        onClick = { userType = "Farmer" },
                                        colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                    )
                                    Text("Farmer", fontSize = 14.sp)
                                }

                                Spacer(modifier = Modifier.width(8.dp))

                                // Market option
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (userType == "Market") primaryColor.copy(alpha = 0.2f) else Color.Transparent)
                                        .padding(8.dp)
                                        .clickable { userType = "Market" }
                                ) {
                                    RadioButton(
                                        selected = userType == "Market",
                                        onClick = { userType = "Market" },
                                        colors = RadioButtonDefaults.colors(selectedColor = primaryColor)
                                    )
                                    Text("Market", fontSize = 14.sp)
                                }
                            }
                        }
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
                        "Accept Terms & Conditions",
                        termsChecked,
                        { termsChecked = it },
                        primaryColor
                    )

                    DataPrivacy(
                        "Data Privacy Consent",
                        privacyChecked,
                        { privacyChecked = it },
                        primaryColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Register Button
                    Button(
                        onClick = {
                            if (!termsChecked || !privacyChecked) {
                                Toast.makeText(context, "You must accept the terms and privacy policy", Toast.LENGTH_SHORT).show()
                            } else if (userType.isEmpty()) {
                                Toast.makeText(context, "Please select a user type", Toast.LENGTH_SHORT).show()
                            } else {
                                val formattedPhone = formatPhoneNumber(phoneNumber)

                                if (password != confirmpass) {
                                    Toast.makeText(context, "Passwords do not match", Toast.LENGTH_SHORT).show()
                                } else if (password.length < 6) {
                                    Toast.makeText(context, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                                } else if (firstname.isEmpty() || lastname.isEmpty() || email.isEmpty() || address.isEmpty()) {
                                    Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
                                } else {
                                    authViewModel.signup(email, password, firstname, lastname, address, formattedPhone, userType, confirmpass)
                                }
                            }
                        },
                        enabled = authState.value != AuthState.Loading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryColor),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Text(
                            text = "Register",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    if (authState.value is AuthState.Error) {
                        Text(
                            text = (authState.value as AuthState.Error).message,
                            color = Color.Red,
                            modifier = Modifier.padding(top = 8.dp),
                            textAlign = TextAlign.Center
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
fun TermsAndCondition(
    label: String,
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
                .fillMaxWidth()
                .weight(1f)
                .clickable { showDialog = true }
        )
    }
}

@Composable
fun DataPrivacy(
    label: String,
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
                .fillMaxWidth()
                .weight(1f)
                .clickable { showDialog = true }
        )
    }
}

fun formatPhoneNumber(input: String): String {
    val cleaned = input.replace("\\s".toRegex(), "").replace("-", "")

    return when {
        cleaned.startsWith("+63") -> cleaned
        cleaned.startsWith("09") -> "+63" + cleaned.removePrefix("0")
        else -> "+63"
    }
}