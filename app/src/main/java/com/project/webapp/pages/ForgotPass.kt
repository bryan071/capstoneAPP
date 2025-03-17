package com.project.webapp.pages

import android.app.Activity
import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.dashboards.TopBar
import java.util.concurrent.TimeUnit

@Composable
fun ForgotPass(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel,
    activity: Activity
) {
    var phoneNumber by remember { mutableStateOf("") }
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(1) }
    var verificationId by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) } // Stores error messages
    val auth = FirebaseAuth.getInstance()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TopBar()
        Text(
            text = "Supporting farmers, reducing waste!",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(25.dp))

        Text("Forgot Password", fontSize = 20.sp, fontWeight = FontWeight.Bold)

        Spacer(modifier = Modifier.height(20.dp))

        when (step) {
            1 -> PhoneStep(phoneNumber, { phone ->
                phoneNumber = phone
                checkPhoneNumberExists(phone, auth, activity, { id ->
                    verificationId = id
                    step = 2
                }) { error ->
                    errorMessage = error
                }
            }, navController)
            2 -> OTPStep(otp, { enteredOtp ->
                otp = enteredOtp
                verifyOTP(verificationId, otp, auth) {
                    step = 3
                }
            }, navController)
            3 -> ResetPasswordStep(newPassword, { password ->
                newPassword = password
                resetPassword(auth, password) {
                    navController.navigate(Route.LOGIN)
                }
            }, navController)
        }
    }

    // Show alert dialog if there is an error
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
fun PhoneStep(phoneNumber: String, onNext: (String) -> Unit, navController: NavController) {
    var phone by remember { mutableStateOf(if (phoneNumber.startsWith("+63")) phoneNumber else "+63") }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = phone,
            onValueChange = { input ->
                phone = if (input.startsWith("+63")) input else "+63" + input.trimStart('0')
            },
            label = { Text("Phone Number") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = {
            if (phone.length >= 12) { // Valid PH number with +63
                onNext(phone)
            } else {
                Log.e("PhoneStep", "Invalid phone number format.")
            }
        }) {
            Text("Send OTP")
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { navController.navigate(Route.LOGIN) }) {
            Text("Back to Login")
        }
    }
}

@Composable
fun OTPStep(otp: String, onVerify: (String) -> Unit, navController: NavController) {
    var enteredOtp by remember { mutableStateOf(otp) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = enteredOtp,
            onValueChange = { enteredOtp = it },
            label = { Text("Enter OTP") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { onVerify(enteredOtp) }) {
            Text("Verify OTP")
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { navController.navigate(Route.LOGIN) }) {
            Text("Back to Login")
        }
    }
}

@Composable
fun ResetPasswordStep(newPassword: String, onReset: (String) -> Unit, navController: NavController) {
    var password by remember { mutableStateOf(newPassword) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("New Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        Spacer(modifier = Modifier.height(10.dp))
        Button(onClick = { onReset(password) }) {
            Text("Reset Password")
        }
        Spacer(modifier = Modifier.height(10.dp))
        TextButton(onClick = { navController.navigate(Route.LOGIN) }) {
            Text("Back to Login")
        }
    }
}

fun sendVerificationCode(
    phoneNumber: String,
    auth: FirebaseAuth,
    activity: Activity,
    onCodeSent: (String) -> Unit
) {
    if (!phoneNumber.startsWith("+") || phoneNumber.length < 10) {
        Log.e("sendVerificationCode", "Invalid phone number format: $phoneNumber")
        return
    }

    val options = PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber.trim()) // Trim whitespace
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(activity)
        .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
            override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                Log.d("ForgotPass", "Verification Completed: ${credential.smsCode}")
            }

            override fun onVerificationFailed(e: FirebaseException) {
                Log.e("ForgotPass", "Verification Failed: ${e.message}")
            }

            override fun onCodeSent(
                verificationId: String,
                token: PhoneAuthProvider.ForceResendingToken
            ) {
                Log.d("ForgotPass", "OTP Sent: $verificationId")
                onCodeSent(verificationId)
            }
        })
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
}

fun verifyOTP(verificationId: String, otp: String, auth: FirebaseAuth, onSuccess: () -> Unit) {
    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
    auth.signInWithCredential(credential).addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("ForgotPass", "OTP Verified Successfully")
            onSuccess()
        } else {
            Log.e("ForgotPass", "OTP Verification Failed: ${task.exception?.message}")
        }
    }
}

fun resetPassword(auth: FirebaseAuth, newPassword: String, onComplete: () -> Unit) {
    auth.currentUser?.updatePassword(newPassword)?.addOnCompleteListener { task ->
        if (task.isSuccessful) {
            Log.d("ForgotPass", "Password Reset Successful")
            onComplete()
        } else {
            Log.e("ForgotPass", "Password Reset Failed: ${task.exception?.message}")
        }
    }
}

fun checkPhoneNumberExists(
    phoneNumber: String,
    auth: FirebaseAuth,
    activity: Activity,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit
) {
    val db = FirebaseFirestore.getInstance()
    val formattedPhone = phoneNumber.trim() // Remove spaces

    Log.d("FirestoreCheck", "Checking phone number: $formattedPhone") // Debugging

    db.collection("users")
        .whereEqualTo("phoneNumber", formattedPhone)
        .get()
        .addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                Log.d("FirestoreCheck", "Phone number found in Firestore!")
                sendVerificationCode(formattedPhone, auth, activity, onSuccess)
            } else {
                Log.e("FirestoreCheck", "Phone number NOT found in Firestore.")
                onError("This phone number is not linked to any account.")
            }
        }
        .addOnFailureListener {
            Log.e("FirestoreCheck", "Firestore query failed: ${it.message}")
            onError("Failed to check phone number: ${it.message}")
        }
}

