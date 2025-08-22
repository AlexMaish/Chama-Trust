package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.CycleWithBeneficiaries
import kotlinx.coroutines.flow.Flow

interface CycleRepository {
    suspend fun startNewCycle(
        weeklyAmount: Int,
        monthlyAmount: Int,
        totalMembers: Int,
        startDate: Long,
        groupId: String,
        beneficiariesPerMeeting: Int // Add this parameter
    ): Result<Cycle>
    suspend fun endCurrentCycle(cycleId: String, endDate: Long = System.currentTimeMillis()): Result<Unit>
    fun getCurrentCycle(): Flow<Cycle?>
    fun getAllCycles(): Flow<List<Cycle>>
    suspend fun getCycleById(cycleId: String): Cycle?
    suspend fun getActiveCycle(): Cycle?
    suspend fun getCycleStats(cycleId: String): CycleStats
    suspend fun getTotalSavings(): Int?
    suspend fun getCycleHistory(): List<Cycle>
    suspend fun getCyclesForGroup(groupId: String): List<Cycle>
    suspend fun getActiveCycleForGroup(groupId: String): Cycle?
    suspend fun getCyclesByGroupId(groupId: String): List<Cycle>
    suspend fun getCyclesByGroup(groupId: String): List<Cycle>
    suspend fun getCyclesWithBeneficiaries(groupId: String): List<CycleWithBeneficiaries>
    suspend fun deleteCycle(cycleId: String)

    suspend fun getUnsyncedCycles(): List<Cycle>
    suspend fun markCycleSynced(cycle: Cycle)

    suspend fun getUnsyncedCyclesForGroup(groupId: String): List<Cycle>

    suspend fun insertCycle(cycle: Cycle)


    suspend fun markAsDeleted(cycleId: String, timestamp: Long)
    suspend fun getDeletedCycles(): List<Cycle>
    suspend fun permanentDelete(cycleId: String)

}

data class CycleStats(
    val totalCollected: Int,
    val totalDistributed: Int,
    val remainingMembers: Int,
    val completedWeeks: Int
)