package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.GroupMember
@Dao
interface GroupMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(groupMember: GroupMember)

    @Query("DELETE FROM group_members WHERE group_id = :groupId AND user_id = :userId")
    suspend fun removeMember(groupId: String, userId: String)

    @Query("SELECT * FROM group_members WHERE group_id = :groupId")
    suspend fun getGroupMembers(groupId: String): List<GroupMember>

    @Query("UPDATE group_members SET is_admin = :isAdmin WHERE group_id = :groupId AND user_id = :userId")
    suspend fun updateAdminStatus(groupId: String, userId: String, isAdmin: Boolean)

    @Query("SELECT group_id FROM group_members WHERE user_id = :userId")
    suspend fun getGroupIdsForUser(userId: String): List<String>

    @Query("UPDATE group_members SET is_synced = 1 WHERE group_id = :groupId AND user_id = :userId")
    suspend fun markAsSynced(groupId: String, userId: String)

    @Query("SELECT * FROM group_members WHERE is_synced = 0")
    suspend fun getUnsyncedGroupMembers(): List<GroupMember>

    // 🔹 Soft delete
    @Query("UPDATE group_members SET is_deleted = 1, deleted_at = :timestamp WHERE group_id = :groupId AND user_id = :userId")
    suspend fun markAsDeleted(groupId: String, userId: String, timestamp: Long)

    // 🔹 Get all soft-deleted members
    @Query("SELECT * FROM group_members WHERE is_deleted = 1")
    suspend fun getDeletedGroupMembers(): List<GroupMember>

    // 🔹 Permanently delete
    @Query("DELETE FROM group_members WHERE group_id = :groupId AND user_id = :userId")
    suspend fun permanentDelete(groupId: String, userId: String)
}