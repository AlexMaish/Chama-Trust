package com.example.chamabuddy.presentation.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.chamabuddy.presentation.viewmodel.FilterType
import com.example.chamabuddy.presentation.viewmodel.SavingsEntry
import com.example.chamabuddy.presentation.viewmodel.SavingsFilterState
import com.example.chamabuddy.presentation.viewmodel.SavingsFilterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

val DeepNavy = Color(0xFF0A1D3A)
val GoldAccent = Color(0xFFFFD700)
val SoftCream = Color(0xFFF8F5F0)
val LightGold = Color(0xFFFFF9E6)
val DarkGold = Color(0xFFD4AF37)
val CardWhite = Color(0xFFFFFFFF)
val TextDark = Color(0xFF2D2D2D)
val TextLight = Color(0xFF757575)
val ExpandableCardColor = Color(0xFFF5F7FA)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavingsFilterScreen(
    groupId: String,
    onBack: () -> Unit,
    viewModel: SavingsFilterViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val filterType by viewModel.filterType.collectAsState()
    val savingsByDate by viewModel.savingsByDate.collectAsState()
    val savingsByMonth by viewModel.savingsByMonth.collectAsState()
    val savingsByMappedMonth by viewModel.savingsByMappedMonth.collectAsState()

    LaunchedEffect(groupId) {
        viewModel.loadGroupSavings(groupId)
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Savings History",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = DeepNavy
                )
            )
        },
        containerColor = SoftCream
    ) { padding ->
        when (state) {
            is SavingsFilterState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GoldAccent)
                }
            }
            is SavingsFilterState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        (state as SavingsFilterState.Error).message,
                        color = DeepNavy,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            is SavingsFilterState.Loaded -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    FilterTypeToggle(
                        currentFilter = filterType,
                        onFilterChange = { viewModel.setFilterType(it) },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    when (filterType) {
                        FilterType.DATE -> DateFilteredList(savingsByDate)
                        FilterType.MONTH -> MonthFilteredList(savingsByMonth)
                        FilterType.MAPPED_MONTH -> MonthFilteredList(savingsByMappedMonth)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilterTypeToggle(
    currentFilter: FilterType,
    onFilterChange: (FilterType) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                "Filter by:",
                modifier = Modifier.padding(bottom = 8.dp),
                fontWeight = FontWeight.SemiBold,
                color = TextDark,
                fontSize = 16.sp
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    selected = currentFilter == FilterType.DATE,
                    onClick = { onFilterChange(FilterType.DATE) },
                    label = { Text("Date") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepNavy,
                        selectedLabelColor = Color.White,
                        containerColor = LightGold,
                        labelColor = TextDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == FilterType.DATE,
                        borderColor = Color.Transparent,
                        selectedBorderColor = GoldAccent,
                        disabledBorderColor = Color.Transparent,
                        borderWidth = 0.dp,
                        selectedBorderWidth = 2.dp
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilterChip(
                    selected = currentFilter == FilterType.MONTH,
                    onClick = { onFilterChange(FilterType.MONTH) },
                    label = { Text("Entry Month") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepNavy,
                        selectedLabelColor = Color.White,
                        containerColor = LightGold,
                        labelColor = TextDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == FilterType.MONTH,
                        borderColor = Color.Transparent,
                        selectedBorderColor = GoldAccent,
                        disabledBorderColor = Color.Transparent,
                        borderWidth = 0.dp,
                        selectedBorderWidth = 2.dp
                    )
                )

                Spacer(modifier = Modifier.width(8.dp))

                FilterChip(
                    selected = currentFilter == FilterType.MAPPED_MONTH,
                    onClick = { onFilterChange(FilterType.MAPPED_MONTH) },
                    label = { Text("Mapped Month") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DeepNavy,
                        selectedLabelColor = Color.White,
                        containerColor = LightGold,
                        labelColor = TextDark
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = currentFilter == FilterType.MAPPED_MONTH,
                        borderColor = Color.Transparent,
                        selectedBorderColor = GoldAccent,
                        disabledBorderColor = Color.Transparent,
                        borderWidth = 0.dp,
                        selectedBorderWidth = 2.dp
                    )
                )
            }
        }
    }
}

@Composable
fun DateFilteredList(savingsByDate: Map<Long, List<SavingsEntry>>) {
    val sortedDates = savingsByDate.keys.sortedDescending()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sortedDates.forEach { date ->
            val entries = savingsByDate[date] ?: emptyList()
            val total = entries.sumOf { it.amount }

            item {
                ExpandableDateCard(date, total, entries)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun MonthFilteredList(savingsByMonth: Map<String, List<SavingsEntry>>) {
    // Sort months in descending order (expecting keys like "January 2025")
    val sortedMonths = savingsByMonth.keys.sortedByDescending {
        SimpleDateFormat("MMMM yyyy", Locale.getDefault()).parse(it)?.time ?: 0L
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        sortedMonths.forEach { month ->
            val entries = savingsByMonth[month] ?: emptyList()
            val total = entries.sumOf { it.amount }

            item {
                ExpandableMonthCard(month, total, entries)
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun ExpandableDateCard(date: Long, total: Int, entries: List<SavingsEntry>) {
    var expanded by remember { mutableStateOf(false) }
    val dateFormat = remember { SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()) }
    val dateString = remember(date) { dateFormat.format(Date(date)) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ExpandableCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = dateString,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${entries.size} entries",
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(DarkGold, GoldAccent)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "KES $total",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(
                        color = LightGold,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    entries.forEach { entry ->
                        SavingsEntryItem(entry)
                        if (entry != entries.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ExpandableMonthCard(month: String, total: Int, entries: List<SavingsEntry>) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(animationSpec = tween(durationMillis = 300)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = ExpandableCardColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = month,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = "${entries.size} entries",
                        fontSize = 12.sp,
                        color = TextLight
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(DarkGold, GoldAccent)
                                ),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "KES $total",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Icon(
                        imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = if (expanded) "Collapse" else "Expand",
                        tint = TextLight,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            AnimatedVisibility(
                visible = expanded,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                ) {
                    Divider(
                        color = LightGold,
                        thickness = 1.dp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    entries.forEach { entry ->
                        SavingsEntryItem(entry)
                        if (entry != entries.last()) {
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SavingsEntryItem(entry: SavingsEntry) {
    val dateFormat = remember { SimpleDateFormat("h:mm a", Locale.getDefault()) }
    val timeString = remember(entry.entryDate) { dateFormat.format(Date(entry.entryDate)) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.memberName,
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark,
                    fontSize = 14.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Recorded at: $timeString",
                    fontSize = 11.sp,
                    color = TextLight
                )
            }

            Box(
                modifier = Modifier
                    .background(
                        color = LightGold,
                        shape = RoundedCornerShape(6.dp)
                    )
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "KES ${entry.amount}",
                    fontWeight = FontWeight.Bold,
                    color = DeepNavy,
                    fontSize = 13.sp
                )
            }
        }
    }
}