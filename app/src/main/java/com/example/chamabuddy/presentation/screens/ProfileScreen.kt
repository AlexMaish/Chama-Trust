package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.CycleEvent
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

    val memberTotals by savingsViewModel.memberTotals.collectAsState()

    val savingsState by savingsViewModel.state.collectAsState()
    val memberSavings by savingsViewModel.memberSavings.collectAsState()
    val memberState by memberViewModel.state.collectAsState()
    val activeCycle by homeViewModel.activeCycle.collectAsState()

    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var determinedMonth by remember { mutableStateOf("") }

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val totalSavings = remember(memberTotals) {
        memberSavings.sumOf { it.amount }
    }



    val totalSavingsByCycle = remember(memberSavings) {
        memberSavings.sumOf { it.amount }
    }


    LaunchedEffect(Unit) {
        homeViewModel.activeCycle.collect { cycle ->
            cycle?.cycleId?.let { savingsViewModel.initializeCycleId(it) }  // Changed to new method name
        }
    }

    val recorderNames = remember { mutableStateMapOf<String, String>() }
    LaunchedEffect(memberSavings) {
        // Get unique recorder IDs
        val recorderIds = memberSavings
            .map { it.recordedBy }
            .filter { it != null && it.isNotBlank() }
            .distinct()

        // Fetch names for each recorder
        recorderIds.forEach { id ->
            val result = memberViewModel.getMemberNameById(memberId)
            result?.let { name -> recorderNames[memberId] = name }
        }
    }
    // Function to determine the target month
    fun determineSavingsMonth(): String {
        if (memberSavings.isEmpty() || activeCycle == null) {
            return SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Calendar.getInstance().time)
        }

        val savingsByMonth = memberSavings.groupBy { entry ->
            try {
                SimpleDateFormat("MM/yyyy", Locale.getDefault())
                    .format(Date(entry.entryDate.toLong()))
            } catch (e: Exception) {
                "01/1970"
            }
        }

        val currentDate = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(currentDate.time)
        val cycleStartDate = Date(activeCycle!!.startDate)
        val calendar = Calendar.getInstance().apply { time = cycleStartDate }
        val monthlyTarget = activeCycle!!.monthlySavingsAmount

        // Iterate from cycle start to current month
        while (calendar.before(currentDate) || calendar.get(Calendar.MONTH) == currentDate.get(Calendar.MONTH)) {
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

    // Fetch member details and savings
    LaunchedEffect(memberId, activeCycle?.cycleId) {
        memberViewModel.handleEvent(MemberEvent.GetMemberDetails(memberId))
        activeCycle?.cycleId?.let { cycleId ->
            savingsViewModel.handleEvent(SavingsEvent.GetMemberSavings(cycleId, memberId))
        } ?: run {
            savingsViewModel.handleEvent(SavingsEvent.ResetSavingsState)
        }
    }

    // Update determined month when dependencies change
    LaunchedEffect(memberSavings, activeCycle) {
        determinedMonth = determineSavingsMonth()
    }

    val allMonths = remember(activeCycle, memberSavings) {
        val months = mutableSetOf<String>()  // Use Set to avoid duplicates
        val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())

        // 1. Add all months from savings entries
        memberSavings.forEach { entry ->
            try {
                val date = Date(entry.entryDate.toLong())
                months.add(format.format(date))
            } catch (e: Exception) {
                // Handle error if needed
            }
        }

        // 2. Add upcoming months (current + next 2 months)
        activeCycle?.let { cycle ->
            val endCal = Calendar.getInstance().apply {
                add(Calendar.MONTH, 2)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            val startCal = Calendar.getInstance().apply {
                time = Date(cycle.startDate)
                set(Calendar.DAY_OF_MONTH, 1)
            }

            while (!startCal.after(endCal)) {
                months.add(format.format(startCal.time))
                startCal.add(Calendar.MONTH, 1)
            }
        }

        // 3. Sort the months chronologically
        months.toList().sortedWith(compareBy {
            try {
                format.parse(it)?.time ?: 0L
            } catch (e: Exception) {
                0L
            }
        })
    }

    val groupedByMonth = remember(memberSavings) {
        memberSavings.groupBy { entry ->
            try {
                // Parse the stored timestamp to get target month
                val date = Date(entry.entryDate.toLong())
                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date)
            } catch (e: Exception) {
                "Unknown"
            }
        }
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
                    // Logout icon-button in the top bar
                    IconButton(onClick = {
                        authViewModel.logout()
                        // Clear **all** backstack and go to auth
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (activeCycle == null) return@FloatingActionButton
                    amount = activeCycle?.monthlySavingsAmount?.toString() ?: ""
                    selectedMonth = ""
                    showAddSavingsDialog = true
                }
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Savings")
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

            // Calculate collapse progress (0f = fully expanded, 1f = fully collapsed)


            // Smoother nested scroll connection


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
                        Text("Monthly Savings", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                        Spacer(Modifier.height(16.dp))
                    }
                }


                // Savings list
                if (allMonths.isEmpty()) {
                    item {
                        Box(Modifier.fillMaxWidth(), Alignment.Center) {
                            Text(if (activeCycle == null) "No active cycle" else "No months available")
                        }
                    }
                } else {
                    items(allMonths) { monthName ->
                        val monthKey = convertMonthToInputFormat(monthName)
                        val entries = groupedByMonth[monthName] ?: emptyList()
                        val monthlyTarget = activeCycle?.monthlySavingsAmount ?: 0
                        val currentTotal = entries.sumOf { it.amount }
                        val isCurrentMonth = monthName == determinedMonthDisplay

                        MonthlySavingsCard(
                            month = monthName,
                            entries = entries,
                            monthlyTarget = monthlyTarget,
                            onClick = {
                                selectedMonth = monthKey
                                amount = (monthlyTarget - currentTotal).coerceAtLeast(0).toString()
                                showAddSavingsDialog = true
                            },
                            isCurrentMonth = isCurrentMonth,
                            recorderNames = recorderNames,
                            modifier = Modifier.padding(horizontal = 16.dp) // Add horizontal padding
                        )
                    }
                }
            }

            // Collapsible Profile Header
            Box(
                modifier = Modifier
                    .height(
                        // Collapse but keep minimum height
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
                // Error handling
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

                // Member info

                val member = (memberState as? MemberState.MemberDetails)?.member ?: return@Box

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    // Animated avatar
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

                    // Name with fade-out animation
                    Text(
                        member.name,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(Modifier.height(8.dp))

                    // Phone number with fade-out animation
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
            val currentTotal = groupedByMonth[convertToDisplayFormat(selectedMonth)]?.sumOf { it.amount } ?: 0
            AddSavingsDialog(
                amount = amount,
                onAmountChange = { amount = it },
                month = selectedMonth,
                onMonthChange = { selectedMonth = it },
                monthlyTarget = activeCycle?.monthlySavingsAmount ?: 0,
                onDismiss = { showAddSavingsDialog = false },
                getCurrentTotal = { monthInput ->
                    val displayMonth = convertToDisplayFormat(monthInput)
                    groupedByMonth[displayMonth]?.sumOf { it.amount } ?: 0
                },
                onSave = {
                    val targetCycleId = activeCycle?.cycleId ?: run {
                        scope.launch { snackbarHostState.showSnackbar("No active cycle found") }
                        return@AddSavingsDialog
                    }
                    val monthlyTargetValue = activeCycle?.monthlySavingsAmount ?: 0
                    val targetMonth = selectedMonth.ifBlank { determinedMonth }
                    val amountValue = amount.toIntOrNull() ?: 0

                    val displayMonth = convertToDisplayFormat(targetMonth)
                    val currentTotalForMonth = groupedByMonth[displayMonth]?.sumOf { it.amount } ?: 0
                    val remaining = monthlyTargetValue - currentTotalForMonth

                    if (amountValue > remaining) {
                        scope.launch {
                            snackbarHostState.showSnackbar("Amount exceeds remaining target! Max: KES $remaining")
                        }
                        return@AddSavingsDialog
                    }

                    scope.launch {
                        savingsViewModel.handleEvent(
                            SavingsEvent.RecordSavings(
                                cycleId = targetCycleId,
                                monthYear = targetMonth,
                                memberId = memberId,
                                amount = amountValue,
                                recordedBy = memberId,
                                groupId = groupId
                            )
                        )
                        showAddSavingsDialog = false
                        snackbarHostState.showSnackbar("Savings recorded")
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
    // Calculate current total dynamically based on selected month
    val currentTotalForMonth = remember(month) {
        getCurrentTotal(month)
    }

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
    modifier: Modifier = Modifier // Added modifier parameter
) {
    val totalSavings = entries.sumOf { it.amount }
    val isComplete = totalSavings >= monthlyTarget
    val isFuture = remember(month) {
        try {
            val format = SimpleDateFormat("MMM yyyy", Locale.getDefault())
            format.parse(month)?.after(Date()) ?: false
        } catch (e: Exception) {
            false
        }
    }
    val remaining = monthlyTarget - totalSavings
    val progress = remember(totalSavings, monthlyTarget) {
        if (monthlyTarget > 0) totalSavings.toFloat() / monthlyTarget else 1f
    }

    Card(
        modifier = modifier.fillMaxWidth(), // Use the passed modifier
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
                        // Use recorder name instead of ID
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

