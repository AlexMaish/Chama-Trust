package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
import com.example.chamabuddy.presentation.viewmodel.MeetingState
import com.example.chamabuddy.presentation.viewmodel.MeetingViewModel
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.math.max

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
    var showAllMembersDialog by remember { mutableStateOf(false) }

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

                LaunchedEffect(beneficiary) {
                    adjustedAmount = beneficiary.amountReceived.toString()
                }

                val beneficiaryCount by produceState(initialValue = 1) {
                    value = viewModel.getBeneficiaryCountForMeeting(beneficiary.meetingId)
                }

                val members = remember(meeting) {
                    when {
                        meeting == null -> emptyList<Any>()
                        else -> {
                            val activeMembersField = runCatching {
                                val f = meeting::class.members.firstOrNull { it.name == "activeMembers" }
                                f?.call(meeting)
                            }.getOrNull()
                            val membersField = runCatching {
                                val f = meeting::class.members.firstOrNull { it.name == "members" || it.name == "participants" }
                                f?.call(meeting)
                            }.getOrNull()
                            @Suppress("UNCHECKED_CAST")
                            (activeMembersField as? List<*>) ?: (membersField as? List<*>) ?: emptyList<Any>()
                        }
                    }
                }

                val initialMembersToShow = 3
                val showSeeMoreButton = members.size > initialMembersToShow

                Column(
                    modifier = Modifier
                        .padding(16.dp)
                        .padding(paddingValues)
                ) {
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

                    Text(
                        text = "Active Members",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                    )
                    Spacer(Modifier.height(8.dp))

                    if (members.isEmpty()) {
                        Text(
                            text = "No active members",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            members.take(initialMembersToShow).forEach { member ->
                                val displayName = getMemberDisplayName(member)
                                MemberChip(name = displayName, modifier = Modifier.padding(end = 8.dp))
                            }

                            if (showSeeMoreButton) {
                                Spacer(modifier = Modifier.weight(1f))
                                TextButton(onClick = { showAllMembersDialog = true }) {
                                    Text("See More")
                                }
                            }
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    OutlinedTextField(
                        value = adjustedAmount,
                        onValueChange = { adjustedAmount = it },
                        label = { Text("Amount Received") },
                        keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                            keyboardType = KeyboardType.Number
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    meeting?.let { nonNullMeeting ->
                        if (nonNullMeeting.totalCollected > 0 && beneficiaryCount > 0) {
                            val defaultAmount = nonNullMeeting.totalCollected / max(1, beneficiaryCount)
                            Text(
                                text = "Default amount: $defaultAmount (Total: ${nonNullMeeting.totalCollected} ÷ $beneficiaryCount)",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(24.dp))

                    Button(
                        onClick = {
                            try {
                                val newAmount = adjustedAmount.toInt()
                                isEditing = true
                                viewModel.handleEvent(
                                    MeetingEvent.UpdateBeneficiaryAmount(beneficiaryId, newAmount)
                                )
                            } catch (e: NumberFormatException) {
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

                if (showAllMembersDialog) {
                    AlertDialog(
                        onDismissRequest = { showAllMembersDialog = false },
                        title = { Text(text = "All Active Members") },
                        text = {
                            if (members.isEmpty()) {
                                Text("No members available.")
                            } else {
                                LazyColumn {
                                    items(members) { member ->
                                        val displayName = getMemberDisplayName(member)
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            MemberAvatarInitials(name = displayName)
                                            Spacer(modifier = Modifier.padding(8.dp))
                                            Text(text = displayName, style = MaterialTheme.typography.bodyMedium)
                                        }
                                    }
                                }
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = { showAllMembersDialog = false }) {
                                Text("Close")
                            }
                        }
                    )
                }
            }

            else -> {
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

private fun getMemberDisplayName(member: Any?): String {
    return when (member) {
        is String -> member
        else -> {
            try {
                val nameProp = member?.let { m ->
                    m::class.members.firstOrNull {
                        it.name == "name" || it.name == "fullName" || it.name == "displayName"
                    }
                }
                val idProp = member?.let { m ->
                    m::class.members.firstOrNull {
                        it.name == "memberName" || it.name == "firstName"
                    }
                }
                (nameProp?.call(member) ?: idProp?.call(member))?.toString() ?: member.toString()
            } catch (e: Exception) {
                member?.toString() ?: "Unknown Member"
            }
        }
    }
}

@Composable
private fun MemberChip(name: String, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemberAvatarInitials(name = name)
        Spacer(modifier = Modifier.padding(6.dp))
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1
        )
    }
}

@Composable
private fun MemberAvatarInitials(name: String, modifier: Modifier = Modifier) {
    val initials = name
        .split(" ")
        .filter { it.isNotBlank() }
        .map { it.first().uppercaseChar() }
        .take(2)
        .joinToString("")

    Box(
        modifier = modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials.ifEmpty { "M" },
            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
    }
}