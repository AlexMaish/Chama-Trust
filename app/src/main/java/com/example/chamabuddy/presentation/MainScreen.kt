package com.example.chamabuddy.presentation

import android.R.attr.type
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
import com.example.chamabuddy.presentation.screens.GroupsHomeScreen
import com.example.chamabuddy.presentation.screens.SavingsScreen
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    var currentGroupId by remember { mutableStateOf("") }
    var isBottomBarVisible by remember { mutableStateOf(true) }

    val bottomBarItems = listOf(
        TabItem(HomeDestination, Icons.Filled.Home, "Home"),
        TabItem(SavingsDestination, Icons.Filled.Savings, "Savings"),
        TabItem(BeneficiaryDestination, Icons.Filled.MonetizationOn, "Beneficiary"),
        TabItem(MembersDestination, Icons.Filled.People, "Members"),
        TabItem(ProfileDestination, Icons.Filled.Person, "Profile")
    )

    Scaffold(
        bottomBar = {
            AnimatedVisibility(visible = isBottomBarVisible) {
                BottomBar(
                    navController = navController,
                    items = bottomBarItems,
                    currentGroupId = currentGroupId
                )
            }
        }
    ) { innerPadding ->
        MainNavHost(
            navController = navController,
            innerPadding = innerPadding,
            onBottomBarVisibilityChange = { visible -> isBottomBarVisible = visible },
            onGroupSelected = { groupId -> currentGroupId = groupId },
            currentGroupId = currentGroupId
        )
    }
}

data class TabItem(
    val destination: NavigationDestination,
    val icon: ImageVector,
    val title: String
)











