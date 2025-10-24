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

    @Query("SELECT * FROM benefits WHERE groupId = :groupId AND is_deleted = 0 ORDER BY date DESC")
    fun getBenefits(groupId: String): Flow<List<BenefitEntity>>

    @Query("SELECT * FROM benefits WHERE groupId = :groupId AND name = :name AND amount = :amount AND date = :date AND is_deleted = 0")
    suspend fun findSimilarBenefit(groupId: String, name: String, amount: Double, date: Long): BenefitEntity?

    @Query("SELECT COALESCE(SUM(amount), 0) FROM benefits WHERE groupId = :groupId AND is_deleted = 0")
    fun getTotal(groupId: String): Flow<Double>

    @Query("UPDATE benefits SET is_synced = 1 WHERE benefitId = :benefitId")
    suspend fun markAsSynced(benefitId: String)

    @Query("SELECT * FROM benefits WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedBenefits(): List<BenefitEntity>

    @Query("SELECT * FROM benefits WHERE benefitId = :benefitId AND is_deleted = 0 LIMIT 1")
    suspend fun getBenefitById(benefitId: String): BenefitEntity?

    @Query("UPDATE benefits SET is_deleted = 1, deleted_at = :timestamp WHERE benefitId = :benefitId")
    suspend fun markAsDeleted(benefitId: String, timestamp: Long)

    @Query("SELECT * FROM benefits WHERE is_deleted = 1")
    suspend fun getDeletedBenefits(): List<BenefitEntity>

    @Query("DELETE FROM benefits WHERE benefitId = :benefitId")
    suspend fun permanentDelete(benefitId: String)

    @Update
    suspend fun update(benefit: BenefitEntity)
}
