package com.example.chamabuddy.domain.model

data class CycleSummary(
    val cycle: Cycle,
    val meetingsHeld: Int,
    val totalCollected: Int,
    val totalDistributed: Int,
    val remainingMembers: Int
)