package com.project.webapp.pages

import CartViewModel
import android.app.Activity
import android.content.Context
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseTooManyRequestsException
import com.google.firebase.auth.*
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.components.TopBar
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

// Constants
private val PrimaryColor = Color(0xFF0DA54B)
private const val OTP_LENGTH = 6
private const val RESEND_TIMEOUT = 60
private const val MIN_PASSWORD_LENGTH = 8

// Data class for managing state
data class ForgotPasswordState(
    val phoneNumber: String = "",
    val otp: List<String> = List(OTP_LENGTH) { "" },
    val newPassword: String = "",
    val confirmPassword: String = "",
    val currentStep: Int = 1,
    val verificationId: String = "",
    val resendToken: PhoneAuthProvider.ForceResendingToken? = null,
    val errorMessage: String? = null,
    val isLoading: Boolean = false,
    val passwordVisible: Boolean = false,
    val confirmPasswordVisible: Boolean = false,
    val countdown: Int = RESEND_TIMEOUT,
    val canResendOtp: Boolean = false,
    val userType: String? = null
) {
    val isPasswordValid: Boolean
        get() = newPassword.length >= MIN_PASSWORD_LENGTH &&
                newPassword.any { it.isLetter() } &&
                newPassword.any { it.isDigit() }

    val passwordsMatch: Boolean
        get() = newPassword == confirmPassword && confirmPassword.isNotEmpty()

    val otpString: String
        get() = otp.joinToString("")
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun ForgotPass(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    activity: Activity,
    cartViewModel: CartViewModel
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val auth = remember { FirebaseAuth.getInstance() }

    var state by remember { mutableStateOf(ForgotPasswordState()) }
    val otpFocusRequesters = remember { List(OTP_LENGTH) { FocusRequester() } }

    // Load user type on initialization
    LaunchedEffect(Unit) {
        FirebaseAuth.getInstance().currentUser?.uid?.let { userId ->
            FirebaseFirestore.getInstance().collection("users")
                .document(userId)
                .get()
                .addOnSuccessListener { document ->
                    state = state.copy(userType = document.getString("userType"))
                }
        }
    }

    // Countdown timer for OTP resend
    LaunchedEffect(state.currentStep, state.canResendOtp) {
        if (state.currentStep == 2 && !state.canResendOtp) {
            var countdown = RESEND_TIMEOUT
            while (countdown > 0) {
                delay(1000)
                countdown--
                state = state.copy(countdown = countdown)
            }
            state = state.copy(canResendOtp = true)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar
            Header(
                userType = state.userType,
                currentStep = state.currentStep,
                navController = navController,
                cartViewModel = cartViewModel,
                onBack = {
                    if (state.currentStep > 1) {
                        state = state.copy(currentStep = state.currentStep - 1)
                    } else {
                        navController.navigate(Route.LOGIN)
                    }
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Logo and Title
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "Logo",
                modifier = Modifier.size(80.dp)
            )
            Text(
                text = "Supporting farmers, reducing waste!",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = PrimaryColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            StepIndicator(currentStep = state.currentStep)

            Spacer(modifier = Modifier.height(32.dp))

            // Step Content
            when (state.currentStep) {
                1 -> PhoneStep(
                    phoneNumber = state.phoneNumber,
                    onPhoneChanged = { state = state.copy(phoneNumber = it) },
                    onSubmit = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        state = state.copy(isLoading = true)

                        PhoneVerificationManager.checkAndSendOtp(
                            phoneNumber = state.phoneNumber,
                            auth = auth,
                            activity = activity,
                            context = context,
                            onSuccess = { verificationId, token ->
                                state = state.copy(
                                    isLoading = false,
                                    verificationId = verificationId,
                                    resendToken = token,
                                    currentStep = 2
                                )
                            },
                            onError = { error ->
                                state = state.copy(isLoading = false, errorMessage = error)
                            }
                        )
                    }
                )

                2 -> OTPStep(
                    otpValues = state.otp,
                    onOtpChanged = { index, value ->
                        if (value.length <= 1 && value.all { it.isDigit() }) {
                            val newOtp = state.otp.toMutableList()
                            newOtp[index] = value
                            state = state.copy(otp = newOtp)

                            if (value.isNotEmpty() && index < OTP_LENGTH - 1) {
                                otpFocusRequesters[index + 1].requestFocus()
                            }
                        }
                    },
                    focusRequesters = otpFocusRequesters,
                    onSubmit = {
                        if (state.otpString.length == OTP_LENGTH) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            state = state.copy(isLoading = true)

                            PhoneVerificationManager.verifyOTP(
                                verificationId = state.verificationId,
                                otp = state.otpString,
                                auth = auth,
                                onSuccess = {
                                    state = state.copy(isLoading = false, currentStep = 3)
                                },
                                onError = { error ->
                                    state = state.copy(isLoading = false, errorMessage = error)
                                }
                            )
                        } else {
                            state = state.copy(errorMessage = "Please enter a valid 6-digit OTP")
                        }
                    },
                    countdown = state.countdown,
                    canResend = state.canResendOtp,
                    onResend = {
                        PhoneVerificationManager.resendOTP(
                            phoneNumber = state.phoneNumber,
                            resendToken = state.resendToken,
                            auth = auth,
                            activity = activity,
                            onCodeSent = { verificationId, token ->
                                state = state.copy(
                                    verificationId = verificationId,
                                    resendToken = token,
                                    canResendOtp = false
                                )
                                Toast.makeText(context, "OTP resent successfully", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                state = state.copy(errorMessage = error)
                            }
                        )
                    }
                )

                3 -> ResetPasswordStep(
                    password = state.newPassword,
                    confirmPassword = state.confirmPassword,
                    passwordVisible = state.passwordVisible,
                    confirmPasswordVisible = state.confirmPasswordVisible,
                    isPasswordValid = state.isPasswordValid,
                    passwordsMatch = state.passwordsMatch,
                    onPasswordChange = { state = state.copy(newPassword = it) },
                    onConfirmPasswordChange = { state = state.copy(confirmPassword = it) },
                    onTogglePasswordVisibility = {
                        state = state.copy(passwordVisible = !state.passwordVisible)
                    },
                    onToggleConfirmPasswordVisibility = {
                        state = state.copy(confirmPasswordVisible = !state.confirmPasswordVisible)
                    },
                    onSubmit = {
                        if (state.isPasswordValid && state.passwordsMatch) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            state = state.copy(isLoading = true)

                            PasswordManager.resetPassword(
                                auth = auth,
                                newPassword = state.newPassword,
                                onComplete = {
                                    state = state.copy(isLoading = false)
                                    coroutineScope.launch {
                                        Toast.makeText(
                                            context,
                                            "Password reset successful!",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        delay(500)
                                        navController.navigate(Route.LOGIN) {
                                            popUpTo(Route.LOGIN) { inclusive = true }
                                        }
                                    }
                                },
                                onError = { error ->
                                    state = state.copy(isLoading = false, errorMessage = error)
                                }
                            )
                        } else {
                            state = state.copy(
                                errorMessage = when {
                                    !state.isPasswordValid ->
                                        "Password must be at least 8 characters with letters and numbers"
                                    !state.passwordsMatch -> "Passwords do not match"
                                    else -> null
                                }
                            )
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = state.currentStep != 3,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                TextButton(onClick = { navController.navigate(Route.LOGIN) }) {
                    Text("Back to Login", color = PrimaryColor)
                }
            }
        }

        if (state.isLoading) LoadingDialog()
    }

    // Error Dialog
    state.errorMessage?.let { error ->
        AlertDialog(
            onDismissRequest = { state = state.copy(errorMessage = null) },
            title = { Text("Error") },
            text = { Text(error) },
            confirmButton = {
                Button(onClick = { state = state.copy(errorMessage = null) }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
private fun Header(
    userType: String?,
    currentStep: Int,
    navController: NavController,
    cartViewModel: CartViewModel,
    onBack: () -> Unit
) {
    userType?.let { type ->
        TopBar(navController, cartViewModel, userType = type)
    } ?: run {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Text(
                text = "Forgot Password",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

@Composable
fun StepIndicator(currentStep: Int) {
    val steps = listOf("Phone", "Verify", "Reset")

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        steps.forEachIndexed { index, label ->
            val stepNumber = index + 1
            val isActive = stepNumber <= currentStep

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    modifier = Modifier.size(40.dp),
                    shape = CircleShape,
                    color = if (isActive) PrimaryColor else Color(0xFFE0E0E0)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stepNumber.toString(),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = if (isActive) PrimaryColor else Color.Gray,
                    fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal
                )
            }

            if (stepNumber < steps.size) {
                Surface(
                    modifier = Modifier
                        .height(2.dp)
                        .weight(1f),
                    color = if (stepNumber < currentStep) PrimaryColor else Color(0xFFE0E0E0)
                ) {}
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PhoneStep(
    phoneNumber: String,
    onPhoneChanged: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Extract just the digits without +63
    val displayNumber = phoneNumber.removePrefix("+63")
    val isValid = displayNumber.length == 10 && displayNumber.startsWith("9")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter Phone Number",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We'll send a verification code to this number",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = displayNumber,
            onValueChange = { input ->
                // Only allow digits and limit to 10 characters
                val cleaned = input.filter { it.isDigit() }.take(10)
                // Always prepend +63
                onPhoneChanged(if (cleaned.isNotEmpty()) "+63$cleaned" else "")
            },
            label = { Text("Phone Number") },
            placeholder = { Text("9XXXXXXXXX") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Number,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    if (isValid) onSubmit()
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Text(
                    text = "+63",
                    color = Color.Gray,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 16.dp)
                )
            },
            isError = phoneNumber.isNotEmpty() && !isValid,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        if (phoneNumber.isNotEmpty() && !isValid) {
            Text(
                text = "Please enter a valid 10-digit phone number starting with 9",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            enabled = isValid,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                disabledContainerColor = Color.Gray
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Send Verification Code",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OTPStep(
    otpValues: List<String>,
    onOtpChanged: (Int, String) -> Unit,
    focusRequesters: List<FocusRequester>,
    onSubmit: () -> Unit,
    countdown: Int,
    canResend: Boolean,
    onResend: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter Verification Code",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "We've sent a 6-digit code to your phone",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            otpValues.forEachIndexed { index, value ->
                OutlinedTextField(
                    value = value,
                    onValueChange = { onOtpChanged(index, it) },
                    modifier = Modifier
                        .width(48.dp)
                        .height(56.dp)
                        .focusRequester(focusRequesters[index]),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (index < OTP_LENGTH - 1) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (index < OTP_LENGTH - 1) focusRequesters[index + 1].requestFocus() },
                        onDone = {
                            focusManager.clearFocus()
                            keyboardController?.hide()
                            if (otpValues.joinToString("").length == OTP_LENGTH) onSubmit()
                        }
                    ),
                    colors = TextFieldDefaults.outlinedTextFieldColors(
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = Color.Gray
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                text = if (canResend) "Didn't receive the code? " else "Resend in ${countdown}s",
                fontSize = 14.sp,
                color = Color.Gray
            )
            if (canResend) {
                TextButton(onClick = onResend) {
                    Text(
                        text = "Resend",
                        color = PrimaryColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Button(
            onClick = onSubmit,
            enabled = otpValues.joinToString("").length == OTP_LENGTH,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                disabledContainerColor = Color.Gray
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Verify Code",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResetPasswordStep(
    password: String,
    confirmPassword: String,
    passwordVisible: Boolean,
    confirmPasswordVisible: Boolean,
    isPasswordValid: Boolean,
    passwordsMatch: Boolean,
    onPasswordChange: (String) -> Unit,
    onConfirmPasswordChange: (String) -> Unit,
    onTogglePasswordVisibility: () -> Unit,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSubmit: () -> Unit
) {
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create New Password",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Set a strong password for your account",
            fontSize = 14.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = { Text("New Password") },
            singleLine = true,
            visualTransformation = if (passwordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible)
                            Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible)
                            "Hide password" else "Show password",
                        tint = Color.Gray
                    )
                }
            },
            isError = password.isNotEmpty() && !isPasswordValid,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        if (password.isNotEmpty()) {
            PasswordStrengthIndicator(password = password)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible)
                VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    if (isPasswordValid && passwordsMatch) onSubmit()
                }
            ),
            trailingIcon = {
                IconButton(onClick = onToggleConfirmPasswordVisibility) {
                    Icon(
                        imageVector = if (confirmPasswordVisible)
                            Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible)
                            "Hide password" else "Show password",
                        tint = Color.Gray
                    )
                }
            },
            isError = confirmPassword.isNotEmpty() && !passwordsMatch,
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        if (confirmPassword.isNotEmpty() && !passwordsMatch) {
            Text(
                text = "Passwords do not match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, top = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = onSubmit,
            enabled = isPasswordValid && passwordsMatch,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PrimaryColor,
                disabledContainerColor = Color.Gray
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = "Reset Password",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun PasswordStrengthIndicator(password: String) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
        val checks = listOf(
            "At least 8 characters" to (password.length >= MIN_PASSWORD_LENGTH),
            "Contains letters and numbers" to (password.any { it.isLetter() } && password.any { it.isDigit() })
        )

        checks.forEach { (text, isValid) ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                    contentDescription = null,
                    tint = if (isValid) Color.Green else Color.Red,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isValid) Color.Green else Color.Red
                )
            }
            if (checks.indexOf(text to isValid) < checks.size - 1) {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun LoadingDialog() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.4f)),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .size(100.dp)
                .padding(16.dp),
            elevation = CardDefaults.cardElevation(4.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator(
                    color = PrimaryColor,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Loading...",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

// Utility Objects
object ValidationUtils {
    fun isValidPhoneNumber(phoneNumber: String): Boolean {
        // Must be +639XXXXXXXXX (10 digits after +63, starting with 9)
        return Regex("^\\+639\\d{9}$").matches(phoneNumber)
    }
}

object PhoneVerificationManager {
    private const val TAG = "PhoneVerification"

    fun checkAndSendOtp(
        phoneNumber: String,
        auth: FirebaseAuth,
        activity: Activity,
        context: Context,
        onSuccess: (String, PhoneAuthProvider.ForceResendingToken?) -> Unit,
        onError: (String) -> Unit
    ) {
        if (!ValidationUtils.isValidPhoneNumber(phoneNumber)) {
            onError("Invalid phone number format. Please use 10 digits starting with 9")
            return
        }

        val firestore = FirebaseFirestore.getInstance()

        Log.d(TAG, "Checking phone number: $phoneNumber")

        // Create list of possible phone number formats
        val phoneFormats = listOf(
            phoneNumber,                           // +639XXXXXXXXX
            phoneNumber.removePrefix("+63"),       // 9XXXXXXXXX
            phoneNumber.removePrefix("+"),         // 639XXXXXXXXX
            "0${phoneNumber.removePrefix("+63")}", // 09XXXXXXXXX
            phoneNumber.replace("+63", "63")       // 639XXXXXXXXX (no plus)
        )

        Log.d(TAG, "Searching for formats: $phoneFormats")

        // Get all users and check manually (more flexible)
        firestore.collection("users")
            .get()
            .addOnSuccessListener { querySnapshot ->
                var foundUser = false

                for (document in querySnapshot.documents) {
                    val storedPhone = document.getString("phoneNumber")
                    Log.d(TAG, "Checking against stored: $storedPhone")

                    // Normalize both numbers for comparison
                    val normalizedStored = normalizePhoneNumber(storedPhone)
                    val normalizedInput = normalizePhoneNumber(phoneNumber)

                    if (normalizedStored == normalizedInput) {
                        Log.d(TAG, "Match found! User: ${document.id}")
                        foundUser = true
                        sendOtp(phoneNumber, auth, activity, onSuccess, onError)
                        break
                    }
                }

                if (!foundUser) {
                    Log.e(TAG, "No matching phone number found")
                    onError("This phone number is not registered. Please check your number or sign up.")
                }
            }
            .addOnFailureListener { e ->
                Log.e(TAG, "Failed to check phone number", e)
                onError("Something went wrong. Please try again. Error: ${e.message}")
            }
    }

    private fun normalizePhoneNumber(phone: String?): String {
        if (phone == null) return ""

        // Remove all non-digit characters
        val digitsOnly = phone.replace(Regex("[^0-9]"), "")

        // Normalize to 10 digits (9XXXXXXXXX format)
        return when {
            digitsOnly.startsWith("639") && digitsOnly.length == 12 -> digitsOnly.substring(2) // 639XXXXXXXXX -> 9XXXXXXXXX
            digitsOnly.startsWith("63") && digitsOnly.length == 11 -> digitsOnly.substring(1)  // 639XXXXXXXX -> 9XXXXXXXX
            digitsOnly.startsWith("09") && digitsOnly.length == 11 -> digitsOnly.substring(1)  // 09XXXXXXXXX -> 9XXXXXXXXX
            digitsOnly.startsWith("9") && digitsOnly.length == 10 -> digitsOnly                // 9XXXXXXXXX (correct)
            digitsOnly.startsWith("0") && digitsOnly.length == 10 -> "9${digitsOnly.substring(1)}" // Handle 0XXXXXXXXX
            else -> {
                Log.w(TAG, "Unhandled phone format: $phone (digits: $digitsOnly)")
                digitsOnly // Return as-is if doesn't match patterns
            }
        }
    }

    private fun sendOtp(
        phoneNumber: String,
        auth: FirebaseAuth,
        activity: Activity,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken?) -> Unit,
        onError: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Verification completed automatically")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Verification failed: ${e.message}", e)
                val errorMessage = when (e) {
                    is FirebaseAuthInvalidCredentialsException ->
                        "Invalid phone number. Please check and try again."
                    is FirebaseTooManyRequestsException ->
                        "Too many requests. Please try again later."
                    else -> "Verification failed: ${e.message}"
                }
                onError(errorMessage)
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "OTP sent successfully to $phoneNumber")
                onCodeSent(verificationId, token)
            }
        }

        // Ensure phone number is in correct format for Firebase (+639XXXXXXXXX)
        val formattedPhone = if (phoneNumber.startsWith("+")) {
            phoneNumber
        } else if (phoneNumber.startsWith("09")) {
            "+63${phoneNumber.substring(1)}"
        } else if (phoneNumber.startsWith("9")) {
            "+63$phoneNumber"
        } else {
            phoneNumber
        }

        Log.d(TAG, "Sending OTP to: $formattedPhone")

        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(formattedPhone.trim())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendOTP(
        phoneNumber: String,
        resendToken: PhoneAuthProvider.ForceResendingToken?,
        auth: FirebaseAuth,
        activity: Activity,
        onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
        onError: (String) -> Unit
    ) {
        val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d(TAG, "Verification completed on resend")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e(TAG, "Resend verification failed: ${e.message}", e)
                onError("Failed to resend verification code: ${e.message}")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d(TAG, "OTP resent successfully")
                onCodeSent(verificationId, token)
            }
        }

        val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber.trim())
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)

        resendToken?.let { optionsBuilder.setForceResendingToken(it) }

        PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
    }

    fun verifyOTP(
        verificationId: String,
        otp: String,
        auth: FirebaseAuth,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (verificationId.isEmpty()) {
            onError("Verification ID is missing. Please request a new OTP.")
            return
        }

        val credential = PhoneAuthProvider.getCredential(verificationId, otp)

        auth.signInWithCredential(credential)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "OTP verification successful")
                    onSuccess()
                } else {
                    Log.e(TAG, "OTP verification failed", task.exception)
                    onError("Invalid OTP. Please try again.")
                }
            }
    }
}

object PasswordManager {
    private const val TAG = "PasswordManager"

    fun resetPassword(
        auth: FirebaseAuth,
        newPassword: String,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        val user = auth.currentUser

        if (user == null) {
            onError("No user signed in. Please verify again.")
            return
        }

        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Password updated successfully")
                    onComplete()
                } else {
                    Log.e(TAG, "Password update failed", task.exception)
                    onError("Failed to reset password. Please try again.")
                }
            }
    }
}