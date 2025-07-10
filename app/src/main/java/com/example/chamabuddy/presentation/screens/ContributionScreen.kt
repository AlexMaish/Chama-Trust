package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
import com.example.chamabuddy.presentation.viewmodel.MeetingState
import com.example.chamabuddy.presentation.viewmodel.MeetingViewModel
import kotlinx.coroutines.launch

object ContributionDestination : NavigationDestination {
    override val route = "contribution"
    override val title = "Record Contributions"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$meetingIdArg}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionScreen(
    meetingId: String,
    navigateToBeneficiarySelection: () -> Unit,
    navigateBack: () -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val state by viewModel.contributionState.collectAsState()
    val meetingState by viewModel.state.collectAsState()
    val isNextEnabled = state.members.isNotEmpty() && !state.isLoading && state.error == null
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }



    LaunchedEffect(meetingState) {
        when (val s = meetingState) {
            is MeetingState.ContributionRecorded -> {
                if (s.success) {
                    val status = viewModel.getMeetingStatus(meetingId)

                    // Force navigation if beneficiaries not selected or incomplete
                    if (!status.beneficiariesSelected ||
                        status.beneficiaryCount < status.requiredBeneficiaryCount) {

                        snackbarHostState.showSnackbar("Please select beneficiaries")
                        navigateToBeneficiarySelection()
                    } else {
                        navigateBack()
                    }
                }
            }
            is MeetingState.BeneficiariesSelected -> {
                if (s.success) {
                    // After saving beneficiaries, navigate back to cycle detail
                    navigateBack()
                }
            }
            is MeetingState.Error -> {
                snackbarHostState.showSnackbar("Error: ${s.message}")
            }
            else -> {}
        }
    }

    LaunchedEffect(meetingId) {
        viewModel.handleEvent(MeetingEvent.GetContributionsForMeeting(meetingId))
    }

    // Handle meeting state changes
    LaunchedEffect(meetingState) {
        when (val s = meetingState) {
            is MeetingState.ContributionRecorded -> {
                if (s.success) {
                    // Check if beneficiaries are selected
                    val status = viewModel.getMeetingStatus(meetingId)
                    if (status.beneficiariesSelected) {
                        // Navigate back to cycle detail
                        navigateBack()
                    } else {
                        // Show message and navigate to beneficiary selection
                        snackbarHostState.showSnackbar("Select beneficiaries first to save meeting")
                        navigateToBeneficiarySelection()
                    }
                }
            }
            is MeetingState.Error -> {
                snackbarHostState.showSnackbar("Error: ${s.message}")
            }
            else -> {}
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSavedContributionState()?.let { savedState ->
            viewModel.restoreContributions(savedState)
        } ?: viewModel.handleEvent(
            MeetingEvent.GetContributionsForMeeting(meetingId)
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(ContributionDestination.title) },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    Button(
                        onClick = {
                            if (isNextEnabled) {
                                viewModel.saveContributionState(state.contributions)
                                navigateToBeneficiarySelection()
                            }
                        },
                        enabled = isNextEnabled,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryContainer,
                            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
                        ),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 4.dp,
                            pressedElevation = 8.dp
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.CheckCircle,
                                contentDescription = "Select Beneficiaries",
                                modifier = Modifier.size(18.dp)
                            )
                            Text("Select Beneficiaries")
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        // Save contributions first
                        viewModel.handleEvent(
                            MeetingEvent.RecordContributions(
                                meetingId = meetingId,
                                contributions = state.contributions
                            )
                        )
                    }
                },
                icon = { Icon(Icons.Default.Check, contentDescription = "Save") },
                text = { Text("Save Meeting") }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Total Collected", fontWeight = FontWeight.Medium)
                    Text(
                        text = "KES ${state.totalCollected}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier
            .padding(paddingValues)
            .fillMaxSize()
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = "Error: ${state.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.members.isEmpty() -> {
                    Text(
                        text = "No active members",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(16.dp)
                    ) {
                        items(state.members) { member ->
                            ContributionItem(
                                member = member,
                                amount = state.weeklyAmount,
                                hasContributed = state.contributions[member.memberId] ?: false,
                                onContributionChange = { contributed ->
                                    coroutineScope.launch {
                                        viewModel.updateContributionStatus(member.memberId, contributed)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionItem(
    member: Member,
    amount: Int,
    hasContributed: Boolean,
    onContributionChange: (Boolean) -> Unit
) {
    val containerColor = when {
        !member.isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        hasContributed -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = containerColor),
        elevation = CardDefaults.elevatedCardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alpha(if (member.isActive) 1f else 0.6f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KES $amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Checkbox positioned at the end right of the card
            Checkbox(
                checked = hasContributed,
                onCheckedChange = if (member.isActive) onContributionChange else null,
                enabled = member.isActive,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}