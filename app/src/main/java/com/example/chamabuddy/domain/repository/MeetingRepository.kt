package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.* import kotlinx.coroutines.flow.Flow
import java.util.*

interface MeetingRepository {
    suspend fun createWeeklyMeeting(
        cycleId: String,
        meetingDate: Date,
        recordedBy: String?,
        groupId: String
    ): Result<WeeklyMeeting>

    suspend fun getMeetingWithDetails(meetingId: String): MeetingWithDetails?
    fun getMeetingsForCycle(cycleId: String): Flow<List<MeetingWithDetails>>
    suspend fun updateMeetingStatus(
        meetingId: String,
        hasContributions: Boolean,
        hasBeneficiaries: Boolean
    )
    suspend fun recordContributions(
        meetingId: String,
        contributions: Map<String, Boolean>
    ): Result<Unit>
    suspend fun selectBeneficiaries(
        meetingId: String,
        beneficiaryIds: List<String>
    ): Result<Unit>
    suspend fun getMeetingById(meetingId: String): WeeklyMeeting?
    suspend fun getLatestMeeting(cycleId: String): WeeklyMeeting?
    suspend fun getMeetingStatus(meetingId: String): MeetingStatus    suspend fun getCycleWeeklyAmount(cycleId: String): Int
    suspend fun hasContributed(meetingId: String, memberId: String): Boolean
    suspend fun getBeneficiariesForMeeting(meetingId: String): List<BeneficiaryAndAmount>
    suspend fun deleteBeneficiariesForMeeting(meetingId: String)
    suspend fun getBeneficiariesByCycle(cycleId: String): List<Beneficiary>
    suspend fun getMeetingWithCycle(meetingId: String): WeeklyMeetingWithCycle?
    suspend fun getCyclesByGroup(groupId: String): List<Cycle>
    suspend fun deleteMeeting(meetingId: String)

    suspend fun getUnsyncedMeetings(): List<WeeklyMeeting>
    suspend fun markMeetingSynced(meeting: WeeklyMeeting)
    suspend fun insertMeeting(meeting: WeeklyMeeting)


    suspend fun markAsDeleted(meetingId: String, timestamp: Long)
    suspend fun getDeletedMeetings(): List<WeeklyMeeting>
    suspend fun permanentDelete(meetingId: String)


}