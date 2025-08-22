package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.WelfareBeneficiary

@Dao
interface WelfareBeneficiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: WelfareBeneficiary)

    @Query("DELETE FROM welfare_beneficiaries WHERE meetingId = :meetingId")
    suspend fun deleteBeneficiariesForMeeting(meetingId: String)

    @Query("SELECT * FROM welfare_beneficiaries WHERE meetingId = :meetingId")
    suspend fun getBeneficiariesForMeeting(meetingId: String): List<WelfareBeneficiary>


    @Query("SELECT * FROM welfare_beneficiaries WHERE isSynced = 0")
    suspend fun getUnsyncedBeneficiaries(): List<WelfareBeneficiary>

    @Query("UPDATE welfare_beneficiaries SET isSynced = 1 WHERE beneficiaryId = :beneficiaryId")
    suspend fun markAsSynced(beneficiaryId: String)

    @Query("SELECT * FROM welfare_beneficiaries WHERE beneficiaryId = :beneficiaryId")
    suspend fun getBeneficiaryById(beneficiaryId: String): WelfareBeneficiary?


    // 🔹 Soft delete
    @Query("UPDATE welfare_beneficiaries SET isDeleted = 1, deletedAt = :timestamp WHERE beneficiaryId = :beneficiaryId")
    suspend fun markAsDeleted(beneficiaryId: String, timestamp: Long)

    // 🔹 Get all soft-deleted beneficiaries
    @Query("SELECT * FROM welfare_beneficiaries WHERE isDeleted = 1")
    suspend fun getDeletedBeneficiaries(): List<WelfareBeneficiary>

    // 🔹 Permanently delete
    @Query("DELETE FROM welfare_beneficiaries WHERE beneficiaryId = :beneficiaryId")
    suspend fun permanentDelete(beneficiaryId: String)


}