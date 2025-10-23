package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.R
import com.example.chamabuddy.presentation.viewmodel.CreateCycleViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateCycleScreen(
    navController: NavHostController,
    navigateBack: () -> Unit,
    groupId: String
) {
    val viewModel: CreateCycleViewModel = hiltViewModel()
    val uiState by viewModel.uiState.collectAsState()
    val isCreating by viewModel.isCreating.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val creationSuccess by viewModel.creationSuccess.collectAsState()

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(groupId) {
        viewModel.setGroupId(groupId)
    }

    LaunchedEffect(creationSuccess) {
        if (creationSuccess) {
            navController.previousBackStackEntry?.savedStateHandle?.set("cycle_created", true)
            navigateBack()
        }
    }

    LaunchedEffect(errorMessage) {
        errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearErrorMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(stringResource(R.string.create_new_cycle)) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    if (!isCreating) {
                        viewModel.createCycle()
                    }
                },
                icon = {
                    if (isCreating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Check, contentDescription = "Create")
                    }
                },
                text = { Text(if (isCreating) "Creating..." else "Create Cycle") },
                modifier = Modifier.padding(16.dp)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            OutlinedTextField(
                value = uiState.weeklyAmount.toString(),
                onValueChange = { viewModel.updateWeeklyAmount(it) },
                label = { Text("Weekly Contribution (KES)") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.monthlySavingsAmount.toString(),
                onValueChange = { viewModel.updateMonthlySavingsAmount(it) },
                label = { Text("Monthly Savings (KES)") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            OutlinedTextField(
                value = uiState.beneficiariesPerMeeting.toString(),
                onValueChange = { viewModel.updateBeneficiariesPerMeeting(it) },
                label = { Text("Beneficiaries per Meeting") },
                keyboardOptions = KeyboardOptions.Default.copy(
                    keyboardType = KeyboardType.Number
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            DatePickerCard(
                selectedDate = uiState.startDate,
                onDateSelected = { viewModel.updateStartDate(it) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerCard(
    selectedDate: Long,
    onDateSelected: (Long) -> Unit
) {
    var showDatePicker by remember { mutableStateOf(false) }
    val dateFormatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }
    val formattedDate = dateFormatter.format(Date(selectedDate))

    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text("Start Date", style = MaterialTheme.typography.labelLarge)
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(onClick = { showDatePicker = true }) {
                Text(formattedDate, style = MaterialTheme.typography.bodyLarge)
            }
        }
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let {
                            onDateSelected(it)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}