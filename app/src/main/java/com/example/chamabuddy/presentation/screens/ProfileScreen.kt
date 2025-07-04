package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
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
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.presentation.navigation.AuthDestination
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.CycleEvent
import com.example.chamabuddy.presentation.viewmodel.CycleWithSavings
import com.example.chamabuddy.presentation.viewmodel.HomeViewModel
import com.example.chamabuddy.presentation.viewmodel.MemberEvent
import com.example.chamabuddy.presentation.viewmodel.MemberState
import com.example.chamabuddy.presentation.viewmodel.MemberViewModel
import com.example.chamabuddy.presentation.viewmodel.SavingsEvent
import com.example.chamabuddy.presentation.viewmodel.SavingsState
import com.example.chamabuddy.presentation.viewmodel.SavingsViewModel
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ProfileScreen(
    groupId: String,
    memberId: String,
    navigateBack: () -> Unit,
    navController: NavHostController,
    savingsViewModel: SavingsViewModel = hiltViewModel(),
    memberViewModel: MemberViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel()
) {
    // Get current user for 'recorded by' functionality
    val currentUser by authViewModel.currentUser.collectAsState()
    val currentUserPhone = currentUser?.phoneNumber
    val currentMember = remember {
        derivedStateOf {
            savingsViewModel.members.value.values.find { it.phoneNumber == currentUserPhone }
        }
    }
    val currentMemberId by authViewModel.currentMemberId.collectAsState()
    var loadingMemberId by remember { mutableStateOf(false) }
    LaunchedEffect(groupId) {
        loadingMemberId = true
        authViewModel.loadCurrentMemberId(groupId)
        loadingMemberId = false
    }
    val memberTotals by savingsViewModel.memberTotals.collectAsState()
    val savingsState by savingsViewModel.state.collectAsState()
    val memberState by memberViewModel.state.collectAsState()
    val activeCycle by homeViewModel.activeCycle.collectAsState()

    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var determinedMonth by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val allMemberCycles by savingsViewModel.allMemberCycles.collectAsState()
    val totalSavings = remember(allMemberCycles) {
        allMemberCycles.flatMap { it.savingsEntries }.sumOf { it.amount }
    }

    LaunchedEffect(memberId) {
        savingsViewModel.handleEvent(SavingsEvent.GetAllMemberCycles(memberId))
    }

    LaunchedEffect(Unit) {
        homeViewModel.activeCycle.collect { cycle ->
            cycle?.cycleId?.let { savingsViewModel.initializeCycleId(it) }
        }
    }

    val recorderNames = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(allMemberCycles) {
        val uniqueRecorderIds = allMemberCycles
            .flatMap { it.savingsEntries }
            .mapNotNull { it.recordedBy }
            .distinct()

        // Add current member's name to recorderNames
        currentMember.value?.let { member ->
            recorderNames[member.memberId] = member.name
        }

        // Fetch other recorder names
        uniqueRecorderIds.forEach { id ->
            if (!recorderNames.containsKey(id)) {
                memberViewModel.getMemberNameById(id)?.let { name ->
                    recorderNames[id] = name
                }
            }
        }
    }
    // Function to determine the target month
    fun determineSavingsMonth(): String {
        if (allMemberCycles.isEmpty() || activeCycle == null) {
            return SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        val currentDate = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(currentDate.time)

        // Get the active cycle
        val activeCycleWithSavings = allMemberCycles.find { it.cycle.cycleId == activeCycle?.cycleId }
            ?: return currentMonth

        val monthlyTarget = activeCycleWithSavings.cycle.monthlySavingsAmount

        // Group savings by month for the active cycle
        val savingsByMonth = activeCycleWithSavings.savingsEntries.groupBy { entry ->
            try {
                SimpleDateFormat("MM/yyyy", Locale.getDefault())
                    .format(Date(entry.entryDate.toLong()))
            } catch (e: Exception) {
                "01/1970"
            }
        }

        val cycleStartDate = Date(activeCycle!!.startDate)
        val calendar = Calendar.getInstance().apply { time = cycleStartDate }

        // Iterate from cycle start to current month
        while (!calendar.after(currentDate)) {
            val monthKey = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calendar.time)

            // Skip future months
            if (calendar.after(currentDate)) break

            val monthlyTotal = savingsByMonth[monthKey]?.sumOf { it.amount } ?: 0

            // Found an incomplete month
            if (monthlyTotal < monthlyTarget) {
                return monthKey
            }

            calendar.add(Calendar.MONTH, 1)
        }

        // All previous months are complete, return current month
        return currentMonth
    }

    val determinedMonthDisplay = remember(determinedMonth) {
        convertToDisplayFormat(determinedMonth)
    }

    // Get active cycle
    LaunchedEffect(Unit) {
        homeViewModel.handleEvent(CycleEvent.GetActiveCycle)
    }

    // Fetch member details
    LaunchedEffect(memberId) {
        memberViewModel.handleEvent(MemberEvent.GetMemberDetails(memberId))
    }

    // Update determined month when dependencies change
    LaunchedEffect(allMemberCycles, activeCycle) {
        determinedMonth = determineSavingsMonth()
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
                actions = {
                    IconButton(onClick = {
                        authViewModel.logout()
                        navController.navigate(AuthDestination.route) {
                            popUpTo(0) { inclusive = true }
                        }
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        snackbarHost = {
            SnackbarHost(hostState = snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    modifier = Modifier.padding(8.dp)
                )
            }
        },
        floatingActionButton = {
            if (activeCycle != null) {
                FloatingActionButton(
                    onClick = {
                        amount = activeCycle?.monthlySavingsAmount?.toString() ?: ""
                        selectedMonth = ""
                        showAddSavingsDialog = true
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Savings")
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val scrollState = rememberLazyListState()
            val profileHeight = 250.dp
            val minHeaderHeight = 130.dp

            LazyColumn(
                state = scrollState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = profileHeight),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title section
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Spacer(Modifier.height(16.dp))
                        Divider()
                        Spacer(Modifier.height(16.dp))
                        Text("Savings History", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Total Savings: KES $totalSavings",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(16.dp))
                    }
                }

                // Savings list
                if (allMemberCycles.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth(), Alignment.Center) {
                            Text("No savings history available")
                        }
                    }
                } else {
                    allMemberCycles.forEachIndexed { index, cycleWithSavings ->
                        item(key = "cycle_${cycleWithSavings.cycle.cycleId}") {
                            CycleSavingsSection(
                                cycleWithSavings = cycleWithSavings,
                                recorderNames = recorderNames,
                                memberId = memberId,
                                onAddSavings = { month, amountToAdd ->
                                    selectedMonth = month
                                    amount = amountToAdd.toString()
                                    showAddSavingsDialog = true
                                },
                                isActiveCycle = cycleWithSavings.cycle.cycleId == activeCycle?.cycleId,
                                determinedMonthDisplay = determinedMonthDisplay
                            )
                        }

                        if (index < allMemberCycles.lastIndex) {
                            item(key = "divider_$index") {
                                Divider(
                                    thickness = 4.dp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Collapsible Profile Header
            Box(
                modifier = Modifier
                    .height(
                        maxOf(
                            minHeaderHeight,
                            profileHeight - scrollState.firstVisibleItemScrollOffset.dp
                        )
                    )
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .align(Alignment.TopCenter),
                contentAlignment = Alignment.Center
            ) {
                if (memberState is MemberState.Loading || savingsState is SavingsState.Loading) {
                    Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                    return@Box
                }

                (memberState as? MemberState.Error)?.let { errorState ->
                    Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        Text(errorState.message)
                    }
                    return@Box
                }

                (savingsState as? SavingsState.Error)?.let { errorState ->
                    Box(Modifier.fillMaxWidth(), Alignment.Center) {
                        LaunchedEffect(errorState) {
                            snackbarHostState.showSnackbar("Error: ${errorState.message}")
                        }
                    }
                    return@Box
                }

                val member = (memberState as? MemberState.MemberDetails)?.member ?: return@Box

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
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

                    Spacer(Modifier.height(4.dp))
                    Text(
                        member.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        member.phoneNumber,
                        fontSize = 18.sp,
                        color = Color.Gray
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Total Savings: KES $totalSavings",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Savings dialog
        if (showAddSavingsDialog) {
            AddSavingsDialog(
                amount = amount,
                onAmountChange = { amount = it },
                month = selectedMonth,
                onMonthChange = { selectedMonth = it },
                monthlyTarget = activeCycle?.monthlySavingsAmount ?: 0,
                onDismiss = { showAddSavingsDialog = false },
                getCurrentTotal = { monthInput ->
                    // Calculate current total for the month from all cycles
                    val displayMonth = convertToDisplayFormat(monthInput)
                    allMemberCycles.flatMap { it.savingsEntries }
                        .filter {
                            try {
                                val date = Date(it.entryDate.toLong())
                                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date) == displayMonth
                            } catch (e: Exception) { false }
                        }
                        .sumOf { it.amount }
                },
                onSave = {
                    val targetCycleId = activeCycle?.cycleId ?: run {
                        scope.launch { snackbarHostState.showSnackbar("No active cycle found") }
                        return@AddSavingsDialog
                    }
                    val monthlyTargetValue = activeCycle?.monthlySavingsAmount ?: 0
                    val targetMonth = selectedMonth.ifBlank { determinedMonth }
                    val amountValue = amount.toIntOrNull() ?: 0

                    // Fix: Get current member ID properly
                    val recordedById = if (!loadingMemberId) {
                        currentMemberId ?: run {
                            scope.launch {
                                snackbarHostState.showSnackbar("Member ID not found. Please try again.")
                            }
                            return@AddSavingsDialog
                        }
                    } else {
                        scope.launch {
                            snackbarHostState.showSnackbar("Loading member information...")
                        }
                        return@AddSavingsDialog
                    }


                    val displayMonth = convertToDisplayFormat(targetMonth)
                    val currentTotalForMonth = allMemberCycles.flatMap { it.savingsEntries }
                        .filter {
                            try {
                                val date = Date(it.entryDate.toLong())
                                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date) == displayMonth
                            } catch (e: Exception) { false }
                        }
                        .sumOf { it.amount }
                    val remaining = monthlyTargetValue - currentTotalForMonth

                    if (amountValue > remaining) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Amount exceeds remaining target! Max: KES $remaining")
                        }
                        return@AddSavingsDialog
                    }

                    scope.launch {
                        try {
                        savingsViewModel.handleEvent(
                            SavingsEvent.RecordSavings(
                                cycleId = targetCycleId,
                                monthYear = targetMonth,
                                memberId = memberId,
                                amount = amountValue,
                                recordedBy = recordedById, // Now a String
                                groupId = groupId
                            )
                        )
                            showAddSavingsDialog = false
                            snackbarHostState.showSnackbar("Savings recorded")
                        } catch (e: Exception) {
                            snackbarHostState.showSnackbar("Error: ${e.message}")
                        }
                    }
                },
                determinedMonth = determinedMonth
            )
        }
    }
}

private fun convertToDisplayFormat(inputMonth: String): String {
    return try {
        val parsedDate = SimpleDateFormat("MM/yyyy", Locale.getDefault()).parse(inputMonth)
        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(parsedDate)
    } catch (e: Exception) {
        inputMonth
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
    determinedMonth: String,
    getCurrentTotal: (String) -> Int
) {
    val currentTotalForMonth = remember(month) { getCurrentTotal(month) }
    val remaining = remember(amount, currentTotalForMonth) {
        val entered = amount.toIntOrNull() ?: 0
        monthlyTarget - currentTotalForMonth - entered
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Savings") },
        text = {
            Column {
                Text(
                    text = "Auto-selected month: $determinedMonth",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(16.dp))

                OutlinedTextField(
                    value = amount,
                    onValueChange = {
                        if (it.isEmpty() || it.toIntOrNull() != null) onAmountChange(it)
                    },
                    label = { Text("Amount") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Remaining to target: KES ${maxOf(0, remaining)}",
                    color = if (remaining > 0) Color.Red else Color.Green
                )
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = month,
                    onValueChange = {
                        if (it.matches(Regex("^\\d{0,2}/?\\d{0,4}\$"))) {
                            onMonthChange(it)
                        }
                    },
                    label = { Text("Month (MM/YYYY)") },
                    placeholder = { Text("e.g. 06/2025") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))
                Text("Monthly target: KES $monthlyTarget", color = MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = { Button(onClick = onSave) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun MonthlySavingsCard(
    month: String,
    entries: List<MonthlySavingEntry>,
    monthlyTarget: Int,
    onClick: () -> Unit,
    isCurrentMonth: Boolean,
    recorderNames: Map<String, String>,
    isFuture: Boolean,
    isComplete: Boolean,
    modifier: Modifier = Modifier
) {
    val totalSavings = entries.sumOf { it.amount }
    val remaining = monthlyTarget - totalSavings
    val progress = remember(totalSavings, monthlyTarget) {
        if (monthlyTarget > 0) totalSavings.toFloat() / monthlyTarget else 1f
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        onClick = { if (isCurrentMonth && !isComplete) onClick() }
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween) {
                Text(month, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(
                    text = when {
                        isFuture -> "Upcoming"
                        isComplete -> "Completed"
                        isCurrentMonth -> "Current"
                        else -> "Incomplete"
                    },
                    color = when {
                        isFuture -> MaterialTheme.colorScheme.primary
                        isComplete -> Color.Green
                        isCurrentMonth -> MaterialTheme.colorScheme.primary
                        else -> Color.Red
                    }
                )
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = if (isComplete) Color.Green else MaterialTheme.colorScheme.primary
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "KES $totalSavings / KES $monthlyTarget",
                modifier = Modifier.align(Alignment.End),
                fontSize = 14.sp
            )
            Spacer(Modifier.height(8.dp))

            if (entries.isNotEmpty()) {
                val dateFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
                entries.forEach { entry ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth(),
                            Arrangement.SpaceBetween
                        ) {
                            val date = try {
                                Date(entry.entryDate.toLong())
                            } catch (e: Exception) {
                                Date()
                            }
                            Text(dateFormat.format(date))
                            Text("KES ${entry.amount}", fontWeight = FontWeight.Medium)
                        }
                        Text(
                            "Recorded by: ${recorderNames[entry.recordedBy] ?: "Unknown"}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp))
            }

            if (!isComplete) {
                Button(
                    onClick = onClick,
                    enabled = !isFuture,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Add Savings (KES $remaining left)")
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

@Composable
fun CycleSavingsSection(
    cycleWithSavings: CycleWithSavings,
    recorderNames: Map<String, String>,
    memberId: String,
    onAddSavings: (String, Int) -> Unit,
    isActiveCycle: Boolean,
    determinedMonthDisplay: String
) {
    val cycle = cycleWithSavings.cycle
    val entries = cycleWithSavings.savingsEntries
    val monthlyTarget = cycle.monthlySavingsAmount

    // Generate months for this cycle
    val allMonths = generateMonthsForCycle(
        startDate = cycle.startDate,
        endDate = cycle.endDate,
        isActive = isActiveCycle
    )

    // Group entries by month
    val groupedByMonth = entries.groupBy { entry ->
        try {
            val date = Date(entry.entryDate.toLong())
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
        } catch (e: Exception) {
            "Unknown"
        }
    }

    Column(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Cycle header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Cycle: ${formatDate(cycle.startDate)} - ${
                    cycle.endDate?.let { formatDate(it) } ?: "Present"
                }",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Monthly: KES $monthlyTarget",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Monthly cards for this cycle
        allMonths.forEach { monthName ->
            val monthKey = convertMonthToInputFormat(monthName)
            val monthEntries = groupedByMonth[monthName] ?: emptyList()
            val currentTotal = monthEntries.sumOf { it.amount }
            val isCurrentMonth = monthName == determinedMonthDisplay && isActiveCycle
            val isFuture = isFutureMonth(monthName)
            val isComplete = currentTotal >= monthlyTarget

            // Only show incomplete months for past cycles
            if (!isActiveCycle && isComplete) return@forEach

            MonthlySavingsCard(
                month = monthName,
                entries = monthEntries,
                monthlyTarget = monthlyTarget,
                onClick = {
                    onAddSavings(monthKey, monthlyTarget - currentTotal)
                },
                isCurrentMonth = isCurrentMonth,
                recorderNames = recorderNames,
                isFuture = isFuture,
                isComplete = isComplete,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }
    }
}

// Helper functions
private fun generateMonthsForCycle(
    startDate: Long,
    endDate: Long?,
    isActive: Boolean
): List<String> {
    val months = mutableListOf<String>()
    val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val startCal = Calendar.getInstance().apply {
        time = Date(startDate)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val endCal = if (endDate != null) {
        Calendar.getInstance().apply {
            time = Date(endDate)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    } else if (isActive) {
        // For active cycle: show until current + 2 months
        Calendar.getInstance().apply {
            add(Calendar.MONTH, 2)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    } else {
        // For completed cycles without end date
        Calendar.getInstance()
    }

    while (!startCal.after(endCal)) {
        months.add(format.format(startCal.time))
        startCal.add(Calendar.MONTH, 1)
    }

    return months
}

private fun formatDate(timestamp: Long): String {
    return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))
}

private fun isFutureMonth(monthName: String): Boolean {
    return try {
        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val monthDate = format.parse(monthName)
        monthDate?.after(Date()) ?: false
    } catch (e: Exception) {
        false
    }
}