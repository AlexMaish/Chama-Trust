package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.MemberContribution
import com.example.chamabuddy.domain.model.MemberWelfareContribution
import com.example.chamabuddy.domain.model.Welfare
import com.example.chamabuddy.domain.model.WelfareBeneficiary
import com.example.chamabuddy.domain.model.WelfareMeeting
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface WelfareRepository {
    suspend fun createWelfare(groupId: String, name: String, userId: String, amount: Int)    fun getWelfaresForGroup(groupId: String): Flow<List<Welfare>>
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

    // New methods
    suspend fun recordContributions(meetingId: String, contributions: Map<String, Int>)
    suspend fun selectBeneficiaries(meetingId: String, beneficiaryIds: List<String>)
    suspend fun getContributionsForMeeting(meetingId: String): List<MemberWelfareContribution>
    suspend fun getBeneficiariesForMeeting(meetingId: String): List<WelfareBeneficiary>

    suspend fun getUnsyncedWelfares(): List<Welfare>
    suspend fun markWelfareAsSynced(welfareId: String)
    suspend fun getUnsyncedMeetings(): List<WelfareMeeting>
    suspend fun markMeetingAsSynced(meetingId: String)

    suspend fun deleteMeeting(meetingId: String)
}