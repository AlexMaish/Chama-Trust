package com.example.chamabuddy.presentation.screens


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.viewmodel.WelfareMeetingViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelfareBeneficiarySelectionScreen(
    meetingId: String,
    navigateBack: () -> Unit,
    navController: NavHostController,
    viewModel: WelfareMeetingViewModel = hiltViewModel()
) {
    val beneficiaryState by viewModel.beneficiaryState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var selectedBeneficiaries by remember { mutableStateOf<List<Member>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(meetingId) {
        viewModel.loadAllMembersForBeneficiarySelection(meetingId)
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Select Welfare Beneficiaries") },
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
                    if (selectedBeneficiaries.isEmpty()) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar("Please select at least one beneficiary")
                        }
                    } else {
                        coroutineScope.launch {
                            viewModel.confirmBeneficiarySelection(
                                meetingId,
                                selectedBeneficiaries.map { it.memberId }
                            )
                            snackbarHostState.showSnackbar("Beneficiaries Saved Successfully")
                            navigateBack()
                        }
                    }
                },
                icon = { Icon(Icons.Default.Check, contentDescription = "Save") },
                text = { Text("Save Beneficiaries") }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            when {
                beneficiaryState.isLoading -> CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                beneficiaryState.error != null -> Text(
                    text = "Error: ${beneficiaryState.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                beneficiaryState.eligibleMembers.isEmpty() -> Text(
                    text = "No members available",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> Column(modifier = Modifier.fillMaxSize()) {
                    Text(
                        "Select beneficiaries for this welfare",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(beneficiaryState.eligibleMembers) { member ->
                            WelfareBeneficiaryCandidateItem(
                                member = member,
                                isSelected = selectedBeneficiaries.contains(member),
                                onSelect = {
                                    if (selectedBeneficiaries.contains(member)) {
                                        selectedBeneficiaries = selectedBeneficiaries - member
                                    } else {
                                        selectedBeneficiaries = selectedBeneficiaries + member
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun WelfareBeneficiaryCandidateItem(
    member: Member,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = onSelect
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column {
                Text(
                    text = member.name,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = member.phoneNumber,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}