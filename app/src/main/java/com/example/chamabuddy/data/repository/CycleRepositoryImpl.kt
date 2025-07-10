package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.BeneficiaryDao
import com.example.chamabuddy.data.local.CycleDao
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.WeeklyMeetingDao
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.CycleStats
import com.example.chamabuddy.domain.repository.GroupRepository
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
    private val groupRepository: GroupRepository,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : CycleRepository {

    override suspend fun startNewCycle(
        weeklyAmount: Int,
        monthlyAmount: Int,
        totalMembers: Int,
        startDate: Long,
        groupId: String,
        beneficiariesPerMeeting: Int
    ): Result<Cycle> = withContext(dispatcher) {
        try {
            // Get actual member count
            val groupWithMembers = groupRepository.getGroupWithMembers(groupId)
            val actualMemberCount = groupWithMembers?.members?.size ?: 0

            // End any active cycle in this group
            cycleDao.getActiveCycleByGroupId(groupId)?.let {
                cycleDao.endCycle(it.cycleId)
            }

            val newCycle = Cycle(
                isActive = true,
                startDate = startDate,
                weeklyAmount = weeklyAmount,
                monthlySavingsAmount = monthlyAmount,
                totalMembers = actualMemberCount,
                groupId = groupId,
                cycleId = UUID.randomUUID().toString(),
                totalSavings = 0,
                beneficiariesPerMeeting = beneficiariesPerMeeting
            )

            cycleDao.insertCycle(newCycle)
            println("Created cycle: $newCycle")

            Result.success(newCycle)
        } catch (e: Exception) {
            Result.failure(e)
        }
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

    override suspend fun getCyclesForGroup(groupId: String): List<Cycle> {
        return cycleDao.getCyclesByGroupId(groupId)
    }

    override suspend fun getActiveCycleForGroup(groupId: String): Cycle? {
        return cycleDao.getActiveCycleByGroupId(groupId)
    }

    override suspend fun getCyclesByGroupId(groupId: String): List<Cycle> {
        return withContext(Dispatchers.IO) {
            cycleDao.getCyclesByGroupId(groupId)
        }
    }
}
