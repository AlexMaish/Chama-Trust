package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.WelfareBeneficiaryDao
import com.example.chamabuddy.data.local.WelfareContributionDao
import com.example.chamabuddy.data.local.WelfareDao
import com.example.chamabuddy.data.local.WelfareMeetingDao
import com.example.chamabuddy.domain.model.MemberWelfareContribution
import com.example.chamabuddy.domain.model.Welfare
import com.example.chamabuddy.domain.model.WelfareBeneficiary
import com.example.chamabuddy.domain.model.WelfareMeeting
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.WelfareRepository
import kotlinx.coroutines.flow.Flow
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WelfareRepositoryImpl @Inject constructor(
    private val welfareDao: WelfareDao,
    private val welfareMeetingDao: WelfareMeetingDao,
    private val welfareContributionDao: WelfareContributionDao,
    private val welfareBeneficiaryDao: WelfareBeneficiaryDao,
    private val memberRepository: MemberRepository // Added to fetch member names
) : WelfareRepository {

    override suspend fun createWelfare(groupId: String, name: String, userId: String, amount: Int) {
        val welfare = Welfare(
            welfareId = UUID.randomUUID().toString(),
            groupId = groupId,
            name = name,
            amount = amount,
            createdBy = userId
        )
        welfareDao.insert(welfare)
    }

    override fun getWelfaresForGroup(groupId: String): Flow<List<Welfare>> {
        return welfareDao.getWelfaresByGroup(groupId)
    }

    override suspend fun getWelfareById(welfareId: String): Welfare? {
        return welfareDao.getWelfareById(welfareId)
    }

    override suspend fun createWelfareMeeting(
        welfareId: String,
        meetingDate: Date,
        recordedBy: String?,
        groupId: String,
        welfareAmount: Int
    ): String {
        val meetingId = UUID.randomUUID().toString()
        val meeting = WelfareMeeting(
            meetingId = meetingId,
            welfareId = welfareId,
            meetingDate = meetingDate.time,
            totalCollected = 0,
            recordedBy = recordedBy,
            groupId = groupId,
            welfareAmount = welfareAmount
        )
        welfareMeetingDao.insertMeeting(meeting)
        return meetingId
    }

    override suspend fun deleteMeeting(meetingId: String) {
        welfareContributionDao.deleteContributionsForMeeting(meetingId)
        welfareBeneficiaryDao.deleteBeneficiariesForMeeting(meetingId)
        welfareMeetingDao.deleteMeeting(meetingId)
    }

    override suspend fun recordContributions(meetingId: String, contributions: Map<String, Int>) {
        welfareContributionDao.deleteContributionsForMeeting(meetingId)

        val meeting = welfareMeetingDao.getMeetingById(meetingId)
            ?: throw IllegalStateException("Meeting not found")

        var totalCollected = 0
        contributions.forEach { (memberId, amount) ->
            if (amount > 0) {
                val contribution = MemberWelfareContribution(
                    contributionId = UUID.randomUUID().toString(),
                    meetingId = meetingId,
                    memberId = memberId,
                    amountContributed = amount,
                    contributionDate = System.currentTimeMillis(),
                    isLate = false,
                    groupId = meeting.groupId
                )
                welfareContributionDao.insertContribution(contribution)
                totalCollected += amount
            }
        }

        // Get member names for contributors
        val contributorNames = contributions
            .filter { it.value > 0 }
            .keys
            .mapNotNull { memberRepository.getMemberById(it)?.name }

        // Update meeting with contributors info
        val updatedMeeting = meeting.copy(
            totalCollected = totalCollected,
            contributorSummaries = contributorNames
        )
        welfareMeetingDao.updateMeeting(updatedMeeting)
    }

    override suspend fun selectBeneficiaries(meetingId: String, beneficiaryIds: List<String>) {
        welfareBeneficiaryDao.deleteBeneficiariesForMeeting(meetingId)

        val meeting = welfareMeetingDao.getMeetingById(meetingId)
            ?: throw IllegalStateException("Meeting not found")

        val totalCollected = meeting.totalCollected
        val beneficiaryCount = beneficiaryIds.size.coerceAtLeast(1)
        val amountPerBeneficiary = totalCollected / beneficiaryCount

        beneficiaryIds.forEach { memberId ->
            val beneficiary = WelfareBeneficiary(
                beneficiaryId = UUID.randomUUID().toString(),
                meetingId = meetingId,
                memberId = memberId,
                amountReceived = amountPerBeneficiary,
                dateAwarded = Date().time,
                groupId = meeting.groupId
            )
            welfareBeneficiaryDao.insertBeneficiary(beneficiary)
        }

        // Get member names for beneficiaries
        val beneficiaryNames = beneficiaryIds.mapNotNull {
            memberRepository.getMemberById(it)?.name
        }

        // Update meeting with beneficiary names
        val updatedMeeting = meeting.copy(
            beneficiaryNames = beneficiaryNames
        )
        welfareMeetingDao.updateMeeting(updatedMeeting)
    }

    override suspend fun getContributionsForMeeting(meetingId: String): List<MemberWelfareContribution> {
        return welfareContributionDao.getContributionsForMeeting(meetingId)
    }

    override suspend fun getBeneficiariesForMeeting(meetingId: String): List<WelfareBeneficiary> {
        return welfareBeneficiaryDao.getBeneficiariesForMeeting(meetingId)
    }

    override fun getMeetingsForWelfare(welfareId: String): Flow<List<WelfareMeeting>> {
        return welfareMeetingDao.getMeetingsForWelfare(welfareId)
    }

    override suspend fun getMeetingById(meetingId: String): WelfareMeeting? {
        return welfareMeetingDao.getMeetingById(meetingId)
    }

    override suspend fun getUnsyncedWelfares(): List<Welfare> {
        return welfareDao.getUnsyncedWelfares()
    }

    override suspend fun markWelfareAsSynced(welfareId: String) {
        welfareDao.markAsSynced(welfareId)
    }

    override suspend fun getUnsyncedMeetings(): List<WelfareMeeting> {
        return welfareMeetingDao.getUnsyncedMeetings()
    }

    override suspend fun markMeetingAsSynced(meetingId: String) {
        welfareMeetingDao.markAsSynced(meetingId)
    }
}
