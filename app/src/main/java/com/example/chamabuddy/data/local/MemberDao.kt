package com.example.chamabuddy.data.local

import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.Member

@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

    @Update
    suspend fun updateMember(member: Member)

    @Delete
    suspend fun deleteMember(member: Member)

    @Query("SELECT * FROM member WHERE member_id = :memberId AND is_deleted = 0")
    suspend fun getMemberById(memberId: String): Member?

    @Query("SELECT * FROM member WHERE is_admin = 1 AND is_deleted = 0 ORDER BY name ASC")
    fun getAllAdmins(): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    fun getActiveMembers(): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE is_deleted = 0 ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("""
        SELECT * FROM member 
        WHERE is_deleted = 0 AND (
            name LIKE '%' || :query || '%' OR 
            nickname LIKE '%' || :query || '%' OR 
            phone_number LIKE '%' || :query || '%'
        )
        ORDER BY name ASC
    """)
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE name = :name AND is_deleted = 0 LIMIT 1")
    suspend fun getMemberByName(name: String): Member?

    @Query("SELECT COUNT(*) FROM member WHERE is_active = 1 AND is_deleted = 0")
    suspend fun getActiveMembersCount(): Int

    @Query("""
        SELECT m.* FROM member m
        INNER JOIN group_members gm ON m.user_id = gm.user_id
        WHERE gm.group_id = :groupId AND m.is_active = 1 AND m.is_deleted = 0
        ORDER BY m.name ASC
    """)
    suspend fun getActiveMembersForGroup(groupId: String): List<Member>

    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_deleted = 0 ORDER BY name ASC")
    fun getMembersByGroupFlow(groupId: String): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE group_id = :groupId AND phone_number = :phoneNumber AND is_deleted = 0")
    suspend fun getMemberByPhoneInGroup(groupId: String, phoneNumber: String): Member?

    @Query("""
        SELECT * FROM member 
        WHERE group_id = :groupId 
        AND REPLACE(REPLACE(phone_number, ' ', ''), '-', '') = :normalizedPhone
        AND is_deleted = 0
    """)
    suspend fun getMemberByNormalizedPhone(groupId: String, normalizedPhone: String): Member?

    @Query("SELECT COUNT(*) FROM member WHERE group_id = :groupId AND is_admin = 1 AND is_deleted = 0")
    suspend fun getAdminCount(groupId: String): Int

    @Query("UPDATE member SET is_admin = :isAdmin WHERE member_id = :memberId")
    suspend fun updateAdminStatus(memberId: String, isAdmin: Boolean)

    @Query("UPDATE member SET is_synced = 1 WHERE member_id = :memberId")
    suspend fun markAsSynced(memberId: String)

    @Query("SELECT * FROM member WHERE is_synced = 0 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getUnsyncedMembers(): List<Member>

    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_synced = 0 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getUnsyncedMembersForGroup(groupId: String): List<Member>

    @Query("UPDATE member SET is_deleted = 1, deleted_at = :timestamp WHERE member_id = :memberId")
    suspend fun markAsDeleted(memberId: String, timestamp: Long)

    @Query("SELECT * FROM member WHERE is_deleted = 1 ORDER BY name ASC")
    suspend fun getDeletedMembers(): List<Member>


    @Query("UPDATE member SET is_active = :isActive, is_synced = 0 WHERE member_id = :memberId")
    suspend fun updateActiveStatus(memberId: String, isActive: Boolean)

    @Query("DELETE FROM member WHERE member_id = :memberId")
    suspend fun permanentDelete(memberId: String)

    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getMembersByGroup(groupId: String): List<Member>

    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_active = 1 AND is_deleted = 0 ORDER BY name ASC")
    suspend fun getActiveMembersByGroup(groupId: String): List<Member>

    @Query("SELECT * FROM member WHERE user_id = :userId AND group_id = :groupId AND is_deleted = 0 LIMIT 1")
    suspend fun getMemberByUserId(userId: String, groupId: String): Member?

    @Query("SELECT * FROM member WHERE user_id = :userId AND group_id = :groupId LIMIT 1")
    suspend fun getMemberByUserIdAndGroupId(userId: String, groupId: String): Member?
}
