package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.BeneficiaryDao
import com.example.chamabuddy.data.local.CycleDao
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.WeeklyMeetingDao
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.model.CycleWithBeneficiaries
import com.example.chamabuddy.domain.model.BeneficiaryWithMember

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
            // 1. End current active cycle (if exists)
            cycleDao.getActiveCycleByGroupId(groupId)?.let { activeCycle ->
                endCurrentCycle(activeCycle.cycleId, System.currentTimeMillis())
            }

            // 2. Create new active cycle (endDate = null)
            val newCycle = Cycle(
                cycleId = UUID.randomUUID().toString(),
                isActive = true,
                startDate = startDate,
                endDate = null, // New cycles have no end date
                weeklyAmount = weeklyAmount,
                monthlySavingsAmount = monthlyAmount,
                totalMembers = totalMembers,
                groupId = groupId,
                totalSavings = 0,
                beneficiariesPerMeeting = beneficiariesPerMeeting,
                cycleNumber = calculateNextCycleNumber(groupId)
            )

            cycleDao.insertCycle(newCycle)
            Result.success(newCycle)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }



//    override suspend fun endCurrentCycle(cycleId: String, endDate: Long): Result<Unit> = withContext(dispatcher) {
//        try {
//            val activeCycle = cycleDao.getActiveCycle()
//                ?: return@withContext Result.failure(IllegalStateException("No active cycle found"))
//
//            if (activeCycle.cycleId != cycleId) {
//                return@withContext Result.failure(IllegalArgumentException("Mismatched cycle ID"))
//            }
//
//            cycleDao.updateCycle(
//                activeCycle.copy(
//                    endDate = endDate,
//                    isActive = false
//                )
//            )
//            Result.success(Unit)
//        } catch (e: Exception) {
//            Result.failure(e)
//        }
//    }
override suspend fun endCurrentCycle(cycleId: String, endDate: Long): Result<Unit> {
    val cycle = getCycleById(cycleId) ?: return Result.failure(IllegalStateException("Cycle not found"))
    val updatedCycle = cycle.copy(
        isActive = false,
        endDate = endDate // Set end date when completing
    )
    cycleDao.updateCycle(updatedCycle)
    return Result.success(Unit)
}

    private suspend fun calculateNextCycleNumber(groupId: String): Int {
        val lastCycle = cycleDao.getLastCycleByGroupId(groupId)
        return (lastCycle?.cycleNumber ?: 0) + 1
    }

    override suspend fun getCyclesByGroup(groupId: String): List<Cycle> {
        return cycleDao.getCyclesByGroup(groupId)
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



    override suspend fun getCyclesWithBeneficiaries(groupId: String): List<CycleWithBeneficiaries> {
        return withContext(dispatcher) {
            val cycles = cycleDao.getCyclesByGroupId(groupId)
            cycles.map { cycle ->
                val beneficiaries = beneficiaryDao.getBeneficiariesByCycle(cycle.cycleId)
                val beneficiariesWithMembers = beneficiaries.mapNotNull { beneficiary ->
                    val member = memberDao.getMemberById(beneficiary.memberId)
                    if (member != null) {
                        BeneficiaryWithMember(beneficiary, member)
                    } else {
                        null // Skip if member not found
                    }
                }
                CycleWithBeneficiaries(cycle, beneficiariesWithMembers)
            }
        }
    }


    override suspend fun deleteCycle(cycleId: String) {
        cycleDao.deleteCycleById(cycleId)
    }


}
