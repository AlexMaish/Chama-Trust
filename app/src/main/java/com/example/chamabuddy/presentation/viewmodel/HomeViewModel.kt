package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.GroupWithMembers
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.BeneficiaryRepository
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.MeetingRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.SavingsRepository
import com.example.chamabuddy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val meetingRepository: MeetingRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val savingsRepository: SavingsRepository,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _groupData = MutableStateFlow<GroupWithMembers?>(null)
    val groupData: StateFlow<GroupWithMembers?> = _groupData.asStateFlow()

    private val _totalSavings = MutableStateFlow(0)
    val totalSavings: StateFlow<Int> = _totalSavings.asStateFlow()

    private val _state = MutableStateFlow<CycleState>(CycleState.Idle)
    val state: StateFlow<CycleState> = _state.asStateFlow()

    private val _activeCycle = MutableStateFlow<Cycle?>(null)
    val activeCycle: StateFlow<Cycle?> = _activeCycle.asStateFlow()

    private val _userGroups = MutableStateFlow<List<Group>>(emptyList())
    val userGroups: StateFlow<List<Group>> = _userGroups.asStateFlow()


    private val _totalGroupSavings = MutableStateFlow(0)
    val totalGroupSavings: StateFlow<Int> = _totalGroupSavings.asStateFlow()

    private val _currentGroupId = MutableStateFlow<String?>(null)
    var currentGroupId: StateFlow<String?> = _currentGroupId.asStateFlow()

    fun setCurrentGroup(groupId: String) {
        _currentGroupId.value = groupId
    }

    private val _showSnackbar = MutableStateFlow(false)
    val showSnackbar: StateFlow<Boolean> = _showSnackbar.asStateFlow()

    private val _snackbarMessage = MutableStateFlow("")
    val snackbarMessage: StateFlow<String> = _snackbarMessage.asStateFlow()


    private val _allCyclesForGroup = MutableStateFlow<List<Cycle>>(emptyList())
    val allCyclesForGroup: StateFlow<List<Cycle>> = _allCyclesForGroup.asStateFlow()

    fun loadAllCyclesForGroup(groupId: String) {
        viewModelScope.launch {
            _allCyclesForGroup.value = cycleRepository.getCyclesByGroupId(groupId)
                .sortedByDescending { it.cycleId }
        }
    }
    fun setSnackbarMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun showSnackbar() {
        _showSnackbar.value = true
    }

    fun resetSnackbar() {
        _showSnackbar.value = false
        _snackbarMessage.value = ""
    }

    fun refreshCycles() {
        _currentGroupId.value?.let { groupId ->
            loadCyclesForGroup(groupId)
        }
    }

    fun loadGroupData(groupId: String) {
        viewModelScope.launch {
            // Change from getGroupsByIds to getGroupById
            val group = groupRepository.getGroupById(groupId) ?: return@launch
            val members = memberRepository.getMembersByGroup(groupId)

            // Create GroupWithMembers instead of GroupData
            _groupData.value = GroupWithMembers(
                group = group,
                members = members
            )

            // Also load total savings for the group
            _totalGroupSavings.value = savingsRepository.getTotalGroupSavings(groupId)
        }
    }

    fun getCurrentGroupId(): String? {
        return _currentGroupId.value
    }

    init {
        loadUserGroups()
    }


    fun loadCyclesForGroup(groupId: String) {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                println("Loading cycles for group: $groupId")
                var cycles = cycleRepository.getCyclesByGroupId(groupId)

                // Update each cycle with its total savings
                cycles = cycles.map { cycle ->
                    val total = savingsRepository.getTotalSavingsByCycle(cycle.cycleId)
                    cycle.copy(totalSavings = total)
                }

                println("Loaded ${cycles.size} cycles")
                _state.value = CycleState.CycleHistory(cycles)
                _totalSavings.value = cycles.sumOf { it.totalSavings }
            } catch (e: Exception) {
                println("Error loading cycles: ${e.message}")
                _state.value = CycleState.Error(e.message ?: "Failed to load cycles")
            }
        }
    }

    fun loadUserGroups() {
        viewModelScope.launch {
            try {
                val userId = userRepository.getCurrentUserId()
                if (userId != null) {
                    val groupIds = userRepository.getUserGroups(userId).getOrThrow()
                    _userGroups.value = groupRepository.getGroupsByIds(groupIds)
                } else {
                    _snackbarMessage.value = "No user logged in"
                    _showSnackbar.value = true
                }
            } catch (e: Exception) {
                _snackbarMessage.value = "Error loading groups: ${e.message}"
                _showSnackbar.value = true
            }
        }
    }
    fun deleteCycle(cycleId: String) {
        viewModelScope.launch {
            try {
                cycleRepository.deleteCycle(cycleId)
                // Refresh cycles after deletion
                _currentGroupId.value?.let { loadCyclesForGroup(it) }
                setSnackbarMessage("Cycle deleted successfully")
                showSnackbar()
            } catch (e: Exception) {
                setSnackbarMessage("Failed to delete cycle: ${e.message}")
                showSnackbar()
            }
        }
    }
    fun showGroupRequiredMessage() {
        _snackbarMessage.value = "Create a group or join one first using the navigation menu"
        _showSnackbar.value = true
    }

    private fun getTotalSavings() {
        viewModelScope.launch {
            try {
                _totalSavings.value = savingsRepository.getTotalSavings()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun getCycleHistory() {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                val cycles = cycleRepository.getAllCycles().first()
                val cyclesWithSavings = cycles.map { cycle ->
                    val totalSavings = savingsRepository.getTotalSavingsByCycle(cycle.cycleId)
                    cycle.copy(totalSavings = totalSavings)
                }
                _state.value = CycleState.CycleHistory(cyclesWithSavings)
            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "Failed to load cycle history")
            }
        }
    }

    fun handleEvent(event: CycleEvent) {
        when (event) {
            CycleEvent.GetActiveCycle -> getActiveCycle()
            CycleEvent.GetCycleHistory -> {
                getCycleHistory()
                getTotalSavings()
            }
            is CycleEvent.StartNewCycle -> startNewCycle(event)
            is CycleEvent.EndCurrentCycle -> endCurrentCycle(event)
            is CycleEvent.GetCycleStats -> getCycleStats(event.cycleId)
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
                val now = System.currentTimeMillis()

                // 1. End current cycle with timestamp
                val currentActiveCycle = cycleRepository.getActiveCycleForGroup(event.groupId)
                currentActiveCycle?.let { activeCycle ->
                    cycleRepository.endCurrentCycle(activeCycle.cycleId, now)

                    // 2. Create incomplete savings months for the ended cycle
                    savingsRepository.createIncompleteMonths(
                        cycleId = activeCycle.cycleId,
                        startDate = activeCycle.startDate,
                        endDate = now,
                        monthlyTarget = activeCycle.monthlySavingsAmount,
                        groupId = activeCycle.groupId
                    )
                }

                // 3. Start the new cycle
                val result = cycleRepository.startNewCycle(
                    weeklyAmount = event.weeklyAmount,
                    monthlyAmount = event.monthlyAmount,
                    totalMembers = event.totalMembers,
                    startDate = now,
                    groupId = event.groupId,
                    beneficiariesPerMeeting = event.beneficiariesPerMeeting
                )

                if (result.isSuccess) {
                    // 4. Refresh data after new cycle creation
                    loadCyclesForGroup(event.groupId)
                    loadGroupData(event.groupId)
                } else {
                    _state.value = CycleState.Error(result.exceptionOrNull()?.message ?: "Failed to start new cycle")
                }

            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "An unexpected error occurred")
            }
        }
    }

    private fun endCurrentCycle(event: CycleEvent.EndCurrentCycle) {
        viewModelScope.launch {
            _state.value = CycleState.Loading
            try {
                // Get the current active cycle before ending it
                val endedCycle = cycleRepository.getActiveCycle()

                // End the current cycle if exists
                endedCycle?.let { cycle ->
                    val endResult = cycleRepository.endCurrentCycle(cycle.cycleId)
                    if (endResult.isSuccess) {
                        // Create incomplete months for the ended cycle
                        savingsRepository.createIncompleteMonths(
                            cycle.cycleId,
                            cycle.startDate,
                            System.currentTimeMillis(),
                            cycle.monthlySavingsAmount,
                            cycle.groupId
                        )
                    } else {
                        _state.value = CycleState.Error(
                            endResult.exceptionOrNull()?.message ?: "Failed to end cycle"
                        )
                        return@launch
                    }
                }

                // Start new cycle with adjusted parameters
                val newCycleResult = cycleRepository.startNewCycle(
                    event.weeklyAmount,
                    event.monthlyAmount,
                    event.totalMembers,
                    event.startDate,
                    event.groupId,
                    event.beneficiariesPerMeeting
                )

                if (newCycleResult.isSuccess) {
                    _activeCycle.value = newCycleResult.getOrNull()
                    _state.value = CycleState.ActiveCycle(newCycleResult.getOrNull()!!)
                } else {
                    _state.value = CycleState.Error(
                        newCycleResult.exceptionOrNull()?.message ?: "Failed to start new cycle"
                    )
                }
            } catch (e: Exception) {
                _state.value = CycleState.Error(e.message ?: "Failed to end cycle")
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

sealed class CycleState {
    object Idle : CycleState()
    object Loading : CycleState()
    data class ActiveCycle(val cycle: Cycle) : CycleState()
    data class CycleHistory(val cycles: List<Cycle>) : CycleState()
    data class CycleStats(val stats: com.example.chamabuddy.domain.repository.CycleStats) : CycleState()
    data class Error(val message: String) : CycleState()
    object CycleEnded : CycleState()
}

sealed class CycleEvent {
    object GetActiveCycle : CycleEvent()
    object GetCycleHistory : CycleEvent()
    data class StartNewCycle(
        val weeklyAmount: Int,
        val monthlyAmount: Int,
        val totalMembers: Int,
        val startDate: Long,
        val groupId: String,
        val beneficiariesPerMeeting: Int
    ) : CycleEvent()

    data class EndCurrentCycle(
        val weeklyAmount: Int,
        val monthlyAmount: Int,
        val totalMembers: Int,
        val startDate: Long,
        val groupId: String,
        val beneficiariesPerMeeting: Int
    ) : CycleEvent()

    data class GetCycleStats(val cycleId: String) : CycleEvent()
    object ResetCycleState : CycleEvent()
}

data class GroupData(
    val group: Group,
    val members: List<Member>
)