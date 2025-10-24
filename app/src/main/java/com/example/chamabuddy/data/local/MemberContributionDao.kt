package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.MemberContribution
import kotlinx.coroutines.flow.Flow

@Dao
interface MemberContributionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertContribution(contribution: MemberContribution)

    @Query("SELECT * FROM MemberContribution WHERE meeting_id = :meetingId")
    suspend fun getContributorsForMeeting(meetingId: String): List<MemberContribution>

    @Query("""
        SELECT m.* FROM Member m
        JOIN MemberContribution mc ON m.member_id = mc.member_id
        WHERE mc.meeting_id = :meetingId
    """)
    suspend fun getMembersWithContributions(meetingId: String): List<Member>
    @Query(
        """
        SELECT COUNT(*) FROM MemberContribution
        WHERE meeting_id = :meetingId AND member_id = :memberId
    """
    )
    suspend fun hasContributed(meetingId: String, memberId: String): Int

    @Query(
        """
        SELECT SUM(amount_contributed) FROM MemberContribution
        WHERE meeting_id = :meetingId
    """
    )
    suspend fun getTotalContributedForMeeting(meetingId: String): Int

    @Query("SELECT * FROM MemberContribution WHERE meeting_id = :meetingId")
    fun getAllContributionsForMeeting(meetingId: String): Flow<List<MemberContribution>>


    @Query("SELECT * FROM MemberContribution WHERE meeting_id = :meetingId")
    suspend fun getContributionsByMeeting(meetingId: String): List<MemberContribution>

    @Query("SELECT * FROM MemberContribution WHERE member_id = :memberId")
    suspend fun getContributionsByMember(memberId: String): List<MemberContribution>

    @Query("DELETE FROM MemberContribution WHERE contribution_id = :contributionId")
    suspend fun deleteContribution(contributionId: String)

    @Query("SELECT * FROM MemberContribution WHERE meeting_id = :meetingId")
    fun getContributorsForMeetingSync(meetingId: String): List<MemberContribution>


    @Query("SELECT * FROM MemberContribution WHERE meeting_id = :meetingId")
    suspend fun getContributionsForMeeting(meetingId: String): List<MemberContribution>

    @Query("DELETE FROM MemberContribution WHERE meeting_id = :meetingId")
    suspend fun deleteContributionsForMeeting(meetingId: String)


    @Query("UPDATE MemberContribution SET is_synced = 1 WHERE contribution_id = :contributionId")
    suspend fun markAsSynced(contributionId: String)

    @Query("SELECT * FROM MemberContribution WHERE is_synced = 0")
    suspend fun getUnsyncedContributions(): List<MemberContribution>

    @Query("SELECT * FROM MemberContribution WHERE contribution_id = :contributionId")
    suspend fun getContributionById(contributionId: String): MemberContribution?



    @Query("UPDATE MemberContribution SET is_deleted = 1, deleted_at = :timestamp WHERE contribution_id = :contributionId")
    suspend fun markAsDeleted(contributionId: String, timestamp: Long)

    @Query("SELECT * FROM MemberContribution WHERE is_deleted = 1")
    suspend fun getDeletedContributions(): List<MemberContribution>

    @Query("DELETE FROM MemberContribution WHERE contribution_id = :contributionId")
    suspend fun permanentDelete(contributionId: String)
}
