// File: com/example/chamabuddy/presentation/viewmodel/MeetingViewModel.kt
package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.*
import com.example.chamabuddy.domain.repository.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class MeetingViewModel @Inject constructor(
    private val meetingRepository: MeetingRepository,
    private val memberRepository: MemberRepository,
    private val contributionRepository: MemberContributionRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : ViewModel() {


    private val _state = MutableStateFlow<MeetingState>(MeetingState.Idle)
    val state: StateFlow<MeetingState> = _state.asStateFlow()

    private val _contributionState = MutableStateFlow<ContributionTrackingState>(
        ContributionTrackingState()
    )
    val contributionState: StateFlow<ContributionTrackingState> = _contributionState.asStateFlow()

    private val _beneficiaryState = MutableStateFlow(BeneficiarySelectionState())
    val beneficiaryState: StateFlow<BeneficiarySelectionState> = _beneficiaryState.asStateFlow()

    private val _eligibleBeneficiaries = MutableStateFlow<List<Member>>(emptyList())
    val eligibleBeneficiaries: StateFlow<List<Member>> = _eligibleBeneficiaries.asStateFlow()

    fun handleEvent(event: MeetingEvent) {
        when (event) {
            is MeetingEvent.CreateMeeting -> createMeeting(event)
            is MeetingEvent.RecordContributions -> recordContributions(event)
            is MeetingEvent.SelectBeneficiaries -> selectBeneficiaries(event)
            is MeetingEvent.GetMeetingStatus -> getMeetingStatus(event)
            is MeetingEvent.GetMeetingsForCycle -> getMeetingsForCycle(event)
            is MeetingEvent.LoadEligibleBeneficiaries -> loadEligibleBeneficiaries(event.meetingId)
            is MeetingEvent.GetContributionsForMeeting -> loadMembersForContribution(event.meetingId)
            is MeetingEvent.ConfirmBeneficiarySelection -> confirmBeneficiarySelection(
                event.meetingId,
                event.firstBeneficiaryId,
                event.secondBeneficiaryId
            )
            is MeetingEvent.LoadBeneficiaryDetails -> loadBeneficiaryDetails(event.beneficiaryId)
            MeetingEvent.ResetMeetingState -> resetState()
        }
    }

    private fun loadMembersForContribution(meetingId: String) {
        viewModelScope.launch {
            // Reset state to loading
            _contributionState.value = ContributionTrackingState(
                isLoading = true,
                members = emptyList(),
                contributions = emptyMap()
            )

            try {
                // Get ALL members (not just active)
                val allMembers = memberRepository.getAllMembers().first()

                // Get existing contributions
                val existingContributions = contributionRepository.getContributionsForMeeting(meetingId)
                    .associate { it.memberId to true }

                // Get meeting details
                val meeting = meetingRepository.getMeetingById(meetingId)
                val weeklyAmount = meeting?.let {
                    meetingRepository.getCycleWeeklyAmount(it.cycleId)
                } ?: 0

                // Create contribution map (default to false for all members)
                val contributions = allMembers.associate { member ->
                    member.memberId to (existingContributions[member.memberId] ?: false)
                }

                // Calculate total collected
                val totalCollected = contributions.count { it.value } * weeklyAmount

                _contributionState.value = ContributionTrackingState(
                    isLoading = false,
                    meeting = meeting,
                    members = allMembers,
                    contributions = contributions,
                    weeklyAmount = weeklyAmount,
                    totalCollected = totalCollected
                )
            } catch (e: Exception) {
                _contributionState.value = ContributionTrackingState(
                    isLoading = false,
                    error = e.message ?: "Failed to load members"
                )
            }
        }
    }

    suspend fun updateContributionStatus(memberId: String, hasContributed: Boolean) {
        val currentContributions = _contributionState.value.contributions.toMutableMap()
        currentContributions[memberId] = hasContributed

        val weeklyAmount = _contributionState.value.meeting?.let {
            meetingRepository.getCycleWeeklyAmount(it.cycleId)
        } ?: 0

        val total = currentContributions.count { it.value } * weeklyAmount

        _contributionState.value = _contributionState.value.copy(
            contributions = currentContributions,
            totalCollected = total
        )
    }

    private fun createMeeting(event: MeetingEvent.CreateMeeting) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val result = meetingRepository.createWeeklyMeeting(
                    cycleId = event.cycleId,
                    meetingDate = event.date,
                    recordedBy = event.recordedBy
                )
                if (result.isSuccess) {
                    _state.value = MeetingState.MeetingCreated(result.getOrNull()!!)
                } else {
                    _state.value = MeetingState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to create meeting"
                    )
                }
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to create meeting")
            }
        }
    }

    private fun recordContributions(event: MeetingEvent.RecordContributions) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val result = meetingRepository.recordContributions(
                    event.meetingId,
                    event.contributions
                )
                if (result.isSuccess) {
                    _state.value = MeetingState.ContributionRecorded(true)
                } else {
                    _state.value = MeetingState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to record contributions"
                    )
                }
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to record contributions")
            }
        }
    }

    private fun selectBeneficiaries(event: MeetingEvent.SelectBeneficiaries) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val result = meetingRepository.selectBeneficiaries(
                    event.meetingId,
                    event.firstBeneficiaryId,
                    event.secondBeneficiaryId
                )
                if (result.isSuccess) {
                    _state.value = MeetingState.BeneficiariesSelected(
                        success = true,
                        beneficiaries = listOf(event.firstBeneficiaryId, event.secondBeneficiaryId),
                        meetingStatus = meetingRepository.getMeetingStatus(event.meetingId)
                    )
                } else {
                    _state.value = MeetingState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to select beneficiaries"
                    )
                }
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to select beneficiaries")
            }
        }
    }

    private fun loadEligibleBeneficiaries(meetingId: String) {

        viewModelScope.launch {
            _beneficiaryState.value = BeneficiarySelectionState(isLoading = true)
            try {
                val meeting = meetingRepository.getMeetingById(meetingId)
                    ?: throw IllegalStateException("Meeting not found")

                val cycleId = meeting.cycleId
                val allActiveMembers = memberRepository.getActiveMembers().first()

                // Get ALL beneficiaries in this cycle
                val cycleBeneficiaries = beneficiaryRepository.getBeneficiariesByCycle(cycleId)
                    .map { it.memberId }
                    .toSet()

                // Filter: Only members who haven't received in THIS CYCLE
                val eligibleMembers = allActiveMembers.filter { member ->
                    !cycleBeneficiaries.contains(member.memberId)
                }

                _beneficiaryState.value = BeneficiarySelectionState(
                    isLoading = false,
                    eligibleMembers = eligibleMembers
                )
            } catch (e: Exception) {
                _beneficiaryState.value = BeneficiarySelectionState(
                    isLoading = false,
                    error = "Error: ${e.message ?: "Unknown error"}"
                )
            }
        }
    }

    private fun confirmBeneficiarySelection(
        meetingId: String,
        firstBeneficiaryId: String,
        secondBeneficiaryId: String
    ) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val meeting = meetingRepository.getMeetingById(meetingId)
                    ?: throw IllegalStateException("Meeting not found")
                val actualCycleId = meeting.cycleId
                val weeklyAmount = meetingRepository.getCycleWeeklyAmount(actualCycleId)

                // DELETE EXISTING BENEFICIARIES FIRST
                deleteExistingBeneficiaries(meetingId)

                // Create new beneficiaries
                val firstBeneficiary = Beneficiary(
                    beneficiaryId = UUID.randomUUID().toString(),
                    memberId = firstBeneficiaryId,
                    cycleId = actualCycleId,
                    meetingId = meetingId,
                    amountReceived = weeklyAmount,
                    dateAwarded = Date(),
                    paymentOrder = 1
                )

                val secondBeneficiary = Beneficiary(
                    beneficiaryId = UUID.randomUUID().toString(),
                    memberId = secondBeneficiaryId,
                    cycleId = actualCycleId,
                    meetingId = meetingId,
                    amountReceived = weeklyAmount,
                    dateAwarded = Date(),
                    paymentOrder = 2
                )

                beneficiaryRepository.insertBeneficiary(firstBeneficiary)
                beneficiaryRepository.insertBeneficiary(secondBeneficiary)

                // Update meeting status
                meetingRepository.updateMeetingStatus(
                    meetingId,
                    hasContributions = true,
                    hasBeneficiaries = true
                )

                _state.value = MeetingState.BeneficiariesSelected(
                    success = true,
                    beneficiaries = listOf(firstBeneficiaryId, secondBeneficiaryId),
                    meetingStatus = meetingRepository.getMeetingStatus(meetingId)
                )
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to select beneficiaries")
            }
        }
    }
    private fun loadBeneficiaryDetails(beneficiaryId: String) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val beneficiary = beneficiaryRepository
                    .getBeneficiaryById(beneficiaryId)
                    ?: throw IllegalStateException("Beneficiary not found")

                val member = memberRepository
                    .getMemberById(beneficiary.memberId)
                    ?: throw IllegalStateException("Member not found")

                val meeting = meetingRepository
                    .getMeetingById(beneficiary.meetingId)

                _state.value = MeetingState.BeneficiaryDetails(
                    beneficiary = beneficiary,
                    member = member,
                    meeting = meeting
                )
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to load beneficiary details")
            }
        }
    }
    suspend fun getMeetingStatus(meetingId: String): MeetingStatus {
        return meetingRepository.getMeetingStatus(meetingId)
    }
    private fun getMeetingStatus(event: MeetingEvent.GetMeetingStatus) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val status = meetingRepository.getMeetingStatus(event.meetingId)
                _state.value = MeetingState.MeetingStatus(status)
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to get meeting status")
            }
        }
    }

    private fun getMeetingsForCycle(event: MeetingEvent.GetMeetingsForCycle) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val meetings = meetingRepository.getMeetingsForCycle(event.cycleId).first()
                _state.value = MeetingState.MeetingsLoaded(meetings)
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to load meetings")
            }
        }
    }

    private suspend fun deleteExistingBeneficiaries(meetingId: String) {
        beneficiaryRepository.deleteBeneficiariesForMeeting(meetingId)
    }

    private fun resetState() {
        _state.value = MeetingState.Idle
    }
}

