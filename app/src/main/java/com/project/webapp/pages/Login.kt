package com.project.webapp.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import com.project.webapp.Viewmodel.AuthState
import com.project.webapp.Viewmodel.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route


@Composable
fun Login(
    modifier: Modifier = Modifier,
    navController: NavController,
    authViewModel: AuthViewModel
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    val authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current

    LaunchedEffect(authState.value) {
        when (val state = authState.value) {
            is AuthState.Authenticated -> {
                val normalizedUserType = state.userType.trim().lowercase()
                when (normalizedUserType) {
                    "farmer" -> navController.navigate(Route.FARMER_DASHBOARD)
                    "market" -> navController.navigate(Route.MARKET_DASHBOARD)
                    "organization" -> navController.navigate(Route.ORG_DASHBOARD)
                    else -> Toast.makeText(context, "User type not found: $normalizedUserType", Toast.LENGTH_SHORT).show()
                }
            }
            is AuthState.Error -> Toast.makeText(
                context, state.message, Toast.LENGTH_SHORT
            ).show()
            else -> Unit
        }
    }



    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
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
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.padding(vertical = 25.dp))

        Text(text = "Login", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Email Address") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            visualTransformation = PasswordVisualTransformation()
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = { authViewModel.login(email, password) },
            enabled = authState.value !is AuthState.Loading,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0DA54B))
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

        TextButton(onClick = { navController.navigate(Route.FORGOT_PASSWORD) }) {
            Text(text = "Forgot Password?", color = Color.Black)
        }

        TextButton(onClick = { navController.navigate(Route.REGISTER) }) {
            Text(text = "Don't have an account? Register here.", fontSize = 15.sp, color = Color.Black)
        }
    }
}





