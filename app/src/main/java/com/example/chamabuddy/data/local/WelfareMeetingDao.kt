package com.example.chamabuddy.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.WelfareMeeting
import kotlinx.coroutines.flow.Flow

@Dao
interface WelfareMeetingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: WelfareMeeting)

    @Query("SELECT * FROM welfare_meetings WHERE welfareId = :welfareId")
    fun getMeetingsForWelfare(welfareId: String): Flow<List<WelfareMeeting>>

    @Query("SELECT * FROM welfare_meetings WHERE meetingId = :meetingId")
    suspend fun getMeetingById(meetingId: String): WelfareMeeting?

    @Query("SELECT * FROM welfare_meetings WHERE isSynced = 0")
    suspend fun getUnsyncedMeetings(): List<WelfareMeeting>

    @Query("UPDATE welfare_meetings SET isSynced = 1 WHERE meetingId = :meetingId")
    suspend fun markAsSynced(meetingId: String)


    @androidx.room.Update
    suspend fun updateMeeting(meeting: WelfareMeeting)

    @Query("DELETE FROM welfare_meetings WHERE meetingId = :meetingId")
    suspend fun deleteMeeting(meetingId: String)

    @Query("UPDATE welfare_meetings SET isDeleted = 1, deletedAt = :timestamp WHERE meetingId = :meetingId")
    suspend fun markAsDeleted(meetingId: String, timestamp: Long)

    @Query("SELECT * FROM welfare_meetings WHERE isDeleted = 1")
    suspend fun getDeletedMeetings(): List<WelfareMeeting>

    @Query("DELETE FROM welfare_meetings WHERE meetingId = :meetingId")
    suspend fun permanentDelete(meetingId: String)
}
