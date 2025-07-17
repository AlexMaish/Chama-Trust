package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.chamabuddy.domain.model.ExpenseEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Insert
    suspend fun insert(expense: ExpenseEntity)

    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpenses(groupId: String): Flow<List<ExpenseEntity>>

    @Query("SELECT SUM(amount) FROM expenses WHERE groupId = :groupId")
    fun getTotal(groupId: String): Flow<Double>
}