package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onLoadingComplete: () -> Unit
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()

        LaunchedEffect(Unit) {
            delay(2000) // Simulate loading time
            onLoadingComplete()
        }
    }
}

@Preview
@Composable
fun SplashScreenPreview() {
    SplashScreen(onLoadingComplete = {})
}