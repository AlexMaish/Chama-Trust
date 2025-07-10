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
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.viewmodel.SavingsState
import com.example.chamabuddy.presentation.viewmodel.SavingsViewModel

// Premium color scheme
//val PremiumNavy = Color(0xFF0A1D3A)
//val SoftOffWhite = Color(0xFFF8F9FA)
//val VibrantOrange = Color(0xFFFF6B35)
//val CardSurface = Color(0xFFFFFFFF)

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
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // Calculate total savings for all members
    val totalGroupSavings = remember(memberTotals) {
        memberTotals.values.sum()
    }

    val memberSavingsList = remember(members, memberTotals) {
        members.values.map { member ->
            val total = memberTotals[member.memberId] ?: 0
            MemberWithSavings(member, total)
        }
    }


    LaunchedEffect(groupId) {
        if (viewModel.activeCycle.value == null) {
            // Show error or disable UI
        } else {
            viewModel.initializeGroupId(groupId)
        }
    }


    LaunchedEffect(groupId) {
        viewModel.initializeGroupId(groupId)
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        "Member Savings",
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
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
        containerColor = SoftOffWhite
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
                    SavingsContent(innerPadding, memberSavingsList, navigateToProfile, totalGroupSavings)
                }
            }
            is SavingsState.Error -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    Text((state as SavingsState.Error).message, color = PremiumNavy)
                }
            }
            else -> {
                SavingsContent(innerPadding, memberSavingsList, navigateToProfile, totalGroupSavings)
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
    navigateToProfile: (String) -> Unit,
    totalGroupSavings: Int
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Total savings header
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = CardSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Total Group Savings",
                        style = MaterialTheme.typography.titleMedium,
                        color = PremiumNavy.copy(alpha = 0.7f)
                    )
                    Text(
                        "KES $totalGroupSavings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = VibrantOrange
                    )
                }
            }
        }

        // Members list
        items(memberSavingsList) { item ->
            MemberSavingsCard(
                member = item.member,
                totalSavings = item.totalSavings,
                onClick = { navigateToProfile(item.member.memberId) }
            )
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
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
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
                    .background(VibrantOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = member.name.take(1).uppercase(),
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = member.name,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = PremiumNavy
                )
                Text(
                    text = member.phoneNumber,
                    fontSize = 14.sp,
                    color = PremiumNavy.copy(alpha = 0.7f)
                )
            }

            Text(
                text = "KES $totalSavings",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = VibrantOrange
            )
        }
    }
}