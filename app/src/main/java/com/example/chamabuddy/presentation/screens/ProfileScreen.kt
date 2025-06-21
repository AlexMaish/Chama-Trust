package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.CycleEvent
import com.example.chamabuddy.presentation.viewmodel.CycleViewModel
import com.example.chamabuddy.presentation.viewmodel.MemberEvent
import com.example.chamabuddy.presentation.viewmodel.MemberState
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel
import com.example.chamabuddy.presentation.viewmodel.SavingsEvent
import com.example.chamabuddy.presentation.viewmodel.SavingsState
import com.example.chamabuddy.presentation.viewmodel.SavingsViewModel
import java.text.SimpleDateFormat
import java.util.*

object ProfileDestination : NavigationDestination {
    override val route = "profile"
    override val title = "Member Profile"
    const val memberIdArg = "memberId"
    val routeWithArgs = "$route/{$memberIdArg}"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    memberId: String,
    navigateBack: () -> Unit,
    savingsViewModel: SavingsViewModel = hiltViewModel(),
    memberViewModel: MemberViewModel = hiltViewModel(),
    cycleViewModel: CycleViewModel = hiltViewModel() // Add CycleViewModel

) {
    val savingsState by savingsViewModel.state.collectAsState()
    val memberSavings by savingsViewModel.memberSavings.collectAsState()
    val memberState by memberViewModel.state.collectAsState()

    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var isEditing by remember { mutableStateOf(false) }

    // Get the actual cycle ID from the ViewModel
    val currentCycleId by savingsViewModel.currentCycleId.collectAsState()



    // Get active cycle
    LaunchedEffect(Unit) {
        cycleViewModel.handleEvent(CycleEvent.GetActiveCycle)
    }
    val activeCycle by cycleViewModel.activeCycle.collectAsState()

    // Fetch member details and savings when screen loads
    LaunchedEffect(memberId) {
        memberViewModel.handleEvent(MemberEvent.GetMemberDetails(memberId))
        savingsViewModel.handleEvent(SavingsEvent.GetMemberSavings("current_cycle_id", memberId))
    }


    // State to track the determined month
    var determinedMonth by remember { mutableStateOf("") }

    // Function to determine the target month
    fun determineSavingsMonth(): String {
        // 1. Check if there are existing savings
        val savingsByMonth = memberSavings.groupBy {
            SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(it.entryDate)
        }

        // 2. Get current date
        val currentDate = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(currentDate.time)

        // 3. Get active cycle start date
        val cycleStartDate = activeCycle?.startDate?.let { Date(it) } ?: return currentMonth
        val cycleStartCalendar = Calendar.getInstance().apply { time = cycleStartDate }

        // 4. Find the last month with incomplete savings
        val calendar = Calendar.getInstance()
        calendar.time = cycleStartDate

        // Iterate through months from cycle start to current month
        while (calendar.before(currentDate) ){
                val monthKey = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calendar.time)

                // Skip future months
                if (calendar.after(currentDate)) break

                // Calculate total for this month
                val monthlyTotal = savingsByMonth[monthKey]?.sumOf { it.amount } ?: 0

                // Check if savings are incomplete
                if (monthlyTotal < (activeCycle?.monthlySavingsAmount ?: 0)) {
                    return monthKey
                }

                // Move to next month
                calendar.add(Calendar.MONTH, 1)
            }

        // If all previous months are complete, use current month
            return currentMonth
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Member Profile") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    // Set default amount to monthly target
                    amount = activeCycle?.monthlySavingsAmount?.toString() ?: ""
                    selectedMonth = ""
                    isEditing = false
                    showAddSavingsDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Savings")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            // Show loading state if either member or savings is loading
            if (memberState is MemberState.Loading || savingsState is SavingsState.Loading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
                return@Column
            }

            // Handle errors
            when (memberState) {
                is MemberState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((memberState as MemberState.Error).message)
                    }
                    return@Column
                }
                else -> {}
            }

            when (savingsState) {
                is SavingsState.Error -> {
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text((savingsState as SavingsState.Error).message)
                    }
                    return@Column
                }
                else -> {}
            }

            // Display member information
            val member = when (memberState) {
                is MemberState.MemberDetails -> (memberState as MemberState.MemberDetails).member
                else -> null
            }

            if (member == null) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Member not found")
                }
                return@Column
            }

            // Member info section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Profile picture
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = member.name.take(2).uppercase(),
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = member.name,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = member.phoneNumber,
                    fontSize = 18.sp,
                    color = Color.Gray
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            Divider()

            Spacer(modifier = Modifier.height(16.dp))

            // Savings entries by month
            Text(
                text = "Monthly Savings",
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp
            )

            Spacer(modifier = Modifier.height(16.dp))

            val groupedByMonth = memberSavings.groupBy {
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(it.entryDate)
            }

            if (groupedByMonth.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No savings recorded")
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    groupedByMonth.forEach { (monthName, entriesList) ->
                        item {
                            MonthlySavingsCard(
                                month = monthName,
                                entries = entriesList, // Add this parameter
                                monthlyTarget = activeCycle?.monthlySavingsAmount ?: 0,
                                onClick = {
                                    // Pre-fill the dialog with month
                                    selectedMonth = convertMonthToInputFormat(monthName)
                                    amount = ""
                                    isEditing = true
                                    showAddSavingsDialog = true
                                }
                            )
                        }
                    }
                }
            }
        }

        if (showAddSavingsDialog) {
            AddSavingsDialog(
                amount = amount,
                onAmountChange = { amount = it },
                month = selectedMonth,
                onMonthChange = { selectedMonth = it },
                monthlyTarget = activeCycle?.monthlySavingsAmount ?: 0,
                onDismiss = { showAddSavingsDialog = false },
                onSave = {
                    val targetCycleId = currentCycleId ?: run {
                        // Show error or handle missing cycle
                        return@AddSavingsDialog
                    }
                    // Use determined month if user didn't specify
                    val targetMonth = if (selectedMonth.isBlank()) determinedMonth else selectedMonth

                    savingsViewModel.handleEvent(
                        SavingsEvent.RecordSavings(
                            cycleId = targetCycleId, // Use actual cycle ID
                            monthYear = targetMonth,
                            memberId = memberId,
                            amount = amount.toIntOrNull() ?: 0,
                            recordedBy = "current_user_id"
                        )
                    )
                    showAddSavingsDialog = false
                },
                determinedMonth = determinedMonth // Pass to show in UI
            )
        }
    }

    // Determine the month when dialog is shown
    LaunchedEffect(showAddSavingsDialog) {
        if (showAddSavingsDialog) {
            determinedMonth = determineSavingsMonth()
            if (selectedMonth.isBlank()) {
                selectedMonth = determinedMonth
            }
        }
    }
}


