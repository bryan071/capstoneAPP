package com.project.webapp

import CartViewModel
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import com.project.webapp.Viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val authViewModel : AuthViewModel by viewModels()

        setContent {
            Scaffold (modifier = Modifier.fillMaxSize()){
                innerPadding -> AppNav(modifier = Modifier.padding(innerPadding),
                authViewModel = authViewModel, cartViewModel = CartViewModel())
            }
        }
    }
}