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
            if (memberDao.getMemberById(memberId) == null) {
                throw IllegalStateException("Member $memberId does not exist")
            }
            var saving = savingDao.getSavingForMonth(cycleId, monthYear)
            if (saving == null) {
                val cycle = cycleDao.getCycleById(cycleId)
                    ?: throw IllegalStateException("Cycle not found")
                saving = MonthlySaving(
                    savingId = UUID.randomUUID().toString(),
                    cycleId = cycleId,
                    monthYear = monthYear,
                    targetAmount = cycle.monthlySavingsAmount,
                    groupId = groupId
                )
                savingDao.insert(saving)
            }
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
                    entryDate = calendar.timeInMillis.toString(),
                    recordedBy = recordedBy,
                    groupId = groupId,
                )
            )

            val total = savingEntryDao.getEntriesForSaving(saving.savingId)
                .first().sumOf { it.amount }
            savingDao.update(saving.copy(actualAmount = total))

            val monthlyTarget = saving.targetAmount
            val currentTotal = savingEntryDao.getEntriesForSaving(saving.savingId)
                .first().sumOf { it.amount } + amount

            if (currentTotal >= monthlyTarget) {
                val nextMonth = calculateNextMonth(monthYear)
                ensureMonthExists(cycleId, nextMonth, monthlyTarget, groupId)
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
                    groupId = groupId
                )
            )
        }
    }

    override suspend fun getCyclesWithSavingsForMember(memberId: String): List<CycleWithSavings> {
        return withContext(dispatcher) {
            // 1. Get distinct cycle IDs where member has savings
            val cycleIds = savingDao.getDistinctCycleIdsForMember(memberId)
            if (cycleIds.isEmpty()) return@withContext emptyList()

            // 2. Get complete cycle objects
            val cycles = cycleDao.getCyclesByIds(cycleIds)

            // 3. Get savings entries for each cycle
            cycles.map { cycle ->
                val entries = savingEntryDao.getSavingsForMemberInCycle(
                    memberId = memberId,
                    cycleId = cycle.cycleId
                )
                CycleWithSavings(cycle, entries)
            }
        }
    }






    override suspend fun getMemberSavingsTotal(memberId: String): Int {
        return savingEntryDao.getMemberSavingsTotal(memberId) ?: 0
    }

    override fun getMemberSavings(cycleId: String, memberId: String): Flow<List<MonthlySavingEntry>> {
        return savingEntryDao.getMemberSavingsForCycle(cycleId, memberId)
    }

    override fun getCycleSavings(cycleId: String): Flow<List<MonthlySaving>> {
        return savingDao.getSavingsForCycle(cycleId)
    }

    override suspend fun getMemberSavingsTotalByCycle(cycleId: String, memberId: String) = withContext(dispatcher) {
        savingEntryDao.getMemberSavingsTotalByCycle(cycleId, memberId)
    }

    override suspend fun getMonthlySavingsProgress(cycleId: String, monthYear: String) = withContext(dispatcher) {
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


    // Add this method to create missing entries
    override suspend fun createMissingMonthlyEntries(
        memberId: String,
        currentCycleId: String,
        newMonthlyTarget: Int
    ) {
        withContext(dispatcher) {
            try {
                // 1. Get all cycles for the member
                val allCycles = cycleDao.getCyclesForMember(memberId)

                // 2. Find active and previous cycles
                val activeCycle = allCycles.firstOrNull { it.endDate == null }
                val previousCycles = allCycles.filter { it.endDate != null }

                // 3. For each previous cycle, find incomplete months
                previousCycles.forEach { cycle ->
                    val monthsInCycle = generateMonthsForCycle(
                        cycle.startDate,
                        cycle.endDate ?: 0L,
                        false
                    )

                    monthsInCycle.forEach { monthDisplay ->
                        val monthInput = convertMonthToInputFormat(monthDisplay)
                        val existing = savingEntryDao.getSavingsForMonth(
                            cycle.cycleId,
                            memberId,
                            monthInput
                        )

                        val total = existing.sumOf { it.amount }
                        val target = cycle.monthlySavingsAmount

                        // Create entry if savings are incomplete
                        if (total < target) {
                            val saving = savingDao.getSavingForMonth(cycle.cycleId, monthInput)
                                ?: MonthlySaving(
                                    savingId = UUID.randomUUID().toString(),
                                    cycleId = cycle.cycleId,
                                    monthYear = monthInput,
                                    targetAmount = target,
                                    actualAmount = total,
                                    groupId = cycle.groupId
                                ).also { savingDao.insert(it) }

                            // Create a placeholder entry
                            savingEntryDao.insertSavingEntry(
                                MonthlySavingEntry(
                                    entryId = UUID.randomUUID().toString(),
                                    savingId = saving.savingId,
                                    memberId = memberId,
                                    amount = 0,
                                    entryDate = System.currentTimeMillis().toString(),
                                    recordedBy = "system",
                                    groupId = cycle.groupId,
                                    isPlaceholder = true // Mark as incomplete
                                )
                            )
                        }
                    }
                }

                // 4. Create entry for new cycle
                activeCycle?.let {
                    val currentMonth = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                        .format(Date())
                    savingDao.getSavingForMonth(it.cycleId, currentMonth) ?: run {
                        savingDao.insert(
                            MonthlySaving(
                                savingId = UUID.randomUUID().toString(),
                                cycleId = it.cycleId,
                                monthYear = currentMonth,
                                targetAmount = newMonthlyTarget,
                                actualAmount = 0,
                                groupId = it.groupId
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }







}
