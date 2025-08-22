package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.MemberWelfareContribution

@Dao
interface WelfareContributionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: MemberWelfareContribution)

    @Query("DELETE FROM MemberWelfareContribution  WHERE meetingId = :meetingId")
    suspend fun deleteContributionsForMeeting(meetingId: String)

    @Query("SELECT * FROM MemberWelfareContribution  WHERE meetingId = :meetingId")
    suspend fun getContributionsForMeeting(meetingId: String): List<MemberWelfareContribution>


    @Query("SELECT * FROM MemberWelfareContribution WHERE isSynced = 0")
    suspend fun getUnsyncedContributions(): List<MemberWelfareContribution>

    @Query("UPDATE MemberWelfareContribution SET isSynced = 1 WHERE contributionId = :contributionId")
    suspend fun markAsSynced(contributionId: String)

    @Query("SELECT * FROM MemberWelfareContribution WHERE contributionId = :contributionId")
    suspend fun getContributionById(contributionId: String): MemberWelfareContribution?


    // 🔹 Soft delete
    @Query("UPDATE MemberWelfareContribution SET isDeleted = 1, deletedAt = :timestamp WHERE contributionId = :contributionId")
    suspend fun markAsDeleted(contributionId: String, timestamp: Long)

    // 🔹 Get all soft-deleted contributions
    @Query("SELECT * FROM MemberWelfareContribution WHERE isDeleted = 1")
    suspend fun getDeletedContributions(): List<MemberWelfareContribution>

    // 🔹 Permanently delete
    @Query("DELETE FROM MemberWelfareContribution WHERE contributionId = :contributionId")
    suspend fun permanentDelete(contributionId: String)

}
