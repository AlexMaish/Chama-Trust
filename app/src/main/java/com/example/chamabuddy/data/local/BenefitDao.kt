package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.BenefitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BenefitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(benefit: BenefitEntity)

    @Query("SELECT * FROM benefits WHERE groupId = :groupId ORDER BY date DESC")
    fun getBenefits(groupId: String): kotlinx.coroutines.flow.Flow<List<BenefitEntity>>


//    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM benefits WHERE groupId = :groupId")
//    fun getTotal(groupId: String): Flow<Double>

    @Query("SELECT COALESCE(SUM(amount), 0) FROM benefits WHERE groupId = :groupId")
    fun getTotal(groupId: String): kotlinx.coroutines.flow.Flow<Double>


    @Query("UPDATE benefits SET is_synced = 1 WHERE benefitId = :benefitId")
    suspend fun markAsSynced(benefitId: String)

    @Query("SELECT * FROM benefits WHERE is_synced = 0")
    suspend fun getUnsyncedBenefits(): List<BenefitEntity>

    @Query("SELECT * FROM benefits WHERE benefitId = :benefitId LIMIT 1")
    suspend fun getBenefitById(benefitId: String): BenefitEntity?


}
