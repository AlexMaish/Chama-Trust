package com.example.chamabuddy.data.repository

import android.net.ConnectivityManager
import android.net.NetworkCapabilities
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
import com.example.chamabuddy.util.SyncLogger
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
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
    private val dispatcher: CoroutineDispatcher,
    private val firestore: FirebaseFirestore,
    private val connectivityManager: ConnectivityManager
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
            val cycle = cycleDao.getCycleById(cycleId)
                ?: throw IllegalStateException("Cycle not found")

            if (cycle.groupId != groupId) {
                throw IllegalStateException("Cycle does not belong to this group")
            }

            val recorder = memberDao.getMemberById(recordedBy)
                ?: throw IllegalStateException("Recorder (ID: $recordedBy) is not a valid group member")

            val member = memberDao.getMemberById(memberId)
                ?: throw IllegalStateException("Member (ID: $memberId) does not exist")

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

            val currentTotal = savingEntryDao.getCurrentTotalForMemberMonth(
                cycleId, monthYear, memberId
            )
            if (currentTotal + amount > saving.targetAmount) {
                val remaining = saving.targetAmount - currentTotal
                throw IllegalArgumentException(
                    "Cannot save more than monthly target. " +
                            "Remaining: $remaining, Attempted: $amount"
                )
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
                    entryDate = System.currentTimeMillis(),
                    recordedBy = recordedBy,
                    groupId = groupId,
                    monthYear = monthYear,
                    isSynced = false
                )
            )

            val total = savingEntryDao.getEntriesForSaving(saving.savingId)
                .first().sumOf { it.amount }
            savingDao.update(saving.copy(actualAmount = total))

            if (total >= saving.targetAmount) {
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
    override suspend fun getEntryById(entryId: String): MonthlySavingEntry? {
        return savingEntryDao.getEntryById(entryId)
    }

    override suspend fun updateEntry(entry: MonthlySavingEntry) {
        savingEntryDao.updateEntry(entry)
    }
    override suspend fun insertEntry(entry: MonthlySavingEntry) {
        savingEntryDao.insertSavingEntry(entry)
    }


    override suspend fun markAsDeleted(savingId: String, timestamp: Long) =
        savingDao.markAsDeleted(savingId, timestamp)

    override suspend fun getDeletedSavings(): List<MonthlySaving> =
        savingDao.getDeletedSavings()

    override suspend fun permanentDelete(savingId: String) =
        savingDao.permanentDelete(savingId)


    override suspend fun getDeletedEntries(): List<MonthlySavingEntry> {
        return savingEntryDao.getDeletedEntries()
    }

    override suspend fun getDeletedEntities(): List<MonthlySavingEntry> =
        savingEntryDao.getDeletedEntries()

    override suspend fun permanentDeleteEntry(entryId: String) {
        savingEntryDao.permanentDelete(entryId)
    }

    override suspend fun deleteSavingsEntry(entryId: String) {
        withContext(dispatcher) {
            savingEntryDao.markAsDeleted(entryId, System.currentTimeMillis())
        }
    }

    override suspend fun deleteSavingsForMonth(cycleId: String, monthYear: String, groupId: String) {
        withContext(dispatcher) {
            val saving = savingDao.getSavingForMonth(cycleId, monthYear)
            saving?.let {
                savingDao.markAsDeleted(it.savingId, System.currentTimeMillis())

                val entries = savingEntryDao.getEntriesForSaving(it.savingId).first()
                entries.forEach { entry ->
                    savingEntryDao.markAsDeleted(entry.entryId, System.currentTimeMillis())
                }
            }
        }
    }



    override suspend fun getTotalSavingsForMonth(groupId: String, monthYear: String): Int {
        return withContext(dispatcher) {
            savingDao.getTotalSavingsForMonth(groupId, monthYear) ?: 0
        }
    }
    override suspend fun getGroupSavingsEntries(groupId: String): List<MonthlySavingEntry> {
        return withContext(dispatcher) {
            savingEntryDao.getGroupSavingsEntries(groupId)
        }
    }


    override suspend fun getMemberName(memberId: String): String? = withContext(dispatcher) {
        memberDao.getMemberById(memberId)?.name
    }


    override suspend fun deleteSavingsEntryImmediately(entryId: String) {
        withContext(dispatcher) {
            val entry = savingEntryDao.getEntryById(entryId)
            entry?.let {
                savingEntryDao.markAsDeleted(entryId, System.currentTimeMillis())

                if (isOnline()) {
                    try {
                        firestore.collection("groups/${it.groupId}/monthly_saving_entries")
                            .document(entryId)
                            .delete()
                            .await()
                        savingEntryDao.permanentDelete(entryId)
                    } catch (e: Exception) {
                        SyncLogger.e("Immediate entry deletion failed: ${e.message}")
                    }
                }
            }
        }
    }

    override suspend fun deleteSavingsForMonthImmediately(cycleId: String, monthYear: String, groupId: String) {
        withContext(dispatcher) {
            val saving = savingDao.getSavingForMonth(cycleId, monthYear)
            saving?.let {
                savingDao.markAsDeleted(it.savingId, System.currentTimeMillis())

                val entries = savingEntryDao.getEntriesForSaving(it.savingId).first()
                entries.forEach { entry ->
                    savingEntryDao.markAsDeleted(entry.entryId, System.currentTimeMillis())
                }

                if (isOnline()) {
                    try {
                        entries.forEach { entry ->
                            firestore.collection("groups/$groupId/monthly_saving_entries")
                                .document(entry.entryId)
                                .delete()
                                .await()
                            savingEntryDao.permanentDelete(entry.entryId)
                        }

                        firestore.collection("groups/$groupId/monthly_savings")
                            .document(it.savingId)
                            .delete()
                            .await()
                        savingDao.permanentDelete(it.savingId)
                    } catch (e: Exception) {
                        SyncLogger.e("Immediate month deletion failed: ${e.message}")
                    }
                }
            }
        }
    }

    override suspend fun getAllUserSavingsEntries(userId: String): List<MonthlySavingEntry> {
        return withContext(dispatcher) {
            savingEntryDao.getMemberSavingsEntries(userId)
        }
    }

    private fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }


    override suspend fun getMemberMonthlySavingsProgress(memberId: String): List<Pair<String, Int>> {
        return withContext(dispatcher) {
            println("DEBUG: Getting savings entries for member: $memberId")

            val entries = savingEntryDao.getMemberSavingsEntries(memberId)
            println("DEBUG: Found ${entries.size} entries for member $memberId")
            entries.forEach { entry ->
                println("DEBUG: Entry: ${entry.entryId}, Amount: ${entry.amount}, Month: ${entry.monthYear}")
            }
            val monthlySums = entries.groupBy { it.monthYear }
                .map { (monthYear, entries) ->
                    val sum = entries.sumOf { it.amount }
                    println("DEBUG: Month $monthYear total: $sum")
                    monthYear to sum
                }
                .sortedBy { (monthYear, _) ->
                    val format = SimpleDateFormat("MM/yyyy", Locale.getDefault())
                    format.parse(monthYear)?.time ?: 0
                }

            val cumulativeSavings = mutableListOf<Pair<String, Int>>()
            var total = 0

            monthlySums.forEach { (month, amount) ->
                total += amount
                println("DEBUG: Cumulative for $month: $total")
                cumulativeSavings.add(month to total)
            }

            cumulativeSavings
        }
    }
}
