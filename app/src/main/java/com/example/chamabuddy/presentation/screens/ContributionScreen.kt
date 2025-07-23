package com.example.chamabuddy.presentation.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.navigation.CycleDetailDestination
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
import com.example.chamabuddy.presentation.viewmodel.MeetingState
import com.example.chamabuddy.presentation.viewmodel.MeetingViewModel
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel
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
    navController: NavHostController,
    viewModel: MeetingViewModel = hiltViewModel(),
    memberViewModel: MemberViewModel = hiltViewModel()
) {
    val state by viewModel.contributionState.collectAsState()
    val meetingState by viewModel.state.collectAsState()
    val isNextEnabled = state.members.isNotEmpty() && !state.isLoading && state.error == null
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val isAdmin by memberViewModel.currentUserIsAdmin.collectAsState()

    Log.d("ContributionScreen", "Rendering as admin: $isAdmin")

    fun navigateToCycleDetail(groupId: String, cycleId: String) {
        navController.navigate("${CycleDetailDestination.route}/$groupId/$cycleId") {
            popUpTo(ContributionDestination.route) { inclusive = true }
        }
    }

    LaunchedEffect(meetingState) {
        when (val s = meetingState) {
            is MeetingState.MeetingDeleted -> {
                if (s.success) {
                    state.meeting?.let { meeting ->
                        navigateToCycleDetail(meeting.groupId, meeting.cycleId)
                    }
                    snackbarHostState.showSnackbar("Meeting deleted successfully")
                }
            }
            is MeetingState.ContributionRecorded -> {
                if (s.success) {
                    val status = viewModel.getMeetingStatus(meetingId)
                    if (!status.beneficiariesSelected) {
                        snackbarHostState.showSnackbar("Select beneficiaries to complete meeting")
                        navigateToBeneficiarySelection()
                    } else {
                        state.meeting?.let { meeting ->
                            navigateToCycleDetail(meeting.groupId, meeting.cycleId)
                        }
                        snackbarHostState.showSnackbar("Meeting Saved Successfully")
                    }
                }
            }
            is MeetingState.Error -> snackbarHostState.showSnackbar("Error: ${s.message}")
            else -> {}
        }
    }

    LaunchedEffect(meetingId) {
        viewModel.handleEvent(MeetingEvent.GetContributionsForMeeting(meetingId))
    }

    BackHandler { coroutineScope.launch { viewModel.saveContributionState(state.contributions); state.meeting?.let { navController.navigate("${CycleDetailDestination.route}/${it.groupId}/${it.cycleId}") { popUpTo(ContributionDestination.route) { inclusive = true } } } ?: navController.popBackStack() } }

    LaunchedEffect(Unit) {
        viewModel.loadSavedContributionState()?.let(viewModel::restoreContributions)
            ?: viewModel.handleEvent(MeetingEvent.GetContributionsForMeeting(meetingId))
    }

    Scaffold(
        containerColor = Color(0xFFF5F5F5),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
                title = {
                    Text(
                        ContributionDestination.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { coroutineScope.launch { viewModel.saveContributionState(state.contributions); navController.popBackStack() } }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Meeting")
                        }
                    }
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.saveContributionState(state.contributions)
                                navigateToBeneficiarySelection()
                            }
                        },
                        enabled = isNextEnabled,
                        shape = MaterialTheme.shapes.large
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Select Beneficiaries")
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    coroutineScope.launch {
                        if (isAdmin) {
                            viewModel.handleEvent(MeetingEvent.RecordContributions(meetingId, state.contributions))
                        } else snackbarHostState.showSnackbar("Only admins can perform this action")
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
                    Text("KES ${state.totalCollected}", style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Meeting") },
                    text = { Text("Are you sure you want to delete this meeting? This action cannot be undone.") },
                    confirmButton = {
                        TextButton(onClick = {
                            showDeleteDialog = false
                            viewModel.handleEvent(MeetingEvent.DeleteMeeting(meetingId))
                        }) {
                            Text("Delete", color = MaterialTheme.colorScheme.error)
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showDeleteDialog = false }) {
                            Text("Cancel")
                        }
                    }
                )
            }

            when {
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text("Error: ${state.error}", color = MaterialTheme.colorScheme.error, modifier = Modifier.align(Alignment.Center))
                state.members.isEmpty() -> Text("No active members", style = MaterialTheme.typography.bodyLarge, modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.members) { member ->
                        ContributionItem(
                            member = member,
                            amount = state.weeklyAmount,
                            hasContributed = state.contributions[member.memberId] ?: false,
                            enabled = member.isActive,
                            onContributionChange = { contributed ->
                                coroutineScope.launch { viewModel.updateContributionStatus(member.memberId, contributed) }
                            },
                            onLongPress = { if (isAdmin) showDeleteDialog = true }
                        )
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
    enabled: Boolean,
    hasContributed: Boolean,
    onContributionChange: (Boolean) -> Unit,
    onLongPress: () -> Unit = {}
) {
    val containerColor = when {
        !member.isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        hasContributed -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.surface
    }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .shadow(6.dp)
            .pointerInput(Unit) { detectTapGestures(onLongPress = { onLongPress() }) },
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
            Column(Modifier.weight(1f)) {
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

            Checkbox(
                checked = hasContributed,
                onCheckedChange = if (enabled) onContributionChange else null,
                enabled = enabled,
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.primary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                ),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }
}

private suspend fun SnackbarHostState.showAdminError() {
    showSnackbar("Only admins can perform this action", duration = SnackbarDuration.Short)
}