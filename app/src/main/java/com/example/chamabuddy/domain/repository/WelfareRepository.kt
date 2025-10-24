package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.MemberWelfareContribution
import com.example.chamabuddy.domain.model.Welfare
import com.example.chamabuddy.domain.model.WelfareBeneficiary
import com.example.chamabuddy.domain.model.WelfareMeeting
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface WelfareRepository {
    suspend fun createWelfare(groupId: String, name: String, userId: String, amount: Int)
    fun getWelfaresForGroup(groupId: String): Flow<List<Welfare>>
    suspend fun getWelfareById(welfareId: String): Welfare?
    suspend fun createWelfareMeeting(
        welfareId: String,
        meetingDate: Date,
        recordedBy: String?,
        groupId: String,
        welfareAmount: Int
    ): String
    fun getMeetingsForWelfare(welfareId: String): Flow<List<WelfareMeeting>>
    suspend fun getMeetingById(meetingId: String): WelfareMeeting?
    suspend fun recordContributions(meetingId: String, contributions: Map<String, Int>)
    suspend fun selectBeneficiaries(meetingId: String, beneficiaryIds: List<String>)
    suspend fun getContributionsForMeeting(meetingId: String): List<MemberWelfareContribution>
    suspend fun getBeneficiariesForMeeting(meetingId: String): List<WelfareBeneficiary>
    suspend fun deleteMeeting(meetingId: String)

    // Sync methods
    suspend fun getUnsyncedWelfares(): List<Welfare>
    suspend fun markWelfareAsSynced(welfareId: String)
    suspend fun getUnsyncedMeetings(): List<WelfareMeeting>
    suspend fun markMeetingAsSynced(meetingId: String)
    suspend fun getUnsyncedContributions(): List<MemberWelfareContribution>
    suspend fun markContributionAsSynced(contributionId: String)
    suspend fun getUnsyncedBeneficiaries(): List<WelfareBeneficiary>
    suspend fun markBeneficiaryAsSynced(beneficiaryId: String)
    suspend fun insertWelfare(welfare: Welfare)
    suspend fun insertMeeting(meeting: WelfareMeeting)
    suspend fun insertContribution(contribution: MemberWelfareContribution)
    suspend fun insertBeneficiary(beneficiary: WelfareBeneficiary)
    suspend fun getContributionById(contributionId: String): MemberWelfareContribution?
    suspend fun getBeneficiaryById(beneficiaryId: String): WelfareBeneficiary?


    suspend fun markAsDeletedBeneficiary(beneficiaryId: String, timestamp: Long)
    suspend fun getDeletedBeneficiaries(): List<WelfareBeneficiary>
    suspend fun permanentDeleteBeneficiary(beneficiaryId: String)


    suspend fun markContributionAsDeleted(contributionId: String, timestamp: Long)
    suspend fun getDeletedContributions(): List<MemberWelfareContribution>
    suspend fun permanentDeleteContribution(contributionId: String)


    suspend fun markWelfareAsDeleted(welfareId: String, timestamp: Long)
    suspend fun getDeletedWelfares(): List<Welfare>
    suspend fun permanentDeleteWelfare(welfareId: String)


    suspend fun markWelfareMeetingAsDeleted(meetingId: String, timestamp: Long)
    suspend fun getDeletedMeetings(): List<WelfareMeeting>
    suspend fun permanentDeleteWelfareMeetings(meetingId: String)



}