@Composable
fun BottomBar(
    navController: NavHostController,
    items: List<TabItem>,
    currentGroupId: String
) {
    NavigationBar {
        val navBackStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = navBackStackEntry?.destination

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.title) },
                label = { Text(item.title) },
                selected = currentDestination?.hierarchy?.any {
                    it.route?.substringBefore('/') == item.destination.route
                } == true,
                onClick = {
                    when (item.destination) {
                        SavingsDestination -> {
                            if (currentGroupId.isNotBlank()) {
                                navController.navigate("${SavingsDestination.route}/$currentGroupId") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        MembersDestination -> {
                            if (currentGroupId.isNotBlank()) {
                                navController.navigate("${MembersDestination.route}/$currentGroupId") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        ProfileDestination -> {
                            if (currentGroupId.isNotBlank()) {
                                navController.navigate("${ProfileDestination.route}/$currentGroupId/current_user") {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        }
                        else -> {
                            navController.navigate(item.destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                }
            )
        }
    }
}
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



















@Composable
fun MainNavHost(
    navController: NavHostController,
    innerPadding: PaddingValues,
    onBottomBarVisibilityChange: (Boolean) -> Unit,
    onGroupSelected: (String) -> Unit,
    currentGroupId: String
) {
    NavHost(
        navController = navController,
        startDestination = GroupsHomeDestination.route,
        modifier = Modifier.padding(innerPadding)
    ){
            composable(route = GroupsHomeDestination.route) {
                GroupsHomeScreen(
                    navigateToGroupCycles = { groupId ->
                        navController.navigate("${HomeDestination.route}/$groupId")
                        onGroupSelected(groupId)
                    },
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
        composable(
            route = ProfileDestination.routeWithArgs,
            arguments = listOf(
                navArgument(ProfileDestination.groupIdArg) { type = NavType.StringType },
                navArgument(ProfileDestination.memberIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString(ProfileDestination.groupIdArg) ?: ""
            val memberId = backStackEntry.arguments?.getString(ProfileDestination.memberIdArg) ?: ""
            ProfileScreen(
                groupId = groupId,
                memberId = memberId,
                navigateBack = { navController.popBackStack() },
                navController = navController
            )
        }
        composable(
            route = HomeDestination.routeWithArgs,
            arguments = listOf(navArgument(HomeDestination.groupIdArg) {
                type = NavType.StringType
                nullable = true // Correctly declared as nullable
            }
            ) ){ backStackEntry ->
                // Safely retrieve nullable argument
                val groupId: String? = backStackEntry.arguments?.getString(HomeDestination.groupIdArg)

                LaunchedEffect(groupId) {
                    // Handle non-null/non-empty groupId
                    if (!groupId.isNullOrEmpty()) {
                        onGroupSelected(groupId)
                    }
                }

                HomeScreen(
                    groupId = groupId ?: "", // Provide default empty string
                    navigateToCycleDetails = { cycleId ->
                        // Ensure groupId is available for navigation
                        if (!groupId.isNullOrEmpty()) {
                            navController.navigate("${CycleDetailDestination.route}/$groupId/$cycleId")
                        }
                    },
                    navigateToCreateCycle = {
                        if (!groupId.isNullOrEmpty()) {
                            navController.navigate("${CreateCycleDestination.route}/$groupId")
                        }
                    },
                    navigateToGroupManagement = {
                        navController.navigate(GroupsHomeDestination.route)
                    },
                    onBottomBarVisibilityChange = onBottomBarVisibilityChange
                )
            }

        // In MainNavHost
        composable(route = GroupsHomeDestination.route) {
            GroupsHomeScreen(
                navigateToGroupCycles = { groupId ->
                    navController.navigate("${HomeDestination.route}/$groupId")
                },
                onBottomBarVisibilityChange = onBottomBarVisibilityChange
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
            val meetingId =
                backStackEntry.arguments?.getString(MeetingDetailDestination.meetingIdArg) ?: ""
            MeetingDetailScreen(
                meetingId = meetingId,
                navigateBack = { navController.popBackStack() }
            )
        }

        composable(
            route = CycleDetailDestination.routeWithArgs,
            arguments = listOf(
                navArgument(CycleDetailDestination.groupIdArg) { type = NavType.StringType },
                navArgument(CycleDetailDestination.cycleIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val groupId =
                backStackEntry.arguments?.getString(CycleDetailDestination.groupIdArg) ?: ""
            val cycleId =
                backStackEntry.arguments?.getString(CycleDetailDestination.cycleIdArg) ?: ""
            CycleDetailScreenForMeetings(
                navController = navController,
                groupId = groupId,
                cycleId = cycleId,
                navigateToMeetingDetail = { meetingId ->
                    navController.navigate("${MeetingDetailDestination.route}/$meetingId")
                },
                navigateToContribution = { meetingId ->
                    navController.navigate("${ContributionDestination.route}/$meetingId")
                },
                navigateBack = { navController.popBackStack() }
            )
        }


        composable(
            route = ContributionDestination.routeWithArgs,
            arguments = listOf(
                navArgument(ContributionDestination.meetingIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val meetingId =
                backStackEntry.arguments?.getString(ContributionDestination.meetingIdArg) ?: ""
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
                navArgument(BeneficiarySelectionDestination.meetingIdArg) {
                    type = NavType.StringType
                }
            )
        ) { backStackEntry ->
            val meetingId =
                backStackEntry.arguments?.getString(BeneficiarySelectionDestination.meetingIdArg)
                    ?: ""
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
            arguments = listOf(navArgument(ContributionDestination.meetingIdArg) {
                type = NavType.StringType
            }
            )) { backStackEntry ->
            val meetingId =
                backStackEntry.arguments?.getString(ContributionDestination.meetingIdArg) ?: ""
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
            arguments = listOf(navArgument(BeneficiarySelectionDestination.meetingIdArg) {
                type = NavType.StringType
            }
            )) { backStackEntry ->
            val meetingId =
                backStackEntry.arguments?.getString(BeneficiarySelectionDestination.meetingIdArg)
                    ?: ""
            BeneficiarySelectionScreen(
                meetingId = meetingId,
                navigateBack = { navController.popBackStack() },
                onSaveComplete = { navController.popBackStack() }
            )
        }



        composable(
            route = MembersDestination.routeWithArgs,
            arguments = listOf(navArgument(MembersDestination.groupIdArg) {
                type = NavType.StringType
            }
            )) { backStackEntry ->
                val groupId = backStackEntry.arguments?.getString(MembersDestination.groupIdArg) ?: ""
                MembersScreen(
                    groupId = groupId,
                    navigateBack = { navController.popBackStack() },
                    navigateToProfile = { memberId ->
                        navController.navigate("${ProfileDestination.route}/$groupId/$memberId")
                    }
                )
            }

        composable(
            route = ProfileDestination.routeWithArgs,
            arguments = listOf(
                navArgument(ProfileDestination.groupIdArg) { type = NavType.StringType },
                navArgument(ProfileDestination.memberIdArg) { type = NavType.StringType }
            )
        ) { backStack ->
            val groupId = backStack.arguments?.getString(ProfileDestination.groupIdArg) ?: ""
            val memberId = backStack.arguments?.getString(ProfileDestination.memberIdArg) ?: ""
            ProfileScreen(
                groupId = groupId,
                memberId = memberId,
                navigateBack = { navController.popBackStack() },
                navController = navController
            )
        }



        composable(
            route = "${SavingsDestination.route}/{groupId}",
            arguments = listOf(navArgument("groupId") { type = NavType.StringType })
        ) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString("groupId") ?: ""
            SavingsScreen(
                groupId = groupId,
                navigateToProfile = { memberId ->
                    navController.navigate("${ProfileDestination.route}/$groupId/$memberId")
                },
                navigateBack = { navController.popBackStack() }
            )
        }


        composable(route = GroupsHomeDestination.route) {
            GroupsHomeScreen(
                navigateToGroupCycles = { groupId ->
                    // Navigate to HomeScreen with group ID parameter
                    navController.navigate("${HomeDestination.route}/$groupId")
                },
                onBottomBarVisibilityChange = onBottomBarVisibilityChange
            )


                }
        composable(
            route = CreateCycleDestination.routeWithArgs, // This uses routeWithArgs
            arguments = listOf(navArgument(CreateCycleDestination.groupIdArg) {
                type = NavType.StringType
            }
            )) { backStackEntry ->
            val groupId = backStackEntry.arguments?.getString(CreateCycleDestination.groupIdArg) ?: ""
            CreateCycleScreen(
                navigateBack = { navController.popBackStack() },
                groupId = groupId
            )
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

        }
    }

}