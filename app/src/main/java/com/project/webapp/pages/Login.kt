package com.project.webapp.pages

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.app.PendingIntent
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.project.webapp.AuthState
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route


@Composable
fun Login(modifier: Modifier = Modifier,navController: NavController, authViewModel: AuthViewModel) {

    var email by remember {
        mutableStateOf("")
    }
    var password by remember {
        mutableStateOf("")
    }

    var authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current
    val activity = context as Activity

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                val credential = GoogleAuthProvider.getCredential(account.idToken, null)
                authViewModel.googleSignIn(credential)
            } catch (e: ApiException) {
                Log.e("GoogleSignIn", "Google sign-in failed: ${e.message}")
            }
        }
    }

    fun startGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(context.getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        val googleSignInClient = GoogleSignIn.getClient(context, gso)

        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent

            try {
                val pendingIntent = PendingIntent.getActivity(
                    context, 0, signInIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
                )

                val intentSender = pendingIntent.intentSender  // ✅ Correct way to get IntentSender
                val intentSenderRequest = IntentSenderRequest.Builder(intentSender).build()
                googleSignInLauncher.launch(intentSenderRequest)  // ✅ Correct launching method

            } catch (e: Exception) {
                Log.e("GoogleSignIn", "Error launching Google Sign-In: ${e.message}")
            }
        }
    }


    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.Authenticated -> navController.navigate("dashboard")
            is AuthState.Error -> Toast.makeText(
                context,
                (authState.value as AuthState.Error).message, Toast.LENGTH_SHORT).show()

            else -> Unit
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally

    ) {
        Image(
            painter = painterResource(id = R.drawable.logo), contentDescription = "login image",
            modifier = Modifier.fillMaxWidth().size(80.dp)
        )

        Text(text = "Supporting farmers, reducing waste! ",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable {})


        Spacer(modifier = Modifier.padding(vertical = 25.dp))

        Text(text = "Login", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(value = email, onValueChange = {
            email = it
        }, label = {
            Text(text = "Email Address")
        })

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(value = password, onValueChange = {
            password = it
        }, label = {
            Text(text = "Password")
        }, visualTransformation = PasswordVisualTransformation())

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                authViewModel.login(email, password) //dito pwede mailagay yung database
            },
            enabled = authState != AuthState.Loading
        ) {
            Text(text = "Login")
        }

        if (authState.value is AuthState.Error) {
            Text(
                text = (authState.value as AuthState.Error).message,
                color = Color.Red,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        TextButton(onClick = {
            navController.navigate(Route.forgot)
        }) {
            Text(text = "Forgot Password?")
        }

        TextButton(onClick = {
            navController.navigate(Route.register)
        }) {
            Text(text = "Don't have an account? Register here.", fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(text = "or Sign in with ")

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 120.dp, vertical = 20.dp),
            Arrangement.SpaceEvenly
        ) {

            GoogleSignInButton { startGoogleSignIn() }

            Image(painter = painterResource(id = R.drawable.facebook),
                contentDescription = "Facebook",
                modifier = Modifier
                    .size(40.dp)
                    .clickable {

                    }
            )
        }
    }
}
@Composable
fun GoogleSignInButton(onClick: () -> Unit) {
    Image(
        painter = painterResource(id = R.drawable.google),
        contentDescription = "Google",
        modifier = Modifier
            .size(40.dp)
            .clickable { onClick() }
    )
}


