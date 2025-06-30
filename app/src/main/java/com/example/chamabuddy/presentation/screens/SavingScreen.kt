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



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsScreen(
    groupId: String,
    navigateToProfile: (String) -> Unit,
    navigateBack: () -> Unit,
    viewModel: SavingsViewModel = hiltViewModel()
) {




    val state by viewModel.state.collectAsState()
    val members by viewModel.members.collectAsState()
    val memberTotals by viewModel.memberTotals.collectAsState()

    // Calculate total savings for each member
    val memberSavingsList = remember(members, memberTotals) {
        members.values.map { member ->
            val total = memberTotals[member.memberId] ?: 0
            MemberWithSavings(member, total)
        }
    }
    LaunchedEffect(groupId) {
        viewModel.initializeGroupId(groupId)
    }
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
                    SavingsContent(innerPadding, memberSavingsList, navigateToProfile)
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
                SavingsContent(innerPadding, memberSavingsList, navigateToProfile)
            }
        }
    }
}

// Data class to hold member with their savings
data class MemberWithSavings(val member: Member, val totalSavings: Int)

@Composable
private fun SavingsContent(
    innerPadding: PaddingValues,
    memberSavingsList: List<MemberWithSavings>,
    navigateToProfile: (String) -> Unit
) {
    if (memberSavingsList.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center
        ) {
            Text("No members available")
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(memberSavingsList) { item ->
                MemberSavingsCard(
                    member = item.member,
                    totalSavings = item.totalSavings,
                    onClick = { navigateToProfile(item.member.memberId) }
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