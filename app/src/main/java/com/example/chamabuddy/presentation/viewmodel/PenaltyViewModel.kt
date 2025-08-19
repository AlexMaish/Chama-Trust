package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.Penalty
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.PenaltyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PenaltyViewModel @Inject constructor(
    private val repository: PenaltyRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    // Penalties state
    private val _penalties = MutableStateFlow<List<Penalty>>(emptyList())
    val penalties: StateFlow<List<Penalty>> = _penalties.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    // Members state
    private val _members = MutableStateFlow<List<Member>>(emptyList())
    val members: StateFlow<List<Member>> = _members.asStateFlow()

    // Filtered members state
    private val _filteredMembers = MutableStateFlow<List<Member>>(emptyList())
    val filteredMembers: StateFlow<List<Member>> = _filteredMembers.asStateFlow()

    fun loadData(groupId: String) {
        viewModelScope.launch {
            // Load penalties
            launch {
                repository.getPenalties(groupId).collect { _penalties.value = it }
            }
            // Load total
            launch {
                repository.getTotalAmount(groupId).collect { _total.value = it }
            }
            // Load members
            launch {
                memberRepository.getMembersByGroupFlow(groupId).collect {
                    _members.value = it
                    _filteredMembers.value = it // initialize filtered list
                }
            }
        }
    }

    fun filterMembers(query: String) {
        _filteredMembers.value = if (query.isBlank()) {
            _members.value
        } else {
            _members.value.filter { it.name.contains(query, ignoreCase = true) }
        }
    }

    fun showAddDialog() { _showDialog.value = true }
    fun hideAddDialog() { _showDialog.value = false }

    fun addPenalty(penalty: Penalty) {
        viewModelScope.launch {
            repository.addPenalty(penalty)
            hideAddDialog()
        }
    }
}
