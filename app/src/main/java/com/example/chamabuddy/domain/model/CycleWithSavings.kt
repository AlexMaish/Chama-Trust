package com.example.chamabuddy.domain.model

data class CycleWithSavings(
    val cycle: Cycle,
    val savingsEntries: List<MonthlySavingEntry>
)