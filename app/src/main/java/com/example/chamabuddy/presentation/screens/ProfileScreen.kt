package com.example.chamabuddy.presentation.screens

import android.R.interpolator.cycle
import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
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
import com.example.chamabuddy.presentation.viewmodel.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

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
    val members by savingsViewModel.members.collectAsState()

    val currentUserIsAdmin by memberViewModel.currentUserIsAdmin.collectAsState()

    LaunchedEffect(groupId, currentUser?.userId) {
        currentUser?.userId?.let { userId ->
            memberViewModel.loadCurrentUserRole(groupId, userId)
        }
    }

    val currentMember = members[memberId]  // More efficient than find()
    val isAdmin = currentMember?.isAdmin ?: false



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
    val activeCycle by savingsViewModel.activeCycle.collectAsState()

    LaunchedEffect(groupId) {
        savingsViewModel.initializeGroupId(groupId)
        homeViewModel.handleEvent(CycleEvent.GetActiveCycle) // Force refresh
    }

    var showAddSavingsDialog by remember { mutableStateOf(false) }
    var selectedMonth by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var determinedMonth by remember { mutableStateOf("") }

    // State for deletion
    var showDeleteEntryDialog by remember { mutableStateOf(false) }
    var showDeleteMonthDialog by remember { mutableStateOf(false) }
    var entryToDelete by remember { mutableStateOf<MonthlySavingEntry?>(null) }
    var monthToDelete by remember { mutableStateOf<Pair<String, String>?>(null) } // (cycleId, monthYear)

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val allMemberCycles by savingsViewModel.allMemberCycles.collectAsState()
    val totalSavings = remember(allMemberCycles) {
        allMemberCycles.flatMap { it.savingsEntries }.sumOf { it.amount }
    }

    // Admin check - FIXED: Use proper initialization

    // State for expanded/collapsed cycles
    val expandedCycles = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(memberId) {
        savingsViewModel.handleEvent(SavingsEvent.GetAllMemberCycles(memberId))
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

        currentMember?.let { member ->
            recorderNames[member.memberId] = member.name
        }

        uniqueRecorderIds.forEach { id ->
            if (!recorderNames.containsKey(id)) {
                memberViewModel.getMemberNameById(id)?.let { name ->
                    recorderNames[id] = name
                }
            }
        }
    }

    // Handle deletion states
    when (savingsState) {
        is SavingsState.EntryDeleted -> {
            LaunchedEffect(Unit) {
                snackbarHostState.showSnackbar("Entry deleted")
                savingsViewModel.handleEvent(SavingsEvent.ResetSavingsState)
            }
        }
        is SavingsState.MonthDeleted -> {
            LaunchedEffect(Unit) {
                snackbarHostState.showSnackbar("Month's savings deleted")
                savingsViewModel.handleEvent(SavingsEvent.ResetSavingsState)
                savingsViewModel.handleEvent(SavingsEvent.GetAllMemberCycles(memberId))
            }
        }
        else -> {}
    }

    // Function to determine the target month
    fun determineSavingsMonth(): String {
        if (allMemberCycles.isEmpty() || activeCycle == null) {
            return SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())
        }

        val currentDate = Calendar.getInstance()
        val currentMonth = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(currentDate.time)

        val activeCycleWithSavings = allMemberCycles
            .sortedByDescending { it.cycle.cycleId }
            .firstOrNull { it.cycle.cycleId == activeCycle?.cycleId }
            ?: return SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(Date())

        val monthlyTarget = activeCycleWithSavings.cycle.monthlySavingsAmount

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

        while (!calendar.after(currentDate)) {
            val monthKey = SimpleDateFormat("MM/yyyy", Locale.getDefault()).format(calendar.time)

            if (calendar.after(currentDate)) break

            val monthlyTotal = savingsByMonth[monthKey]?.sumOf { it.amount } ?: 0

            if (monthlyTotal < monthlyTarget) {
                return monthKey
            }

            calendar.add(Calendar.MONTH, 1)
        }

        return currentMonth
    }

    val determinedMonthDisplay = remember(determinedMonth) {
        convertToDisplayFormat(determinedMonth)
    }

    LaunchedEffect(Unit) {
        homeViewModel.handleEvent(CycleEvent.GetActiveCycle)
    }

    LaunchedEffect(memberId) {
        memberViewModel.handleEvent(MemberEvent.GetMemberDetails(memberId))
    }

    LaunchedEffect(allMemberCycles, activeCycle) {
        determinedMonth = determineSavingsMonth()
    }

    val member = when (memberState) {
        is MemberState.MemberDetails -> (memberState as MemberState.MemberDetails).member
        else -> null
    }
    val allCycles by homeViewModel.allCyclesForGroup.collectAsState()

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val scrollState = rememberLazyListState()

    LaunchedEffect(groupId) {
        savingsViewModel.handleEvent(SavingsEvent.GetAllMemberCycles(memberId))
        homeViewModel.loadAllCyclesForGroup(groupId)
    }
    if (currentMember == null) {
        Log.e("ProfileScreen", "Member $memberId not found in group")
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
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
            Log.d("ProfileScreen", "Showing FAB for admin: $currentUserIsAdmin, activeCycle: ${activeCycle?.cycleId}")
            if (currentUserIsAdmin && activeCycle != null) {
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
            } else {
                Log.d("ProfileScreen", "FAB hidden. Admin: $currentUserIsAdmin, ActiveCycle: $activeCycle")
            }
        }
    )
     { innerPadding ->
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
                            CircularProgressIndicator(color = PremiumNavy)
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }

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
                                determinedMonthDisplay = determinedMonthDisplay,
                                currentUserIsAdmin = currentUserIsAdmin,
                                onDeleteEntry = { entry ->
                                    entryToDelete = entry
                                    showDeleteEntryDialog = true
                                },
                                onDeleteMonth = { cycleId, monthKey ->
                                    monthToDelete = Pair(cycleId, monthKey)
                                    showDeleteMonthDialog = true
                                }
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

        if (showAddSavingsDialog && currentUserIsAdmin) {
            AddSavingsDialog(
                amount = amount,
                onAmountChange = { amount = it },
                month = selectedMonth,
                onMonthChange = { selectedMonth = it },
                cycles = allCycles,
                onDismiss = { showAddSavingsDialog = false },
                getCurrentTotal = { cycleId, monthInput ->
                    val displayMonth = convertToDisplayFormat(monthInput)
                    allMemberCycles.find { it.cycle.cycleId == cycleId }?.savingsEntries
                        ?.filter {
                            try {
                                val date = Date(it.entryDate.toLong())
                                SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date) == displayMonth
                            } catch (e: Exception) { false }
                        }
                        ?.sumOf { it.amount } ?: 0
                },
                // ProfileScreen.kt
                onSave = { cycleId ->
                    scope.launch {
                        try {
                            // 🔹 Validation: Ensure cycle exists and belongs to group
                            val cycle = allCycles.find { it.cycleId == cycleId }
                                ?: throw Exception("Selected cycle not found")

                            if (cycle.groupId != groupId) {
                                throw Exception("Cycle does not belong to this group")
                            }

                            val recordedById = if (!loadingMemberId) {
                                currentMemberId ?: throw Exception("Member ID not found")
                            } else {
                                throw Exception("Loading member information...")
                            }

                            val targetMonth = selectedMonth.ifBlank { determinedMonth }
                            val amountValue = amount.toIntOrNull() ?: 0
                            val displayMonth = convertToDisplayFormat(targetMonth)

                            // 🔹 Local validation: check remaining amount
                            val currentTotalForMonth = allMemberCycles.find { it.cycle.cycleId == cycleId }?.savingsEntries
                                ?.filter {
                                    try {
                                        val date = Date(it.entryDate.toLong())
                                        SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(date) == displayMonth
                                    } catch (e: Exception) { false }
                                }
                                ?.sumOf { it.amount } ?: 0

                            val remaining = cycle.monthlySavingsAmount - currentTotalForMonth
                            if (amountValue > remaining) {
                                snackbarHostState.showSnackbar("Amount exceeds remaining target! Max: KES $remaining")
                                return@launch
                            }

                            // 🔹 Repository call (may throw validation errors like oversaving)
                            savingsViewModel.handleEvent(
                                SavingsEvent.RecordSavings(
                                    cycleId = cycleId,
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
                            // 🔹 Handle validation errors from repository (like oversaving)
                            val errorMessage = when {
                                e.message?.contains("Cannot save more than monthly target") == true ->
                                    e.message!!
                                else -> "Error: ${e.message}"
                            }
                            snackbarHostState.showSnackbar(errorMessage)
                        }
                    }
                },
                determinedMonth = determinedMonth
            )
        }

        // Deletion dialogs
        if (showDeleteEntryDialog) {
            DeleteConfirmationDialog(
                title = "Delete Entry",
                message = "Are you sure you want to delete this savings entry?",
                onConfirm = {
                    entryToDelete?.let {
                        savingsViewModel.handleEvent(SavingsEvent.DeleteEntry(it.entryId, memberId))
                    }
                    showDeleteEntryDialog = false
                },
                onDismiss = { showDeleteEntryDialog = false }
            )
        }

         if (showDeleteMonthDialog) {
             DeleteConfirmationDialog(
                 title = "Delete Month",
                 message = "Are you sure you want to delete all entries for this month?",
                 onConfirm = {
                     monthToDelete?.let { (cycleId, monthYear) ->
                         savingsViewModel.handleEvent(SavingsEvent.DeleteMonth(cycleId, monthYear, groupId, memberId))
                     }
                     showDeleteMonthDialog = false
                 },
                 onDismiss = { showDeleteMonthDialog = false }
             )
         }
    }
}

