package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.navigation.NavigationDestination
import com.example.chamabuddy.presentation.viewmodel.SavingsState // Add this import
import com.example.chamabuddy.presentation.viewmodel.SavingsViewModel
import com.example.chamabuddy.presentation.navigation.SavingsDestination // Add this import

object SavingsDestination : NavigationDestination {
    override val route = "savings"
    override val title = "Savings"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    navigateToProfile: (String) -> Unit,
    navigateBack: () -> Unit,
    viewModel: SavingsViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val members by viewModel.members.collectAsState()
    val memberTotals by viewModel.memberTotals.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Member Savings") },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        when (state) {
            is SavingsState.Loading -> {
                if (members.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                } else {
                    // Show content while loading updates
                    SavingsContent(innerPadding, members, memberTotals, navigateToProfile)
                }
            }
            is SavingsState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text((state as SavingsState.Error).message)
                }
            }
            else -> {
                SavingsContent(innerPadding, members, memberTotals, navigateToProfile)
            }
        }
    }
}

@Composable
private fun SavingsContent(
    innerPadding: PaddingValues,
    members: Map<String, Member>,
    memberTotals: Map<String, Int>,
    navigateToProfile: (String) -> Unit
) {
    if (members.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No members available")
        }
    } else {
        val memberList = members.values.toList()

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(memberList) { member ->
                val totalSavings = memberTotals[member.memberId] ?: 0

                MemberSavingsCard(
                    member = member,
                    totalSavings = totalSavings,
                    onClick = { navigateToProfile(member.memberId) }
                )
            }
        }
    }
}

@Composable
fun MemberSavingsCard(
    member: Member,
    totalSavings: Int,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(2).uppercase(),
                    color = MaterialTheme.colorScheme.onPrimary,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    fontWeight = FontWeight.Medium,
                    fontSize = 16.sp
                )
                Text(
                    text = member.phoneNumber,
                    fontSize = 14.sp,
                    color = Color.Gray
                )
            }

            Text(
                text = "KES $totalSavings",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}