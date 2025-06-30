package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.BeneficiaryDao
import com.example.chamabuddy.data.local.CycleDao
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.WeeklyMeetingDao
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.CycleStats
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.util.*
import javax.inject.Inject

class CycleRepositoryImpl @Inject constructor(
    private val cycleDao: CycleDao,
    private val meetingDao: WeeklyMeetingDao,
    private val beneficiaryDao: BeneficiaryDao,
    private val memberDao: MemberDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CycleRepository {

    override suspend fun startNewCycle(
        weeklyAmount: Int,
        monthlySavingsAmount: Int,
        totalMembers: Int,
        startDate: Long,
        groupId: String
    ): Result<Cycle> = withContext(dispatcher) {
        try {
            cycleDao.getActiveCycle()?.let { cycleDao.endCycle(it.cycleId) }

            val newCycle = Cycle(
                isActive = true,
                cycleId = UUID.randomUUID().toString(),
                startDate = startDate,
                weeklyAmount = weeklyAmount,
                monthlySavingsAmount = monthlySavingsAmount,
                totalMembers = totalMembers,
                totalSavings = 0,
                groupId = groupId
            )

            cycleDao.insertCycle(newCycle)
            Result.success(newCycle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getActiveCycleForGroup(groupId: String): Cycle? =
        withContext(dispatcher) {
            // Add a new method in CycleDao to get active cycle by group
            cycleDao.getActiveCycleForGroup(groupId)
        }

    override suspend fun endCurrentCycle(): Result<Unit> = withContext(dispatcher) {
        try {
            val activeCycle = cycleDao.getActiveCycle()
                ?: return@withContext Result.failure(IllegalStateException("No active cycle found"))

            cycleDao.updateCycle(
                activeCycle.copy(
                    endDate = System.currentTimeMillis(),
                    isActive = false
                )
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getCurrentCycle(): Flow<Cycle?> = cycleDao.observeActiveCycle()
    override fun getAllCycles(): Flow<List<Cycle>> = cycleDao.getAllCycles()

    override suspend fun getCycleById(cycleId: String): Cycle? =
        withContext(dispatcher) { cycleDao.getCycleById(cycleId) }

    override suspend fun getActiveCycle(): Cycle? = withContext(dispatcher) {
        cycleDao.getActiveCycle()
    }

    override suspend fun getCycleStats(cycleId: String): CycleStats = withContext(dispatcher) {
        val cycle = cycleDao.getCycleById(cycleId)
            ?: throw NoSuchElementException("Cycle not found")

        val meetings = meetingDao.getMeetingsForCycle(cycleId).first()
        val members = memberDao.getActiveMembers().first()

        val totalCollected = meetings.sumOf { it.totalCollected }
        val totalDistributed = meetings.sumOf { meeting ->
            val beneficiaries = beneficiaryDao.getBeneficiariesForMeeting(meeting.meetingId)
            beneficiaries.size * cycle.weeklyAmount
        }

        val remainingMembers = members.count { member ->
            beneficiaryDao.hasReceivedInCycle(member.memberId, cycleId) == 0
        }

        CycleStats(
            totalCollected = totalCollected,
            totalDistributed = totalDistributed,
            remainingMembers = remainingMembers,
            completedWeeks = meetings.size
        )
    }

    override suspend fun getTotalSavings(): Int? = withContext(dispatcher) {
        cycleDao.getTotalSavings()
    }

    override suspend fun getCycleHistory(): List<Cycle> = withContext(dispatcher) {
        cycleDao.getCycleHistory()
    }
}