package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.WeeklyMeeting
import com.example.chamabuddy.domain.model.WeeklyMeetingWithCycle
import kotlinx.coroutines.flow.Flow




@Dao
interface WeeklyMeetingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: WeeklyMeeting)

    @Update
    suspend fun updateMeeting(meeting: WeeklyMeeting)

    @Query("SELECT * FROM WeeklyMeeting WHERE meeting_id = :meetingId")
    suspend fun getMeetingById(meetingId: String): WeeklyMeeting?

    @Query("SELECT * FROM WeeklyMeeting WHERE cycle_id = :cycleId AND is_deleted = 0 ORDER BY meeting_date DESC")
    fun getMeetingsForCycle(cycleId: String): Flow<List<WeeklyMeeting>>


    @Query(
        """
        SELECT * FROM WeeklyMeeting 
        WHERE cycle_id = :cycleId 
        ORDER BY meeting_date DESC 
        LIMIT 1
    """
    )
    suspend fun getLatestMeetingForCycle(cycleId: String): WeeklyMeeting?

    @Transaction
    @Query("SELECT * FROM WeeklyMeeting WHERE meeting_id = :meetingId")
    suspend fun getMeetingWithCycle(meetingId: String): WeeklyMeetingWithCycle?


    @Query("DELETE FROM WeeklyMeeting WHERE meeting_id = :meetingId")
    suspend fun deleteMeeting(meetingId: String)



    @Query("UPDATE WeeklyMeeting SET is_synced = 1 WHERE meeting_id = :meetingId")
    suspend fun markAsSynced(meetingId: String)

    @Query("SELECT * FROM WeeklyMeeting WHERE is_synced = 0")
    suspend fun getUnsyncedMeetings(): List<WeeklyMeeting>


    // 🔹 Soft delete
    @Query("UPDATE WeeklyMeeting SET is_deleted = 1, deleted_at = :timestamp WHERE meeting_id = :meetingId")
    suspend fun markAsDeleted(meetingId: String, timestamp: Long)

    // 🔹 Get all soft-deleted meetings
    @Query("SELECT * FROM WeeklyMeeting WHERE is_deleted = 1")
    suspend fun getDeletedMeetings(): List<WeeklyMeeting>

    // 🔹 Permanently delete
    @Query("DELETE FROM WeeklyMeeting WHERE meeting_id = :meetingId")
    suspend fun permanentDelete(meetingId: String)

}























