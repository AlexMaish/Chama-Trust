// ContributionScreen.kt
package com.example.chamabuddy.presentation.screens

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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


    var hasInitialized by rememberSaveable(meetingId) { mutableStateOf(false) }

    val activeMembersCount = remember(state.members) {
        state.members.count { it.isActive }
    }
    val expectedTotal = remember(activeMembersCount, state.weeklyAmount) {
        activeMembersCount * state.weeklyAmount
    }

    // Define navigation function inside composable scope
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
        if (!hasInitialized) {
            viewModel.handleEvent(MeetingEvent.GetContributionsForMeeting(meetingId))
            hasInitialized = true
        }
    }

    BackHandler {
        coroutineScope.launch {
            viewModel.saveContributionState(state.contributions)
            state.meeting?.let {
                navController.navigate("${CycleDetailDestination.route}/${it.groupId}/${it.cycleId}") {
                    popUpTo(ContributionDestination.route) { inclusive = true }
                }
            } ?: navController.popBackStack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadSavedContributionState()?.let(viewModel::restoreContributions)
            ?: viewModel.handleEvent(MeetingEvent.GetContributionsForMeeting(meetingId))
    }


    DisposableEffect(Unit) {
        onDispose {
            viewModel.saveContributionState(state.contributions)
        }
    }


    Scaffold(
        containerColor = Color(0xFFF8F9FC),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF4A55A2),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                title = {
                    Text(
                        ContributionDestination.title,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            coroutineScope.launch {
                                viewModel.saveContributionState(state.contributions)
                                navController.popBackStack()
                            }
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isAdmin) {
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete Meeting", tint = Color.White)
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
                        shape = MaterialTheme.shapes.large,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF3BBA9C),
                            disabledContainerColor = Color(0xFFA0A0A0)
                        )
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
                containerColor = Color(0xFF4A55A2),
                contentColor = Color.White,
                icon = { Icon(Icons.Default.Check, contentDescription = "Save") },
                text = { Text("Save Meeting") }
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 8.dp,
                shadowElevation = 8.dp,
                color = Color(0xFF4A55A2),
                shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    LinearProgressIndicator(
                        progress = if (expectedTotal > 0) state.totalCollected.toFloat() / expectedTotal else 0f,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp),
                        color = Color(0xFF3BBA9C),
                        trackColor = Color(0xFFE0E0E0).copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            "Total Collected",
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                        Text(
                            "KES ${state.totalCollected} / $expectedTotal",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFFF8F9FC), Color(0xFFE6E9FF)),
                        startY = 0f,
                        endY = 1000f
                    )
                )
        ) {
            if (showDeleteDialog) {
                AlertDialog(
                    onDismissRequest = { showDeleteDialog = false },
                    title = { Text("Delete Meeting", fontWeight = FontWeight.Bold) },
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
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = Color(0xFF4A55A2)
                )
                state.error != null -> Text(
                    "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center),
                    textAlign = TextAlign.Center
                )
                state.members.isEmpty() -> Text(
                    "No active members",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center))
                else -> LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(16.dp)
                ) {
                    items(state.members, key = { it.memberId }) { member ->
                        ContributionItem(
                            member = member,
                            amount = state.weeklyAmount,
                            hasContributed = state.contributions[member.memberId] ?: false,
                            onContributionChange = { contributed ->
                                coroutineScope.launch {
                                    viewModel.updateContributionStatus(member.memberId, contributed)
                                }
                            },
                            onLongPress = { if (isAdmin) showDeleteDialog = true }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ContributionItem(
    member: Member,
    amount: Int,
    hasContributed: Boolean,
    onContributionChange: (Boolean) -> Unit,
    onLongPress: () -> Unit = {}
) {
    val containerColor = when {
        !member.isActive -> Color(0xFFE0E0E0).copy(alpha = 0.6f)
        hasContributed -> Color(0xFF3BBA9C).copy(alpha = 0.2f)
        else -> Color.White
    }

    val borderColor = if (hasContributed) Color(0xFF3BBA9C) else Color.Transparent

    // Fixed border implementation using Modifier.border
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border( // Use Modifier.border instead of Card border parameter
                width = 1.dp,
                color = borderColor,
                shape = MaterialTheme.shapes.medium
            )
            .combinedClickable(
                onClick = {
                    if (member.isActive) {
                        onContributionChange(!hasContributed)
                    }
                },
                onLongClick = onLongPress
            ),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = MaterialTheme.shapes.medium
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = if (member.isActive) Color(0xFF2C2C2C) else Color(0xFF7B7B7B)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "KES $amount",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (member.isActive) Color(0xFF4A55A2) else Color(0xFFA0A0A0)
                )
            }

            if (member.isActive) {
                Checkbox(
                    checked = hasContributed,
                    onCheckedChange = onContributionChange,
                    colors = CheckboxDefaults.colors(
                        checkedColor = Color(0xFF3BBA9C),
                        uncheckedColor = Color(0xFF4A55A2),
                        checkmarkColor = Color.White
                    ),
                    modifier = Modifier.padding(start = 8.dp)
                )
            } else {
                Text(
                    "Inactive",
                    color = Color(0xFFA0A0A0),
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}

private suspend fun SnackbarHostState.showAdminError() {
    showSnackbar("Only admins can perform this action")
}