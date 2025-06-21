package com.example.chamabuddy.presentation.screens


import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.CycleEvent
import com.example.chamabuddy.presentation.viewmodel.CycleState
import com.example.chamabuddy.presentation.viewmodel.CycleViewModel
import java.text.SimpleDateFormat

import java.util.*

object HomeDestination : NavigationDestination {
    override val route = "home"
    override val title = "ChamaBuddy"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navigateToCycleDetails: (String) -> Unit,
    navigateToCreateCycle: () -> Unit,
    navigateToMembers: () -> Unit, // Keep this if needed
    viewModel: CycleViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.handleEvent(CycleEvent.GetCycleHistory)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("ChamaBuddy", fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            "Humuka",
                            style = MaterialTheme.typography.bodyMedium,
                            fontSize = 14.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = navigateToCreateCycle,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New Cycle")
            }
        }
    ) { innerPadding ->
        when (state) {
            is CycleState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            is CycleState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text((state as CycleState.Error).message)
                }
            }
            is CycleState.CycleHistory -> {
                val cycles = (state as CycleState.CycleHistory).cycles
                if (cycles.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {

                        Text("No cycles found. Start a new cycle!")
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(cycles) { cycle ->
                            CycleCard(cycle = cycle, onClick = {
                                navigateToCycleDetails(cycle.cycleId)
                            })
                        }
                    }
                }
            }
            else -> {}
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CycleCard(cycle: Cycle, onClick: () -> Unit) {
//    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())

    val statusColor = Color(0xFF9E9E9E)
    val statusText = "Completed"

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
//            .animateItemPlacement(),
//        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Cycle #${cycle.cycleId.takeLast(4)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Badge(
                    containerColor = statusColor.copy(alpha = 0.2f),
                    contentColor = statusColor
                ) {
                    Text(statusText)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Start Date", fontSize = 12.sp, color = Color.Gray)
                    Text(dateFormat.format(Date(cycle.startDate)), fontWeight = FontWeight.Medium)
                }
                cycle.endDate?.let {
                    Column {
                        Text("End Date", fontSize = 12.sp, color = Color.Gray)
                        Text(dateFormat.format(Date(it)), fontWeight = FontWeight.Medium)
                    }
                }
            }


            Spacer(modifier = Modifier.height(16.dp))

            HorizontalDivider()

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Total Saved", fontSize = 12.sp, color = Color.Gray)
//                    Text("KES ${cycle.totalSavings}", fontWeight = FontWeight.Bold)
                }
                Column {
                    Text("Members", fontSize = 12.sp, color = Color.Gray)
                    Text("${cycle.totalMembers}", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}






