package com.example.chamabuddy.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.data.local.CycleDao
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
    private val beneficiaryRepository: BeneficiaryRepository,
    private val cycleDao: CycleDao
) : ViewModel() {
    private var currentCycleId: String? = null

    private val _state = MutableStateFlow<MeetingState>(MeetingState.Idle)
    val state: StateFlow<MeetingState> = _state.asStateFlow()

    private val _contributionState = MutableStateFlow(ContributionTrackingState())
    val contributionState: StateFlow<ContributionTrackingState> = _contributionState.asStateFlow()

    private val _beneficiaryState = MutableStateFlow(BeneficiarySelectionState())
    val beneficiaryState: StateFlow<BeneficiarySelectionState> = _beneficiaryState.asStateFlow()

    private val _savedContributionState = mutableStateOf<Map<String, Boolean>?>(null)

    fun saveContributionState(contributions: Map<String, Boolean>) {
        _savedContributionState.value = contributions
    }


    private val _currentUserIsAdmin = MutableStateFlow(false)
    val currentUserIsAdmin: StateFlow<Boolean> = _currentUserIsAdmin.asStateFlow()

    private val _currentUserIsOwner = MutableStateFlow(false)
    val currentUserIsOwner: StateFlow<Boolean> = _currentUserIsOwner.asStateFlow()

    fun loadSavedContributionState(): Map<String, Boolean>? {
        return _savedContributionState.value?.also {
            _savedContributionState.value = null
        }
    }
    fun handleEvent(event: MeetingEvent) {
        when (event) {
            is MeetingEvent.CreateMeeting -> createMeeting(event)
            is MeetingEvent.RecordContributions -> recordContributions(event)
            is MeetingEvent.SelectBeneficiaries -> selectBeneficiaries(event)
            is MeetingEvent.GetMeetingStatus -> getMeetingStatus(event)
            is MeetingEvent.UpdateBeneficiaryAmount -> updateBeneficiaryAmount(event)
            is MeetingEvent.GetMeetingsForCycle -> getMeetingsForCycle(event)
            is MeetingEvent.LoadEligibleBeneficiaries -> loadEligibleBeneficiaries(event.meetingId)
            is MeetingEvent.GetContributionsForMeeting -> loadMembersForContribution(event.meetingId)
            is MeetingEvent.ConfirmBeneficiarySelection -> confirmBeneficiarySelection(
                event.meetingId,
                event.beneficiaryIds
            )
            is MeetingEvent.LoadBeneficiaryDetails -> loadBeneficiaryDetails(event.beneficiaryId)
            MeetingEvent.ResetMeetingState -> resetState()
            is MeetingEvent.DeleteMeeting -> deleteMeeting(event.meetingId)

        }
    }


    private fun deleteMeeting(meetingId: String) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                meetingRepository.deleteMeeting(meetingId)
                // Reload meetings after deletion using current view model instance
                currentCycleId?.let { cycleId ->
                    getMeetingsForCycle(MeetingEvent.GetMeetingsForCycle(cycleId))
                }
                _state.value = MeetingState.MeetingDeleted(true)
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to delete meeting")
            }
        }
    }
    fun validateAdminAction(action: () -> Unit, onError: () -> Unit) {
        if (currentUserIsAdmin.value) {
            action()
        } else {
            onError()
        }
    }

    fun loadUserPermissions(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val member = memberRepository.getMemberByUserId(userId, groupId)
                _currentUserIsAdmin.value = member?.isAdmin ?: false
                _currentUserIsOwner.value = member?.isOwner ?: false
            } catch (e: Exception) {
                _currentUserIsAdmin.value = false
                _currentUserIsOwner.value = false
            }
        }
    }

    private fun updateBeneficiaryAmount(event: MeetingEvent.UpdateBeneficiaryAmount) {
        viewModelScope.launch {
            try {
                beneficiaryRepository.updateBeneficiaryAmount(
                    event.beneficiaryId,
                    event.newAmount
                )
                // Reload beneficiary details
                loadBeneficiaryDetails(event.beneficiaryId)
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to update amount")
            }
        }
    }


    private fun loadMembersForContribution(meetingId: String) {
        viewModelScope.launch {
            _contributionState.value = ContributionTrackingState(isLoading = true)

            try {
                val meeting = meetingRepository.getMeetingById(meetingId)
                    ?: throw IllegalStateException("Meeting not found")

                // Fetch only active members of this group
                val allMembers = memberRepository.getActiveMembersByGroup(meeting.groupId)

                val existingContributions = contributionRepository.getContributionsForMeeting(meetingId)
                    .associate { it.memberId to true }

                val meetingWithCycle = meetingRepository.getMeetingWithCycle(meetingId)
                val weeklyAmount = meetingWithCycle?.cycle?.weeklyAmount ?: 0

                val contributions = allMembers.associate { member ->
                    member.memberId to (existingContributions[member.memberId] ?: false)
                }

                val totalCollected = contributions.count { it.value } * weeklyAmount

                _contributionState.value = ContributionTrackingState(
                    isLoading = false,
                    meeting = meetingWithCycle?.meeting,
                    meetingWithCycle = meetingWithCycle,
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
    suspend fun getBeneficiaryCountForMeeting(meetingId: String): Int {
        return beneficiaryRepository.getBeneficiaryCountForMeeting(meetingId)
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
                    recordedBy = event.recordedBy,
                    groupId = event.groupId
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
        loadEligibleBeneficiaries(event.meetingId)
    }


    private fun loadEligibleBeneficiaries(meetingId: String) {
        viewModelScope.launch {
            _beneficiaryState.value = BeneficiarySelectionState(isLoading = true)
            try {
                val meeting = meetingRepository.getMeetingById(meetingId)
                    ?: throw IllegalStateException("Meeting not found")
                val cycleId = meeting.cycleId
                val groupId = meeting.groupId

                // Only active group members
                val allActiveMembers = memberRepository.getActiveMembersByGroup(groupId)

                // Get existing beneficiaries for this meeting
                val existingBeneficiaries = beneficiaryRepository.getBeneficiariesForMeeting(meetingId)
                val existingBeneficiaryIds = existingBeneficiaries.map { it.memberId }.toSet()

                // Members who haven't received in this cycle
                val cycleBeneficiaries = beneficiaryRepository.getBeneficiariesByCycle(cycleId)
                    .map { it.memberId }
                    .toSet()
                    .minus(existingBeneficiaryIds) // Exclude current meeting's beneficiaries

                val eligibleMembers = allActiveMembers.filter { member ->
                    !cycleBeneficiaries.contains(member.memberId)
                }

                // Get existing beneficiary members
                val existingBeneficiaryMembers = allActiveMembers.filter { member ->
                    existingBeneficiaryIds.contains(member.memberId)
                }

                // Get max beneficiaries from cycle
                val cycle = cycleDao.getCycleById(cycleId)
                    ?: throw IllegalStateException("Cycle not found")
                val maxBeneficiaries = cycle.beneficiariesPerMeeting

                _beneficiaryState.value = BeneficiarySelectionState(
                    isLoading = false,
                    eligibleMembers = eligibleMembers,
                    existingBeneficiaries = existingBeneficiaryMembers,
                    maxBeneficiaries = maxBeneficiaries
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
        beneficiaryIds: List<String>
    ) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                val meeting = meetingRepository.getMeetingById(meetingId)
                    ?: throw IllegalStateException("Meeting not found")
                val totalCollected = meeting.totalCollected
                val beneficiaryCount = beneficiaryIds.size.coerceAtLeast(1)
                val amountPerBeneficiary = totalCollected / beneficiaryCount

                beneficiaryRepository.deleteBeneficiariesForMeeting(meetingId)

                beneficiaryIds.take(beneficiaryState.value.maxBeneficiaries).forEachIndexed { index, beneficiaryId ->
                    val beneficiary = Beneficiary(
                        beneficiaryId = UUID.randomUUID().toString(),
                        memberId = beneficiaryId,
                        cycleId = meeting.cycleId,
                        meetingId = meetingId,
                        amountReceived = amountPerBeneficiary,
                        dateAwarded = Date(),
                        paymentOrder = index + 1,
                        groupId = meeting.groupId
                    )
                    beneficiaryRepository.insertBeneficiary(beneficiary)
                }

                // Update meeting status
                meetingRepository.updateMeetingStatus(
                    meetingId,
                    hasContributions = true,
                    hasBeneficiaries = beneficiaryIds.isNotEmpty()
                )

                _state.value = MeetingState.BeneficiariesSelected(
                    success = true,
                    beneficiaries = beneficiaryIds,
                    meetingStatus = meetingRepository.getMeetingStatus(meetingId),
                    meeting = meeting
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
                val beneficiary = beneficiaryRepository.getBeneficiaryById(beneficiaryId)
                    ?: throw IllegalStateException("Beneficiary not found")
                val member = memberRepository.getMemberById(beneficiary.memberId)
                    ?: throw IllegalStateException("Member not found")
                val meeting = meetingRepository.getMeetingById(beneficiary.meetingId)

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


    private fun getMeetingStatus(event: MeetingEvent.GetMeetingStatus) {
        viewModelScope.launch {
            _state.value = MeetingState.Loading
            try {
                // Get domain model MeetingStatus
                val domainStatus = meetingRepository.getMeetingStatus(event.meetingId)

                // Create state representation
                _state.value = MeetingState.MeetingStatusState(domainStatus)
            } catch (e: Exception) {
                _state.value = MeetingState.Error(e.message ?: "Failed to get meeting status")
            }
        }
    }
    suspend fun getMeetingStatus(meetingId: String): MeetingStatus {
        return meetingRepository.getMeetingStatus(meetingId)
    }

    private fun getMeetingsForCycle(event: MeetingEvent.GetMeetingsForCycle) {
        currentCycleId = event.cycleId
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
    private fun resetState() {
        _state.value = MeetingState.Idle
    }




    fun restoreContributions(contributions: Map<String, Boolean>) {
        val weeklyAmount = _contributionState.value.weeklyAmount
        val totalCollected = contributions.count { it.value } * weeklyAmount

        _contributionState.update { currentState ->
            currentState.copy(
                contributions = contributions,
                totalCollected = totalCollected
            )
        }
    }




}

sealed class MeetingState {
    object Idle : MeetingState()
    object Loading : MeetingState()
    data class MeetingsLoaded(val meetings: List<MeetingWithDetails>) : MeetingState()
    data class MeetingCreated(val meeting: WeeklyMeeting) : MeetingState()

    // Renamed to avoid conflict
    data class MeetingStatusState(val status: com.example.chamabuddy.domain.model.MeetingStatus) : MeetingState()

    data class ContributionRecorded(val success: Boolean) : MeetingState()
    data class BeneficiariesSelected(
        val success: Boolean,
        val beneficiaries: List<String>,
        val meetingStatus: MeetingStatus,
        val meeting: WeeklyMeeting? = null
    ) : MeetingState()
    data class Error(val message: String) : MeetingState()
    data class BeneficiaryDetails(
        val beneficiary: Beneficiary,
        val member: Member,
        val meeting: WeeklyMeeting? = null
    ) : MeetingState()

    data class MeetingDeleted(val success: Boolean) : MeetingState()

}

sealed class MeetingEvent {
    data class CreateMeeting(val cycleId: String, val date: Date, val recordedBy: String?, val groupId: String) : MeetingEvent()
    data class RecordContributions(val meetingId: String, val contributions: Map<String, Boolean>) : MeetingEvent()
    data class SelectBeneficiaries(val meetingId: String) : MeetingEvent()
    data class GetMeetingStatus(val meetingId: String) : MeetingEvent()
    data class GetMeetingsForCycle(val cycleId: String) : MeetingEvent()
    data class LoadEligibleBeneficiaries(val meetingId: String) : MeetingEvent()
    data class GetContributionsForMeeting(val meetingId: String) : MeetingEvent()
    data class ConfirmBeneficiarySelection(
        val meetingId: String,
        val beneficiaryIds: List<String>
    ) : MeetingEvent()
    data class DeleteMeeting(val meetingId: String) : MeetingEvent()
    data class LoadBeneficiaryDetails(val beneficiaryId: String) : MeetingEvent()
    data class UpdateBeneficiaryAmount(val beneficiaryId: String, val newAmount: Int) : MeetingEvent()
    object ResetMeetingState : MeetingEvent()
}

data class ContributionTrackingState(
    val isLoading: Boolean = false,
    val meeting: WeeklyMeeting? = null,
    val meetingWithCycle: WeeklyMeetingWithCycle? = null, // Add this
    val members: List<Member> = emptyList(),
    val contributions: Map<String, Boolean> = emptyMap(),
    val totalCollected: Int = 0,
    val weeklyAmount: Int = 0,
    val error: String? = null
)

data class BeneficiarySelectionState(
    val isLoading: Boolean = false,
    val eligibleMembers: List<Member> = emptyList(),
    val existingBeneficiaries: List<Member> = emptyList(),
    val maxBeneficiaries: Int = 2,
    val error: String? = null
)