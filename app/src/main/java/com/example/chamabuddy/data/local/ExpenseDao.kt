package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow
@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpenses(groupId: String): kotlinx.coroutines.flow.Flow<List<ExpenseEntity>>

//    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM expenses WHERE groupId = :groupId")
//    fun getTotal(groupId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE groupId = :groupId")
    fun getTotal(groupId: String): kotlinx.coroutines.flow.Flow<Double>

    @Query("UPDATE expenses SET is_synced = 1 WHERE expenseId = :expenseId")
    suspend fun markAsSynced(expenseId: String)

    @Query("SELECT * FROM expenses WHERE is_synced = 0")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>


    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?



    // 🔹 Soft delete
    @Query("UPDATE expenses SET is_deleted = 1, deleted_at = :timestamp WHERE expenseId = :expenseId")
    suspend fun markAsDeleted(expenseId: String, timestamp: Long)

    // 🔹 Get all soft-deleted records
    @Query("SELECT * FROM expenses WHERE is_deleted = 1")
    suspend fun getDeletedExpenses(): List<ExpenseEntity>

    // 🔹 Permanently delete
    @Query("DELETE FROM expenses WHERE expenseId = :expenseId")
    suspend fun permanentDelete(expenseId: String)


}
