package com.example.chamabuddy.presentation.viewmodel

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class SavingsViewModel @Inject constructor(
    private val savingsRepository: SavingsRepository,
    private val memberRepository: MemberRepository,
    private val cycleRepository: CycleRepository,
) : ViewModel() {


    private val _savingsData = MutableStateFlow<Map<String, List<Int>>>(emptyMap())
    val savingsData: StateFlow<Map<String, List<Int>>> = _savingsData

    private val _state = MutableStateFlow<SavingsState>(SavingsState.Idle)
    val state: StateFlow<SavingsState> = _state.asStateFlow()
    private val _memberSavings = MutableStateFlow<List<MonthlySavingEntry>>(emptyList())
    val memberSavings: StateFlow<List<MonthlySavingEntry>> = _memberSavings.asStateFlow()
    private val _cycleSavings = MutableStateFlow<List<MonthlySaving>>(emptyList())
    val cycleSavings: StateFlow<List<MonthlySaving>> = _cycleSavings.asStateFlow()
    private val _members = MutableStateFlow<Map<String, Member>>(emptyMap())
    val members: StateFlow<Map<String, Member>> = _members.asStateFlow()
    private val _memberTotals = MutableStateFlow<Map<String, Int>>(emptyMap())
    val memberTotals: StateFlow<Map<String, Int>> = _memberTotals.asStateFlow()
    private val _activeCycle = MutableStateFlow<Cycle?>(null)
    val activeCycle: StateFlow<Cycle?> = _activeCycle.asStateFlow()
    private val _groupMemberTotals = MutableStateFlow<Map<String, Int>>(emptyMap())
    val groupMemberTotals: StateFlow<Map<String, Int>> = _groupMemberTotals.asStateFlow()
    private val _totalGroupSavings = MutableStateFlow(0)
    val totalGroupSavings: StateFlow<Int> = _totalGroupSavings.asStateFlow()
    private val _allMemberCycles = MutableStateFlow<List<CycleWithSavings>>(emptyList())
    val allMemberCycles: StateFlow<List<CycleWithSavings>> = _allMemberCycles.asStateFlow()


    fun updateUiAfterEntryDeletion(entryId: String) {
        val currentCycles = _allMemberCycles.value
        val updatedCycles = currentCycles.map { cycle ->
            cycle.copy(savingsEntries = cycle.savingsEntries.filterNot { it.entryId == entryId })
        }
        _allMemberCycles.value = updatedCycles
    }
    private var _groupId: String by mutableStateOf("")
    private var _cycleId: String by mutableStateOf("")

    fun initializeGroupId(id: String) {
        _groupId = id
        loadActiveCycle()
        loadActiveMembers()
        loadAllMemberSavingsTotalsByGroup()
    }

    private fun loadAllMemberSavingsTotalsByGroup() {
        viewModelScope.launch {
            try {
                println("DEBUG: Loading all member savings for group $_groupId")

                val activeMembers = memberRepository.getMembersByGroup(_groupId)
                println("DEBUG: Active members in group $_groupId -> $activeMembers")

                val totals = mutableMapOf<String, Int>()
                var groupTotal = 0

                activeMembers.forEach { member ->
                    println("DEBUG: Fetching savings for member ${member.memberId} (${member.name})")
                    val total = savingsRepository.getMemberSavingsTotalInGroup(
                        groupId = _groupId,
                        memberId = member.memberId
                    )
                    println("DEBUG: Member ${member.memberId} total savings = $total")

                    totals[member.memberId] = total
                    groupTotal += total
                    println("DEBUG: Running groupTotal = $groupTotal")
                }

                _groupMemberTotals.value = totals
                _totalGroupSavings.value = groupTotal
                println("DEBUG: Final memberTotals = $totals")
                println("DEBUG: Final totalGroupSavings = $groupTotal")

            } catch (e: Exception) {
                println("ERROR: Failed to load member savings for group $_groupId -> ${e.message}")
                e.printStackTrace()
            }
        }
    }


    suspend fun getMemberIdForUser(groupId: String, userId: String): String? {
        return withContext(Dispatchers.IO) {
            memberRepository.getMemberByUserIdAndGroupId(userId, groupId)?.memberId
        }
    }

    fun loadUserSavingsData(groups: List<Group>, userId: String) {
        viewModelScope.launch {
            println("DEBUG: Loading user $userId savings across ${groups.size} groups")
            val savingsData = mutableMapOf<String, List<Int>>()
            if (userId.isNullOrEmpty()) {
                println("DEBUG: User ID is null, skipping load")
                return@launch
            }
            groups.forEach { group ->
                println("DEBUG: Fetching monthly savings progress for user $userId in group ${group.groupId} (${group.name})")
                val savings = savingsRepository.getMemberMonthlySavingsProgress(userId)
                println("DEBUG: Raw savings data for group ${group.name}: $savings")

                savingsData[group.name] = savings.map { it.second }
                println("DEBUG: Processed savings values for chart in group ${group.name}: ${savingsData[group.name]}")
            }

            _savingsData.value = savingsData
            println("DEBUG: Final _savingsData for chart: ${_savingsData.value}")
        }
    }

    suspend fun getGroupSavingsEntries(groupId: String): List<MonthlySavingEntry> {
        return withContext(Dispatchers.IO) {
            savingsRepository.getGroupSavingsEntries(groupId)
        }
    }
    fun initializeCycleId(id: String) {
        _cycleId = id
        viewModelScope.launch {
            activeCycle.value?.let { cycle ->
                val endDate = cycle.endDate ?: return@launch
                savingsRepository.createIncompleteMonths(
                    cycleId = cycle.cycleId,
                    startDate = cycle.startDate,
                    endDate = endDate,
                    monthlyTarget = cycle.monthlySavingsAmount,
                    groupId = _groupId
                )
            }
            loadActiveMembers()
            loadAllMemberSavingsTotalsByCycle()
        }
    }

    init {
        loadActiveMembers()
        loadAllMemberSavingsTotalsByCycle()
    }
    private fun loadActiveCycle() {
        viewModelScope.launch {
            try {
                val cycle = cycleRepository.getActiveCycleForGroup(_groupId)
                _activeCycle.value = cycle
                cycle?.cycleId?.let { initializeCycleId(it) }
            } catch (e: Exception) {
                _activeCycle.value = null
            }
        }
    }
//    private fun loadActiveCycle() {
//        viewModelScope.launch {
//            _activeCycle.value = cycleRepository.getActiveCycleForGroup(_groupId)
//            activeCycle.value?.cycleId?.let { initializeCycleId(it) }
//        }
//    }

    private fun loadActiveMembers() {
        viewModelScope.launch {
            try {
                memberRepository.getMembersByGroupFlow(_groupId).collect { members ->
                    _members.value = members.associateBy { it.memberId }
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    private val _memberMonthlySavings = MutableStateFlow<List<Pair<String, Int>>>(emptyList())
    val memberMonthlySavings: StateFlow<List<Pair<String, Int>>> = _memberMonthlySavings.asStateFlow()

    fun loadMemberMonthlySavings(memberId: String) {
        viewModelScope.launch {
            _memberMonthlySavings.value = savingsRepository.getMemberMonthlySavingsProgress(memberId)
        }
    }

    private fun deleteEntry(event: SavingsEvent.DeleteEntry) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                savingsRepository.deleteSavingsEntryImmediately(event.entryId)
                _state.value = SavingsState.EntryDeleted
                getAllMemberCycles(event.memberId)
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to delete entry")
            }
        }
    }

    private fun deleteMonth(event: SavingsEvent.DeleteMonth) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                savingsRepository.deleteSavingsForMonthImmediately(
                    event.cycleId,
                    event.monthYear,
                    event.groupId
                )
                _state.value = SavingsState.MonthDeleted
                getAllMemberCycles(event.memberId)
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to delete month")
            }
        }
    }
    private fun loadAllMemberSavingsTotalsByCycle() {
        viewModelScope.launch {
            try {
                val activeMembers = memberRepository.getMembersByGroup(_groupId)
                val totals = mutableMapOf<String, Int>()

                activeMembers.forEach { member ->
                    val total = savingsRepository.getMemberSavingsTotalByGroupAndCycle(
                        groupId = _groupId,
                        cycleId = _cycleId,
                        memberId = member.memberId
                    )
                    totals[member.memberId] = total
                }
                _memberTotals.value = totals
            } catch (e: Exception) {
            }
        }
    }

    fun handleEvent(event: SavingsEvent) {
        when (event) {
            is SavingsEvent.EnsureMonthExists -> ensureMonthExists(event)
            is SavingsEvent.RecordSavings -> recordSavings(event)
            is SavingsEvent.GetMemberSavings -> getMemberSavings(event)
            is SavingsEvent.GetCycleSavings -> getCycleSavings(event)
            is SavingsEvent.GetSavingsProgress -> getSavingsProgress(event)
            is SavingsEvent.GetMemberSavingsTotalByCycle -> getMemberSavingsTotalByCycle(event)
            is SavingsEvent.GetMemberSavingsTotal -> getMemberSavingsTotal(event)
            is SavingsEvent.GetAllMemberCycles -> getAllMemberCycles(event.memberId)
            is SavingsEvent.GetMemberSavingsTotalInGroup -> getMemberSavingsTotalInGroup(event)
            SavingsEvent.ResetSavingsState -> resetState()
            is SavingsEvent.DeleteEntry -> deleteEntry(event)
            is SavingsEvent.DeleteMonth -> deleteMonth(event)
        }
    }



    private fun getAllMemberCycles(memberId: String) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                var cyclesWithSavings = savingsRepository.getCycleWithSavingsForMember(memberId)
                val activeCycle = _activeCycle.value

                if (activeCycle != null) {
                    val hasActiveCycle = cyclesWithSavings.any { it.cycle.cycleId == activeCycle.cycleId }
                    if (!hasActiveCycle) {
                        cyclesWithSavings += CycleWithSavings(
                            cycle = activeCycle,
                            savingsEntries = emptyList()
                        )
                    }
                }

                val sorted = cyclesWithSavings.sortedByDescending { it.cycle.startDate }
                _allMemberCycles.value = sorted
                _state.value = SavingsState.Idle
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to load cycles")
            }
        }
    }

    private fun getMemberSavingsTotalInGroup(event: SavingsEvent.GetMemberSavingsTotalInGroup) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                val total = savingsRepository.getMemberSavingsTotalInGroup(
                    event.groupId,
                    event.memberId
                )
                _state.value = SavingsState.MemberSavingsTotal(total)
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to load savings total")
            }
        }
    }

    private fun recordSavings(event: SavingsEvent.RecordSavings) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                val cycle = cycleRepository.getCycleById(event.cycleId)
                if (cycle == null || cycle.groupId != event.groupId) {
                    throw IllegalStateException("Invalid cycle or cycle does not belong to the group")
                }

                val result = savingsRepository.recordMonthlySavings(
                    cycleId = event.cycleId,
                    monthYear = event.monthYear,
                    memberId = event.memberId,
                    amount = event.amount,
                    recordedBy = event.recordedBy,
                    groupId = event.groupId
                )

                if (result.isSuccess) {
                    getAllMemberCycles(event.memberId)
                    _state.value = SavingsState.SavingsRecorded(true)
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Failed to record savings"
                    _state.value = SavingsState.Error(errorMsg)
                }
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to record savings")
            }
        }
    }

    suspend fun getTotalSavingsForMonth(groupId: String, monthYear: String): Int {
        return withContext(Dispatchers.IO) {
            savingsRepository.getTotalSavingsForMonth(groupId, monthYear)
        }
    }

    private fun getMemberSavings(event: SavingsEvent.GetMemberSavings) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                savingsRepository.getMemberSavings(event.cycleId, event.memberId)
                    .collect { entries ->
                        _memberSavings.value = entries
                        _state.value = SavingsState.SavingsEntriesLoaded(entries)
                    }
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to load savings")
            }
        }
    }

    private fun getMemberSavingsTotal(event: SavingsEvent.GetMemberSavingsTotal) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                val total = savingsRepository.getMemberSavingsTotal(event.memberId)
                _state.value = SavingsState.MemberSavingsTotal(total)
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to load savings total")
            }
        }
    }

    private fun getCycleSavings(event: SavingsEvent.GetCycleSavings) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                savingsRepository.getCycleSavings(event.cycleId).collect { savings ->
                    _cycleSavings.value = savings
                    _state.value = SavingsState.SavingsLoaded(savings)
                }
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to load cycle savings")
            }
        }
    }

    private fun getSavingsProgress(event: SavingsEvent.GetSavingsProgress) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                val domainProgress = savingsRepository.getMonthlySavingsProgress(
                    event.cycleId,
                    event.monthYear
                )

                val presentationProgress = SavingsState.SavingsProgress(
                    targetAmount = domainProgress.targetAmount,
                    currentAmount = domainProgress.currentAmount,
                    membersCompleted = domainProgress.membersCompleted,
                    totalMembers = domainProgress.totalMembers
                )

                _state.value = presentationProgress
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to load savings progress")
            }
        }
    }

    private fun getMemberSavingsTotalByCycle(event: SavingsEvent.GetMemberSavingsTotalByCycle) {
        viewModelScope.launch {
            _state.value = SavingsState.Loading
            try {
                val total = savingsRepository.getMemberSavingsTotalByCycle(
                    event.cycleId,
                    event.memberId
                )
                _state.value = SavingsState.MemberSavingsTotal(total)
            } catch (e: Exception) {
                _state.value = SavingsState.Error(e.message ?: "Failed to load savings total")
            }
        }
    }

    private fun ensureMonthExists(event: SavingsEvent.EnsureMonthExists) {
        viewModelScope.launch {
            try {
                savingsRepository.ensureMonthExists(
                    event.cycleId,
                    event.monthYear,
                    event.targetAmount,
                    event.groupId
                )
            } catch (e: Exception) {
            }
        }
    }

    private fun resetState() {
        _state.value = SavingsState.Idle
    }
}

