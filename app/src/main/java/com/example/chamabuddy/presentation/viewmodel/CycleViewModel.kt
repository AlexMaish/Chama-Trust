package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.repository.BeneficiaryRepository
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.MeetingRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class CycleViewModel  @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val meetingRepository: MeetingRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
) : ViewModel() {



    private val _state = MutableStateFlow<CycleState>(CycleState.Idle)
    val state: StateFlow<CycleState> = _state.asStateFlow()

    private val _activeCycle = MutableStateFlow<Cycle?>(null)
    val activeCycle: StateFlow<Cycle?> = _activeCycle.asStateFlow()



    fun handleEvent(event: CycleEvent) {
        when (event) {
            CycleEvent.GetActiveCycle -> getActiveCycle()
            CycleEvent.GetCycleHistory -> getCycleHistory()
            is CycleEvent.StartNewCycle -> startNewCycle(event)
            CycleEvent.EndCurrentCycle -> endCurrentCycle()
            is CycleEvent.GetCycleStats -> getCycleStats(event.cycleId)
//            CycleEvent.HandleOddMemberTransition -> handleOddMemberTransition()
            CycleEvent.ResetCycleState -> resetState()
        }
    }

    private fun getActiveCycle() {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                val cycle = cycleRepository.getActiveCycle()
                _activeCycle.value = cycle
                if (cycle != null) {
                    _state.value = CycleState.ActiveCycle(cycle)
                } else {
                    _state.value = CycleState.Error("No active cycle")
                }
            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "Failed to get active cycle")
            }
        }
    }

    private fun startNewCycle(event: CycleEvent.StartNewCycle) {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                val result = cycleRepository.startNewCycle(
                    event.weeklyAmount,
                    event.monthlyAmount,
                    event.totalMembers,
                    event.startDate
                )
                if (result.isSuccess) {
                    _activeCycle.value = result.getOrNull()
                    _state.value = CycleState.ActiveCycle(result.getOrNull()!!)
                } else {
                    _state.value = CycleState.Error(
                        result.exceptionOrNull()?.message ?: "Failed to start new cycle"
                    )
                }
            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "Failed to start new cycle")
            }
        }
    }

    private fun endCurrentCycle() {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                val result = cycleRepository.endCurrentCycle()
                if (result.isSuccess) {
                    _activeCycle.value = null
                    _state.value = CycleState.CycleEnded
                } else {
                    _state.value =
                        CycleState.Error(result.exceptionOrNull()?.message ?: "Failed to end cycle")
                }
            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "Failed to end cycle")
            }
        }
    }

//    private fun handleOddMemberTransition() {
//        viewModelScope.launch {
//            _state.value = CycleState.Loading
//            try {
//                val result = cycleRepository.handleOddMemberTransition()
//                if (result.isSuccess) {
//                    getActiveCycle() // Refresh the current cycle
//                } else {
//                    _state.value = CycleState.Error(
//                        result.exceptionOrNull()?.message ?: "Failed to handle transition"
//                    )
//                }
//            } catch (e: Exception) {
//                _state.value = CycleState.Error(e.message ?: "Failed to handle transition")
//            }
//        }
//    }

    private fun getCycleHistory() {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                val cycles = cycleRepository.getAllCycles().first()
                _state.value = CycleState.CycleHistory(cycles)
            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "Failed to load cycle history")
            }
        }
    }

    private fun getCycleStats(cycleId: String) {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                val stats = cycleRepository.getCycleStats(cycleId)
                _state.value = CycleState.CycleStats(stats)
            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "Failed to load cycle stats")
            }
        }
    }

    private fun resetState() {
        _state.value = CycleState.Idle
    }
}



// Cycle State
sealed class CycleState {
    object Idle : CycleState()
    object Loading : CycleState()
    data class ActiveCycle(val cycle: Cycle) : CycleState()
    data class CycleHistory(val cycles: List<Cycle>) : CycleState()
    data class CycleStats(val stats: com.example.chamabuddy.domain.repository.CycleStats) : CycleState()
    data class Error(val message: String) : CycleState()
    object CycleEnded : CycleState()
}

// Cycle Events
sealed class CycleEvent {
    object GetActiveCycle : CycleEvent()
    object GetCycleHistory : CycleEvent()
    data class StartNewCycle(
        val weeklyAmount: Int,
        val monthlyAmount: Int,
        val totalMembers: Int,
        val startDate: Long // Add this parameter
    ) : CycleEvent()
    object EndCurrentCycle : CycleEvent()
    data class GetCycleStats(val cycleId: String) : CycleEvent()
    object ResetCycleState : CycleEvent()
}



