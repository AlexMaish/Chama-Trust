package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.ExpenseDao
import com.example.chamabuddy.domain.model.ExpenseEntity
import com.example.chamabuddy.domain.repository.ExpenseRepository
import java.util.UUID
import javax.inject.Inject

class ExpenseRepositoryImpl @Inject constructor(
    private val expenseDao: ExpenseDao
) : ExpenseRepository {


    override suspend fun findSimilarExpense(groupId: String, title: String, amount: Double, date: Long): ExpenseEntity? {
        return expenseDao.findSimilarExpense(groupId, title, amount, date)
    }

    override suspend fun addExpense(expense: ExpenseEntity) {
        val newId = if (expense.expenseId.isEmpty()) UUID.randomUUID().toString() else expense.expenseId
        val updatedExpense = expense.copy(expenseId = newId, isSynced = false)
        expenseDao.insert(updatedExpense)
    }


    override fun getExpenses(groupId: String) = expenseDao.getExpenses(groupId)

    override fun getTotal(groupId: String) = expenseDao.getTotal(groupId)

    override suspend fun getUnsyncedExpenses(): List<ExpenseEntity> =
        expenseDao.getUnsyncedExpenses()

    override suspend fun markExpenseSynced(expense: ExpenseEntity) {
        expenseDao.markAsSynced(expense.expenseId)
    }

    override suspend fun getExpenseById(expenseId: String): ExpenseEntity? {
        return expenseDao.getExpenseById(expenseId)
    }

    override suspend fun insertExpense(expense: ExpenseEntity) {
        expenseDao.insert(expense)
    }


    override suspend fun markAsDeleted(expenseId: String, timestamp: Long) =
        expenseDao.markAsDeleted(expenseId, timestamp)

    override suspend fun getDeletedExpenses(): List<ExpenseEntity> =
        expenseDao.getDeletedExpenses()

    override suspend fun permanentDelete(expenseId: String) =
        expenseDao.permanentDelete(expenseId)


    override suspend fun updateExpense(expense: ExpenseEntity) {
        expenseDao.update(expense)
    }
}
