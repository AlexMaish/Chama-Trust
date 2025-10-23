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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel
import com.example.chamabuddy.presentation.viewmodel.WelfareMeetingViewModel
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.WelfareViewModel
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun WelfareContributionScreen(
    meetingId: String,
    welfareId: String,
    groupId: String,
    navigateToBeneficiarySelection: (String) -> Unit,
    navController: NavHostController,
    viewModel: WelfareMeetingViewModel = hiltViewModel(),
    welfareViewModel: WelfareViewModel = hiltViewModel(),
    memberViewModel: MemberViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    val state by viewModel.contributionState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val isAdmin by memberViewModel.currentUserIsAdmin.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    val recordedByName = currentUser?.username ?: "Unknown"

    var showAmountDialog by remember { mutableStateOf(false) }
    var selectedMember by remember { mutableStateOf<Member?>(null) }
    var customAmount by remember { mutableStateOf("") }
    var customAmountError by remember { mutableStateOf(false) }

    var hasInitialized by rememberSaveable(meetingId) { mutableStateOf(false) }
    var isNewMeeting by remember { mutableStateOf(meetingId == "new") }
    var actualMeetingId by remember { mutableStateOf(if (isNewMeeting) "" else meetingId) }


    var isMeetingCreated by remember { mutableStateOf(false) }
    var currentMeetingId by remember { mutableStateOf(if (meetingId == "new") "" else meetingId) }


    val activeMembersCount = remember(state.members) { state.members.count { it.isActive } }
    val baseTarget = activeMembersCount * state.welfareAmount
    val extraTotal = state.contributions.values.sumOf { amount ->
        maxOf(0, amount - state.welfareAmount)
    }
    val expectedTotal = baseTarget + extraTotal


    if (groupId.isBlank() || welfareId.isBlank()) {
        Text("Error: Missing group or welfare ID")
        return
    }


    LaunchedEffect(meetingId) {
        if (meetingId != "new" && meetingId.isNotBlank()) {
            viewModel.loadMembersForContribution(meetingId)
        }
    }
    LaunchedEffect(groupId, currentUser) {
        currentUser?.let { user ->
            memberViewModel.loadCurrentUserRole(groupId, user.userId)
        }
    }
    LaunchedEffect(state.meeting) {
        state.meeting?.let { meeting ->
            val groupId = meeting.groupId
            val currentUserId = authViewModel.currentUserId.value
            if (currentUserId != null) {
                memberViewModel.loadCurrentUserRole(groupId, currentUserId)
            }
        }
    }

    LaunchedEffect(meetingId, welfareId, groupId) {
        if (!hasInitialized) {
            if (isNewMeeting) {
                val welfareAmount = welfareViewModel.getWelfareAmount(welfareId)
                viewModel.loadMembersForNewMeeting(groupId, welfareAmount)
            } else {
                viewModel.loadMembersForContribution(meetingId)
            }
            hasInitialized = true
        }
    }

    LaunchedEffect(meetingId, welfareId, groupId) {
        if (!hasInitialized) {
            if (isNewMeeting) {
                val welfareAmount = welfareViewModel.getWelfareAmount(welfareId)
                viewModel.loadMembersForNewMeeting(groupId, welfareAmount)
            } else {
                viewModel.loadMembersForContribution(meetingId)
            }
            hasInitialized = true
        }
    }

    var isCreatingMeeting by remember { mutableStateOf(false) }


//    BackHandler {
//        if (isNewMeeting && state.contributions.isNotEmpty() &&
//            state.contributions.any { it.value > 0 } && !isMeetingCreated && !isCreatingMeeting) {
//            isCreatingMeeting = true
//            coroutineScope.launch {
//                try {
//                    val newMeetingId = welfareViewModel.createWelfareMeeting(
//                        welfareId = welfareId,
//                        meetingDate = Date(),
//                        recordedBy = recordedByName,
//                        groupId = groupId,
//                        welfareAmount = state.welfareAmount
//                    )
//
//                    if (newMeetingId != null) {
//                        viewModel.recordContributions(newMeetingId, state.contributions)
//                        isMeetingCreated = true
//                        snackbarHostState.showSnackbar("Meeting created successfully")
//                    }
//                } catch (e: Exception) {
//                    snackbarHostState.showSnackbar("Error creating meeting: ${e.message}")
//                } finally {
//                    isCreatingMeeting = false
//                    navController.popBackStack()
//                }
//            }
//        } else {
//            coroutineScope.launch {
//                viewModel.saveContributionState(state.contributions)
//                navController.popBackStack()
//            }
//        }
//    }


    BackHandler {
        navController.popBackStack()
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
                        if (isNewMeeting) "New Welfare Meeting" else "Record Welfare Contributions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = {
                            navController.popBackStack()
                        }
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    if (isAdmin) {
                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    if (isNewMeeting && !isMeetingCreated && !isCreatingMeeting) {
                                        isCreatingMeeting = true
                                        try {
                                            val newMeetingId = welfareViewModel.createWelfareMeeting(
                                                welfareId = welfareId,
                                                meetingDate = Date(),
                                                recordedBy = recordedByName,
                                                groupId = groupId,
                                                welfareAmount = state.welfareAmount
                                            )

                                            if (newMeetingId != null) {
                                                viewModel.recordContributions(newMeetingId, state.contributions)
                                                currentMeetingId = newMeetingId
                                                isMeetingCreated = true
                                                isNewMeeting = false
                                                navigateToBeneficiarySelection(newMeetingId)
                                            }
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error creating meeting: ${e.message}")
                                        } finally {
                                            isCreatingMeeting = false
                                        }
                                    } else {
                                        try {
                                            viewModel.recordContributions(currentMeetingId, state.contributions)
                                            snackbarHostState.showSnackbar("Contributions saved successfully")
                                            navigateToBeneficiarySelection(currentMeetingId)
                                        } catch (e: Exception) {
                                            snackbarHostState.showSnackbar("Error saving contributions: ${e.message}")
                                        }
                                    }
                                }
                            },
                            enabled = state.members.isNotEmpty() && !(isNewMeeting && isMeetingCreated) && !isCreatingMeeting,
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
                }
            )
        },
        floatingActionButton = {
            if (isAdmin) {
                ExtendedFloatingActionButton(
                    onClick = {
                        if (isNewMeeting && isMeetingCreated) {
                            // Meeting already created, just navigate back
                            navController.popBackStack()
                            return@ExtendedFloatingActionButton
                        }

                        coroutineScope.launch {
                            if (isNewMeeting) {
                                try {
                                    val newMeetingId = welfareViewModel.createWelfareMeeting(
                                        welfareId = welfareId,
                                        meetingDate = Date(),
                                        recordedBy = recordedByName,
                                        groupId = groupId,
                                        welfareAmount = state.welfareAmount
                                    )

                                    if (newMeetingId != null) {
                                        viewModel.recordContributions(newMeetingId, state.contributions)
                                        isMeetingCreated = true
                                        snackbarHostState.showSnackbar("Meeting created successfully")
                                        navController.popBackStack()
                                    }
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error creating meeting: ${e.message}")
                                }
                            } else {
                                try {
                                    viewModel.recordContributions(
                                        meetingId,
                                        state.contributions
                                    )
                                    snackbarHostState.showSnackbar("Contributions saved successfully")
                                } catch (e: Exception) {
                                    snackbarHostState.showSnackbar("Error saving contributions: ${e.message}")
                                }
                            }
                        }
                    },
                    containerColor = Color(0xFF4A55A2),
                    contentColor = Color.White,
                    icon = { Icon(Icons.Default.Check, contentDescription = "Save") },
                    text = { Text(if (isNewMeeting) "Create Meeting" else "Save Contribution") }
                )
            }
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
                        val amount = state.contributions[member.memberId] ?: state.welfareAmount
                        WelfareContributionItem(
                            member = member,
                            amount = amount,
                            defaultAmount = state.welfareAmount,
                            hasContributed = amount > 0,
                            onContributionChange = { contributed ->
                                coroutineScope.launch {
                                    viewModel.updateContributionStatus(member.memberId, contributed)
                                }
                            },
                            onSetAmount = {
                                selectedMember = member
                                customAmount = amount.toString()
                                customAmountError = false
                                showAmountDialog = true
                            },
                            isAdmin = isAdmin
                        )
                    }
                }
            }

            if (showAmountDialog && selectedMember != null && isAdmin) {
                AlertDialog(
                    onDismissRequest = {
                        showAmountDialog = false
                        customAmountError = false
                    },
                    title = { Text("Contribution Amount") },
                    text = {
                        Column {
                            Text("Enter amount for ${selectedMember!!.name}:")
                            Spacer(Modifier.height(8.dp))
                            TextField(
                                value = customAmount,
                                onValueChange = {
                                    customAmount = it
                                    customAmountError = false
                                },
                                keyboardOptions = KeyboardOptions.Default.copy(
                                    keyboardType = KeyboardType.Number
                                ),
                                isError = customAmountError,
                                supportingText = {
                                    if (customAmountError) {
                                        Text("Enter a valid amount")
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "Default amount: KES ${state.welfareAmount}",
                                color = Color.Gray,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = {
                                val amount = customAmount.toIntOrNull()
                                if (amount != null && amount >= 0) {
                                    coroutineScope.launch {
                                        try {
                                            viewModel.updateContributionAmount(
                                                selectedMember!!.memberId,
                                                amount
                                            )
                                        } catch (e: Exception) {
                                            Log.e("WelfareScreen", "Error updating amount: ${e.message}")
                                        }
                                        showAmountDialog = false
                                        customAmount = ""
                                    }
                                } else {
                                    customAmountError = true
                                }
                            }
                        ) {
                            Text("Set Amount")
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = {
                            showAmountDialog = false
                            customAmountError = false
                        }) {
                            Text("Cancel")
                        }
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun WelfareContributionItem(
    member: Member,
    amount: Int,
    defaultAmount: Int,
    hasContributed: Boolean,
    onContributionChange: (Boolean) -> Unit,
    onSetAmount: () -> Unit,
    isAdmin: Boolean // New parameter
) {
    val containerColor = when {
        !member.isActive -> Color(0xFFE0E0E0).copy(alpha = 0.6f)
        hasContributed -> Color(0xFF3BBA9C).copy(alpha = 0.2f)
        else -> Color.White
    }

    val borderColor = if (hasContributed) Color(0xFF3BBA9C) else Color.Transparent

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(60.dp)
            .border(1.dp, borderColor, MaterialTheme.shapes.medium)
            .combinedClickable(
                enabled = isAdmin && member.isActive,
                onClick = {
                    if (member.isActive) {
                        onContributionChange(!hasContributed)
                    }
                }
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "KES $amount",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (amount != defaultAmount) Color(0xFFFF6B35) else Color(0xFF4A55A2)
                    )
                    Spacer(Modifier.width(8.dp))
                    if (amount != defaultAmount && amount > 0) {
                        Text(
                            "(+${amount - defaultAmount})",
                            fontSize = 12.sp,
                            color = Color(0xFFFF6B35),
                            modifier = Modifier.padding(start = 4.dp)
                        )
                    }
                }
            }

            if (member.isActive) {
                if (isAdmin) {
                    // Show admin controls
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // Button to set custom amount
                        OutlinedButton(
                            onClick = onSetAmount,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text("Set", fontSize = 12.sp)
                        }
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
                    }
                } else {
                    // Show view-only status for non-admin users
                    if (hasContributed) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Contributed",
                            tint = Color(0xFF3BBA9C)
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Cancel,
                            contentDescription = "Not contributed",
                            tint = Color(0xFFA0A0A0)
                        )
                    }
                }
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