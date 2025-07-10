package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.*
import com.example.chamabuddy.domain.model.*
import com.example.chamabuddy.domain.repository.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.*
import java.util.UUID
import javax.inject.Inject

class MeetingRepositoryImpl @Inject constructor(
    private val meetingDao: WeeklyMeetingDao,
    private val contributionDao: MemberContributionDao,
    private val beneficiaryDao: BeneficiaryDao,
    private val memberDao: MemberDao,
    private val cycleDao: CycleDao,
    private val weeklyMeetingDao: WeeklyMeetingDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : MeetingRepository {

    override suspend fun createWeeklyMeeting(
        cycleId: String,
        meetingDate: Date,
        recordedBy: String?,
        groupId: String
    ) = withContext(dispatcher) {
        try {
            val meeting = WeeklyMeeting(
                meetingId = UUID.randomUUID().toString(),
                cycleId = cycleId,
                meetingDate = meetingDate,
                totalCollected = 0,
                recordedBy = recordedBy,
                groupId = groupId
            )
            meetingDao.insertMeeting(meeting)
            Result.success(meeting)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMeetingWithCycle(meetingId: String): WeeklyMeetingWithCycle? {
        return weeklyMeetingDao.getMeetingWithCycle(meetingId)
    }

    override suspend fun getMeetingWithDetails(meetingId: String): MeetingWithDetails? =
        withContext(dispatcher) {
            val meeting = meetingDao.getMeetingById(meetingId) ?: return@withContext null
            val beneficiaries = beneficiaryDao.getBeneficiariesForMeeting(meetingId)
                .map { beneficiary ->
                    val member = memberDao.getMemberById(beneficiary.memberId)
                    BeneficiaryMember(
                        memberId = beneficiary.memberId,
                        memberName = member?.name ?: "Unknown",
                        amountReceived = beneficiary.amountReceived
                    )
                }
            val contributors = contributionDao.getContributorsForMeeting(meetingId)
                .map { contribution ->
                    val member = memberDao.getMemberById(contribution.memberId)
                    ContributorMember(
                        memberId = contribution.memberId,
                        memberName = member?.name ?: "Unknown",
                        amountContributed = contribution.amountContributed
                    )
                }

            MeetingWithDetails(
                meeting = meeting,
                beneficiaries = beneficiaries,
                contributors = contributors
            )
        }

    override fun getMeetingsForCycle(cycleId: String): Flow<List<MeetingWithDetails>> =
        meetingDao.getMeetingsForCycle(cycleId).map { meetings ->
            meetings.map { meeting ->
                val beneficiaries = beneficiaryDao.getBeneficiariesForMeeting(meeting.meetingId)
                    .map { beneficiary ->
                        val member = memberDao.getMemberById(beneficiary.memberId)
                        BeneficiaryMember(
                            memberId = beneficiary.memberId,
                            memberName = member?.name ?: "Unknown",
                            amountReceived = beneficiary.amountReceived
                        )
                    }
                val contributors = contributionDao.getContributorsForMeeting(meeting.meetingId)
                    .map { contribution ->
                        val member = memberDao.getMemberById(contribution.memberId)
                        ContributorMember(
                            memberId = contribution.memberId,
                            memberName = member?.name ?: "Unknown",
                            amountContributed = contribution.amountContributed
                        )
                    }

                MeetingWithDetails(
                    meeting = meeting,
                    beneficiaries = beneficiaries,
                    contributors = contributors
                )
            }
        }

    override suspend fun updateMeetingStatus(
        meetingId: String,
        hasContributions: Boolean,
        hasBeneficiaries: Boolean
    ) {
        // Implementation can be added later
    }

    override suspend fun recordContributions(
        meetingId: String,
        contributions: Map<String, Boolean>
    ): Result<Unit> = withContext(dispatcher) {
        try {
            val meeting = meetingDao.getMeetingById(meetingId)
                ?: throw IllegalStateException("Meeting not found")
            val cycle = cycleDao.getCycleById(meeting.cycleId)
                ?: throw IllegalStateException("Cycle not found")

            contributionDao.deleteContributionsForMeeting(meetingId)
            var totalCollected = 0
            contributions.forEach { (memberId, hasContributed) ->
                if (hasContributed) {
                    contributionDao.insertContribution(
                        MemberContribution(
                            contributionId = UUID.randomUUID().toString(),
                            meetingId = meetingId,
                            memberId = memberId,
                            amountContributed = cycle.weeklyAmount,
                            contributionDate = Date().toString(),
                            isLate = false,
                            groupId = meeting.groupId
                        )
                    )
                    totalCollected += cycle.weeklyAmount
                }
            }
            meetingDao.updateMeeting(meeting.copy(totalCollected = totalCollected))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun selectBeneficiaries(
        meetingId: String,
        beneficiaryIds: List<String>
    ): Result<Unit> = withContext(dispatcher) {
        try {
            val meeting = meetingDao.getMeetingById(meetingId)
                ?: throw IllegalStateException("Meeting not found")
            val cycle = cycleDao.getCycleById(meeting.cycleId)
                ?: throw IllegalStateException("Cycle not found")

            // Enforce max beneficiaries
            if (beneficiaryIds.size > cycle.beneficiariesPerMeeting) {
                throw IllegalArgumentException(
                    "Cannot select more than ${cycle.beneficiariesPerMeeting} beneficiaries"
                )
            }

            // Delete existing beneficiaries
            beneficiaryDao.deleteBeneficiariesForMeeting(meetingId)

            // Insert new beneficiaries
            beneficiaryIds.forEachIndexed { index, beneficiaryId ->
                beneficiaryDao.insertBeneficiary(
                    Beneficiary(
                        beneficiaryId = UUID.randomUUID().toString(),
                        meetingId = meetingId,
                        memberId = beneficiaryId,
                        amountReceived = cycle.weeklyAmount,
                        paymentOrder = index + 1,
                        cycleId = cycle.cycleId,
                        dateAwarded = Date(),
                        groupId = meeting.groupId
                    )
                )
            }

            // Update meeting status (if needed)
            // This function might be empty as we're computing status dynamically
            updateMeetingStatus(
                meetingId,
                hasContributions = true, // Assuming contributions are recorded first
                hasBeneficiaries = beneficiaryIds.isNotEmpty()
            )

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getMeetingById(meetingId: String): WeeklyMeeting? {
        return meetingDao.getMeetingById(meetingId)
    }

    override suspend fun getLatestMeeting(cycleId: String): WeeklyMeeting? =
        withContext(dispatcher) {
            meetingDao.getLatestMeetingForCycle(cycleId)
        }


    override suspend fun getMeetingStatus(meetingId: String): MeetingStatus {
        val meeting = meetingDao.getMeetingById(meetingId)
            ?: throw IllegalStateException("Meeting not found")
        val cycle = cycleDao.getCycleById(meeting.cycleId)
            ?: throw IllegalStateException("Cycle not found")

        // Get active group members
        val activeMembers = memberDao.getActiveMembersByGroup(meeting.groupId)

        val beneficiaries = beneficiaryDao.getBeneficiariesForMeeting(meetingId)
        val contributors = contributionDao.getContributorsForMeeting(meetingId)

        return MeetingStatus(
            totalExpected = cycle.weeklyAmount * activeMembers.size,
            totalContributed = meeting.totalCollected,
            beneficiariesSelected = beneficiaries.isNotEmpty(),
            beneficiaryCount = beneficiaries.size,
            requiredBeneficiaryCount = cycle.beneficiariesPerMeeting,
            fullyRecorded = contributors.size == activeMembers.size
        )
    }

    override suspend fun getCycleWeeklyAmount(cycleId: String): Int {
        return cycleDao.getCycleById(cycleId)?.weeklyAmount ?: 0
    }

    override suspend fun hasContributed(meetingId: String, memberId: String): Boolean =
        withContext(dispatcher) {
            contributionDao.hasContributed(meetingId, memberId) > 0
        }

    override suspend fun getBeneficiariesForMeeting(meetingId: String): List<BeneficiaryAndAmount> =
        withContext(dispatcher) {
            beneficiaryDao.getBeneficiariesForMeeting(meetingId)
                .map { beneficiary ->
                    BeneficiaryAndAmount(
                        memberId = beneficiary.memberId,
                        amountReceived = beneficiary.amountReceived
                    )
                }
        }

    override suspend fun deleteBeneficiariesForMeeting(meetingId: String) {
        beneficiaryDao.deleteBeneficiariesForMeeting(meetingId)
    }

    override suspend fun getBeneficiariesByCycle(cycleId: String): List<Beneficiary> {
        return beneficiaryDao.getBeneficiariesByCycle(cycleId)
    }
}