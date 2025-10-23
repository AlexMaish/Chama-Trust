package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.Common.Resource
import com.example.chamabuddy.R
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel



@Composable
fun LoginScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginSuccess: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val loginState by viewModel.loginState.collectAsState()

    var identifier by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SoftOffWhite)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(280.dp)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(PremiumNavy, PremiumNavy.copy(alpha = 0.8f)),
                        startY = 0f,
                        endY = 500f
                    ),
                    shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                )
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .align(Alignment.Center),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_chama_logo),
                    contentDescription = "App Logo",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(SoftOffWhite)
                        .padding(16.dp),
                    contentScale = ContentScale.Fit
                )

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Welcome Back",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = PremiumNavy
                )

                Text(
                    text = "Sign in to continue",
                    style = MaterialTheme.typography.bodyMedium,
                    color = PremiumNavy.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                OutlinedTextField(
                    value = identifier,
                    onValueChange = { identifier = it },
                    label = { Text("Phone Number") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Email, contentDescription = "Email") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftOffWhite,
                        unfocusedContainerColor = SoftOffWhite,
                        focusedIndicatorColor = PremiumNavy,
                        unfocusedIndicatorColor = PremiumNavy.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = "Password") },
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = SoftOffWhite,
                        unfocusedContainerColor = SoftOffWhite,
                        focusedIndicatorColor = PremiumNavy,
                        unfocusedIndicatorColor = PremiumNavy.copy(alpha = 0.5f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(24.dp))

                when (loginState) {
                    is Resource.Loading -> CircularProgressIndicator(color = VibrantOrange)
                    is Resource.Error -> Text(
                        text = (loginState as Resource.Error).message ?: "Login failed",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    is Resource.Success -> {
                        LaunchedEffect(loginState) {
                            (loginState as Resource.Success).data?.let { user ->
                                onLoginSuccess(user.userId)
                            }
                        }
                    }
                    else -> {}
                }

                Button(
                    onClick = {
                        viewModel.loginUser(identifier.trim(), password)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Login", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text("Don't have an account? ", color = PremiumNavy.copy(alpha = 0.7f))
                    TextButton(onClick = onNavigateToRegister) {
                        Text("Register", color = PremiumNavy, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}