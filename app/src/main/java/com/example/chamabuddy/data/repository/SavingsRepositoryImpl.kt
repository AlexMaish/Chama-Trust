package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.AppDatabase
import com.example.chamabuddy.data.local.CycleDao
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.MonthlySavingDao
import com.example.chamabuddy.data.local.MonthlySavingEntryDao
import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.domain.model.SavingsProgress
import com.example.chamabuddy.domain.repository.SavingsRepository
import com.example.chamabuddy.presentation.viewmodel.CycleWithSavings
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

class SavingsRepositoryImpl @Inject constructor(
    private val db: AppDatabase,
    private val savingDao: MonthlySavingDao,
    private val savingEntryDao: MonthlySavingEntryDao,
    private val cycleDao: CycleDao,
    private val memberDao: MemberDao,
    private val dispatcher: CoroutineDispatcher
) : SavingsRepository {

    override suspend fun recordMonthlySavings(
        cycleId: String,
        monthYear: String,
        memberId: String,
        amount: Int,
        recordedBy: String,
        groupId: String
    ) = withContext(dispatcher) {
        try {
            // 1. Check if cycle belongs to the group
            val cycle = cycleDao.getCycleById(cycleId)
                ?: throw IllegalStateException("Cycle not found")

            if (cycle.groupId != groupId) {
                throw IllegalStateException("Cycle does not belong to this group")
            }

            // 2. Validate member and recorder
            val recorder = memberDao.getMemberById(recordedBy)
                ?: throw IllegalStateException("Recorder (ID: $recordedBy) is not a valid group member")

            val member = memberDao.getMemberById(memberId)
                ?: throw IllegalStateException("Member (ID: $memberId) does not exist")

            // 3. Get or create MonthlySaving record
            var saving = savingDao.getSavingForMonth(cycleId, monthYear)
            if (saving == null) {
                saving = MonthlySaving(
                    savingId = UUID.randomUUID().toString(),
                    cycleId = cycleId,
                    monthYear = monthYear,
                    targetAmount = cycle.monthlySavingsAmount,
                    groupId = groupId,
                    isSynced = false
                )
                savingDao.insert(saving)
            }

            // 4. Create and insert new saving entry
            val monthFormat = SimpleDateFormat("MM/yyyy", Locale.getDefault())
            val targetDate = monthFormat.parse(monthYear)
            val calendar = Calendar.getInstance().apply {
                time = targetDate ?: Date()
                set(Calendar.DAY_OF_MONTH, 1)
            }

            savingEntryDao.insertSavingEntry(
                MonthlySavingEntry(
                    entryId = UUID.randomUUID().toString(),
                    savingId = saving.savingId,
                    memberId = memberId,
                    amount = amount,
                    entryDate = System.currentTimeMillis(), // Current timestamp
                    recordedBy = recordedBy,
                    groupId = groupId,
                    monthYear = monthYear ,
                    isSynced = false
                )
            )

            // 5. Update total saved
            val total = savingEntryDao.getEntriesForSaving(saving.savingId)
                .first().sumOf { it.amount }
            savingDao.update(saving.copy(actualAmount = total))

            // 6. Optionally auto-create next month if target met
            val currentTotal = total
            if (currentTotal >= saving.targetAmount) {
                val nextMonth = calculateNextMonth(monthYear)
                createMissingMonthlyEntries(cycleId, nextMonth, saving.targetAmount, groupId)
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun getTotalSavings(): Int = withContext(dispatcher) {
        savingEntryDao.getTotalSavings() ?: 0
    }

    override suspend fun getTotalSavingsByCycle(cycleId: String): Int = withContext(dispatcher) {
        savingEntryDao.getTotalSavingsByCycle(cycleId) ?: 0
    }


    private fun calculateNextMonth(currentMonth: String): String {
        val format = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val date = format.parse(currentMonth) ?: return currentMonth
        val calendar = Calendar.getInstance().apply { time = date }
        calendar.add(Calendar.MONTH, 1)
        return format.format(calendar.time)
    }

    override suspend fun ensureMonthExists(
        cycleId: String,
        monthYear: String,
        targetAmount: Int,
        groupId: String
    ) {
        if (savingDao.getSavingForMonth(cycleId, monthYear) == null) {
            savingDao.insert(
                MonthlySaving(
                    savingId = UUID.randomUUID().toString(),
                    cycleId = cycleId,
                    monthYear = monthYear,
                    targetAmount = targetAmount,
                    actualAmount = 0,
                    groupId = groupId,
                    isSynced = false
                )
            )
        }
    }

    override suspend fun createMissingMonthlyEntries(
        cycleId: String,
        monthYear: String,
        targetAmount: Int,
        groupId: String
    ) {
        if (savingDao.getSavingForMonth(cycleId, monthYear) == null) {
            savingDao.insert(
                MonthlySaving(
                    savingId = UUID.randomUUID().toString(),
                    cycleId = cycleId,
                    monthYear = monthYear,
                    targetAmount = targetAmount,
                    actualAmount = 0,
                    groupId = groupId
                )
            )
        }
    }


    override suspend fun getMemberSavingsTotal(memberId: String): Int {
        return savingEntryDao.getMemberSavingsTotal(memberId) ?: 0
    }

    override fun getMemberSavings(
        cycleId: String,
        memberId: String
    ): Flow<List<MonthlySavingEntry>> {
        return savingEntryDao.getMemberSavingsForCycle(cycleId, memberId)
    }

    override fun getCycleSavings(cycleId: String): Flow<List<MonthlySaving>> {
        return savingDao.getSavingsForCycle(cycleId)
    }

    override suspend fun getMemberSavingsTotalByCycle(cycleId: String, memberId: String) =
        withContext(dispatcher) {
            savingEntryDao.getMemberSavingsTotalByCycle(cycleId, memberId)
        }

    override suspend fun getMonthlySavingsProgress(cycleId: String, monthYear: String) =
        withContext(dispatcher) {
            val saving = savingDao.getSavingForMonth(cycleId, monthYear)
                ?: throw IllegalStateException("Monthly saving not found")
            val activeMembers = memberDao.getActiveMembers().first()
            val entries = savingEntryDao.getEntriesForSaving(saving.savingId).first()
            val currentAmount = entries.sumOf { it.amount }
            val membersCompleted = entries
                .groupBy { it.memberId }
                .count { (_, memberEntries) ->
                    memberEntries.sumOf { it.amount } >= saving.targetAmount
                }

            SavingsProgress(
                targetAmount = saving.targetAmount * activeMembers.size,
                currentAmount = currentAmount,
                membersCompleted = membersCompleted,
                totalMembers = activeMembers.size
            )
        }

    override suspend fun getCycleWithSavingsForMember(memberId: String): List<CycleWithSavings> {
        return withContext(dispatcher) {
            val cycleIds = savingDao.getDistinctCycleIdsForMember(memberId)
            if (cycleIds.isEmpty()) emptyList()
            else {
                val cycles = cycleDao.getCyclesByIds(cycleIds)
                cycles.map { cycle ->
                    val entries = savingEntryDao.getSavingsForMemberInCycle(
                        memberId = memberId,
                        cycleId = cycle.cycleId
                    )
                    CycleWithSavings(cycle, entries)
                }
            }
        }
    }


    override suspend fun createIncompleteMonths(
        cycleId: String,
        startDate: Long,
        endDate: Long,
        monthlyTarget: Int,
        groupId: String
    ) = withContext(dispatcher) {
        // Extend end date by 2 months
        val calendar = Calendar.getInstance().apply {
            time = Date(endDate)
            add(Calendar.MONTH, 2)
        }
        val extendedEndDate = calendar.timeInMillis

        val months = generateMonthsBetweenDates(startDate, extendedEndDate)
        months.forEach { month ->
            ensureMonthExists(cycleId, month, monthlyTarget, groupId)
        }
    }
    private fun generateMonthsBetweenDates(start: Long, end: Long): List<String> {
        val format = SimpleDateFormat("MM/yyyy", Locale.getDefault())
        val months = mutableListOf<String>()
        val calendar = Calendar.getInstance().apply {
            time = Date(start)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        val endCalendar = Calendar.getInstance().apply {
            time = Date(end)
            set(Calendar.DAY_OF_MONTH, 1)
        }

        while (!calendar.after(endCalendar)) {
            months.add(format.format(calendar.time))
            calendar.add(Calendar.MONTH, 1)
        }

        return months
    }
    override suspend fun getMemberSavingsTotalInGroup(
        groupId: String,
        memberId: String
    ): Int = withContext(dispatcher) {
        savingEntryDao.getMemberSavingsTotalInGroup(groupId, memberId)
    }


    override suspend fun getMemberSavingsTotalByGroupAndCycle(
        groupId: String,
        cycleId: String,
        memberId: String
    ): Int {
        return savingEntryDao.getTotalForMemberInGroupCycle(
            memberId = memberId,
            groupId = groupId,
            cycleId = cycleId
        )
    }


    override suspend fun getTotalGroupSavings(groupId: String): Int {
        return savingEntryDao.getTotalSavingsByGroup(groupId) ?: 0
    }

    override suspend fun deleteSavingsEntry(entryId: String) {
        savingEntryDao.deleteEntry(entryId)
    }

    override suspend fun deleteSavingsForMonth(cycleId: String, monthYear: String, groupId: String) {
        val saving = savingDao.getSavingForMonth(cycleId, monthYear)
        saving?.let {
            savingDao.deleteSaving(it.savingId)
        }
    }




    override suspend fun getUnsyncedSavings(): List<MonthlySaving> =
        withContext(dispatcher) {
            savingDao.getUnsyncedSavings()
        }

    override suspend fun getUnsyncedEntries(): List<MonthlySavingEntry> =
        withContext(dispatcher) {
            savingEntryDao.getUnsyncedEntries()
        }

    override suspend fun markSavingSynced(saving: MonthlySaving) {
        withContext(dispatcher) {
            savingDao.markAsSynced(saving.savingId)
        }
    }

    override suspend fun markEntrySynced(entry: MonthlySavingEntry) {
        withContext(dispatcher) {
            savingEntryDao.markAsSynced(entry.entryId)
        }
    }

    override suspend fun getSavingById(savingId: String): MonthlySaving? = withContext(dispatcher) {
        savingDao.getSavingById(savingId)
    }

    override suspend fun insertSaving(saving: MonthlySaving) = withContext(dispatcher) {
        savingDao.insert(saving)
    }


}
