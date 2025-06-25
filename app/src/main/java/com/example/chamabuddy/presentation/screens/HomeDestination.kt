package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.filled.Menu
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.core.Spring.DampingRatioLowBouncy
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Spacer
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.DrawerState
import androidx.compose.material3.rememberDrawerState
import kotlinx.coroutines.launch
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.R
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.presentation.BottomBar
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.CycleEvent
import com.example.chamabuddy.presentation.viewmodel.CycleState
import com.example.chamabuddy.presentation.viewmodel.CycleViewModel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

object HomeDestination : NavigationDestination {
    override val route = "home"
    override val title = "ChamaBuddy"
}

val PremiumNavy = Color(0xFF0A1D3A)
val SoftOffWhite = Color(0xFFF8F9FA)
val VibrantOrange = Color(0xFFFF6B35)
val SoftGreen = Color(0xFF4CAF50)
val CardSurface = Color(0xFFFFFFFF)
val LightAccentBlue = Color(0xFFE3F2FD)
val SearchBarGray = Color(0xFFF0F0F0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToCycleDetails: (String) -> Unit,
    navigateToCreateCycle: () -> Unit,
    navController: NavHostController,
    onBottomBarVisibilityChange: (Boolean) -> Unit,
    viewModel: CycleViewModel = hiltViewModel()
) {

    val listState = rememberLazyListState()
    var bottomBarVisible by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    // Scroll direction tracking
    var lastScrollPosition by remember { mutableStateOf(0) }
    var isScrollingDown by remember { mutableStateOf(false) }

    // Propagate visibility changes to parent
    LaunchedEffect(bottomBarVisible) {
        onBottomBarVisibilityChange(bottomBarVisible)
    }

    // Drawer state handling
    LaunchedEffect(drawerState) {
        snapshotFlow { drawerState.isOpen }
            .collect { isOpen ->
                bottomBarVisible = !isOpen
            }
    }

    // Scroll detection logic
    LaunchedEffect(listState) {
        snapshotFlow { listState.firstVisibleItemScrollOffset }
            .collect { currentOffset ->
                val currentIndex = listState.firstVisibleItemIndex

                // Calculate scroll delta
                val delta = currentOffset - lastScrollPosition
                lastScrollPosition = currentOffset

                // Determine scroll direction
                when {
                    // Scrolling down
                    delta > 5 -> {
                        isScrollingDown = true
                        bottomBarVisible = false
                    }
                    // Scrolling up
                    delta < -5 -> {
                        isScrollingDown = false
                        bottomBarVisible = true
                    }
                }

                // Always show at top
                if (currentIndex == 0 && currentOffset == 0) {
                    bottomBarVisible = true
                }

                // Handle non-scrollable content
                if (!listState.canScrollForward && !listState.canScrollBackward) {
                    bottomBarVisible = true
                }
            }
    }

    val state by viewModel.state.collectAsState()
    LaunchedEffect(Unit) {
        viewModel.handleEvent(CycleEvent.GetCycleHistory)
    }
    val totalSavings by viewModel.totalSavings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            Box(modifier = Modifier.width(280.dp)) {
                SideNavigationDrawerContent(
                    onClose = { scope.launch { drawerState.close() } },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
            topBar = {
                LargeTopAppBar(
                    title = {
                        Text(
                            text = "ChamaTrust",
                            fontWeight = FontWeight.Bold,
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
                        Text(
                            text = "KES $totalSavings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    },
                    colors = TopAppBarDefaults.largeTopAppBarColors(
                        containerColor = PremiumNavy,
                        scrolledContainerColor = PremiumNavy,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    ),
                    scrollBehavior = scrollBehavior
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = navigateToCreateCycle,
                    containerColor = VibrantOrange
                ) {
                    Icon(Icons.Default.Add, "New Cycle", tint = Color.White)
                }
            },
            containerColor = SoftOffWhite
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Background curve at the top
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp)
                        .background(
                            brush = Brush.verticalGradient(
                                colors = listOf(PremiumNavy, PremiumNavy.copy(alpha = 0.8f)),
                                startY = 0f,
                                endY = 500f
                            ),
                            shape = RoundedCornerShape(bottomStart = 40.dp, bottomEnd = 40.dp)
                        )
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp)
                ) {
                    Spacer(modifier = Modifier.height(1.dp))

                    // Greeting section
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = spring(stiffness = Spring.StiffnessLow))
                    ) {
                        Column {
                            Text(
                                "Hi Alex",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            Text(
                                "Welcome to ChamaTrust, Your Merry go round Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.padding(top = 4.dp, bottom = 2.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    when (state) {
                        is CycleState.Loading -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                CircularProgressIndicator(color = VibrantOrange)
                            }
                        }
                        is CycleState.Error -> {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    (state as CycleState.Error).message,
                                    color = PremiumNavy
                                )
                            }
                        }
                        is CycleState.CycleHistory -> {
                            val cycles = (state as CycleState.CycleHistory).cycles

                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp))
                                    .background(Color.White),
                                color = Color.White
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp)
                                ) {
                                    Text(
                                        "All Cycles",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = PremiumNavy,
                                        modifier = Modifier.padding(vertical = 16.dp)
                                    )

                                    if (cycles.isEmpty()) {
                                        EmptyDashboard(navigateToCreateCycle)
                                    } else {
                                        LazyColumn(
                                            modifier = Modifier.fillMaxSize(),
                                            state = listState,
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            items(cycles) { cycle ->
                                                PremiumCycleCard(
                                                    cycle = cycle,
                                                    onClick = { navigateToCycleDetails(cycle.cycleId) }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumCycleCard(cycle: Cycle, onClick: () -> Unit) {
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val statusColor = if (cycle.endDate == null) SoftGreen else Color(0xFF9E9E9E)
    val statusText = if (cycle.endDate == null) "Active" else "Completed"

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Cycle #${cycle.cycleId.takeLast(4)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = PremiumNavy
                )
                Badge(
                    containerColor = statusColor.copy(alpha = 0.2f),
                    contentColor = statusColor
                ) {
                    Text(statusText, fontWeight = FontWeight.Medium)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Start Date", fontSize = 12.sp, color = PremiumNavy.copy(alpha = 0.6f))
                    Text(
                        dateFormat.format(cycle.startDate),
                        fontWeight = FontWeight.Medium,
                        color = PremiumNavy
                    )
                }
                cycle.endDate?.let {
                    Column {
                        Text("End Date", fontSize = 12.sp, color = PremiumNavy.copy(alpha = 0.6f))
                        Text(
                            dateFormat.format(it),
                            fontWeight = FontWeight.Medium,
                            color = PremiumNavy
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Divider(color = LightAccentBlue, thickness = 1.dp)

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Saved", fontSize = 12.sp, color = PremiumNavy.copy(alpha = 0.6f))
                    Text(
                        "KES ${cycle.totalSavings}",
                        fontWeight = FontWeight.Bold,
                        color = PremiumNavy
                    )
                }
                Column {
                    Text("Members", fontSize = 12.sp, color = PremiumNavy.copy(alpha = 0.6f))
                    Text(
                        "${cycle.totalMembers}",
                        fontWeight = FontWeight.Bold,
                        color = PremiumNavy
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyDashboard(onCreateClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Image(
            painter = painterResource(R.drawable.ic_empty_state),
            contentDescription = "No active cycles",
            modifier = Modifier.size(120.dp),
            contentScale = ContentScale.Fit
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "No Active Cycles",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = PremiumNavy,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Start your first cycle and manage your chama efficiently",
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
            Text("Start New Cycle", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SideNavigationDrawerContent(
    onClose: () -> Unit,
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
                painter = painterResource(R.drawable.ic_chama_logo),
                contentDescription = "Logo",
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .border(2.dp, Color.White, CircleShape)
            )
            Spacer(modifier = Modifier.height(8.dp))
            IconButton(onClick = onClose) {
                Icon(Icons.Default.Close, "Close", tint = Color.White)
            }
        }

        Text(
            "Your Groups",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            modifier = Modifier.padding(vertical = 16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0x44FFFFFF)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "Groups will appear here",
                color = Color.White.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            Text(
                "About Us",
                style = MaterialTheme.typography.titleLarge,
                color = Color.White,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            ContactItem(
                text = "0795301955",
                icon = Icons.Default.Phone
            )

            ContactItem(
                text = "alexdemaish@gmail.com",
                icon = Icons.Default.Email
            )

            Row(
                modifier = Modifier.padding(top = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                SocialIcon(R.drawable.ic_github, "GitHub")
                SocialIcon(R.drawable.ic_facebook, "Facebook")
                SocialIcon(R.drawable.ic_linkedin, "LinkedIn")
            }
        }
    }
}

@Composable
fun ContactItem(text: String, icon: ImageVector) {
    Row(
        modifier = Modifier.padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = text,
            color = Color.White,
            fontSize = 16.sp
        )
    }
}

@Composable
fun SocialIcon(iconRes: Int, description: String) {
    Icon(
        painter = painterResource(iconRes),
        contentDescription = description,
        tint = Color.White,
        modifier = Modifier
            .size(32.dp)
            .clickable { /* Handle social link click */ }
    )
}