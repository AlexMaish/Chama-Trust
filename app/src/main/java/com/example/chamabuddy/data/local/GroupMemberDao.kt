package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.chamabuddy.domain.model.GroupMember
@Dao
interface GroupMemberDao {
    @Insert
    suspend fun insert(groupMember: GroupMember)

    @Query("DELETE FROM group_members WHERE group_id = :groupId AND user_id = :userId")
    suspend fun removeMember(groupId: String, userId: String)

    @Query("SELECT * FROM group_members WHERE group_id = :groupId")
    suspend fun getGroupMembers(groupId: String): List<GroupMember>

    @Query("UPDATE group_members SET is_admin = :isAdmin WHERE group_id = :groupId AND user_id = :userId")
    suspend fun updateAdminStatus(groupId: String, userId: String, isAdmin: Boolean)

    @Query("SELECT group_id FROM group_members WHERE user_id = :userId")
    suspend fun getGroupIdsByUserId(userId: String): List<String>

    @Query("SELECT group_id FROM group_members WHERE user_id = :userId")
    suspend fun getGroupsForUser(userId: String): List<String>
}