sealed class MeetingState {
    object Idle : MeetingState()
    object Loading : MeetingState()
    data class MeetingsLoaded(val meetings: List<MeetingWithDetails>) : MeetingState()
    data class MeetingCreated(val meeting: WeeklyMeeting) : MeetingState()
    data class MeetingStatus(val status: com.example.chamabuddy.domain.repository.MeetingStatus) : MeetingState()
    data class ContributionRecorded(val success: Boolean) : MeetingState()
    data class BeneficiariesSelected(
        val success: Boolean,
        val beneficiaries: List<String>,
        val meetingStatus: com.example.chamabuddy.domain.repository.MeetingStatus
    ) : MeetingState()
    data class Error(val message: String) : MeetingState()
    data class EligibleBeneficiariesLoaded(
        val members: List<Member>,
        val alreadySelected: List<String> = emptyList()
    ) : MeetingState()
    data class BeneficiaryDetails(
        val beneficiary: Beneficiary,
        val member: Member,
        val meeting: WeeklyMeeting? = null
    ) : MeetingState()

}

sealed class MeetingEvent {
    data class CreateMeeting(val cycleId: String, val date: Date, val recordedBy: String?) : MeetingEvent()
    data class RecordContributions(
        val meetingId: String,
        val contributions: Map<String, Boolean>
    ) : MeetingEvent()
    data class SelectBeneficiaries(
        val meetingId: String,
        val firstBeneficiaryId: String,
        val secondBeneficiaryId: String
    ) : MeetingEvent()
    data class GetMeetingStatus(val meetingId: String) : MeetingEvent()
    data class GetMeetingsForCycle(val cycleId: String) : MeetingEvent()
    data class LoadEligibleBeneficiaries(val meetingId: String) : MeetingEvent()
    data class GetContributionsForMeeting(val meetingId: String) : MeetingEvent()
    data class ConfirmBeneficiarySelection(
        val meetingId: String,
        val firstBeneficiaryId: String,
        val secondBeneficiaryId: String
    ) : MeetingEvent()
    data class LoadBeneficiaryDetails(val beneficiaryId: String) : MeetingEvent()
    object ResetMeetingState : MeetingEvent()

}

data class ContributionTrackingState(
    val isLoading: Boolean = false,
    val meeting: WeeklyMeeting? = null,
    val members: List<Member> = emptyList(),
    val contributions: Map<String, Boolean> = emptyMap(),
    val totalCollected: Int = 0,
    val weeklyAmount: Int = 0,
    val error: String? = null
)

data class BeneficiarySelectionState(
    val isLoading: Boolean = false,
    val eligibleMembers: List<Member> = emptyList(),
    val error: String? = null
)