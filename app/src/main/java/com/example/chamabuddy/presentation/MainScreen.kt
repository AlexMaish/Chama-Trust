package com.example.chamabuddy.presentation

import androidx.compose.foundation.lazy.rememberLazyListState
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.drop
import kotlin.math.abs
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.flow.collectLatest

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.chamabuddy.R
import com.example.chamabuddy.presentation.navigation.*
import com.example.chamabuddy.presentation.screens.BeneficiaryDetailScreen
import com.example.chamabuddy.presentation.screens.BeneficiarySelectionDestination
import com.example.chamabuddy.presentation.screens.BeneficiarySelectionScreen
import com.example.chamabuddy.presentation.screens.ContributionDestination
import com.example.chamabuddy.presentation.screens.ContributionScreen
import com.example.chamabuddy.presentation.screens.CreateCycleScreen
import com.example.chamabuddy.presentation.screens.CycleDetailScreenForMeetings
import com.example.chamabuddy.presentation.screens.HomeScreen
import com.example.chamabuddy.presentation.screens.MembersScreen
import com.example.chamabuddy.presentation.screens.ProfileScreen
import com.example.chamabuddy.presentation.screens.SavingsScreen // Add this import
import com.example.chamabuddy.presentation.navigation.SavingsDestination
import com.example.chamabuddy.presentation.screens.SavingsScreen
@Composable
fun MainScreen() {



    val navController = rememberNavController()
    val bottomBarItems = listOf(
        TabItem(HomeDestination, Icons.Filled.Home, "Home"),
        TabItem(SavingsDestination, Icons.Filled.Savings, "Savings"),
        TabItem(BeneficiaryDestination, Icons.Filled.MonetizationOn, "Beneficiary"),
        TabItem(MembersDestination, Icons.Filled.People, "Members"),
        TabItem(ProfileDestination, Icons.Filled.Person, "Profile")
    )
    var isBottomBarVisible by remember { mutableStateOf(true) }


    Scaffold(
        bottomBar = {
            // Animate bottom bar visibility
            AnimatedVisibility(
                visible = isBottomBarVisible,
                enter = fadeIn(animationSpec = tween(100)),
                exit = fadeOut(animationSpec = tween(100))
            ) {
                BottomBar(navController = navController, items = bottomBarItems)
            }
        }
    )  { innerPadding ->
        MainNavHost(
            navController = navController,
            innerPadding = innerPadding,
            onBottomBarVisibilityChange = { visible ->
                isBottomBarVisible = visible
            }
        )
    }
}

data class TabItem(
    val destination: NavigationDestination,
    val icon: ImageVector,
    val title: String
)

