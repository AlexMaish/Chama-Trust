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

    @Query("SELECT SUM(amount) FROM expenses WHERE groupId = :groupId")
    fun getTotal(groupId: String): kotlinx.coroutines.flow.Flow<Double>

    @Query("UPDATE expenses SET is_synced = 1 WHERE expenseId = :expenseId")
    suspend fun markAsSynced(expenseId: String)

    @Query("SELECT * FROM expenses WHERE is_synced = 0")
    suspend fun getUnsyncedExpenses(): List<ExpenseEntity>


    @Query("SELECT * FROM expenses WHERE expenseId = :expenseId LIMIT 1")
    suspend fun getExpenseById(expenseId: String): ExpenseEntity?



}
