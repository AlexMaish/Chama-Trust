package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.GroupMember
import com.example.chamabuddy.domain.model.GroupWithMembers
import kotlinx.coroutines.flow.Flow


@Dao
interface GroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group)

    @Query("SELECT * FROM `groups` WHERE group_id = :groupId")
    suspend fun getGroup(groupId: String): Group?

    @Delete
    suspend fun deleteGroup(group: Group)

    @Query("SELECT * FROM `groups`")
    suspend fun getAllGroups(): List<Group>

    @Query("SELECT * FROM `groups` WHERE group_id IN (:ids)")
    suspend fun getGroupsByIds(ids: List<String>): List<Group>

    @Query("SELECT * FROM `groups` WHERE name = :name AND admin_id = :userId")
    suspend fun findGroupByName(name: String, userId: String): Group?

    @Transaction
    @Query("SELECT * FROM `groups` WHERE group_id = :groupId")
    suspend fun getGroupWithMembers(groupId: String): GroupWithMembers?

    @Query("UPDATE `groups` SET is_synced = 1 WHERE group_id = :groupId")
    suspend fun markAsSynced(groupId: String)

    @Query("SELECT * FROM `groups`WHERE is_synced = 0")
    suspend fun getUnsyncedGroups(): List<Group>


    @Query("SELECT * FROM `groups` WHERE group_id = :groupId AND is_synced = 0")
    suspend fun getUnsyncedGroup(groupId: String): Group?

    @Query("SELECT * FROM `groups` WHERE group_id  IN (SELECT group_id FROM user_groups WHERE user_id = :userId)")
    fun observeUserGroups(userId: String): Flow<List<Group>>


    @Query("UPDATE `groups` SET is_deleted = 1, deleted_at = :timestamp WHERE group_id = :groupId")
    suspend fun markAsDeleted(groupId: String, timestamp: Long)

    @Query("SELECT * FROM `groups` WHERE is_deleted = 1")
    suspend fun getDeletedGroups(): List<Group>

    @Query("DELETE FROM `groups`WHERE group_id = :groupId")
    suspend fun permanentDelete(groupId: String)
}