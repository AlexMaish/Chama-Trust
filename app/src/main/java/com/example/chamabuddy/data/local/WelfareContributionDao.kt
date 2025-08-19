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
}
