package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Penalty
import com.example.chamabuddy.domain.repository.PenaltyRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PenaltyViewModel @Inject constructor(
    private val repository: PenaltyRepository
) : ViewModel() {
    private val _penalties = MutableStateFlow<List<Penalty>>(emptyList())
    val penalties: StateFlow<List<Penalty>> = _penalties.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    fun loadData(groupId: String) {
        viewModelScope.launch {
            repository.getPenalties(groupId).collect { _penalties.value = it }
            repository.getTotalAmount(groupId).collect { _total.value = it }
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

