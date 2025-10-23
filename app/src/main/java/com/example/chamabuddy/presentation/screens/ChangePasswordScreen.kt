package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.Common.Resource
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangePasswordScreen(
    viewModel: AuthViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    var oldPassword by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    val changePasswordState by viewModel.changePasswordState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.clearChangePasswordState()
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Change Password") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
        ) {
            OutlinedTextField(
                value = oldPassword,
                onValueChange = { oldPassword = it },
                label = { Text("Current Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SoftOffWhite,
                    unfocusedContainerColor = SoftOffWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = newPassword,
                onValueChange = { newPassword = it },
                label = { Text("New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SoftOffWhite,
                    unfocusedContainerColor = SoftOffWhite
                )
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = confirmPassword,
                onValueChange = { confirmPassword = it },
                label = { Text("Confirm New Password") },
                visualTransformation = PasswordVisualTransformation(),
                modifier = Modifier.fillMaxWidth(),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SoftOffWhite,
                    unfocusedContainerColor = SoftOffWhite
                ),
                isError = newPassword != confirmPassword && confirmPassword.isNotEmpty()
            )

            if (newPassword != confirmPassword && confirmPassword.isNotEmpty()) {
                Text(
                    "Passwords don't match",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            when (changePasswordState) {
                is Resource.Loading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                is Resource.Error -> Text(
                    text = (changePasswordState as Resource.Error).message ?: "Error",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                is Resource.Success -> {
                    Text(
                        text = "Password changed successfully!",
                        color = Color.Green,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        textAlign = TextAlign.Center
                    )
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (newPassword != confirmPassword) {
                        return@Button
                    }
                    viewModel.changePassword(oldPassword, newPassword)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                enabled = oldPassword.isNotBlank() &&
                        newPassword.isNotBlank() &&
                        confirmPassword.isNotBlank() &&
                        newPassword == confirmPassword,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
            ) {
                Text("Change Password", fontWeight = FontWeight.Bold)
            }
        }
    }
}
