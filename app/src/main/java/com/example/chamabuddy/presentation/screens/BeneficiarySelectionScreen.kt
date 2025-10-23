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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.navigation.CycleDetailDestination
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
import com.example.chamabuddy.presentation.viewmodel.MeetingState
import com.example.chamabuddy.presentation.viewmodel.MeetingViewModel
import kotlinx.coroutines.launch

object BeneficiarySelectionDestination : NavigationDestination {
    override val route = "beneficiary_selection"
    override val title = "Select Beneficiaries"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$meetingIdArg}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiarySelectionScreen(
    meetingId: String,
    navigateBack: () -> Unit,
    navController: NavHostController,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val beneficiaryState by viewModel.beneficiaryState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val maxBeneficiaries = beneficiaryState.maxBeneficiaries
    var selectedBeneficiaries by remember { mutableStateOf<List<Member>>(emptyList()) }
    val coroutineScope = rememberCoroutineScope()


    LaunchedEffect(beneficiaryState.existingBeneficiaries) {
        if (beneficiaryState.existingBeneficiaries.isNotEmpty()) {
            selectedBeneficiaries = beneficiaryState.existingBeneficiaries
        }
    }

    LaunchedEffect(meetingId) {
        viewModel.handleEvent(MeetingEvent.LoadEligibleBeneficiaries(meetingId))
    }
    val meetingState by viewModel.state.collectAsState()

    LaunchedEffect(meetingState) {
        when (val s = meetingState) {
            is MeetingState.BeneficiariesSelected -> {
                if (s.success) {

                    s.meeting?.let { meeting ->
                        navController.navigate("${CycleDetailDestination.route}/${meeting.groupId}/${meeting.cycleId}") {
                            popUpTo(BeneficiarySelectionDestination.route) { inclusive = true }
                        }
                    }

//                    snackbarHostState.showSnackbar("Beneficiaries Saved Successfully")
                }
                viewModel.handleEvent(MeetingEvent.ResetMeetingState)
            }
            else -> {}
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Select Beneficiaries") },
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
                            viewModel.handleEvent(
                                MeetingEvent.ConfirmBeneficiarySelection(
                                    meetingId,
                                    selectedBeneficiaries.map { it.memberId }
                                )
                            )

                            snackbarHostState.showSnackbar("Beneficiaries Saved Successfully")
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
                beneficiaryState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                beneficiaryState.error != null -> {
                    Text(
                        text = "Error: ${beneficiaryState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                beneficiaryState.eligibleMembers.isEmpty() &&
                        beneficiaryState.existingBeneficiaries.isEmpty() -> {
                    Text(
                        text = "No eligible beneficiaries available",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            "Select up to $maxBeneficiaries beneficiaries for this meeting",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )
                        Text(
                            "Existing selections are pre-selected",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (beneficiaryState.existingBeneficiaries.isNotEmpty()) {
                                item {
                                    Text(
                                        "Current Beneficiaries",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                items(beneficiaryState.existingBeneficiaries) { member ->
                                    BeneficiaryCandidateItem(
                                        member = member,
                                        isSelected = selectedBeneficiaries.contains(member),
                                        onSelect = {
                                            selectedBeneficiaries = selectedBeneficiaries - member
                                        }
                                    )
                                }
                            }

                            if (beneficiaryState.eligibleMembers.isNotEmpty()) {
                                item {
                                    Text(
                                        "Eligible Members",
                                        style = MaterialTheme.typography.titleSmall,
                                        modifier = Modifier.padding(16.dp)
                                    )
                                }
                                items(beneficiaryState.eligibleMembers) { member ->
                                    BeneficiaryCandidateItem(
                                        member = member,
                                        isSelected = selectedBeneficiaries.contains(member),
                                        onSelect = {
                                            if (selectedBeneficiaries.size < maxBeneficiaries) {
                                                selectedBeneficiaries = selectedBeneficiaries + member
                                            } else {
                                                coroutineScope.launch {
                                                    snackbarHostState.showSnackbar(
                                                        "Max $maxBeneficiaries beneficiaries allowed"
                                                    )
                                                }
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
    }
}
@Composable
fun BeneficiaryCandidateItem(
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
