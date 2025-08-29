package com.example.chamabuddy.presentation.viewmodel

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.WelfareMeeting
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.WelfareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class WelfareMeetingViewModel @Inject constructor(
    private val welfareRepository: WelfareRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _contributionState = MutableStateFlow(WelfareContributionTrackingState())
    val contributionState: StateFlow<WelfareContributionTrackingState> = _contributionState.asStateFlow()

    private val _beneficiaryState = MutableStateFlow(WelfareBeneficiarySelectionState())
    val beneficiaryState: StateFlow<WelfareBeneficiarySelectionState> = _beneficiaryState.asStateFlow()

    // Persist contributions as amounts (Map<memberId, amount>) when navigating away
    private val _savedContributionState = mutableStateOf<Map<String, Int>?>(null)

    fun saveContributionState(contributions: Map<String, Int>) {
        _savedContributionState.value = contributions
    }

    fun loadSavedContributionState(): Map<String, Int>? {
        return _savedContributionState.value?.also {
            _savedContributionState.value = null
        }
    }

    suspend fun loadMembersForContribution(meetingId: String) {
        _contributionState.value = WelfareContributionTrackingState(isLoading = true)

        try {
            val meeting = welfareRepository.getMeetingById(meetingId)
                ?: throw IllegalStateException("Welfare meeting not found")
            val groupId = meeting.groupId
            val welfareAmount = meeting.welfareAmount
            val activeMembers = memberRepository.getActiveMembersByGroup(groupId)

            // Existing contributions should provide the actual contributed amounts
            val existingContributions = welfareRepository.getContributionsForMeeting(meetingId)
                .associate { it.memberId to it.amountContributed }

            // Initialize with default amount for members without existing contributions
            val contributions = activeMembers.associate { member ->
                member.memberId to (existingContributions[member.memberId] ?: 0)
            }
            val totalCollected = contributions.values.sum()

            // Get contributor summaries (only those with > 0 contribution)
            val contributorSummaries = contributions
                .filter { it.value > 0 }
                .keys
                .mapNotNull { memberId ->
                    activeMembers.find { it.memberId == memberId }?.name
                }

            _contributionState.value = WelfareContributionTrackingState(
                isLoading = false,
                meeting = meeting.copy(
                    contributorSummaries = contributorSummaries
                ),
                members = activeMembers,
                contributions = contributions,
                totalCollected = totalCollected,
                welfareAmount = welfareAmount
            )
        } catch (e: Exception) {
            _contributionState.value = WelfareContributionTrackingState(
                isLoading = false,
                error = e.message ?: "Failed to load members"
            )
        }
    }


    /**
     * Toggle contribution on/off for a member.
     * - If unchecking (contributed = false) -> set amount to 0
     * - If checking (contributed = true) -> keep last non-zero amount or use default welfareAmount
     */
    suspend fun updateContributionStatus(memberId: String, contributed: Boolean) {
        val currentContributions = _contributionState.value.contributions.toMutableMap()
        if (!contributed) {
            // If unchecking, set amount to 0
            currentContributions[memberId] = 0
        } else {
            // If checking, set to last amount or default
            val currentAmount = currentContributions[memberId] ?: 0
            if (currentAmount == 0) {
                currentContributions[memberId] = _contributionState.value.welfareAmount
            }
        }

        val totalCollected = currentContributions.values.sum()
        _contributionState.value = _contributionState.value.copy(
            contributions = currentContributions,
            totalCollected = totalCollected
        )
    }

    /**
     * Update a member's contributed amount and recalculate totals.
     */
    suspend fun updateContributionAmount(memberId: String, amount: Int) {
        val currentContributions = _contributionState.value.contributions.toMutableMap()
        currentContributions[memberId] = amount

        val totalCollected = currentContributions.values.sum()
        _contributionState.value = _contributionState.value.copy(
            contributions = currentContributions,
            totalCollected = totalCollected
        )
    }

    /**
     * Persist contributions (amounts) to repository.
     */
    suspend fun recordContributions(meetingId: String, contributions: Map<String, Int>) {
        try {
            welfareRepository.recordContributions(meetingId, contributions)
        } catch (e: Exception) {
            throw e
        }
    }


    suspend fun loadMembersForNewMeeting(groupId: String, welfareAmount: Int) {
        _contributionState.value = WelfareContributionTrackingState(isLoading = true)

        try {
            val activeMembers = memberRepository.getActiveMembersByGroup(groupId)
            val contributions = activeMembers.associate { it.memberId to 0 } // Initialize with 0

            _contributionState.value = WelfareContributionTrackingState(
                isLoading = false,
                members = activeMembers,
                contributions = contributions,
                totalCollected = 0,
                welfareAmount = welfareAmount
            )
        } catch (e: Exception) {
            _contributionState.value = WelfareContributionTrackingState(
                isLoading = false,
                error = e.message ?: "Failed to load members"
            )
        }
    }
    fun loadAllMembersForBeneficiarySelection(meetingId: String) {
        viewModelScope.launch {
            _beneficiaryState.value = WelfareBeneficiarySelectionState(isLoading = true)

            try {
                val meeting = welfareRepository.getMeetingById(meetingId)
                    ?: throw IllegalStateException("Meeting not found")

                val groupId = meeting.groupId
                val activeMembers = memberRepository.getActiveMembersByGroup(groupId)

                val existingBeneficiaries = welfareRepository.getBeneficiariesForMeeting(meetingId)
                val existingBeneficiaryIds = existingBeneficiaries.map { it.memberId }.toSet()

                // Filter out members who are already beneficiaries
                val eligibleMembers = activeMembers.filterNot {
                    existingBeneficiaryIds.contains(it.memberId)
                }

                _beneficiaryState.value = WelfareBeneficiarySelectionState(
                    isLoading = false,
                    eligibleMembers = eligibleMembers,
                    existingBeneficiaries = activeMembers.filter {
                        existingBeneficiaryIds.contains(it.memberId)
                    }
                )

            } catch (e: Exception) {
                // Log the error (optional)
                e.printStackTrace()

                // Update UI state to reflect the error
                _beneficiaryState.value = WelfareBeneficiarySelectionState(
                    isLoading = false,
                    error = e.message ?: "Failed to load members for selection"
                )
            }
        }
    }


    suspend fun confirmBeneficiarySelection(meetingId: String, beneficiaryIds: List<String>) {
        try {
            welfareRepository.selectBeneficiaries(meetingId, beneficiaryIds)
        } catch (e: Exception) {
            throw e
        }
    }
}

data class WelfareContributionTrackingState(
    val isLoading: Boolean = false,
    val meeting: WelfareMeeting? = null,
    val members: List<Member> = emptyList(),
    // Map<memberId, amount>
    val contributions: Map<String, Int> = emptyMap(),
    val totalCollected: Int = 0,
    val welfareAmount: Int = 0,
    val error: String? = null
)

data class WelfareBeneficiarySelectionState(
    val isLoading: Boolean = false,
    val eligibleMembers: List<Member> = emptyList(),
    val existingBeneficiaries: List<Member> = emptyList(),
    val error: String? = null
)
