package com.example.chamabuddy.data.repository



import com.example.chamabuddy.data.local.CycleDao
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.MonthlySavingDao
import com.example.chamabuddy.data.local.MonthlySavingEntryDao
import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.domain.repository.SavingsProgress
import com.example.chamabuddy.domain.repository.SavingsRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.inject.Inject
import kotlin.collections.count

class SavingsRepositoryImpl @Inject constructor(
    private val savingDao: MonthlySavingDao,
    private val savingEntryDao: MonthlySavingEntryDao,
    private val cycleDao: CycleDao,
    private val memberDao: MemberDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : SavingsRepository {

    override suspend fun recordMonthlySavings(
        cycleId: String,
        monthYear: String,
        memberId: String,
        amount: Int,
        recordedBy: String
    ) = withContext(dispatcher) {
        try {
            // Parse month/year to find the correct cycle
            val sdf = SimpleDateFormat("MM/yyyy", Locale.getDefault())
            val targetDate = sdf.parse(monthYear) ?: throw IllegalArgumentException("Invalid month format")
            // Find the cycle that was active during this month
            val validCycleId = findCycleForDate(targetDate.time) ?: throw IllegalStateException("No active cycle for this month")

            // Get or create the monthly saving record
            var saving = savingDao.getSavingForMonth(validCycleId, monthYear)
            if (saving == null) {
                val cycle = cycleDao.getCycleById(validCycleId) ?: throw IllegalStateException("Cycle not found")
                saving = MonthlySaving(
                    savingId = UUID.randomUUID().toString(),
                    cycleId = validCycleId,
                    monthYear = monthYear,
                    targetAmount = cycle.monthlySavingsAmount
                )
                savingDao.insertMonthlySaving(saving)
            }




            // Record the entry
            savingEntryDao.insertSavingEntry(
                MonthlySavingEntry(
                    entryId = UUID.randomUUID().toString(),
                    savingId = saving.savingId,
                    memberId = memberId,
                    amount = amount,
                    entryDate = System.currentTimeMillis().toString(),
                    recordedBy = recordedBy
                )
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Helper to find the correct cycle for a date
    private suspend fun findCycleForDate(date: Long): String? {
        val cycles = cycleDao.getAllCycles().first()
        return cycles.firstOrNull { cycle ->
            date >= cycle.startDate &&
                    (cycle.endDate == null || date <= cycle.endDate)
        }?.cycleId
    }

    override fun getMemberSavings(cycleId: String, memberId: String): Flow<List<MonthlySavingEntry>> {
        return savingEntryDao.getMemberSavingsForCycle(cycleId, memberId)
    }

    override fun getCycleSavings(cycleId: String): Flow<List<MonthlySaving>> {
        return savingDao.getSavingsForCycle(cycleId)
    }

    override suspend fun getMemberSavingsTotal(cycleId: String, memberId: String) = withContext(dispatcher) {
        savingEntryDao.getMemberSavingsTotal(cycleId, memberId)
    }

    override suspend fun getMonthlySavingsProgress(cycleId: String, monthYear: String) = withContext (dispatcher) {
        val saving = savingDao.getSavingForMonth(cycleId, monthYear) ?: throw IllegalStateException("Monthly saving not found")
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
}