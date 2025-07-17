package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.chamabuddy.domain.model.BenefitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BenefitDao {
    @Insert
    suspend fun insert(benefit: BenefitEntity)

    @Query("SELECT * FROM benefits WHERE groupId = :groupId ORDER BY date DESC")
    fun getBenefits(groupId: String): Flow<List<BenefitEntity>>

    @Query("SELECT SUM(amount) FROM benefits WHERE groupId = :groupId")
    fun getTotal(groupId: String): Flow<Double>
}