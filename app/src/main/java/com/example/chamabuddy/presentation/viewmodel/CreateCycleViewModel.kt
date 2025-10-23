package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateCycleViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreateCycleState())
    val uiState: StateFlow<CreateCycleState> = _uiState.asStateFlow()

    private val _isCreating = MutableStateFlow(false)
    val isCreating: StateFlow<Boolean> = _isCreating.asStateFlow()

    private val _creationSuccess = MutableStateFlow(false)
    val creationSuccess: StateFlow<Boolean> = _creationSuccess.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    fun updateWeeklyAmount(amount: String) {
        val newAmount = amount.toIntOrNull() ?: 0
        _uiState.value = _uiState.value.copy(weeklyAmount = newAmount)
    }

    fun updateMonthlySavingsAmount(amount: String) {
        val newAmount = amount.toIntOrNull() ?: 0
        _uiState.value = _uiState.value.copy(monthlySavingsAmount = newAmount)
    }

    fun updateStartDate(date: Long) {
        _uiState.value = _uiState.value.copy(startDate = date)
    }


    fun updateBeneficiariesPerMeeting(value: String) {
        val intValue = value.toIntOrNull() ?: return
        _uiState.update { it.copy(beneficiariesPerMeeting = intValue) }
    }

    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun createCycle() {
        viewModelScope.launch {
            _isCreating.value = true
            try {
                println("Creating cycle for group: $groupId")
                val currentState = uiState.value

                if (currentState.weeklyAmount <= 0 || currentState.monthlySavingsAmount <= 0) {
                    _errorMessage.value = "Amounts must be greater than zero"
                    return@launch
                }

                val groupWithMembers = groupRepository.getGroupWithMembers(groupId)
                val actualMemberCount = groupWithMembers?.members?.size ?: 0

                if (actualMemberCount == 0) {
                    _errorMessage.value = "Group has no members"
                    return@launch
                }

                val result = cycleRepository.startNewCycle(
                    weeklyAmount = currentState.weeklyAmount,
                    monthlyAmount = currentState.monthlySavingsAmount,
                    totalMembers = actualMemberCount,
                    startDate = currentState.startDate,
                    groupId = groupId,
                    beneficiariesPerMeeting = uiState.value.beneficiariesPerMeeting
                )

                if (result.isFailure) {
                    throw Exception(result.exceptionOrNull()?.message ?: "Failed to create cycle")
                }

                val cycleId = result.getOrNull()?.cycleId
                if (cycleId.isNullOrEmpty()) {
                    throw Exception("Cycle created but no ID returned")
                }

                println("Cycle created successfully!")
                _creationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = e.message ?: "Failed to create cycle"
            } finally {
                _isCreating.value = false
            }
        }
    }

    private var groupId: String = ""
    fun setGroupId(id: String) {
        groupId = id
    }
}

data class CreateCycleState(
    val weeklyAmount: Int = 200,
    val monthlySavingsAmount: Int = 200,
    val startDate: Long = System.currentTimeMillis(),
    val groupId: String = "",
    val beneficiariesPerMeeting: Int = 2
)
