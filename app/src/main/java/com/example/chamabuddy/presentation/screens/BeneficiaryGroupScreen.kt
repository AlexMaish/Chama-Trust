package com.example.chamabuddy.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.example.chamabuddy.domain.model.BeneficiaryWithMember
import com.example.chamabuddy.domain.model.CycleWithBeneficiaries
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.presentation.viewmodel.BeneficiaryGroupViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


val PremiumGold = Color(0xFFD4AF37)
val SoftGray = Color(0xFFE0E0E0)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BeneficiaryGroupScreen(
    groupId: String,
    navigateBack: () -> Unit,
    navController: NavHostController,
    viewModel: BeneficiaryGroupViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadData(groupId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = PremiumNavy,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                ),
                title = {
                    Text(
                        "Beneficiaries",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = navigateBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PremiumGold
                        )
                    }
                }
            )
        },
        containerColor = SoftOffWhite
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
                .background(SoftOffWhite)
        ) {
            when {
                state.isLoading -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = PremiumNavy
                )

                state.error != null -> Text(
                    "Error: ${state.error}",
                    modifier = Modifier.align(Alignment.Center),
                    color = PremiumNavy
                )

                state.cycles.isEmpty() -> Text(
                    "No beneficiaries found",
                    modifier = Modifier.align(Alignment.Center),
                    color = PremiumNavy
                )

                else -> LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(state.cycles) { cycle ->
                        val benefitedMemberIds = cycle.beneficiaries.map { it.member.memberId }
                        val remainingMembers = state.activeMembers.filterNot { member ->
                            benefitedMemberIds.contains(member.memberId)
                        }

                        ExpandableCycleBeneficiaries(
                            cycle = cycle,
                            activeMembers = state.activeMembers,
                            remainingMembers = remainingMembers
                        )
                    }
                    item { Spacer(modifier = Modifier.height(24.dp)) }
                }
            }
        }
    }
}

@Composable
fun ExpandableCycleBeneficiaries(
    cycle: CycleWithBeneficiaries,
    activeMembers: List<Member>,
    remainingMembers: List<Member>
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, SoftGray, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = CardSurface),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        brush = if (expanded) {
                            horizontalGradient(listOf(LightAccentBlue, LightAccentBlue.copy(alpha = 0.7f)))
                        } else {
                            horizontalGradient(listOf(CardSurface, CardSurface))
                        }
                    )
                    .padding(vertical = 12.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        text = "Cycle #${cycle.cycle.cycleNumber}",
                        fontWeight = FontWeight.Bold,
                        color = PremiumNavy,
                        fontSize = 16.sp
                    )

                    Text(
                        text = formatCycleDates(cycle.cycle.startDate, cycle.cycle.endDate),
                        color = PremiumNavy,
                        fontSize = 14.sp
                    )

                    Text(
                        text = "ID: ${cycle.cycle.cycleId}",
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = PremiumNavy
                    )
                }
            }

            if (expanded) {
                Divider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = LightAccentBlue,
                    thickness = 1.dp
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    SectionHeader("Active Members (${activeMembers.size})")
                    Spacer(modifier = Modifier.height(8.dp))
                    activeMembers.forEach { member ->
                        MemberItem(member = member)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (cycle.beneficiaries.isNotEmpty()) {
                        SectionHeader("Beneficiaries (${cycle.beneficiaries.size})")
                        Spacer(modifier = Modifier.height(8.dp))
                        cycle.beneficiaries.forEach { beneficiary ->
                            BeneficiaryItem(beneficiary = beneficiary)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (remainingMembers.isNotEmpty()) {
                        SectionHeader("Remaining to Benefit (${remainingMembers.size})")
                        Spacer(modifier = Modifier.height(8.dp))
                        remainingMembers.forEach { member ->
                            RemainingMemberItem(member = member)
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        fontWeight = FontWeight.SemiBold,
        color = PremiumNavy,
        fontSize = 16.sp,
        modifier = Modifier.padding(bottom = 4.dp)
    )
}

@Composable
fun MemberItem(member: Member, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SoftOffWhite.copy(alpha = 0.5f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = "Member",
            tint = PremiumNavy,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = member.name,
            color = PremiumNavy,
            fontSize = 15.sp
        )
    }
}

@Composable
fun BeneficiaryItem(beneficiary: BeneficiaryWithMember, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(SoftGreen.copy(alpha = 0.1f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Beneficiary",
            tint = SoftGreen,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column {
            Text(
                text = beneficiary.member.name,
                fontWeight = FontWeight.SemiBold,
                color = PremiumNavy,
                fontSize = 15.sp
            )
            Text(
                text = "Received: Ksh ${beneficiary.beneficiary.amountReceived}",
                color = SoftGreen,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun RemainingMemberItem(member: Member, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(LightAccentBlue.copy(alpha = 0.2f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.Schedule,
            contentDescription = "Pending",
            tint = VibrantOrange,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = member.name,
            color = PremiumNavy,
            fontSize = 15.sp
        )
    }
}

fun horizontalGradient(colors: List<Color>) = Brush.horizontalGradient(colors)

@Composable
fun formatCycleDates(start: Long, end: Long?): String {
    val startDate = remember(start) {
        SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(start))
    }
    val endDate = end?.let {
        remember(it) {
            SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(it))
        }
    } ?: "Active"

    return "$startDate - $endDate"
}