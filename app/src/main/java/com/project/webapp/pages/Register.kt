package com.project.webapp.pages

import android.util.Log
import android.widget.Toast
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.*
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.material3.Checkbox
import androidx.compose.material3.TextButton
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.MutableLiveData
import androidx.navigation.NavController
import com.project.webapp.AuthViewModel
import com.project.webapp.R
import com.project.webapp.Route
import com.project.webapp.popup.Privacy
import com.project.webapp.popup.Terms
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.project.webapp.AuthState


@Composable
fun Register(modifier: Modifier = Modifier, navController: NavController, authViewModel: AuthViewModel) {

    var firstname by remember { mutableStateOf("") }
    var lastname by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmpass by remember { mutableStateOf("") }
    var termsChecked by remember { mutableStateOf(false) }
    var privacyChecked by remember { mutableStateOf(false) }

    var authState = authViewModel.authState.observeAsState()
    val context = LocalContext.current


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

        Text(text = "Register", fontSize = 20.sp, fontWeight = FontWeight.SemiBold)

//Textfields
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 35.dp),
            Arrangement.SpaceBetween
        ) {

            OutlinedTextField(
                modifier = Modifier.width(169.dp),
                value = firstname,
                onValueChange = { firstname = it },
                label = {
                    Text(text = "First Name")
                })

            OutlinedTextField(
                modifier = Modifier.width(169.dp),
                value = lastname,
                onValueChange = { lastname = it },
                label = {
                    Text(text = "Last Name")
                })
        }

        OutlinedTextField(
            modifier = Modifier.width(340.dp),
            value = email,
            onValueChange = { email = it },
            label = { Text(text = "Email Address") })

        OutlinedTextField(
            modifier = Modifier.width(340.dp),
            value = address,
            onValueChange = { address = it },
            label = { Text(text = "Address") })

        OutlinedTextField(
            modifier = Modifier.width(340.dp),
            value = phoneNumber,
            onValueChange = { phoneNumber = it },
            label = { Text(text = "Contact Number") })

        OutlinedTextField(
            modifier = Modifier.width(340.dp),
            value = password,
            onValueChange = { password = it },
            label = { Text(text = "Password") },
            visualTransformation = PasswordVisualTransformation()
        )
        OutlinedTextField(
            modifier = Modifier.width(340.dp),
            value = confirmpass,
            onValueChange = { confirmpass = it },
            label = { Text(text = "Confirm Password") },
            visualTransformation = PasswordVisualTransformation()
        )


        Spacer(modifier = Modifier.height(16.dp))
//Checkbox
        Row(verticalAlignment = Alignment.CenterVertically) {
            TermsAndCondition("Accept Terms & Conditions", termsChecked) { termsChecked = it }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            DataPrivacy("Data Priacy Consent", privacyChecked) { privacyChecked = it }
        }


        Spacer(modifier = Modifier.height(16.dp))

            Button(onClick = {
                authViewModel.signup(email, password, firstname, lastname, address, phoneNumber, confirmpass) //dito pwede mailagay yung database
            }, enabled = authState != AuthState.Loading
            ) {
                Text(text = "Register")
            }
                if (authState.value is AuthState.Error) {
                    Text(
                        text = (authState.value as AuthState.Error).message,
                        color = Color.Red,
                        modifier = Modifier.padding(top = 8.dp)
                    )
        }
        Button(onClick = {
            navController.navigate(Route.login)
        }) {
            Text(text = "Back to Login")

        }

    }

}

@Composable
fun TermsAndCondition(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        Terms(onDismiss = { showDialog = false })
    }

    Row(
        verticalAlignment = Alignment.Top, // Align checkbox with text top
        modifier = Modifier.padding(1.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.width(2.dp)// Adjust for alignment
        )
        TextButton(onClick = { showDialog = true }) {
            Text(text = "Terms And Conditions", modifier = Modifier.padding(start = 1.dp))
        }
    }
}

@Composable
fun DataPrivacy(label: String, isChecked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }

    if (showDialog) {
        Privacy(onDismiss = { showDialog = false })
    }

    Row(
        verticalAlignment = Alignment.Top, // Align checkbox with text top
        modifier = Modifier.padding(2.dp)
    ) {
        Checkbox(
            checked = isChecked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.width(50.dp)
        )
        TextButton(onClick = {showDialog = true}) {
            Text(text = "By checking the box, you confirm that you have read, understood, " +
                    "and agreed to the Data Privacy Act of 2012 and consent to the collection and processing of your " +
                    "personal data in accordance with its provisions.",
            modifier = Modifier.padding(start = 1.dp))
        }
    }
}









