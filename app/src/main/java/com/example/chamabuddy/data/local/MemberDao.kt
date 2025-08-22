package com.example.chamabuddy.data.local

import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.WeeklyMeeting


@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

    @Update
    suspend fun updateMember(member: Member)



    @Delete
    suspend fun deleteMember(member: Member)

    @Query("SELECT * FROM Member WHERE member_id = :memberId")
    suspend fun getMemberById(memberId: String): Member?

    @Query("SELECT * FROM Member WHERE is_admin = 1")
    fun getAllAdmins(): Flow<List<Member>>

    @Query("SELECT * FROM Member WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveMembers(): Flow<List<Member>>

    @Query("SELECT * FROM Member ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM Member WHERE name LIKE '%' || :query || '%' OR nickname LIKE '%' || :query || '%' OR phone_number LIKE '%' || :query || '%'")
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT * FROM Member WHERE name = :name LIMIT 1")
    suspend fun getMemberByName(name: String): Member?

    @Query("SELECT COUNT(*) FROM member WHERE is_active = 1")
    suspend fun getActiveMembersCount(): Int


    @Query("""
        SELECT m.* FROM Member m
        INNER JOIN group_members gm ON m.user_id = gm.user_id
        WHERE gm.group_id = :groupId AND m.is_active = 1
        ORDER BY m.name ASC
    """)
    suspend fun getActiveMembersForGroup(groupId: String): List<Member>



    @Query("SELECT * FROM member WHERE group_id = :groupId")
    fun getMembersByGroupFlow(groupId: String): Flow<List<Member>>



    @Query("SELECT * FROM member WHERE group_id = :groupId AND phone_number = :phoneNumber")
    suspend fun getMemberByPhoneInGroup(groupId: String, phoneNumber: String): Member?


    @Query("SELECT * FROM member WHERE group_id = :groupId AND REPLACE(REPLACE(phone_number, ' ', ''), '-', '') = :normalizedPhone")
    suspend fun getMemberByNormalizedPhone(groupId: String, normalizedPhone: String): Member?



    @Query("SELECT COUNT(*) FROM member WHERE group_id = :groupId AND is_admin = 1")
    suspend fun getAdminCount(groupId: String): Int


    @Query("UPDATE member SET is_admin = :isAdmin WHERE member_id = :memberId")
    suspend fun updateAdminStatus(memberId: String, isAdmin: Boolean)

    @Query("UPDATE member SET is_active = :isActive WHERE member_id = :memberId")
    suspend fun updateActiveStatus(memberId: String, isActive: Boolean)


    @Query("UPDATE member SET is_synced = 1 WHERE member_id = :memberId")
    suspend fun markAsSynced(memberId: String)

    @Query("SELECT * FROM member WHERE is_synced = 0")
    suspend fun getUnsyncedMembers(): List<Member>


    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_synced = 0")
    suspend fun getUnsyncedMembersForGroup(groupId: String): List<Member>

    // 🔹 Soft delete
    @Query("UPDATE member SET is_deleted = 1, deleted_at = :timestamp WHERE member_id = :memberId")
    suspend fun markAsDeleted(memberId: String, timestamp: Long)

    // 🔹 Get all soft-deleted members
    @Query("SELECT * FROM member WHERE is_deleted = 1")
    suspend fun getDeletedMembers(): List<Member>

    // 🔹 Permanently delete
    @Query("DELETE FROM member WHERE member_id = :memberId")
    suspend fun permanentDelete(memberId: String)

    // Add is_deleted = 0 condition to relevant queries
    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_deleted = 0")
    suspend fun getMembersByGroup(groupId: String): List<Member>

    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_active = 1 AND is_deleted = 0")
    suspend fun getActiveMembersByGroup(groupId: String): List<Member>

    @Query("SELECT * FROM member WHERE user_id = :userId AND group_id = :groupId AND is_deleted = 0")
    suspend fun getMemberByUserId(userId: String, groupId: String): Member?

}