@Composable
fun MainNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    onBottomBarVisibilityChange: (Boolean) -> Unit // Add this parameter

) {
    NavHost(
        navController = navController,
        startDestination = HomeDestination.route,
        modifier = Modifier.padding(innerPadding)
    ) {

        composable(route = CreateCycleDestination.route) {
            CreateCycleScreen(
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(route = HomeDestination.route) {
            HomeScreen(
                navigateToCycleDetails = { cycleId ->
                    navController.navigate("${CycleDetailDestination.route}/$cycleId")
                },
                navigateToCreateCycle = {
                    navController.navigate(CreateCycleDestination.route)
                },
                navController = navController,
                onBottomBarVisibilityChange = onBottomBarVisibilityChange
            )
        }



        composable(route = BeneficiaryDestination.route) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Beneficiary Screen")
            }
        }

//        composable(route = MembersDestination.route) {
//            MembersScreen(navigateBack = { navController.popBackStack() })
//        }

        composable(route = ProfileDestination.route) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Profile Screen")
            }
        }

        composable(
            route = CycleDetailDestination.routeWithArgs,
            arguments = listOf(
                navArgument(CycleDetailDestination.cycleIdArg) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val cycleId = backStackEntry.arguments?.getString(CycleDetailDestination.cycleIdArg) ?: ""
            CycleDetailScreenForMeetings(
                cycleId = cycleId,
                navigateToMeetingDetail = { meetingId ->
                    navController.navigate("${MeetingDetailDestination.route}/$meetingId")
                },
                navigateBack = { navController.popBackStack() },
                // Add this parameter
                navigateToContribution = { meetingId ->
                    navController.navigate("${ContributionDestination.route}/$meetingId")
                }
            )
        }

        composable(
            route = MeetingDetailDestination.routeWithArgs,
            arguments = listOf(
                navArgument(MeetingDetailDestination.meetingIdArg) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString(MeetingDetailDestination.meetingIdArg) ?: ""
            MeetingDetailScreen(
                meetingId = meetingId,
                navigateBack = { navController.popBackStack() }
            )
        }
        composable(
            route = CycleDetailDestination.routeWithArgs,
            arguments = listOf(
                navArgument(CycleDetailDestination.cycleIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val cycleId = backStackEntry.arguments?.getString(CycleDetailDestination.cycleIdArg) ?: ""
            CycleDetailScreenForMeetings(
                cycleId = cycleId,
                navigateToMeetingDetail = { meetingId ->
                    navController.navigate("${MeetingDetailDestination.route}/$meetingId")
                },
                navigateBack = { navController.popBackStack() },
                navigateToContribution = { meetingId ->
                    navController.navigate("${ContributionDestination.route}/$meetingId")
                }
            )
        }


        composable(
            route = ContributionDestination.routeWithArgs,
            arguments = listOf(
                navArgument(ContributionDestination.meetingIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString(ContributionDestination.meetingIdArg) ?: ""
            ContributionScreen(
                meetingId = meetingId,
                navigateToBeneficiarySelection = {
                    navController.navigate("${BeneficiarySelectionDestination.route}/$meetingId")
                },
                navigateBack = { navController.popBackStack() }
            )
        }



        composable(
            route = BeneficiarySelectionDestination.routeWithArgs,
            arguments = listOf(
                navArgument(BeneficiarySelectionDestination.meetingIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val meetingId = backStackEntry.arguments?.getString(BeneficiarySelectionDestination.meetingIdArg) ?: ""
            BeneficiarySelectionScreen(
                meetingId = meetingId,
                navigateBack = { navController.popBackStack() },
                onSaveComplete = {
                    // Navigate back to cycle detail screen
                    navController.popBackStack(
                        route = CycleDetailDestination.route,
                        inclusive = false
                    )
                }
            )
        }



        composable(BeneficiaryDetailDestination.routeWithArgs) { backStackEntry ->
            val beneficiaryId = backStackEntry.arguments?.getString("beneficiaryId") ?: ""
            BeneficiaryDetailScreen(
                beneficiaryId = beneficiaryId,
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = ContributionDestination.routeWithArgs,
            arguments = listOf(navArgument(ContributionDestination.meetingIdArg) { type = NavType.StringType }
            )) { backStackEntry ->
                val meetingId = backStackEntry.arguments?.getString(ContributionDestination.meetingIdArg) ?: ""
                ContributionScreen(
                    meetingId = meetingId,
                    navigateToBeneficiarySelection = {
                        navController.navigate("${BeneficiarySelectionDestination.route}/$meetingId")
                    },
                    navigateBack = { navController.popBackStack() }
                )
            }

        composable(
            route = BeneficiarySelectionDestination.routeWithArgs,
            arguments = listOf(navArgument(BeneficiarySelectionDestination.meetingIdArg) { type = NavType.StringType }
            )) { backStackEntry ->
                val meetingId = backStackEntry.arguments?.getString(BeneficiarySelectionDestination.meetingIdArg) ?: ""
                BeneficiarySelectionScreen(
                    meetingId = meetingId,
                    navigateBack = { navController.popBackStack() },
                    onSaveComplete = { navController.popBackStack() }
                )
            }


        composable(route = MembersDestination.route) {
            MembersScreen(
                navigateBack = { navController.popBackStack() },
                navigateToProfile = { memberId ->
                    navController.navigate("${ProfileDestination.route}/$memberId")
                }
            )
        }

        // In your navigation setup:
        composable(
            route = ProfileDestination.routeWithArgs,
            arguments = listOf(navArgument(ProfileDestination.memberIdArg) { type = NavType.StringType }
            ) ){ backStackEntry ->
                val memberId = backStackEntry.arguments?.getString(ProfileDestination.memberIdArg) ?: ""
                ProfileScreen(
                    memberId = memberId,
                    navigateBack = { navController.popBackStack() }
                )
            }


        composable(route = SavingsDestination.route) {
            SavingsScreen(
                navigateToProfile = { memberId ->
                    navController.navigate("${ProfileDestination.route}/$memberId")
                },
                navigateBack = { navController.popBackStack() }
            )
        }


    }
}
@Composable
fun BottomBar(
    navController: NavHostController,
    items: List<TabItem>
) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentDestination?.hierarchy?.any { it.route == item.destination.route } == true,
                onClick = {
                    navController.navigate(item.destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    }
}


//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun SavingsScreen(navigateBack: () -> Unit) {
//    Scaffold(
//        topBar = {
//            CenterAlignedTopAppBar(
//                title = { Text("Savings") },
//                navigationIcon = {
//                    IconButton(onClick = navigateBack) {
//                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
//                    }
//                }
//            )
//        }
//    ) { innerPadding ->
//        Box(
//            modifier = Modifier
//                .fillMaxSize()
//                .padding(innerPadding),
//            contentAlignment = Alignment.Center
//        ) {
//            Text("Savings Screen Content")
//        }
//    }
//}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MeetingDetailScreen(meetingId: String, navigateBack: () -> Unit) {
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Meeting Details") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* Create new contribution */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Contribution")
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("Meeting ID: $meetingId")
        }
    }
}