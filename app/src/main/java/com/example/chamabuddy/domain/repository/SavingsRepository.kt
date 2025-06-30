package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.domain.model.SavingsProgress
import kotlinx.coroutines.flow.Flow

interface SavingsRepository {
    suspend fun recordMonthlySavings(
        cycleId: String,
        monthYear: String,
        memberId: String,
        amount: Int,
        recordedBy: String,
        groupId: String
    ): Result<Unit>

    suspend fun ensureMonthExists(
        cycleId: String,
        monthYear: String,
        targetAmount: Int,
        groupId: String
    )

    suspend fun getMemberSavingsTotal(memberId: String): Int
    fun getMemberSavings(cycleId: String, memberId: String): Flow<List<MonthlySavingEntry>>
    fun getCycleSavings(cycleId: String): Flow<List<MonthlySaving>>
    suspend fun getTotalSavings(): Int
    suspend fun getTotalSavingsByCycle(cycleId: String): Int
    suspend fun getMemberSavingsTotalByCycle(cycleId: String, memberId: String): Int
    suspend fun getMonthlySavingsProgress(cycleId: String, monthYear: String): SavingsProgress }