@Composable
fun AddSavingsDialog(
    amount: String,
    onAmountChange: (String) -> Unit,
    month: String,
    onMonthChange: (String) -> Unit,
    monthlyTarget: Int,
    onDismiss: () -> Unit,
    onSave: () -> Unit,
    determinedMonth: String // Add this parameter
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Savings") },
        text = {
            Column {
                // Show auto-determined month
                Text(
                    text = "Auto-selected month: $determinedMonth",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = onAmountChange,
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = month,
                    onValueChange = onMonthChange,
                    label = { Text("Month (MM/YYYY)") },
                    placeholder = { Text("e.g. 06/2024") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text("Monthly target: KES $monthlyTarget", color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(onClick = onSave) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
@Composable
fun MonthlySavingsCard(
    month: String,
    entries: List<MonthlySavingEntry>,
    monthlyTarget: Int,
    onClick: () -> Unit
) {
    val totalSavings = entries.sumOf { it.amount }
    val isComplete = totalSavings >= monthlyTarget
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        onClick = { if (!isComplete) onClick() }
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = month,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            entries.forEach { entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(dateFormat.format(entry.entryDate))
                    Text("KES ${entry.amount}", fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Divider()

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Total:", fontWeight = FontWeight.Bold)
                Text(
                    "KES ${entries.sumOf { it.amount }}",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status:", fontWeight = FontWeight.Bold)
                if (isComplete) {
                    Text("Completed", color = Color.Green)
                } else {
                    Text("Incomplete", color = Color.Red)
                }

            }
        }
    }

}
private fun convertMonthToInputFormat(displayMonth: String): String {
    return try {
        val parsedDate = SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(displayMonth)
        SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(parsedDate)
    } catch (e: Exception) {
        ""
    }
}