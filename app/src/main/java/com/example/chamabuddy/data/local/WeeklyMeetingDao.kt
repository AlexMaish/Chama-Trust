package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.WeeklyMeeting
import kotlinx.coroutines.flow.Flow




@Dao
interface WeeklyMeetingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMeeting(meeting: WeeklyMeeting)

    @Update
    suspend fun updateMeeting(meeting: WeeklyMeeting)

    @Query("SELECT * FROM WeeklyMeeting WHERE meeting_id = :meetingId")
    suspend fun getMeetingById(meetingId: String): WeeklyMeeting?

    @Query("SELECT * FROM WeeklyMeeting WHERE cycle_id = :cycleId ORDER BY meeting_date DESC")
    fun getMeetingsForCycle(cycleId: String): Flow<List<WeeklyMeeting>>

    @Query("""
        SELECT * FROM WeeklyMeeting 
        WHERE cycle_id = :cycleId 
        ORDER BY meeting_date DESC 
        LIMIT 1
    """)
    suspend fun getLatestMeetingForCycle(cycleId: String): WeeklyMeeting?


}

























//@Dao
//interface WeeklyMeetingDao {
//
//
//    @Delete
//    suspend fun deleteMeeting(meeting: WeeklyMeeting)
//
//    @Insert(onConflict = OnConflictStrategy.REPLACE)
//    suspend fun insertMeeting(meeting: WeeklyMeeting): Long
//
//    @Update
//    suspend fun updateMeeting(meeting: WeeklyMeeting)
//
//    @Query("SELECT * FROM WeeklyMeeting WHERE cycle_id = :cycleId ORDER BY meeting_date DESC")
//    fun getMeetingsForCycle(cycleId: String): Flow<List<WeeklyMeeting>>
//
//    @Query("SELECT * FROM WeeklyMeeting WHERE meeting_id = :meetingId")
//    suspend fun getMeetingById(meetingId: String): WeeklyMeeting?
//
//    @Query("""
//        SELECT * FROM WeeklyMeeting
//        WHERE cycle_id = :cycleId
//        ORDER BY meeting_date DESC
//        LIMIT 1
//    """)
//    suspend fun getLatestMeetingForCycle(cycleId: String): WeeklyMeeting?
//
//    @Query("SELECT SUM(total_collected) FROM WeeklyMeeting WHERE cycle_id = :cycleId")
//    suspend fun getTotalCollectedForCycle(cycleId: String): Int?
//
//
//
//
//}
