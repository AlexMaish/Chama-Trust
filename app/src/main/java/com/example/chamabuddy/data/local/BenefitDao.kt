package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.BenefitEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BenefitDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(benefit: BenefitEntity)

    @Query("SELECT * FROM benefits WHERE groupId = :groupId ORDER BY date DESC")
    fun getBenefits(groupId: String): kotlinx.coroutines.flow.Flow<List<BenefitEntity>>




    @Query("SELECT * FROM benefits WHERE groupId = :groupId AND name = :name AND amount = :amount AND date = :date AND is_deleted = 0")
    suspend fun findSimilarBenefit(groupId: String, name: String, amount: Double, date: Long): BenefitEntity?

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



    @Query("UPDATE benefits SET is_deleted = 1, deleted_at = :timestamp WHERE benefitId = :benefitId")
    suspend fun markAsDeleted(benefitId: String, timestamp: Long)

    // 🔹 Get soft-deleted records
    @Query("SELECT * FROM benefits WHERE is_deleted = 1")
    suspend fun getDeletedBenefits(): List<BenefitEntity>

    // 🔹 Permanently delete
    @Query("DELETE FROM benefits WHERE benefitId = :benefitId")
    suspend fun permanentDelete(benefitId: String)

    @Update
    suspend fun update(benefit: BenefitEntity)
}
