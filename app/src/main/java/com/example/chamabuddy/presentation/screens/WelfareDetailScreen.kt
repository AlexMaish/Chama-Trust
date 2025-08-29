package com.example.chamabuddy.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.WelfareMeeting
import com.example.chamabuddy.presentation.navigation.HomeDestination
import com.example.chamabuddy.presentation.navigation.WelfareContributionDestination
import com.example.chamabuddy.presentation.navigation.WelfareDestination
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel
import com.example.chamabuddy.presentation.viewmodel.WelfareViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WelfareDetailScreen(
    welfareId: String,
    navController: NavHostController,
    groupId: String,
    navigateToMeetingDetail: (String) -> Unit,
    navigateToContribution: (String) -> Unit,
    navigateBack: () -> Unit,
    viewModel: WelfareViewModel = hiltViewModel()
) {
    val meetings by viewModel.meetings.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val authViewModel: AuthViewModel = hiltViewModel()
    val memberViewModel: MemberViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.userId ?: ""
    val isAdmin by memberViewModel.currentUserIsAdmin.collectAsState()
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Sort meetings by date (most recent first)
    val sortedMeetings = remember(meetings) {
        meetings.sortedByDescending { it.meetingDate }
    }

    BackHandler {
        navController.navigate("${HomeDestination.route}/$groupId") {
            popUpTo(WelfareDestination.route) { inclusive = true }
        }
    }

    LaunchedEffect(groupId, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            memberViewModel.loadCurrentUserRole(groupId, currentUserId)
        }
    }

    DisposableEffect(lifecycleOwner, welfareId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.loadMeetings(welfareId)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (groupId.isBlank() || welfareId.isBlank()) {
        Text("Error: Missing group or welfare ID")
        return
    }

    LaunchedEffect(welfareId) {
        viewModel.loadMeetings(welfareId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Welfare Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.navigate("${HomeDestination.route}/$groupId") {
                            popUpTo(WelfareDestination.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            // Only show FAB for admin
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        navController.navigate(
                            "${WelfareContributionDestination.route}/new/$welfareId/$groupId"
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Meeting")
                }
            }
        }
    ) { innerPadding ->
        when {
            sortedMeetings.isEmpty() -> EmptyWelfareMeetings(innerPadding, isAdmin)
            else -> MeetingsList(
                meetings = sortedMeetings,
                innerPadding = innerPadding,
                isAdmin = isAdmin,
                onDeleteMeeting = { meetingId ->
                    coroutineScope.launch {
                        viewModel.deleteMeeting(meetingId)
                        viewModel.loadMeetings(welfareId)
                    }
                },
                onClick = { meeting ->
                    navController.navigate(
                        "${WelfareContributionDestination.route}/${meeting.meetingId}/$welfareId/$groupId"
                    )
                }
            )
        }
    }
}

@Composable
private fun EmptyWelfareMeetings(innerPadding: PaddingValues, isAdmin: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "No meetings yet.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isAdmin) {
                Text(
                    text = "Start by adding your first welfare meeting!",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Only admins can create new meetings.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun MeetingsList(
    meetings: List<WelfareMeeting>,
    innerPadding: PaddingValues,
    isAdmin: Boolean,
    onDeleteMeeting: (String) -> Unit,
    onClick: (WelfareMeeting) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(meetings) { meeting ->
            WelfareMeetingCard(
                meeting = meeting,
                isAdmin = isAdmin,
                onDelete = { onDeleteMeeting(meeting.meetingId) },
                onClick = { onClick(meeting) }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun WelfareMeetingCard(
    meeting: WelfareMeeting,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = {
                    // Only allow long press for admins
                    if (isAdmin) showDeleteDialog = true
                }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Beneficiary summary at the top
            meeting.beneficiaryNames?.takeIf { it.isNotEmpty() }?.let { names ->
                val beneficiaryText = when {
                    names.size <= 3 -> "Beneficiaries: ${names.joinToString(", ")}"
                    else -> "Beneficiaries: ${names.take(3).joinToString(", ")} + ${names.size - 3} more"
                }
                Text(
                    text = beneficiaryText,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF3BBA9C),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Meeting date
            Text(
                text = "Meeting on ${dateFormat.format(Date(meeting.meetingDate))}",
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            // Contributor summary
            meeting.contributorSummaries?.takeIf { it.isNotEmpty() }?.let { summaries ->
                val contributorText = when {
                    summaries.size <= 3 -> "Contributors: ${summaries.joinToString(", ")}"
                    else -> "Contributors: ${summaries.take(3).joinToString(", ")} + ${summaries.size - 3} more"
                }
                Text(
                    text = contributorText,
                    fontSize = 14.sp,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
            }

            // Total collected
            Text(
                text = "Total collected: KES ${meeting.totalCollected}",
                fontSize = 14.sp
            )

            // Recorded by
            meeting.recordedBy?.let { recordedBy ->
                Text(
                    text = "Recorded by: $recordedBy",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }

    // Only show delete dialog for admins
    if (showDeleteDialog && isAdmin) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Meeting") },
            text = { Text("Are you sure you want to delete this welfare meeting?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        onDelete()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}