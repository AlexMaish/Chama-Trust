package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Cycle
import kotlinx.coroutines.flow.Flow

interface CycleRepository {
    // Cycle management
    suspend fun startNewCycle(
        weeklyAmount: Int,
        monthlyAmount: Int,
        totalMembers: Int,
        startDate: Long,
        groupId: String // Add this parameter
    ): Result<Cycle>

    suspend fun endCurrentCycle(): Result<Unit>
    suspend fun getActiveCycle(): Cycle?
    // Data access
    fun getCurrentCycle(): Flow<Cycle?>
    fun getAllCycles(): Flow<List<Cycle>>
    suspend fun getCycleById(cycleId: String): Cycle?
    suspend fun getActiveCycleForGroup(groupId: String): Cycle?

    // Stats
    suspend fun getCycleStats(cycleId: String): CycleStats

    suspend fun getTotalSavings(): Int?
    suspend fun getCycleHistory(): List<Cycle>


//    suspend fun createCycle(cycle: Cycle, groupId: String)
    suspend fun getCyclesForGroup(groupId: String): List<Cycle>

    suspend fun getCyclesByGroupId(groupId: String): List<Cycle>

}

data class CycleStats(
    val totalCollected: Int,
    val totalDistributed: Int,
    val remainingMembers: Int,
    val completedWeeks: Int
)