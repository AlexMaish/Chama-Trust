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


    suspend fun getExpenseById(expenseId: String): ExpenseEntity?
    suspend fun insertExpense(expense: ExpenseEntity)

    suspend fun markAsDeleted(expenseId: String, timestamp: Long)
    suspend fun getDeletedExpenses(): List<ExpenseEntity>
    suspend fun permanentDelete(expenseId: String)
    suspend fun updateExpense(expense: ExpenseEntity)

    suspend fun findSimilarExpense(groupId: String, title: String, amount: Double, date: Long): ExpenseEntity?

}