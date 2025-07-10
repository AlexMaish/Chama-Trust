package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.Cycle
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

// Define colors to match home screen
//val PremiumNavy = Color(0xFF0A1D3A)
//val SoftOffWhite = Color(0xFFF8F9FA)
//val VibrantOrange = Color(0xFFFF6B35)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class, ExperimentalAnimationApi::class)
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

    // State for expanded/collapsed cycles
    val expandedCycles = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(memberId) {
        savingsViewModel.handleEvent(SavingsEvent.GetAllMemberCycles(memberId))
        // Expand all cycles by default
        allMemberCycles.forEach {
            expandedCycles[it.cycle.cycleId] = true
        }
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

    // Get member details
    val member = when (memberState) {
        is MemberState.MemberDetails -> (memberState as MemberState.MemberDetails).member
        else -> null
    }

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberLazyListState()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    // Collapsed title
                    Text(
                        member?.name ?: "Member Profile",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            authViewModel.logout()
                            navController.navigate(AuthDestination.route) {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.largeTopAppBarColors(
                    containerColor = PremiumNavy,
                    scrolledContainerColor = PremiumNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                scrollBehavior = scrollBehavior
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
                    },
                    containerColor = VibrantOrange
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "Add Savings",
                        tint = Color.White
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SoftOffWhite)
                .padding(innerPadding)
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

            LazyColumn(
                state = scrollState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(SoftOffWhite),
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Profile Header
                item {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp)
                    ) {
                        if (member != null) {
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

                            Spacer(Modifier.height(8.dp))
                            Text(
                                member.name,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = PremiumNavy
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                member.phoneNumber,
                                fontSize = 18.sp,
                                color = PremiumNavy.copy(alpha = 0.8f)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Total Savings: KES $totalSavings",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = VibrantOrange
                            )
                        } else {
                            // Loading state
                            CircularProgressIndicator(color = PremiumNavy)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

                // Title section
                item {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                    ) {
                        Text(
                            "Savings History",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = PremiumNavy
                        )
                        Spacer(Modifier.height(8.dp))
                        Divider(color = PremiumNavy.copy(alpha = 0.1f), thickness = 1.dp)
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
                        val cycleId = cycleWithSavings.cycle.cycleId
                        val isExpanded = expandedCycles.getOrDefault(cycleId, true)

                        item(key = "cycle_$cycleId") {
                            CycleSection(
                                cycleWithSavings = cycleWithSavings,
                                expanded = isExpanded,
                                onExpandToggle = {
                                    expandedCycles[cycleId] = !isExpanded
                                },
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
                                    color = PremiumNavy.copy(alpha = 0.1f),
                                    modifier = Modifier.padding(vertical = 24.dp)
                                )
                            }
                        }
                    }
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
                    scope.launch {
                        try {
                            // VALIDATION: Check active cycle exists and belongs to group
                            val activeCycle = homeViewModel.activeCycle.value
                                ?: throw Exception("No active cycle in this group")

                            if (activeCycle.groupId != groupId) {
                                throw Exception("Cycle does not belong to this group")
                            }

                            // Get current member ID
                            val recordedById = if (!loadingMemberId) {
                                currentMemberId ?: throw Exception("Member ID not found")
                            } else {
                                throw Exception("Loading member information...")
                            }

                            val targetMonth = selectedMonth.ifBlank { determinedMonth }
                            val amountValue = amount.toIntOrNull() ?: 0
                            val displayMonth = convertToDisplayFormat(targetMonth)

                            // Calculate remaining target
                            val currentTotalForMonth = allMemberCycles.flatMap { it.savingsEntries }
                                .filter {
                                    try {
                                        val date = Date(it.entryDate.toLong())
                                        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date) == displayMonth
                                    } catch (e: Exception) { false }
                                }
                                .sumOf { it.amount }

                            val remaining = (activeCycle.monthlySavingsAmount ?: 0) - currentTotalForMonth

                            if (amountValue > remaining) {
                                snackbarHostState.showSnackbar("Amount exceeds remaining target! Max: KES $remaining")
                                return@launch
                            }

                            // Record savings
                            savingsViewModel.handleEvent(
                                SavingsEvent.RecordSavings(
                                    cycleId = activeCycle.cycleId,
                                    monthYear = targetMonth,
                                    memberId = memberId,
                                    amount = amountValue,
                                    recordedBy = recordedById,
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
                    color = PremiumNavy,
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
                Text("Monthly target: KES $monthlyTarget", color = PremiumNavy)
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
            ) {
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
                Text(month, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = PremiumNavy)
                Text(
                    text = when {
                        isFuture -> "Upcoming"
                        isComplete -> "Completed"
                        isCurrentMonth -> "Current"
                        else -> "Incomplete"
                    },
                    color = when {
                        isFuture -> PremiumNavy
                        isComplete -> Color.Green
                        isCurrentMonth -> VibrantOrange
                        else -> Color.Red
                    }
                )
            }

            LinearProgressIndicator(
                progress = progress,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                color = if (isComplete) Color.Green else VibrantOrange
            )

            Spacer(Modifier.height(8.dp))
            Text(
                text = "KES $totalSavings / KES $monthlyTarget",
                modifier = Modifier.align(Alignment.End),
                fontSize = 14.sp,
                color = PremiumNavy
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
                            Text(dateFormat.format(date), color = PremiumNavy)
                            Text("KES ${entry.amount}", fontWeight = FontWeight.Medium, color = PremiumNavy)
                        }
                        Text(
                            "Recorded by: ${recorderNames[entry.recordedBy] ?: "Unknown"}",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
                Divider(Modifier.padding(vertical = 8.dp), color = PremiumNavy.copy(alpha = 0.1f))
            }

            if (!isComplete) {
                Button(
                    onClick = onClick,
                    enabled = !isFuture,
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text("Add Savings (KES $remaining left)", color = Color.White)
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
fun CycleSection(
    cycleWithSavings: CycleWithSavings,
    expanded: Boolean,
    onExpandToggle: (String) -> Unit,
    recorderNames: Map<String, String>,
    memberId: String,
    onAddSavings: (String, Int) -> Unit,
    isActiveCycle: Boolean,
    determinedMonthDisplay: String
) {
    val cycle = cycleWithSavings.cycle
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            // Cycle header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle(cycle.cycleId) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        "Cycle: ${cycle.cycleId.takeLast(6)}",
                        fontWeight = FontWeight.Bold,
                        color = PremiumNavy
                    )
                    Text(
                        "Started: ${dateFormat.format(Date(cycle.startDate))}",
                        fontSize = 14.sp,
                        color = PremiumNavy.copy(alpha = 0.7f)
                    )
                    if (cycle.endDate != null) {
                        Text(
                            "Ended: ${dateFormat.format(Date(cycle.endDate))}",
                            fontSize = 14.sp,
                            color = PremiumNavy.copy(alpha = 0.7f)
                        )
                    }
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PremiumNavy
                )
            }

            // Cycle content
            AnimatedVisibility(visible = expanded) {
                Column {
                    // Monthly targets section
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            "Monthly Target:",
                            fontWeight = FontWeight.Medium,
                            color = PremiumNavy
                        )
                        Text(
                            "KES ${cycle.monthlySavingsAmount}",
                            fontWeight = FontWeight.Bold,
                            color = PremiumNavy
                        )
                    }

                    // Monthly savings cards
                    val months = if (isActiveCycle) {
                        generateMonthsUntilFuture(cycle.startDate, 2) // Current + 2 future months
                    } else {
                        generateMonthsForCompletedCycle(cycle)
                    }

                    months.forEach { month ->
                        val monthEntries = cycleWithSavings.savingsEntries.filter {
                            try {
                                val date = Date(it.entryDate.toLong())
                                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date) == month
                            } catch (e: Exception) { false }
                        }

                        val isCurrentMonth = month == determinedMonthDisplay && isActiveCycle
                        val isFuture = isFutureMonth(month)
                        val isComplete = monthEntries.sumOf { it.amount } >= cycle.monthlySavingsAmount

                        MonthlySavingsCard(
                            month = month,
                            entries = monthEntries,
                            monthlyTarget = cycle.monthlySavingsAmount,
                            onClick = {
                                if (!isFuture && !isComplete) {
                                    val inputFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                                    val monthKey = try {
                                        inputFormat.format(dateFormat.parse(month))
                                    } catch (e: Exception) {
                                        ""
                                    }
                                    onAddSavings(monthKey, cycle.monthlySavingsAmount - monthEntries.sumOf { it.amount })
                                }
                            },
                            isCurrentMonth = isCurrentMonth,
                            recorderNames = recorderNames,
                            isFuture = isFuture,
                            isComplete = isComplete,
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
}

// Helper functions
private fun generateMonthsUntilFuture(startDate: Long, futureMonths: Int): List<String> {
    val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val months = mutableListOf<String>()
    val calendar = Calendar.getInstance().apply {
        time = Date(startDate)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val futureCalendar = Calendar.getInstance().apply {
        add(Calendar.MONTH, futureMonths)
    }

    while (!calendar.after(futureCalendar)) {
        months.add(format.format(calendar.time))
        calendar.add(Calendar.MONTH, 1)
    }

    return months
}

private fun generateMonthsForCompletedCycle(cycle: Cycle): List<String> {
    val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
    val months = mutableListOf<String>()
    val calendar = Calendar.getInstance().apply {
        time = Date(cycle.startDate)
        set(Calendar.DAY_OF_MONTH, 1)
    }

    val endCalendar = if (cycle.endDate != null) {
        Calendar.getInstance().apply {
            time = Date(cycle.endDate)
            set(Calendar.DAY_OF_MONTH, 1)
        }
    } else {
        // Shouldn't happen for completed cycle, but handle gracefully
        Calendar.getInstance()
    }

    while (!calendar.after(endCalendar)) {
        months.add(format.format(calendar.time))
        calendar.add(Calendar.MONTH, 1)
    }

    return months
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