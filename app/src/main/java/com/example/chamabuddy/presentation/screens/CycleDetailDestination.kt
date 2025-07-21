
package com.example.chamabuddy.presentation.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
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
import com.example.chamabuddy.domain.model.MeetingWithDetails
import com.example.chamabuddy.presentation.navigation.CycleDetailDestination
import com.example.chamabuddy.presentation.navigation.HomeDestination
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
import com.example.chamabuddy.presentation.viewmodel.MeetingState
import com.example.chamabuddy.presentation.viewmodel.MeetingViewModel
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDetailScreenForMeetings(
    navController: NavHostController,
    groupId: String,
    cycleId: String,
    navigateToMeetingDetail: (String) -> Unit,
    navigateToContribution: (String) -> Unit,
    navigateBack: () -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    // Trigger load when lifecycle starts or cycleId changes
    val state by viewModel.state.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current



    val authViewModel: AuthViewModel = hiltViewModel()
    val memberViewModel: MemberViewModel = hiltViewModel()
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserId = currentUser?.userId ?: ""
    val isAdmin by memberViewModel.currentUserIsAdmin.collectAsState()



    BackHandler {
        navController.navigate("${HomeDestination.route}/$groupId") {
            popUpTo(CycleDetailDestination.route) { inclusive = true }
        }
    }

    LaunchedEffect(groupId, currentUserId) {
        if (currentUserId.isNotEmpty()) {
            memberViewModel.loadCurrentUserRole(groupId, currentUserId)
        }
    }



    DisposableEffect(lifecycleOwner, cycleId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                viewModel.handleEvent(MeetingEvent.GetMeetingsForCycle(cycleId))
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    if (groupId.isBlank() || cycleId.isBlank()) {
        Text("Error: Missing group or cycle ID")
        return
    }
    LaunchedEffect(cycleId) {
        viewModel.handleEvent(MeetingEvent.GetMeetingsForCycle(cycleId))
    }

    LaunchedEffect(state) {
        if (state is MeetingState.MeetingCreated) {
            val meeting = (state as MeetingState.MeetingCreated).meeting
            navigateToContribution(meeting.meetingId)
            viewModel.handleEvent(MeetingEvent.ResetMeetingState)
        }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Cycle Details") },
                navigationIcon = {
                    IconButton(onClick = {
                        // Navigate to HomeDestination
                        navController.navigate("${HomeDestination.route}/$groupId") {
                            popUpTo(CycleDetailDestination.route) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                FloatingActionButton(
                    onClick = {
                        viewModel.handleEvent(
                            MeetingEvent.CreateMeeting(
                                cycleId = cycleId,
                                date = Date(),
                                recordedBy = null,
                                groupId = groupId
                            )
                        )
                    },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Add, contentDescription = "New Meeting")
                }
            }
        }
    ){ innerPadding ->
        when (state) {
            is MeetingState.Loading -> LoadingContent(innerPadding)
            is MeetingState.Error -> ErrorContent(((state as MeetingState.Error).message), innerPadding)
            is MeetingState.MeetingsLoaded -> MeetingsList(
                meetings = (state as MeetingState.MeetingsLoaded).meetings,
                innerPadding = innerPadding,
                isAdmin = isAdmin,
                onDeleteMeeting = { meetingId ->
                    viewModel.handleEvent(MeetingEvent.DeleteMeeting(meetingId))
                },
                onClick = { meeting -> navigateToContribution(meeting.meeting.meetingId) }
            )
            else -> {}
        }
    }
}

@Composable
private fun LoadingContent(innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ErrorContent(message: String, innerPadding: PaddingValues) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentAlignment = Alignment.Center
    ) {
        Text(message)
    }
}

@Composable
private fun MeetingsList(
    meetings: List<MeetingWithDetails>,
    innerPadding: PaddingValues,
    isAdmin: Boolean, // Add isAdmin parameter
    onDeleteMeeting: (String) -> Unit, // Add delete handler
    onClick: (MeetingWithDetails) -> Unit
) {
    if (meetings.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No meetings yet. Start by adding your first meeting!",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(meetings) { meetingWithDetails ->
                MeetingCard(
                    meeting = meetingWithDetails,
                    isAdmin = isAdmin,
                    onDelete = { onDeleteMeeting(meetingWithDetails.meeting.meetingId) },
                    onClick = { onClick(meetingWithDetails) }
                )
            }
        }
    }
}

@Composable
private fun MeetingCard(
    meeting: MeetingWithDetails,
    isAdmin: Boolean,
    onDelete: () -> Unit,
    onClick: () -> Unit
) {

    var showConfirmation by remember { mutableStateOf(false) }

    if (showConfirmation) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("Delete Meeting") },
            text = { Text("Are you sure you want to delete this meeting? This action cannot be undone.") },
            confirmButton = {
                TextButton(onClick = {
                    showConfirmation = false
                    onDelete()
                }) {
                    Text("Delete", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {

            if (isAdmin) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Delete meeting",
                            tint = Color.Red
                        )
                    }
                }
            }
            if (meeting.beneficiaries.isNotEmpty()) {
                Text(
                    text = "Beneficiaries",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF388E3C)
                )
                Spacer(Modifier.height(4.dp))
                meeting.beneficiaries.take(2).forEach { beneficiary ->
                    Text(text = "• ${beneficiary.memberName}", color = Color(0xFF388E3C))
                }
                Spacer(Modifier.height(8.dp))
                Divider()
                Spacer(Modifier.height(8.dp))
            }
            Text(
                text = "Meeting on ${dateFormat.format(meeting.meeting.meetingDate)}",
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.height(8.dp))
            if (meeting.contributors.isNotEmpty()) {
                Text(text = "Contributors:", fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                meeting.contributors.take(3).forEach { contributor ->
                    Text(text = "• ${contributor.memberName}")
                }
                if (meeting.contributors.size > 4) {
                    Text(text = "+ ${meeting.contributors.size - 3} more")
                }
                Spacer(Modifier.height(8.dp))
            }
            meeting.meeting.recordedBy?.let { recordedBy ->
                Text(
                    text = "Recorded by: $recordedBy",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }
    }
}