sealed class SavingsState {
    object Idle : SavingsState()
    object Loading : SavingsState()
    data class SavingsLoaded(val savings: List<MonthlySaving>) : SavingsState()
    data class SavingsEntriesLoaded(val entries: List<MonthlySavingEntry>) : SavingsState()
    data class SavingsProgress(
        val targetAmount: Int,
        val currentAmount: Int,
        val membersCompleted: Int,
        val totalMembers: Int
    ) : SavingsState()
    data class SavingsRecorded(val success: Boolean) : SavingsState()
    data class MemberSavingsTotal(val total: Int) : SavingsState()
    data class Error(val message: String) : SavingsState()
    object EntryDeleted : SavingsState()
    object MonthDeleted : SavingsState()
}

sealed class SavingsEvent {
    data class RecordSavings(
        val monthYear: String,
        val memberId: String,
        val amount: Int,
        val recordedBy: String,
        val cycleId: String,
        val groupId: String
    ) : SavingsEvent()
    data class GetMemberSavingsTotalInGroup(
        val groupId: String,
        val memberId: String
    ) : SavingsEvent()
    data class DeleteEntry(val entryId: String, val memberId: String) : SavingsEvent()
    data class DeleteMonth(val cycleId: String, val monthYear: String, val groupId: String, val memberId: String) : SavingsEvent()
    data class GetAllMemberCycles(val memberId: String) : SavingsEvent()
    data class GetMemberSavingsTotal(val memberId: String) : SavingsEvent()
    data class GetMemberSavings(val cycleId: String, val memberId: String) : SavingsEvent()
    data class GetCycleSavings(val cycleId: String) : SavingsEvent()
    data class GetSavingsProgress(val cycleId: String, val monthYear: String) : SavingsEvent()
    data class GetMemberSavingsTotalByCycle(val cycleId: String, val memberId: String) : SavingsEvent()
    data class EnsureMonthExists(val cycleId: String, val monthYear: String, val targetAmount: Int, val groupId: String) : SavingsEvent()
    object ResetSavingsState : SavingsEvent()
}

data class CycleWithSavings(
    val cycle: Cycle,
    val savingsEntries: List<MonthlySavingEntry>
)