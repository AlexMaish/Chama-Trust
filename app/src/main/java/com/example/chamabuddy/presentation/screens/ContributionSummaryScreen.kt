package com.example.chamabuddy.presentation.screens

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
import com.example.chamabuddy.presentation.viewmodel.MeetingViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContributionSummaryScreen(
    meetingId: String,
    navigateBack: () -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val state by viewModel.contributionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    Log.d("ContributionScreen", "Rendering as non-admin summary")


    LaunchedEffect(meetingId) {
        viewModel.handleEvent(MeetingEvent.GetContributionsForMeeting(meetingId))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Meeting Contributions") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
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
                state.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                state.error != null -> Text(
                    text = "Error: ${state.error}",
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
                state.members.isEmpty() -> Text(
                    text = "No members in this meeting",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.align(Alignment.Center)
                )
                else -> ContributionSummaryList(
                    members = state.members,
                    contributions = state.contributions,
                    weeklyAmount = state.weeklyAmount
                )
            }
        }
    }
}

@Composable
fun ContributionSummaryList(
    members: List<Member>,
    contributions: Map<String, Boolean>,
    weeklyAmount: Int
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(16.dp)
    ) {
        items(members) { member ->
            ContributionSummaryItem(
                member = member,
                amount = weeklyAmount,
                hasContributed = contributions[member.memberId] ?: false
            )
        }
    }
}

@Composable
fun ContributionSummaryItem(
    member: Member,
    amount: Int,
    hasContributed: Boolean
) {
    val containerColor = when {
        !member.isActive -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        hasContributed -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.8f)
        else -> MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.alpha(if (member.isActive) 1f else 0.6f)
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (hasContributed) "Contributed: KES $amount" else "Not contributed",
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (hasContributed) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Icon(
                imageVector = if (hasContributed) Icons.Default.Check else Icons.Default.Close,
                contentDescription = if (hasContributed) "Contributed" else "Not contributed",
                tint = if (hasContributed) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}