private fun convertToDisplayFormat(inputMonth: String): String {
    return try {
        val parsedDate = if (inputMonth.contains("/")) {
            SimpleDateFormat("MM/yyyy", Locale.getDefault()).parse(inputMonth)
        } else {
            SimpleDateFormat("MMM yyyy", Locale.getDefault()).parse(inputMonth)
        }
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
    cycles: List<Cycle>,
    onDismiss: () -> Unit,
    onSave: (String) -> Unit,
    determinedMonth: String,
    getCurrentTotal: (String, String) -> Int
) {
    var selectedCycle by remember { mutableStateOf<Cycle?>(null) }
    val monthlyTarget = selectedCycle?.monthlySavingsAmount ?: 0

    val currentTotalForMonth = remember(month, selectedCycle) {
        selectedCycle?.let { cycle ->
            getCurrentTotal(cycle.cycleId, month)
        } ?: 0
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
                Text("Select Cycle:", color = PremiumNavy, fontWeight = FontWeight.Medium)
                Spacer(Modifier.height(8.dp))
                CycleDropdown(
                    cycles = cycles.sortedByDescending { it.startDate },
                    selectedCycle = selectedCycle,
                    onCycleSelected = { selectedCycle = it },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(16.dp))

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
                onClick = {
                    if (selectedCycle != null) {
                        onSave(selectedCycle!!.cycleId)
                    }
                },
                enabled = selectedCycle != null,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDropdown(
    cycles: List<Cycle>,
    selectedCycle: Cycle?,
    onCycleSelected: (Cycle) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier
    ) {
        OutlinedTextField(
            value = selectedCycle?.let {
                val dateRange = "${formatDateShort(it.startDate)} - ${it.endDate?.let { end -> formatDateShort(end) } ?: "Active"}"
                "Cycle ${it.cycleNumber} (ID: ${it.cycleId.takeLast(6)}): $dateRange"
            } ?: "Select Cycle",
            onValueChange = {},
            readOnly = true,
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
            },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            colors = TextFieldDefaults.outlinedTextFieldColors(
                containerColor = Color.White,
                focusedBorderColor = PremiumNavy,
                unfocusedBorderColor = PremiumNavy.copy(alpha = 0.5f)
            )
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            cycles.forEach { cycle ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Cycle ${cycle.cycleNumber} (ID: ${cycle.cycleId.takeLast(6)}): ${
                                formatDateShort(cycle.startDate)
                            } - ${
                                cycle.endDate?.let { formatDateShort(it) } ?: "Active"
                            }"
                        )
                    },
                    onClick = {
                        onCycleSelected(cycle)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun formatDateShort(timestamp: Long): String {
    return SimpleDateFormat("MMM yyyy", Locale.getDefault()).format(Date(timestamp))
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
    currentUserIsAdmin: Boolean,
    onDeleteEntry: (MonthlySavingEntry) -> Unit,
    onDeleteMonth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalSavings = entries.sumOf { it.amount }
    val remaining = monthlyTarget - totalSavings
    val progress = remember(totalSavings, monthlyTarget) {
        if (monthlyTarget > 0) totalSavings.toFloat() / monthlyTarget else 1f
    }
    var isLongPressed by remember { mutableStateOf(false) }
    Card(
        modifier = modifier
            .fillMaxWidth()
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = {
                        if (currentUserIsAdmin && isCurrentMonth && !isComplete) onClick()
                    },
                    onLongPress = {
                        if (currentUserIsAdmin) {
                            isLongPressed = true
                            onDeleteMonth()
                        }
                    }
                )
            }
            .background(
                if (isLongPressed) LightGray.copy(alpha = 0.3f)
                else Color.Transparent
            )
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
                val entryDateFormat = remember { SimpleDateFormat("MMM dd yyyy", Locale.getDefault()) }
                entries.forEach { entry ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .then(if (currentUserIsAdmin) Modifier.clickable { onDeleteEntry(entry) } else Modifier),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentUserIsAdmin) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete entry",
                                tint = Color.Red,
                                modifier = Modifier
                                    .size(24.dp)
                                    .padding(end = 8.dp)
                            )
                        }
                        Column(Modifier.weight(1f)) {
                            Row(
                                Modifier.fillMaxWidth(),
                                Arrangement.SpaceBetween
                            ) {
                                val date = try {
                                    Date(entry.entryDate.toLong())
                                } catch (e: Exception) {
                                    Date()
                                }
                                Text(
                                    // Format actual entry date
                                    text = try {
                                        entryDateFormat.format(Date(entry.entryDate.toLong()))
                                    } catch (e: Exception) {
                                        "Unknown date"
                                    },
                                    color = PremiumNavy
                                )
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
                }
                Divider(Modifier.padding(vertical = 8.dp), color = PremiumNavy.copy(alpha = 0.1f))
            }

            if (!isComplete) {
                Button(
                    onClick = onClick,
                    enabled = !isFuture && remaining > 0, // Disable when no remaining
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (remaining > 0) VibrantOrange else Color.Gray
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                ) {
                    Text(
                        if (remaining > 0) "Add Savings (KES $remaining left)"
                        else "Target Reached",
                        color = Color.White
                    )
                }
            }
        }
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
    determinedMonthDisplay: String,
    currentUserIsAdmin: Boolean,
    onDeleteEntry: (MonthlySavingEntry) -> Unit,
    onDeleteMonth: (cycleId: String, monthKey: String) -> Unit
) {
    val cycle = cycleWithSavings.cycle
    val dateFormat = SimpleDateFormat("MMM yyyy", Locale.getDefault())

    val months = cycleWithSavings.savingsEntries
        .map { it.monthYear }  // Use target month for grouping
        .distinct()
        .sortedBy { monthYear ->
            try {
                val format = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                format.parse(monthYear)?.time ?: 0L
            } catch (e: Exception) { 0L }
        }


    val startDate = formatDateShort(cycle.startDate)
    val endDate = cycle.endDate?.let { formatDateShort(it) } ?: "Active"
    val cycleIdShort = cycle.cycleId.takeLast(6)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onExpandToggle(cycle.cycleId) }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        "Cycle $cycleIdShort",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = PremiumNavy
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$startDate - $endDate",
                        fontSize = 14.sp,
                        color = PremiumNavy.copy(alpha = 0.7f)
                    )
                }

                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    tint = PremiumNavy
                )
            }

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

            AnimatedVisibility(visible = expanded) {
                Column {
                    months.forEach { month ->
                        val monthEntries = cycleWithSavings.savingsEntries.filter { it.monthYear == month }


                        val isCurrentMonth = month == determinedMonthDisplay && isActiveCycle
                        val isFuture = isFutureMonth(month)
                        val isComplete = monthEntries.sumOf { it.amount } >= cycle.monthlySavingsAmount

                        MonthlySavingsCard(
                            month = convertToDisplayFormat(month),
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
                            currentUserIsAdmin = currentUserIsAdmin,
                            onDeleteEntry = onDeleteEntry,
                            onDeleteMonth = { onDeleteMonth(cycle.cycleId, month) },
                            modifier = Modifier.padding(8.dp)
                        )
                    }
                }
            }
        }
    }
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

@Composable
fun DeleteConfirmationDialog(
    title: String,
    message: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
