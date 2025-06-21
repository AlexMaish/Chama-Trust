//package com.example.chamabuddy.data.repository
//
//import com.example.chamabuddy.domain.model.CycleSummary
//import com.example.chamabuddy.domain.model.FinancialSummary
//import com.example.chamabuddy.domain.repository.ChamaRepository
//import com.example.chamabuddy.domain.repository.CycleRepository
//import com.example.chamabuddy.domain.repository.MeetingRepository
//import com.example.chamabuddy.domain.repository.MemberRepository
//import com.example.chamabuddy.domain.repository.SavingsRepository
//import com.example.chamabuddy.domain.repository.MemberParticipation
//import kotlinx.coroutines.CoroutineDispatcher
//import kotlinx.coroutines.Dispatchers
//import kotlinx.coroutines.flow.first
//import kotlinx.coroutines.withContext
//import java.util.Date
//
///**
// * Concrete implementation of [ChamaRepository] that delegates
// * core repository methods and implements cross-repository operations.
// */
//class ChamaRepositoryImpl(
//    private val memberRepository: MemberRepository,
//    private val cycleRepository: CycleRepository,
//    private val meetingRepository: MeetingRepository,
//    private val savingsRepository: SavingsRepository,
//    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
//) : ChamaRepository,
//    MemberRepository by memberRepository,
//    CycleRepository by cycleRepository,
//    MeetingRepository by meetingRepository,
//    SavingsRepository by savingsRepository {
//
//    override suspend fun getMemberParticipation(memberId: String): MemberParticipation = withContext(dispatcher) {
//        val member = memberRepository.getMemberById(memberId)
//            ?: throw NoSuchElementException("Member not found")
//
//        val currentCycle = cycleRepository.getCurrentCycle().first()
//
//        val totalContributions = currentCycle?.let {
//            val meetings = meetingRepository.getMeetingsForCycle(it.cycleId).first()
//            meetings.count { meeting ->
//                meetingRepository.getMeetingStatus(meeting.meetingId).fullyRecorded &&
//                        meetingRepository.hasContributed(meeting.meetingId, memberId)
//            }
//        } ?: 0
//
//        val timesBenefited = currentCycle?.let {
//            val meetings = meetingRepository.getMeetingsForCycle(it.cycleId).first()
//            meetings.sumOf { meeting ->
//                meetingRepository.getBeneficiariesForMeeting(meeting.meetingId)
//                    .count { b -> b.memberId == memberId }
//            }
//        } ?: 0
//
//        val totalSavings = currentCycle?.let {
//            savingsRepository.getMemberSavingsTotal(it.cycleId, memberId)
//        } ?: 0
//
//        MemberParticipation(
//            totalContributions = totalContributions,
//            timesBenefited = timesBenefited,
//            totalSavings = totalSavings,
//            activeSince = Date(member.joinDate)
//        )
//    }
//
//    override suspend fun getCurrentCycleSummary(): CycleSummary? = withContext(dispatcher) {
//        val currentCycle = cycleRepository.getCurrentCycle().first() ?: return@withContext null
//        val stats = cycleRepository.getCycleStats(currentCycle.cycleId)
//
//        CycleSummary(
//            cycle = currentCycle,
//            meetingsHeld = stats.completedWeeks,
//            totalCollected = stats.totalCollected,
//            totalDistributed = stats.totalDistributed,
//            remainingMembers = stats.remainingMembers
//        )
//    }
//
//    override suspend fun getMemberFinancialSummary(memberId: String): FinancialSummary = withContext(dispatcher) {
//        val currentCycle = cycleRepository.getCurrentCycle().first()
//            ?: throw IllegalStateException("No active cycle")
//
//        val meetings = meetingRepository.getMeetingsForCycle(currentCycle.cycleId).first()
//
//        val totalContributions = meetings.sumOf { meeting ->
//            if (meetingRepository.hasContributed(meeting.meetingId, memberId)) {
//                currentCycle.weeklyAmount
//            } else {
//                0
//            }
//        }
//
//        val totalReceived = meetings.sumOf { meeting ->
//            meetingRepository.getBeneficiariesForMeeting(meeting.meetingId)
//                .count { it.memberId == memberId } * currentCycle.weeklyAmount
//        }
//
//        val totalSavings = savingsRepository.getMemberSavingsTotal(currentCycle.cycleId, memberId)
//
//        FinancialSummary(
//            totalContributions = totalContributions,
//            totalReceived = totalReceived,
//            totalSavings = totalSavings,
//            netBalance = totalReceived - totalContributions
//        )
//    }
//}
