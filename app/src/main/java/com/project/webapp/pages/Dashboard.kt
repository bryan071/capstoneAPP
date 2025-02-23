package com.project.webapp.pages

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.project.webapp.AuthState
import com.project.webapp.AuthViewModel
import com.project.webapp.R


@Composable
fun Dashboard(modifier: Modifier = Modifier,navController: NavController, authViewModel: AuthViewModel){

    var authState = authViewModel.authState.observeAsState()

    LaunchedEffect(authState.value) {
        when (authState.value) {
            is AuthState.UnAuthenticated -> navController.navigate("login")
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
            modifier = Modifier.fillMaxWidth().size(80.dp).clickable {}
        )
        Text(text = "Supporting farmers, reducing waste! ",
            fontSize = 23.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center)


        Spacer(modifier = Modifier.padding(vertical =25.dp))
        Text(text = "Dashboard")
        Spacer(modifier = Modifier.padding(vertical =25.dp))

        TextButton(onClick = {
            authViewModel.logout()
        }) {
            Text( text = "Log out")
        }

    }
    
}
