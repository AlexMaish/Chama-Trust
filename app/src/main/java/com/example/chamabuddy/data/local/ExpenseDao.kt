package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND is_deleted = 0 ORDER BY date DESC")
    fun getExpenses(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE groupId = :groupId AND is_deleted = 0")
    fun getTotal(groupId: String): Flow<Double>

    @Query("UPDATE expenses SET is_synced = 1 WHERE expenseId = :expenseId")
    suspend fun markAsSynced(expenseId: String)

    @Query("SELECT * FROM expenses WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>

    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId AND is_deleted = 0 LIMIT 1")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?

    @Query("UPDATE expenses SET is_deleted = 1, deleted_at = :timestamp WHERE expenseId = :expenseId")
    suspend fun markAsDeleted(expenseId: String, timestamp: Long)

    @Query("SELECT * FROM expenses WHERE is_deleted = 1")
    suspend fun getDeletedExpenses(): List<ExpenseEntity>

    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun permanentDelete(expenseId: String)

    @Update
    suspend fun update(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId AND title = :title AND amount = :amount AND date = :date AND is_deleted = 0")
    suspend fun findSimilarExpense(groupId: String, title: String, amount: Double, date: Long): ExpenseEntity?
}
