package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import java.text.SimpleDateFormat
import java.util.*
import com.example.chamabuddy.R
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.presentation.navigation.ChangePasswordDestination
import com.example.chamabuddy.presentation.viewmodel.AuthViewModel
import com.example.chamabuddy.presentation.viewmodel.GroupHomeViewModel
import com.example.chamabuddy.presentation.viewmodel.HomeViewModel
import com.example.chamabuddy.presentation.viewmodel.SavingsViewModel
import com.example.chamabuddy.workers.SyncWorker

// Add this enum class at the top of the file
enum class GraphType {
    ALL_GROUPS_CUMULATIVE,
    USER_SAVINGS_PER_GROUP
}

// Premium color palette
val PremiumPurple = Color(0xFF7B68EE)

val ChartLineColors = listOf(
    Color(0xFFFF6B35),  // Vibrant Orange
    Color(0xFF4ECDC4),  // Teal
    Color(0xFF45B7D1),  // Light Blue
    Color(0xFF96CEB4),  // Sage Green
    Color(0xFFF9A826),  // Amber
    Color(0xFF6A5ACD),  // Slate Blue
    Color(0xFFD64161)   // Raspberry
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GroupsHomeScreen(
    navigateToGroupCycles: (String) -> Unit,
    parentNavController: NavHostController,
    viewModel: GroupHomeViewModel = hiltViewModel(),
    authViewModel: AuthViewModel = hiltViewModel(),
    savingsViewModel: SavingsViewModel = hiltViewModel(),
    homeViewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val syncStatus by remember { derivedStateOf { SyncWorker.syncStatus.value } }
    val syncState by viewModel.syncState.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()

    // Set default to show "My Savings" graph first
    var graphType by remember { mutableStateOf(GraphType.USER_SAVINGS_PER_GROUP) }

    // Add state for graph loading
    var isGraphLoading by remember { mutableStateOf(false) }

    // Scroll state for detecting scroll direction
    val scrollState = rememberLazyListState()
    var isTopBarVisible by remember { mutableStateOf(true) }
    var previousScroll by remember { mutableStateOf(0) }
    val navController = parentNavController

    // Handle scroll direction to show/hide top bar
    LaunchedEffect(scrollState.isScrollInProgress) {
        if (scrollState.isScrollInProgress) {
            val currentScroll = scrollState.firstVisibleItemScrollOffset
            isTopBarVisible = currentScroll <= previousScroll || currentScroll < 50
            previousScroll = currentScroll
        }
    }

    // Move SnackbarHostState declaration to top so it's available in effects
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle ViewModel sync errors
    LaunchedEffect(syncState) {
        if (syncState is GroupHomeViewModel.SyncState.Error) {
            val error = (syncState as GroupHomeViewModel.SyncState.Error).message
            snackbarHostState.showSnackbar(error)
        }
    }

    // Handle legacy snackbar messages
    LaunchedEffect(uiState.snackbarMessage) {
        uiState.snackbarMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    var groupName by remember { mutableStateOf("") }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val showDialog by remember { derivedStateOf { uiState.showCreateGroupDialog } }
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // State for group deletion
    var groupToDelete by remember { mutableStateOf<Group?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    // Get member counts for all groups
    val groupMemberCounts = remember { uiState.groupMemberCounts.toMutableMap() }

    // Load member counts for each group
    LaunchedEffect(uiState.groups) {
        uiState.groups.forEach { group ->
            if (!groupMemberCounts.containsKey(group.groupId)) {
                homeViewModel.loadGroupData(group.groupId)
            }
        }
    }

    // Collect group data to get member counts
    val groupData by homeViewModel.groupData.collectAsState()
    LaunchedEffect(groupData) {
        groupData?.let {
            groupMemberCounts[it.group.groupId] = it.members.size
            viewModel.updateMemberCounts(groupMemberCounts)
        }
    }

    // Get real savings data for all groups
    val savingsData = remember { mutableStateMapOf<String, List<Int>>() }

    // Key to force recomposition when graph type changes
    val graphTypeKey by remember { derivedStateOf { graphType } }

    // Load savings data when graph type changes
    LaunchedEffect(graphTypeKey, uiState.groups, currentUser?.userId) {
        if (uiState.groups.isEmpty()) {
            savingsData.clear()
            return@LaunchedEffect
        }

        isGraphLoading = true
        val newSavingsData = mutableMapOf<String, List<Int>>()

        withContext(Dispatchers.IO) {
            when (graphType) {
                GraphType.ALL_GROUPS_CUMULATIVE -> {
                    loadCumulativeSavingsAllGroups(uiState.groups, savingsViewModel, newSavingsData)
                }
                GraphType.USER_SAVINGS_PER_GROUP -> {
                    if (!currentUser?.userId.isNullOrEmpty()) {
                        loadUserSavingsPerGroup(
                            uiState.groups,
                            savingsViewModel,
                            newSavingsData,
                            currentUser?.userId
                        )
                    }
                }
            }
        }

        savingsData.clear()
        savingsData.putAll(newSavingsData)
        isGraphLoading = false
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            Box(modifier = Modifier.width(280.dp)) {
                GroupsHomeDrawerContent(
                    currentUser = currentUser,
                    onClose = { scope.launch { drawerState.close() } },
                    onNavigateToChangePassword = {
                        navController.navigate(ChangePasswordDestination.route)
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                AnimatedVisibility(
                    visible = isTopBarVisible,
                    enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                    exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut()
                ) {
                    LargeTopAppBar(
                        title = {
                            Text(
                                "Chama Trust",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(
                                    Icons.Default.Menu,
                                    contentDescription = "Navigation Menu",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            if (uiState.isLoading || !uiState.isSyncComplete) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    modifier = Modifier
                                        .size(24.dp)
                                        .padding(8.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                IconButton(onClick = { viewModel.refreshGroups() }) {
                                    Icon(
                                        Icons.Default.Refresh,
                                        contentDescription = "Refresh",
                                        tint = Color.White
                                    )
                                }
                            }
                        },
                        colors = TopAppBarDefaults.largeTopAppBarColors(
                            containerColor = PremiumNavy,
                            scrolledContainerColor = PremiumNavy,
                            titleContentColor = Color.White
                        ),
                        scrollBehavior = scrollBehavior
                    )
                }
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = { viewModel.showCreateGroupDialog() },
                    containerColor = VibrantOrange
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create new group", tint = Color.White)
                }
            },
            snackbarHost = { SnackbarHost(snackbarHostState) },
            containerColor = SoftOffWhite
        ) { innerPadding ->
            Box(modifier = Modifier.padding(innerPadding)) {

                if (syncState is GroupHomeViewModel.SyncState.SyncingData) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = Color.White)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Syncing your groups...",
                                color = Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                LazyColumn(
                    state = scrollState,
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                ) {
                    // Welcome message section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            Spacer(modifier = Modifier.height(16.dp))

                            AnimatedVisibility(
                                visible = true,
                                enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
                            ) {
                                Column {
                                    Text(
                                        "Welcome, ${currentUser?.username ?: "Member"}!",
                                        style = MaterialTheme.typography.headlineMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumNavy,
                                        modifier = Modifier.padding(top = 8.dp)
                                    )

                                    Text(
                                        "Track your savings across all groups",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = PremiumNavy.copy(alpha = 0.8f)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Graph type selector
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            SingleChoiceSegmentedButtonRow {
                                SegmentedButton(
                                    selected = graphType == GraphType.USER_SAVINGS_PER_GROUP,
                                    onClick = { graphType = GraphType.USER_SAVINGS_PER_GROUP },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("My Savings", fontSize = 12.sp)
                                }
                                SegmentedButton(
                                    selected = graphType == GraphType.ALL_GROUPS_CUMULATIVE,
                                    onClick = { graphType = GraphType.ALL_GROUPS_CUMULATIVE },
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text("All Groups", fontSize = 12.sp)
                                }
                            }
                        }
                    }

                    // Savings graph section
                    item {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp)
                        ) {
                            when (graphType) {
                                GraphType.ALL_GROUPS_CUMULATIVE -> {
                                    if (savingsData.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White),
                                            color = Color.White,
                                            shadowElevation = 4.dp
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp)
                                            ) {
                                                Text(
                                                    "Cumulative Savings By Group",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PremiumNavy
                                                )

                                                Spacer(modifier = Modifier.height(16.dp))

                                                SavingsLineChart(
                                                    data = savingsData,
                                                    isLoading = isGraphLoading || uiState.isLoading,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Legend for all groups chart
                                                GroupSavingsLegend(
                                                    groups = uiState.groups,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    } else {
                                        EmptyGraphPlaceholder(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp),
                                            message = "No cumulative savings data available"
                                        )
                                    }
                                }
                                GraphType.USER_SAVINGS_PER_GROUP -> {
                                    if (savingsData.isNotEmpty()) {
                                        Surface(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color.White),
                                            color = Color.White,
                                            shadowElevation = 4.dp
                                        ) {
                                            Column(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .padding(16.dp)
                                            ) {
                                                Text(
                                                    "My Savings Per Group",
                                                    style = MaterialTheme.typography.titleMedium,
                                                    fontWeight = FontWeight.Bold,
                                                    color = PremiumNavy
                                                )

                                                Spacer(modifier = Modifier.height(16.dp))

                                                SavingsLineChart(
                                                    data = savingsData,
                                                    isLoading = isGraphLoading || uiState.isLoading,
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(180.dp)
                                                )

                                                Spacer(modifier = Modifier.height(8.dp))

                                                // Legend for per-group savings
                                                GroupSavingsLegend(
                                                    groups = uiState.groups,
                                                    modifier = Modifier.fillMaxWidth()
                                                )
                                            }
                                        }
                                    } else {
                                        EmptyGraphPlaceholder(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(280.dp),
                                            message = "No personal savings data available"
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }

                    // Your Groups header
                    item {
                        Text(
                            "Your Groups",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = PremiumNavy,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)
                        )
                    }

                    // Groups list
                    when {
                        uiState.isLoading -> {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(color = VibrantOrange)
                                }
                            }
                        }
                        uiState.groups.isEmpty() -> {
                            item {
                                EmptyGroupsPlaceholder(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(400.dp),
                                    onCreateClick = { viewModel.showCreateGroupDialog() }
                                )
                            }
                        }
                        else -> {
                            items(uiState.groups) { group ->
                                val memberCount = uiState.groupMemberCounts[group.groupId] ?: 0
                                PremiumGroupCard(
                                    group = group,
                                    memberCount = memberCount,
                                    onClick = { navigateToGroupCycles(group.groupId) },
                                    onLongClick = {
                                        if (currentUser?.userId == group.adminId) {
                                            groupToDelete = group
                                            showDeleteDialog = true
                                        }
                                    },
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }

                    // Add some bottom padding
                    item {
                        Spacer(modifier = Modifier.height(80.dp))
                    }
                }
            }
        }
    }

    // Create Group Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.hideCreateGroupDialog() },
            title = { Text("Create New Group", color = PremiumNavy) },
            text = {
                Column {
                    OutlinedTextField(
                        value = groupName,
                        onValueChange = { groupName = it },
                        label = { Text("Group Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        isError = uiState.nameValidationError != null,
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = SoftOffWhite,
                            unfocusedContainerColor = SoftOffWhite
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )

                    uiState.nameValidationError?.let { error ->
                        Text(
                            text = error,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = { viewModel.validateAndCreateGroup(groupName) },
                    enabled = groupName.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { viewModel.hideCreateGroupDialog() }
                ) {
                    Text("Cancel", color = PremiumNavy)
                }
            }
        )
    }

    // Delete Group Confirmation Dialog
    if (showDeleteDialog && groupToDelete != null) {
        AlertDialog(
            onDismissRequest = {
                showDeleteDialog = false
                groupToDelete = null
            },
            title = { Text("Delete Group") },
            text = { Text("Are you sure you want to delete the group '${groupToDelete?.name}'? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        groupToDelete?.groupId?.let { groupId ->
                            viewModel.deleteGroup(groupId)
                        }
                        showDeleteDialog = false
                        groupToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        groupToDelete = null
                    }
                ) {
                    Text("Cancel", color = PremiumNavy)
                }
            }
        )
    }
}

private suspend fun loadCumulativeSavingsAllGroups(
    groups: List<Group>,
    savingsViewModel: SavingsViewModel,
    savingsData: MutableMap<String, List<Int>>
) {
    groups.forEach { group ->
        savingsViewModel.initializeGroupId(group.groupId)

        val calendar = Calendar.getInstance()
        val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val monthlySavings = mutableListOf<Int>()

        // Get savings for the past 12 months for this group
        for (i in 11 downTo 0) {
            calendar.time = Date()
            calendar.add(Calendar.MONTH, -i)
            val monthYear = dateFormat.format(calendar.time)

            val totalSavings = savingsViewModel.getTotalSavingsForMonth(group.groupId, monthYear)
            monthlySavings.add(totalSavings)
        }

        // Convert to cumulative totals
        val cumulativeSavings = mutableListOf<Int>()
        var runningTotal = 0

        monthlySavings.forEach { amount ->
            runningTotal += amount
            cumulativeSavings.add(runningTotal)
        }

        savingsData[group.name] = cumulativeSavings
    }
}

private suspend fun loadUserSavingsPerGroup(
    groups: List<Group>,
    savingsViewModel: SavingsViewModel,
    savingsData: MutableMap<String, List<Int>>,
    userId: String?
) {
    if (userId.isNullOrEmpty()) return

    groups.forEach { group ->
        savingsViewModel.initializeGroupId(group.groupId)

        // Get the member ID for this user in the current group
        val memberId = savingsViewModel.getMemberIdForUser(group.groupId, userId)
        if (memberId != null) {
            // Get user's savings entries for this group using memberId
            val userSavings = savingsViewModel.getGroupSavingsEntries(group.groupId)
                .filter { it.memberId == memberId }

            val monthlySavings = userSavings.groupBy { it.monthYear }
                .mapValues { (_, entries) -> entries.sumOf { it.amount } }

            val calendar = Calendar.getInstance()
            val dateFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
            val cumulativeSavings = mutableListOf<Int>()
            var runningTotal = 0

            val months = List(12) { i ->
                calendar.time = Date()
                calendar.add(Calendar.MONTH, -i)
                dateFormat.format(calendar.time)
            }.sortedBy { it }

            months.forEach { month ->
                runningTotal += monthlySavings[month] ?: 0
                cumulativeSavings.add(runningTotal)
            }

            savingsData[group.name] = cumulativeSavings
        } else {
            savingsData[group.name] = emptyList()
        }
    }
}

@Composable
fun SavingsLineChart(
    data: Map<String, List<Int>>,
    isLoading: Boolean,
    modifier: Modifier = Modifier,
    padding: Dp = 32.dp
) {
    when {
        isLoading -> {
            // Show loading indicator
            Box(
                modifier = modifier,
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
        data.isEmpty() || data.values.all { it.all { value -> value == 0 } } -> {
            // Show empty state
            EmptyGraphPlaceholder(modifier = modifier)
        }
        else -> {
            // Render actual chart
            SavingsLineChartContent(
                data = data,
                modifier = modifier,
                padding = padding
            )
        }
    }
}

@Composable
private fun SavingsLineChartContent(
    data: Map<String, List<Int>>,
    modifier: Modifier = Modifier,
    padding: Dp = 32.dp
) {
    val groups = remember(data) { data.keys.toList() }

    // Calculate max value for scaling
    val maxValue = remember(data) {
        val allValues = data.values.flatten()
        if (allValues.isEmpty()) 100 else max(1, allValues.maxOrNull() ?: 1)
    }

    val months = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")

    Canvas(modifier = modifier) {
        val canvasWidth = size.width - padding.toPx() * 2
        val canvasHeight = size.height - padding.toPx() * 2
        val paddingPx = padding.toPx()

        // Grid lines + Y-axis labels
        val horizontalLines = 5
        for (i in 0..horizontalLines) {
            val y = paddingPx + (canvasHeight / horizontalLines) * i
            drawLine(
                color = Color.LightGray.copy(alpha = 0.3f),
                start = Offset(paddingPx, y),
                end = Offset(paddingPx + canvasWidth, y),
                strokeWidth = 1f
            )
            val value = (maxValue - (maxValue / horizontalLines) * i).toInt()
            drawContext.canvas.nativeCanvas.drawText(
                value.toString(),
                paddingPx - 20,
                y + 5,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10f
                    textAlign = android.graphics.Paint.Align.RIGHT
                }
            )
        }

        // Draw savings lines
        groups.forEachIndexed { groupIndex, groupName ->
            val cumulativeSavings = data[groupName] ?: return@forEachIndexed
            if (cumulativeSavings.isEmpty()) return@forEachIndexed

            val color = if (groups.size == 1) PremiumPurple
            else ChartLineColors[groupIndex % ChartLineColors.size]

            val path = Path()
            val pointSpacing = canvasWidth / (cumulativeSavings.size - 1).coerceAtLeast(1)

            cumulativeSavings.forEachIndexed { index, amount ->
                val x = paddingPx + (pointSpacing * index)
                val y = paddingPx + canvasHeight - (amount.toFloat() / maxValue * canvasHeight)
                val clampedY = y.coerceIn(paddingPx, paddingPx + canvasHeight)

                if (index == 0) path.moveTo(x, clampedY) else path.lineTo(x, clampedY)

                drawCircle(color = color, radius = 4f, center = Offset(x, clampedY))
            }

            drawPath(
                path = path,
                color = color,
                style = Stroke(width = 3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
            )
        }

        // Draw x-axis labels
        val monthWidth = canvasWidth / (months.size - 1).coerceAtLeast(1)
        months.forEachIndexed { index, month ->
            val x = paddingPx + (monthWidth * index)
            val y = paddingPx + canvasHeight + 15
            drawContext.canvas.nativeCanvas.drawText(
                month,
                x,
                y,
                android.graphics.Paint().apply {
                    color = android.graphics.Color.GRAY
                    textSize = 10f
                    textAlign = android.graphics.Paint.Align.CENTER
                }
            )
        }
    }
}

@Composable
fun EmptyGraphPlaceholder(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = "No data",
                tint = Color.Gray,
                modifier = Modifier.size(48.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text("No savings data yet", color = Color.Gray)
        }
    }
}

@Composable
fun GroupSavingsLegend(
    groups: List<Group>,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        groups.take(4).forEachIndexed { index, group ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = ChartLineColors[index % ChartLineColors.size],
                            shape = CircleShape
                        )
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = group.name.take(8) + if (group.name.length > 8) "..." else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = PremiumNavy,
                    maxLines = 1
                )
            }
        }

        if (groups.size > 4) {
            Text(
                text = "+${groups.size - 4} more",
                style = MaterialTheme.typography.bodySmall,
                color = PremiumNavy.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyGraphPlaceholder(
    modifier: Modifier = Modifier,
    message: String = "No data available"
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp)),
        color = Color.White,
        shadowElevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = "No data",
                tint = PremiumNavy.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = PremiumNavy.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun GroupsHomeDrawerContent(
    currentUser: User?,
    onClose: () -> Unit,
    onNavigateToChangePassword: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(PremiumNavy)
            .padding(horizontal = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(R.drawable.ic_profile_placeholder),
                contentDescription = "Profile",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (currentUser == null) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .size(48.dp)
                        .align(Alignment.CenterHorizontally),
                    color = Color.White
                )
            } else {
                Text(
                    currentUser.username,
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White
                )
                Text(
                    currentUser.phoneNumber,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.8f)
                )
            }
        }

        Divider(color = Color.White.copy(alpha = 0.2f), thickness = 1.dp)

        Spacer(modifier = Modifier.height(24.dp))

        DrawerItem(
            icon = Icons.Default.Lock,
            text = "Change Password",
            onClick = {
                onNavigateToChangePassword()
                onClose()
            }
        )
        // Drawer items
        DrawerItem(
            icon = Icons.Default.Settings,
            text = "Settings",
            onClick = { /* Handle settings */ }
        )
        DrawerItem(
            icon = Icons.Default.Help,
            text = "Help & Support",
            onClick = { /* Handle help */ }
        )
        DrawerItem(
            icon = Icons.Default.Info,
            text = "About",
            onClick = { /* Handle about */ }
        )
        DrawerItem(
            icon = Icons.Default.ExitToApp,
            text = "Logout",
            onClick = { /* Handle logout */ }
        )

        Spacer(modifier = Modifier.weight(1f))

        // App version
        Text(
            "Chama Buddy v1.0.0",
            color = Color.White.copy(alpha = 0.6f),
            fontSize = 12.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 24.dp),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun DrawerItem(icon: ImageVector, text: String, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = text,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun EmptyGroupsPlaceholder(modifier: Modifier = Modifier, onCreateClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_state),
            contentDescription = "No groups",
            modifier = Modifier.size(150.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Groups Yet",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PremiumNavy
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Create your first group to get started with Chama Buddy",
            style = MaterialTheme.typography.bodyMedium,
            color = PremiumNavy.copy(alpha = 0.7f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onCreateClick,
            colors = ButtonDefaults.buttonColors(containerColor = VibrantOrange),
            modifier = Modifier.height(50.dp)
        ) {
            Text("Create Group", fontWeight = FontWeight.Bold)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PremiumGroupCard(
    group: Group,
    memberCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPremium = group.name.contains("premium", ignoreCase = true) ||
            group.name.contains("gold", ignoreCase = true) ||
            group.name.contains("vip", ignoreCase = true)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isPremium) 8.dp else 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isPremium) PremiumPurple.copy(alpha = 0.1f) else CardSurface
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isPremium) BorderStroke(1.dp, PremiumGold) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Group profile with different image for premium groups
            if (isPremium) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(PremiumGold, PremiumPurple),
                                center = androidx.compose.ui.geometry.Offset(0.3f, 0.3f),
                                radius = 100f
                            ),
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Premium Group",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = LightAccentBlue,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = group.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleLarge,
                        color = PremiumNavy,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = group.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPremium) PremiumPurple else PremiumNavy
                )

                // Admin name with faded text
                Text(
                    text = "Admin: ${group.adminName ?: "Unknown"}",
                    style = MaterialTheme.typography.bodySmall,
                    color = PremiumNavy.copy(alpha = 0.6f)
                )

                // Total members
                Text(
                    text = "$memberCount members",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isPremium) PremiumGold else PremiumNavy.copy(alpha = 0.8f),
                    fontWeight = if (isPremium) FontWeight.Bold else FontWeight.Normal
                )
            }

            if (isPremium) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Premium",
                    tint = PremiumGold,
                    modifier = Modifier
                        .size(24.dp)
                        .padding(4.dp)
                )
            }

            Icon(
                Icons.Default.ChevronRight,
                contentDescription = "View group",
                tint = if (isPremium) PremiumGold else PremiumNavy.copy(alpha = 0.7f)
            )
        }
    }
}