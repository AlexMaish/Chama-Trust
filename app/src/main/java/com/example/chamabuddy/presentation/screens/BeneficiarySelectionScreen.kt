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
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.MeetingEvent
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
    onSaveComplete: () -> Unit,
    viewModel: MeetingViewModel = hiltViewModel()
) {
    val beneficiaryState by viewModel.beneficiaryState.collectAsState()
    var firstBeneficiary by remember { mutableStateOf<Member?>(null) }
    var secondBeneficiary by remember { mutableStateOf<Member?>(null) }
    val isSaveEnabled = firstBeneficiary != null && secondBeneficiary != null
    val coroutineScope = rememberCoroutineScope()

    // Load eligible beneficiaries
    LaunchedEffect(meetingId) {
        viewModel.handleEvent(MeetingEvent.LoadEligibleBeneficiaries(meetingId))
    }

    Scaffold(
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
            if (isSaveEnabled) {
                ExtendedFloatingActionButton(
                    onClick = {
                        firstBeneficiary?.let { first ->
                            secondBeneficiary?.let { second ->
                                coroutineScope.launch {
                                    viewModel.handleEvent(
                                        MeetingEvent.ConfirmBeneficiarySelection(
                                            meetingId,
                                            first.memberId,
                                            second.memberId
                                        )
                                    )
                                    navigateBack()
                                }
                            }
                        }
                    },
                    icon = { Icon(Icons.Default.Check, contentDescription = "Save") },
                    text = { Text("Save Beneficiaries") }
                )
            }
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
                beneficiaryState.eligibleMembers.isEmpty() -> {
                    Text(
                        text = "No eligible beneficiaries available",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        Text(
                            text = "Select two beneficiaries for this meeting",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(16.dp)
                        )

                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(beneficiaryState.eligibleMembers) { member ->
                                BeneficiaryCandidateItem(
                                    member = member,
                                    isSelected = member == firstBeneficiary || member == secondBeneficiary,
                                    onSelect = {
                                        when {
                                            firstBeneficiary == null -> firstBeneficiary = member
                                            secondBeneficiary == null && firstBeneficiary != member ->
                                                secondBeneficiary = member
                                            firstBeneficiary == member -> firstBeneficiary = null
                                            secondBeneficiary == member -> secondBeneficiary = null
                                            else -> {
                                                // Replace the oldest selection
                                                firstBeneficiary = secondBeneficiary
                                                secondBeneficiary = member
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
