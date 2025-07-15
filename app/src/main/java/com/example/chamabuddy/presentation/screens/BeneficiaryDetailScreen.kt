package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
import com.example.chamabuddy.presentation.viewmodel.MeetingState
import com.example.chamabuddy.presentation.viewmodel.MeetingViewModel
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryDetailScreen(
    beneficiaryId: String,
    navigateBack: () -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    var adjustedAmount by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    LaunchedEffect(beneficiaryId) {
        viewModel.handleEvent(MeetingEvent.LoadBeneficiaryDetails(beneficiaryId))
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
    ) { paddingValues ->
        when (val currentState = state) {
            is MeetingState.Loading ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator()
                }

            is MeetingState.Error ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = currentState.message)
                }

            is MeetingState.BeneficiaryDetails -> {
                val beneficiary = currentState.beneficiary
                val meeting = currentState.meeting
                val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

                // Initialize amount
                LaunchedEffect(beneficiary) {
                    adjustedAmount = beneficiary.amountReceived.toString()
                }

                // Fetch beneficiary count
                val beneficiaryCount by produceState(initialValue = 1) {
                    value = viewModel.getBeneficiaryCountForMeeting(beneficiary.meetingId)
                }

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(paddingValues)
                ) {
                    // Beneficiary details
                    Text(
                        text = "Payment Order: ${beneficiary.paymentOrder}",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Date Awarded: ${dateFormat.format(beneficiary.dateAwarded)}",
                        style = MaterialTheme.typography.bodyLarge
                    )

                    Spacer(Modifier.height(24.dp))

                    // Editable amount field
                    OutlinedTextField(
                        value = adjustedAmount,
                        onValueChange = { adjustedAmount = it },
                        label = { Text("Amount Received") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Default amount info
                    meeting?.let { nonNullMeeting ->
                        // Only calculate if we have valid values
                        if (nonNullMeeting.totalCollected > 0 && beneficiaryCount > 0) {
                            val defaultAmount = nonNullMeeting.totalCollected / beneficiaryCount
                            Text(
                                text = "Default amount: $defaultAmount (Total: ${nonNullMeeting.totalCollected} ÷ $beneficiaryCount)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    // Save button
                    Button(
                        onClick = {
                            try {
                                val newAmount = adjustedAmount.toInt()
                                isEditing = true
                                viewModel.handleEvent(
                                    MeetingEvent.UpdateBeneficiaryAmount(beneficiaryId, newAmount)
                                )
                            } catch (e: NumberFormatException) {
                                // Handle invalid input
                            }
                        },
                        enabled = !isEditing,
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        if (isEditing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Update Amount")
                        }
                    }
                }
            }

            else -> {
                // Handle other states or idle state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("No beneficiary data available")
                }
            }
        }
    }
}

