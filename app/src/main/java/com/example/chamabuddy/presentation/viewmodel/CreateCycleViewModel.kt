package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CreateCycleViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val memberRepository: MemberRepository // Add this dependency

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
    fun clearErrorMessage() {
        _errorMessage.value = null
    }

    fun createCycle() {
        if (_isCreating.value) return

        _isCreating.value = true
        _errorMessage.value = null

        viewModelScope.launch {
            try {
                cycleRepository.startNewCycle(
                    weeklyAmount = uiState.value.weeklyAmount,
                    monthlySavingsAmount = uiState.value.monthlySavingsAmount,
                    totalMembers = getActiveMembersCount(),
                    startDate = uiState.value.startDate
                )
                _creationSuccess.value = true
            } catch (e: Exception) {
                _errorMessage.value = "Failed to create cycle: ${e.localizedMessage}"
            } finally {
                _isCreating.value = false
            }
        }
    }
    private suspend fun getActiveMembersCount(): Int {
        return memberRepository.getActiveMembersCount() // Implement this in your repository
    }


}

data class CreateCycleState(
    val weeklyAmount: Int = 200,
    val monthlySavingsAmount: Int = 200,
    val startDate: Long = System.currentTimeMillis()
)