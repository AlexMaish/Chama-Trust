package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.MeetingWithDetails
import com.example.chamabuddy.domain.model.WeeklyMeeting
import kotlinx.coroutines.flow.Flow
import java.util.Date


interface MeetingRepository {
    suspend fun createWeeklyMeeting(
        cycleId: String,
        meetingDate: Date,
        recordedBy: String?
    ): Result<WeeklyMeeting>

    suspend fun recordContributions(
        meetingId: String,
        contributions: Map<String, Boolean>
    ): Result<Unit>

    suspend fun selectBeneficiaries(
        meetingId: String,
        firstBeneficiaryId: String,
        secondBeneficiaryId: String
    ): Result<Unit>

    suspend fun updateMeetingStatus(
        meetingId: String,
        hasContributions: Boolean,
        hasBeneficiaries: Boolean
    )

    suspend fun hasContributed(meetingId: String, memberId: String): Boolean
    suspend fun getBeneficiariesForMeeting(meetingId: String): List<BeneficiaryAndAmount>
    fun getMeetingsForCycle(cycleId: String): Flow<List<MeetingWithDetails>>
    suspend fun getMeetingById(meetingId: String): WeeklyMeeting?
    suspend fun getLatestMeeting(cycleId: String): WeeklyMeeting?
    suspend fun getMeetingStatus(meetingId: String): MeetingStatus
    suspend fun getCycleWeeklyAmount(cycleId: String): Int
    suspend fun getMeetingWithDetails(meetingId: String): MeetingWithDetails?
}

data class MeetingStatus(
    val totalExpected: Int,
    val totalContributed: Int,
    val beneficiariesSelected: Boolean,
    val fullyRecorded: Boolean
)

data class BeneficiaryAndAmount(
    val memberId: String,
    val amountReceived: Int
)























//interface MeetingRepository {
//    // Meeting operations
//    suspend fun createWeeklyMeeting(
//        cycleId: String,
//        meetingDate: Date,
//        recordedBy: String
//    ): Result<WeeklyMeeting>
//
//    suspend fun recordContributions(
//        meetingId: String,
//        contributions: Map<String, Boolean> // memberId to hasContributed
//    ): Result<Unit>
//
//    suspend fun selectBeneficiaries(
//        meetingId: String,
//        firstBeneficiaryId: String,
//        secondBeneficiaryId: String
//    ): Result<Unit>
//
//    suspend fun hasContributed(meetingId: String, memberId: String): Boolean
//
//    suspend fun getBeneficiariesForMeeting(meetingId: String): List<BeneficiaryAndAmount>
//
//    // Data access
//    fun getMeetingsForCycle(cycleId: String): Flow<List<WeeklyMeeting>>
//    suspend fun getMeetingById(meetingId: String): WeeklyMeeting?
//    suspend fun getLatestMeeting(cycleId: String): WeeklyMeeting?
//
//    // Status checks
//    suspend fun getMeetingStatus(meetingId: String): MeetingStatus
//}
//
//data class MeetingStatus(
//    val totalExpected: Int,
//    val totalContributed: Int,
//    val beneficiariesSelected: Boolean,
//    val fullyRecorded: Boolean
//)
//
//data class BeneficiaryAndAmount(
//    val memberId: String,
//    val amountReceived: Int
//)
