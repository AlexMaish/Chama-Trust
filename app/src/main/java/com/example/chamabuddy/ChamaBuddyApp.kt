// ChamaBuddyApp.kt
package com.example.chamabuddy

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import com.example.chamabuddy.presentation.MainScreen
import com.example.chamabuddy.presentation.navigation.AuthNavHost
import com.example.chamabuddy.presentation.screens.SplashScreen
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.SyncViewModel
import com.example.chamabuddy.ui.theme.ChamaBuddyTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ChamaBuddyApp : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChamaBuddyTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()
                    var showSplash by remember { mutableStateOf(true) }
                    var isAuthenticated by remember { mutableStateOf(false) }

                    val syncViewModel: SyncViewModel = hiltViewModel()

                    val isOnline by syncViewModel.isOnline.collectAsState()
                    LaunchedEffect(isOnline) {
                        if (isOnline) {
                            syncViewModel.triggerSync()
                        }
                    }

                    if (showSplash) {
                        SplashScreen {
                            showSplash = false
                        }
                    } else {
                        if (isAuthenticated) {
                            MainScreen()
                        } else {
                            AuthNavHost(
                                navController = navController,
                                onAuthenticationSuccess = {
                                    isAuthenticated = true
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
