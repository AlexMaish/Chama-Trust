package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.ExpenseDao
import com.example.chamabuddy.domain.model.ExpenseEntity
import com.example.chamabuddy.domain.repository.ExpenseRepository
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {
    override suspend fun addExpense(expense: ExpenseEntity) = expenseDao.insert(expense)
    override fun getExpenses(groupId: String) = expenseDao.getExpenses(groupId)
    override fun getTotal(groupId: String) = expenseDao.getTotal(groupId)



    override suspend fun getUnsyncedExpenses(): List<ExpenseEntity> =
        expenseDao.getUnsyncedExpenses()

    override suspend fun markExpenseSynced(expense: ExpenseEntity) {
        expenseDao.markAsSynced(expense.expenseId)
    }
}
