package com.project.webapp.pages

import CartViewModel
import android.app.Activity
import android.content.Context
import android.util.Base64
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
import com.google.android.play.core.integrity.IntegrityManagerFactory
import com.google.android.play.core.integrity.IntegrityTokenRequest
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
import java.security.SecureRandom
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

val PrimaryColor = Color(0xFF0DA54B)
val backgroundColor = Color(0xFFF5F5F5)
val cardColor = Color.White

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

    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var verificationId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var countdown by remember { mutableStateOf(60) }
    var canResendOtp by remember { mutableStateOf(false) }
    var resendToken by remember { mutableStateOf<PhoneAuthProvider.ForceResendingToken?>(null) }
    var userType by remember { mutableStateOf<String?>(null) }

    val auth = FirebaseAuth.getInstance()
    val scrollState = rememberScrollState()

    val otpFocusRequesters = List(6) { remember { FocusRequester() } }
    val otpValues = remember { mutableStateListOf("", "", "", "", "", "") }

    var isPasswordValid by remember { mutableStateOf(false) }
    var passwordsMatch by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = step, key2 = canResendOtp) {
        if (step == 2 && !canResendOtp) {
            countdown = 60
            while (countdown > 0) {
                delay(1000)
                countdown--
            }
            canResendOtp = true
        }
    }

    LaunchedEffect(Unit) {
        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            FirebaseFirestore.getInstance().collection("users")
                .document(it)
                .get()
                .addOnSuccessListener { document ->
                    userType = document.getString("userType")
                }
        }
    }

    LaunchedEffect(newPassword) {
        isPasswordValid = validatePassword(newPassword)
    }

    LaunchedEffect(newPassword, confirmPassword) {
        passwordsMatch = newPassword == confirmPassword
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
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
                    IconButton(onClick = {
                        if (step > 1) step-- else navController.navigate(Route.LOGIN)
                    }) {
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

            Spacer(modifier = Modifier.height(16.dp))
            Image(
                painter = painterResource(id = R.drawable.logo),
                contentDescription = "login image",
                modifier = Modifier
                    .fillMaxWidth()
                    .size(80.dp)
            )
            Text(
                text = "Supporting farmers, reducing waste!",
                fontSize = 23.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = PrimaryColor
            )

            Spacer(modifier = Modifier.height(24.dp))

            StepIndicator(currentStep = step)

            Spacer(modifier = Modifier.height(32.dp))

            when (step) {
                1 -> PhoneStep(
                    phoneNumber = phoneNumber,
                    onPhoneChanged = { phoneNumber = it },
                    onSubmit = {
                        keyboardController?.hide()
                        focusManager.clearFocus()
                        isLoading = true
                        checkPhoneNumberExists(
                            phoneNumber = phoneNumber,
                            auth = auth,
                            activity = activity,
                            context = context,
                            onSuccess = { id, token ->
                                isLoading = false
                                verificationId = id
                                token?.let { resendToken = it }
                                step = 2
                            },
                            onError = { error ->
                                isLoading = false
                                errorMessage = error
                            }
                        )
                    }
                )

                2 -> OTPStep(
                    otpValues = otpValues,
                    onOtpChanged = { index, value ->
                        if (value.length <= 1) {
                            otpValues[index] = value
                            if (value.isNotEmpty() && index < 5) {
                                otpFocusRequesters[index + 1].requestFocus()
                            }
                            otp = otpValues.joinToString("")
                        }
                    },
                    focusRequesters = otpFocusRequesters,
                    onSubmit = {
                        if (otp.length == 6) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            isLoading = true
                            verifyOTP(
                                verificationId = verificationId,
                                otp = otp,
                                auth = auth,
                                context = context,
                                onSuccess = {
                                    isLoading = false
                                    step = 3
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        } else {
                            errorMessage = "Please enter a valid 6-digit OTP code"
                        }
                    },
                    countdown = countdown,
                    canResend = canResendOtp,
                    onResend = {
                        resendVerificationCode(
                            phoneNumber = phoneNumber,
                            resendToken = resendToken,
                            auth = auth,
                            activity = activity,
                            onCodeSent = { id, token ->
                                verificationId = id
                                resendToken = token
                                canResendOtp = false
                                Toast.makeText(context, "OTP code resent successfully", Toast.LENGTH_SHORT).show()
                            },
                            onError = { error ->
                                errorMessage = error
                            }
                        )
                    }
                )

                3 -> ResetPasswordStep(
                    password = newPassword,
                    confirmPassword = confirmPassword,
                    passwordVisible = passwordVisible,
                    confirmPasswordVisible = confirmPasswordVisible,
                    isPasswordValid = isPasswordValid,
                    passwordsMatch = passwordsMatch,
                    onPasswordChange = { newPassword = it },
                    onConfirmPasswordChange = { confirmPassword = it },
                    onTogglePasswordVisibility = { passwordVisible = !passwordVisible },
                    onToggleConfirmPasswordVisibility = { confirmPasswordVisible = !confirmPasswordVisible },
                    onSubmit = {
                        if (isPasswordValid && passwordsMatch) {
                            keyboardController?.hide()
                            focusManager.clearFocus()
                            isLoading = true
                            resetPassword(
                                auth = auth,
                                newPassword = newPassword,
                                context = context,
                                onComplete = {
                                    isLoading = false
                                    coroutineScope.launch {
                                        Toast.makeText(context, "Password reset successful!", Toast.LENGTH_LONG).show()
                                        delay(500)
                                        navController.navigate(Route.LOGIN) {
                                            popUpTo(Route.LOGIN) { inclusive = true }
                                        }
                                    }
                                },
                                onError = { error ->
                                    isLoading = false
                                    errorMessage = error
                                }
                            )
                        } else {
                            when {
                                !isPasswordValid -> errorMessage = "Password must be at least 8 characters with letters and numbers"
                                !passwordsMatch -> errorMessage = "Passwords do not match"
                            }
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(visible = step != 3, enter = fadeIn(), exit = fadeOut()) {
                TextButton(onClick = { navController.navigate(Route.LOGIN) }) {
                    Text("Back to Login", color = PrimaryColor)
                }
            }
        }

        if (isLoading) LoadingDialog()
    }

    if (errorMessage != null) {
        AlertDialog(
            onDismissRequest = { errorMessage = null },
            title = { Text("Error") },
            text = { Text(errorMessage!!) },
            confirmButton = {
                Button(onClick = { errorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}


@Composable
fun StepIndicator(currentStep: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val steps = listOf("Phone", "Verify", "Reset")
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Enter Phone Number",
            fontSize = 20.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
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
            value = phoneNumber,
            onValueChange = { input ->
                val cleaned = input.replace("[^0-9+]".toRegex(), "")
                if (cleaned.startsWith("+63") || cleaned.isEmpty()) {
                    onPhoneChanged(cleaned)
                } else {
                    onPhoneChanged("+63$cleaned")
                }
            },
            label = { Text("Phone Number") },
            placeholder = { Text("") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Phone,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    if (isValidPhoneNumber(phoneNumber)) {
                        onSubmit()
                    }
                }
            ),
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = {
                Text(
                    text = "+63",
                    color = Color.Gray,
                    modifier = Modifier.padding(start = 16.dp)
                )
            },
            isError = phoneNumber.isNotEmpty() && !isValidPhoneNumber(phoneNumber),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = PrimaryColor,
                unfocusedBorderColor = Color.Gray
            )
        )

        if (phoneNumber.isNotEmpty() && !isValidPhoneNumber(phoneNumber)) {
            Text(
                text = "Please enter a valid phone number (+639XXXXXXXXX)",
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
            enabled = isValidPhoneNumber(phoneNumber),
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
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
            for (i in 0..5) {
                OutlinedTextField(
                    value = otpValues[i],
                    onValueChange = { value ->
                        if (value.length <= 1 && value.all { it.isDigit()}) {
                            onOtpChanged(i, value)
                            if (value.isNotEmpty() && i < 5) {
                                focusRequesters[i + 1].requestFocus()
                            } else if (value.isEmpty() && i > 0) {
                                focusRequesters[i - 1].requestFocus()
                            }
                            if (i == 5 && value.isNotEmpty() && otpValues.joinToString("").length == 6) {
                                keyboardController?.hide()
                                onSubmit()
                            }
                        }
                    },
                    modifier = Modifier
                        .width(48.dp)
                        .height(56.dp)
                        .focusRequester(focusRequesters[i]),
                    textStyle = LocalTextStyle.current.copy(
                        textAlign = TextAlign.Center,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = if (i < 5) ImeAction.Next else ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onNext = { if (i < 5) focusRequesters[i + 1].requestFocus() },
                        onDone = {
                            focusManager.clearFocus()
                            if (otpValues.joinToString("").length == 6) onSubmit()
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
            enabled = otpValues.joinToString("").length == 6,
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
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onBackground
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
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Next
            ),
            trailingIcon = {
                IconButton(onClick = onTogglePasswordVisibility) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (passwordVisible) "Hide password" else "Show password",
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
            PasswordStrengthIndicator(password = password, isValid = isPasswordValid)
        }

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = { Text("Confirm Password") },
            singleLine = true,
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    focusManager.clearFocus()
                    keyboardController?.hide()
                    if (isPasswordValid && passwordsMatch) {
                        onSubmit()
                    }
                }
            ),
            trailingIcon = {
                IconButton(onClick = onToggleConfirmPasswordVisibility) {
                    Icon(
                        imageVector = if (confirmPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
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
fun PasswordStrengthIndicator(password: String, isValid: Boolean) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 8.dp)) {
        val hasMinLength = password.length >= 8
        val hasLetterAndDigit = hasLetterAndDigit(password)

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (hasMinLength) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasMinLength) Color.Green else Color.Red,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "At least 8 characters",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasMinLength) Color.Green else Color.Red
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (hasLetterAndDigit) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = null,
                tint = if (hasLetterAndDigit) Color.Green else Color.Red,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Contains letters and numbers",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasLetterAndDigit) Color.Green else Color.Red
            )
        }
    }
}

// Improved functions

fun sendVerificationCode(
    phoneNumber: String,
    auth: FirebaseAuth,
    activity: Activity,
    onCodeSent: (String, PhoneAuthProvider.ForceResendingToken?) -> Unit,
    onError: (String) -> Unit
) {
    if (!isValidPhoneNumber(phoneNumber)) {
        onError("Invalid phone number format. Please use +63XXXXXXXXXX")
        return
    }

    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("ForgotPass", "Verification Completed: ${credential.smsCode}")
            // We don't auto-sign in here as we want the user to set a new password
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("ForgotPass", "Verification Failed: ${e.message}")
            when (e) {
                is FirebaseAuthInvalidCredentialsException -> onError("Invalid phone number. Please check and try again.")
                is FirebaseTooManyRequestsException -> onError("Too many requests. Please try again later.")
                else -> onError("Verification failed: ${e.message}")
            }
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d("ForgotPass", "OTP Sent: $verificationId")
            onCodeSent(verificationId, token)
        }

        override fun onCodeAutoRetrievalTimeOut(verificationId: String) {
            Log.d("ForgotPass", "Code Auto Retrieval Timeout")
        }
    }

    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber.trim())
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(callbacks)
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

fun resendVerificationCode(
    phoneNumber: String,
    resendToken: PhoneAuthProvider.ForceResendingToken?,
    auth: FirebaseAuth,
    activity: Activity,
    onCodeSent: (String, PhoneAuthProvider.ForceResendingToken) -> Unit,
    onError: (String) -> Unit
) {
    val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            Log.d("ForgotPass", "Verification Completed on resend: ${credential.smsCode}")
        }

        override fun onVerificationFailed(e: FirebaseException) {
            Log.e("ForgotPass", "Verification Failed on resend: ${e.message}")
            onError("Failed to resend verification code: ${e.message}")
        }

        override fun onCodeSent(
            verificationId: String,
            token: PhoneAuthProvider.ForceResendingToken
        ) {
            Log.d("ForgotPass", "OTP Resent: $verificationId")
            onCodeSent(verificationId, token)
        }
    }

    val optionsBuilder = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber.trim())
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(callbacks)

    // Use the resend token if available
    resendToken?.let {
        optionsBuilder.setForceResendingToken(it)
    }

    PhoneAuthProvider.verifyPhoneNumber(optionsBuilder.build())
}

fun verifyOTP(
    verificationId: String,
    otp: String,
    auth: FirebaseAuth,
    context: Context,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    if (verificationId.isEmpty()) {
        onError("Verification ID is missing. Please request a new OTP.")
        return
    }

    val credential = PhoneAuthProvider.getCredential(verificationId, otp)

    // We sign in temporarily to verify the OTP
    auth.signInWithCredential(credential)
        .addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Log.d("ForgotPass", "OTP verification successful")
                onSuccess()
            } else {
                Log.e("ForgotPass", "OTP verification failed", task.exception)
                onError("Invalid OTP. Please try again.")
            }
        }
}


