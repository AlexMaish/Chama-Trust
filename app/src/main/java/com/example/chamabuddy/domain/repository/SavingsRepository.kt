package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.domain.model.SavingsProgress
import com.example.chamabuddy.presentation.viewmodel.CycleWithSavings
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
//    suspend fun getCyclesWithSavingsForMember(memberId: String): List<CycleWithSavings>
    suspend fun getMemberSavingsTotal(memberId: String): Int
    fun getMemberSavings(cycleId: String, memberId: String): Flow<List<MonthlySavingEntry>>
    fun getCycleSavings(cycleId: String): Flow<List<MonthlySaving>>
    suspend fun getTotalSavings(): Int
    suspend fun getTotalSavingsByCycle(cycleId: String): Int
    suspend fun getMemberSavingsTotalByCycle(cycleId: String, memberId: String): Int
    suspend fun getMonthlySavingsProgress(cycleId: String, monthYear: String): SavingsProgress
    suspend fun deleteSavingsEntry(entryId: String)
    suspend fun deleteSavingsForMonth(cycleId: String, monthYear: String, groupId: String)

    suspend fun createMissingMonthlyEntries(
        cycleId: String,
        monthYear: String,
        targetAmount: Int,
        groupId: String
    )

    suspend fun getCycleWithSavingsForMember(memberId: String): List<CycleWithSavings>

    suspend fun getMemberSavingsTotalInGroup(groupId: String, memberId: String): Int
    suspend fun createIncompleteMonths(
        cycleId: String,
        startDate: Long,
        endDate: Long,
        monthlyTarget: Int,
        groupId: String
    )

    suspend fun getMemberSavingsTotalByGroupAndCycle(
        groupId: String,
        cycleId: String,
        memberId: String
    ): Int


    suspend fun getTotalGroupSavings(groupId: String): Int


    suspend fun getUnsyncedSavings(): List<MonthlySaving>
    suspend fun getUnsyncedEntries(): List<MonthlySavingEntry>
    suspend fun markSavingSynced(saving: MonthlySaving)
    suspend fun markEntrySynced(entry: MonthlySavingEntry)

    suspend fun getSavingById(savingId: String): MonthlySaving?
    suspend fun insertSaving(saving: MonthlySaving)


    suspend fun getEntryById(entryId: String): MonthlySavingEntry?
    suspend fun updateEntry(entry: MonthlySavingEntry)

    suspend fun insertEntry(entry: MonthlySavingEntry)

    suspend fun markAsDeleted(savingId: String, timestamp: Long)
    suspend fun getDeletedSavings(): List<MonthlySaving>
    suspend fun permanentDelete(savingId: String)

    suspend fun getTotalSavingsForMonth(groupId: String, monthYear: String): Int

    suspend fun getDeletedEntries(): List<MonthlySavingEntry>
    suspend fun permanentDeleteEntry(entryId: String)

  suspend fun getDeletedEntities(): List<MonthlySavingEntry>

    suspend fun getGroupSavingsEntries(groupId: String): List<MonthlySavingEntry>



    suspend fun getMemberName(memberId: String): String?


    suspend fun deleteSavingsEntryImmediately(entryId: String)
    suspend fun deleteSavingsForMonthImmediately(cycleId: String, monthYear: String, groupId: String)
    suspend fun getMemberMonthlySavingsProgress(memberId: String): List<Pair<String, Int>>


    suspend fun getAllUserSavingsEntries(userId: String): List<MonthlySavingEntry>
}