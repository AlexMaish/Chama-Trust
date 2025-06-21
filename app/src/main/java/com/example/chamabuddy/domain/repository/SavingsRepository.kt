package com.example.chamabuddy.domain.repository


import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    // Monthly savings operations
    suspend fun recordMonthlySavings(
        cycleId: String,
        monthYear: String,
        memberId: String,
        amount: Int,
        recordedBy: String
    ): Result<Unit>

    // Data access
    fun getMemberSavings(cycleId: String, memberId: String): Flow<List<MonthlySavingEntry>>
    fun getCycleSavings(cycleId: String): Flow<List<MonthlySaving>>

    // Stats
    suspend fun getMemberSavingsTotal(cycleId: String, memberId: String): Int
    suspend fun getMonthlySavingsProgress(cycleId: String, monthYear: String): SavingsProgress
}

data class SavingsProgress(
    val targetAmount: Int,
    val currentAmount: Int,
    val membersCompleted: Int,
    val totalMembers: Int
)