fun resetPassword(
    auth: FirebaseAuth,
    newPassword: String,
    context: Context,
    onComplete: () -> Unit,
    onError: (String) -> Unit
) {
    val user = auth.currentUser
    if (user != null) {
        user.updatePassword(newPassword)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d("ForgotPass", "Password updated successfully")
                    onComplete()
                } else {
                    Log.e("ForgotPass", "Password update failed", task.exception)
                    onError("Failed to reset password. Please try again.")
                }
            }
    } else {
        onError("No user signed in. Please verify again.")
    }
}

fun checkPhoneNumberExists(
    phoneNumber: String,
    auth: FirebaseAuth,
    activity: Activity,
    context: Context,
    onSuccess: (String, PhoneAuthProvider.ForceResendingToken?) -> Unit,
    onError: (String) -> Unit
) {
    val firestore = FirebaseFirestore.getInstance()

    firestore.collection("users")
        .whereEqualTo("phoneNumber", phoneNumber)
        .get()
        .addOnSuccessListener { result ->
            if (!result.isEmpty) {
                // Phone number exists, send verification code
                sendVerificationCode(
                    phoneNumber = phoneNumber,
                    auth = auth,
                    activity = activity,
                    onCodeSent = { verificationId, token ->
                        onSuccess(verificationId, token)
                    },
                    onError = { error ->
                        onError(error)
                    }
                )
            } else {
                onError("This phone number is not registered.")
            }
        }
        .addOnFailureListener { e ->
            Log.e("ForgotPass", "Failed to check phone number", e)
            onError("Something went wrong. Please try again.")
        }
}

// Helper functions

fun isValidPhoneNumber(phoneNumber: String): Boolean {
    val regex = Regex("^\\+639\\d{9}$")
    return regex.matches(phoneNumber)
}

fun validatePassword(password: String): Boolean {
    return password.length >= 8 && hasLetterAndDigit(password)
}

fun hasLetterAndDigit(password: String): Boolean {
    val letterPattern = Pattern.compile("[A-Za-z]")
    val digitPattern = Pattern.compile("[0-9]")
    return letterPattern.matcher(password).find() && digitPattern.matcher(password).find()
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