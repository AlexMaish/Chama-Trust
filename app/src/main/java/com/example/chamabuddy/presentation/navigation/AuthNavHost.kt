package com.example.chamabuddy.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.example.chamabuddy.presentation.screens.LoginScreen
import com.example.chamabuddy.presentation.screens.RegisterScreen
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel

@Composable
fun AuthNavHost(
    navController: NavHostController,
    onAuthenticationSuccess: (String) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = LoginDestination.route
    ) {
        composable(route = LoginDestination.route) {
            LoginScreen(
                onLoginSuccess = { userId ->
                    onAuthenticationSuccess(userId)
                },
                onNavigateToRegister = {
                    navController.navigate(RegisterDestination.route)
                }
            )
        }

        composable(route = RegisterDestination.route) {
            RegisterScreen(
                onRegisterSuccess = { userId ->
                    onAuthenticationSuccess(userId)
                },
                onNavigateToLogin = {
                    navController.popBackStack()
                }
            )
        }
    }
}