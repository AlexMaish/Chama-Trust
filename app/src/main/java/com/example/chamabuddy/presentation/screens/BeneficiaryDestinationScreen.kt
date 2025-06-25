package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.presentation.navigation.BeneficiaryDestination
import com.example.chamabuddy.presentation.navigation.BeneficiaryDetailDestination
import com.example.chamabuddy.presentation.viewmodel.BeneficiaryState
import com.example.chamabuddy.presentation.viewmodel.BeneficiaryViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryDetailDestination(
    beneficiaryId: String,
    navigateBack: () -> Unit,
    viewModel: BeneficiaryViewModel = hiltViewModel()
) {

    val state by viewModel.state.collectAsState(initial = BeneficiaryState.Loading)

    LaunchedEffect(beneficiaryId) {
        viewModel.loadBeneficiaryDetails(beneficiaryId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Beneficiary Details") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        when (val currentState = state) {
            is BeneficiaryState.Loading ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }

            is BeneficiaryState.Error ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(currentState.message)
                }

            is BeneficiaryState.Success -> {
                val beneficiary = currentState.beneficiary
                val member = currentState.member
                val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

                Column(Modifier.padding(16.dp)) {
                    Text(
                        "Member: ${member?.name ?: "Unknown"}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                    Spacer(Modifier.height(16.dp))
                    Text("Amount Received: ${beneficiary.amountReceived}", style = MaterialTheme.typography.bodyLarge)
                    Text("Date Awarded: ${dateFormat.format(beneficiary.dateAwarded)}")
                    Text("Payment Order: ${beneficiary.paymentOrder}")
                }
            }
        }
    }
}