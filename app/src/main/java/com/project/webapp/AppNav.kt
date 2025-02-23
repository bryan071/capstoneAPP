package com.project.webapp

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.project.webapp.pages.Dashboard
import com.project.webapp.pages.ForgotPass
import com.project.webapp.pages.Login
import com.project.webapp.pages.Register



@Composable
fun AppNav(modifier: Modifier = Modifier, authViewModel: AuthViewModel){

    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Route.login, builder = {
        composable(Route.login,){
            Login(modifier, navController, authViewModel)
        }
        composable(Route.register,){
            Register(modifier, navController, authViewModel)
        }
        composable(Route.forgot,){
            ForgotPass(modifier, navController, authViewModel)
        }
        composable(Route.dashboard,){
            Dashboard(modifier, navController, authViewModel)
        }
    })
}