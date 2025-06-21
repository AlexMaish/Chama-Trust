package com.example.chamabuddy.domain.repository
import com.example.chamabuddy.domain.model.CycleSummary
import com.example.chamabuddy.domain.model.FinancialSummary
import java.util.Date


interface ChamaRepository :
    MemberRepository,
    CycleRepository,
    MeetingRepository,
    SavingsRepository {

    // Cross-repository operations
    suspend fun getMemberParticipation(memberId: String): MemberParticipation
    suspend fun getCurrentCycleSummary(): CycleSummary?
    suspend fun getMemberFinancialSummary(memberId: String): FinancialSummary
}














data class MemberParticipation(
    val totalContributions: Int,
    val timesBenefited: Int,
    val totalSavings: Int,
    val activeSince: Date
)



















//
//suspend fun getMemberById(memberId: String) = MemberRepository.getMemberById(memberId)
//override fun getAllMembers() = MemberRepository.getAllMembers()
//override fun getActiveMembers() = MemberRepository.getActiveMembers()
//override fun getAllAdmins() = memberRepository.getAllAdmins()
//
//override suspend fun getCurrentCycle() = cycleRepository.getCurrentCycle()
//override suspend fun getCycleStats(cycleId: String) = cycleRepository.getCycleStats(cycleId)
//
//override fun getMeetingsForCycle(cycleId: String) = meetingRepository.getMeetingsForCycle(cycleId)
//override suspend fun getMeetingStatus(meetingId: String) = meetingRepository.getMeetingStatus(meetingId)
//override suspend fun hasContributed(meetingId: String, memberId: String) = meetingRepository.hasContributed(meetingId, memberId)
//override fun getBeneficiariesForMeeting(meetingId: String) = meetingRepository.getBeneficiariesForMeeting(meetingId)
//
//override suspend fun getMemberSavingsTotal(cycleId: String, memberId: String) = savingsRepository.getMemberSavingsTotal(cycleId, memberId)
//}


