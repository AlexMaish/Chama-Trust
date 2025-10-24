package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.ExpenseEntity
import com.example.chamabuddy.domain.repository.ExpenseRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ExpenseViewModel @Inject constructor(
    private val repository: ExpenseRepository
) : ViewModel() {

    private val _expenses = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val expenses: StateFlow<List<ExpenseEntity>> = _expenses.asStateFlow()

    private val _total = MutableStateFlow(0.0)
    val total: StateFlow<Double> = _total.asStateFlow()

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    fun loadData(groupId: String) {
        viewModelScope.launch {
            repository.getExpenses(groupId).collect { _expenses.value = it }
        }
        viewModelScope.launch {
            repository.getTotal(groupId).collect { _total.value = it }
        }
    }


    fun deleteExpense(expenseId: String) {
        viewModelScope.launch {
            repository.markAsDeleted(expenseId, System.currentTimeMillis())
        }
    }

    fun showAddDialog() { _showDialog.value = true }
    fun hideAddDialog() { _showDialog.value = false }

    fun addExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.addExpense(expense)
            hideAddDialog()
        }
    }
}
