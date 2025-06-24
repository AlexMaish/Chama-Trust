package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Cycle
import kotlinx.coroutines.flow.Flow

interface CycleRepository {
    // Cycle management
    suspend fun startNewCycle(
        weeklyAmount: Int,
        monthlySavingsAmount: Int,
        totalMembers: Int,
        startDate: Long
    ): Result<Cycle>

    suspend fun endCurrentCycle(): Result<Unit>
    suspend fun getActiveCycle(): Cycle?
    // Data access
    fun getCurrentCycle(): Flow<Cycle?>
    fun getAllCycles(): Flow<List<Cycle>>
    suspend fun getCycleById(cycleId: String): Cycle?

    // Stats
    suspend fun getCycleStats(cycleId: String): CycleStats
}

data class CycleStats(
    val totalCollected: Int,
    val totalDistributed: Int,
    val remainingMembers: Int,
    val completedWeeks: Int
)