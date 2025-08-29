package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.domain.repository.BenefitRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BenefitViewModel @Inject constructor(
    private val repository: BenefitRepository
) : ViewModel() {

    private val _benefits = MutableStateFlow<List<BenefitEntity>>(emptyList())
    val benefits: StateFlow<List<BenefitEntity>> = _benefits.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    fun loadData(groupId: String) {
        // collect each flow in its own coroutine so neither blocks the other
        viewModelScope.launch {
            repository.getBenefits(groupId).collect { _benefits.value = it }
        }
        viewModelScope.launch {
            repository.getTotal(groupId).collect { _total.value = it }
        }
    }

    fun deleteBenefit(benefitId: String) {
        viewModelScope.launch {
            repository.markAsDeleted(benefitId, System.currentTimeMillis())
        }
    }

    fun showAddDialog() { _showDialog.value = true }
    fun hideAddDialog() { _showDialog.value = false }

    fun addBenefit(benefit: BenefitEntity) {
        viewModelScope.launch {
            repository.addBenefit(benefit)
            hideAddDialog()
        }
    }
}
