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

    Scaffold() { innerPadding ->
        MainNavHost(
            navController = navController,
            innerPadding = innerPadding,
            onGroupSelected = { groupId ->
                currentGroupId = groupId
                navController.navigate("${HomeDestination.route}/$groupId") {
                    popUpTo(GroupsHomeDestination.route) { inclusive = true }
                }
            },
            currentGroupId = currentGroupId
        )
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
                navigateToGroupCycles = onGroupSelected
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
            arguments = listOf(navArgument(HomeDestination.groupIdArg) { type = NavType.StringType }
            ) ){ backStackEntry ->
                val groupId = backStackEntry.arguments?.getString(HomeDestination.groupIdArg) ?: ""
                HomeScreen(
                    navController = navController,
                    groupId = groupId,
                    navigateToCycleDetails = { cycleId ->
                        navController.navigate("${CycleDetailDestination.route}/$groupId/$cycleId")
                    },
                    navigateToCreateCycle = {
                        navController.navigate("${CreateCycleDestination.route}/$groupId")
                    },
                    navigateToGroupManagement = {
                        navController.navigate(GroupsHomeDestination.route)
                    }
                )
            }

//        composable(route = GroupsHomeDestination.route) {
//            GroupsHomeScreen(
//                navigateToGroupCycles = { groupId ->
//                    navController.navigate("${MainTabsDestination.route}/$groupId")
//                }
//            )
//        }




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
            arguments = listOf(
                navArgument(MembersDestination.groupIdArg) { type = NavType.StringType }
            )
        ) { backStackEntry ->
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
            route = SavingsDestination.routeWithArgs,
            arguments = listOf(navArgument(SavingsDestination.groupIdArg) {
                type = NavType.StringType
            }
            ) ){ backStackEntry ->
                val groupId = backStackEntry.arguments?.getString(SavingsDestination.groupIdArg) ?: ""
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
                }
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
                navController = navController,
                navigateBack = { navController.popBackStack() },
                groupId = groupId
            )


        }
    }

}