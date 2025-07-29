package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.domain.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

interface ExpenseRepository {
    suspend fun addExpense(expense: ExpenseEntity)
    fun getExpenses(groupId: String): Flow<List<ExpenseEntity>>
    fun getTotal(groupId: String): Flow<Double>

    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>
    suspend fun markExpenseSynced(expense: ExpenseEntity)
}