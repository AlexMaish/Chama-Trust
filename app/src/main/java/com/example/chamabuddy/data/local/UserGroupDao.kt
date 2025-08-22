package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.UserGroup

@Dao
interface UserGroupDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserGroup(userGroup: UserGroup)

    @Query("SELECT * FROM user_groups WHERE user_id = :userId AND group_id = :groupId LIMIT 1")
    suspend fun getUserGroup(userId: String, groupId: String): UserGroup?

    @Query("SELECT group_id FROM user_groups WHERE user_id = :userId")
    suspend fun getGroupIdsForUser(userId: String): List<String>

    // Add this function to fix the reference
    @Query("SELECT group_id FROM user_groups WHERE user_id = :userId")
    suspend fun getUserGroups(userId: String): List<String>

//    @Transaction
//    @Query("SELECT * FROM `groups` WHERE group_id IN (SELECT group_id FROM user_groups WHERE user_id = :userId)")
//    suspend fun getUserGroupsWithDetails(userId: String): List<Group>

    @Query("SELECT * FROM `groups` WHERE group_id IN " +
            "(SELECT group_id FROM user_groups WHERE user_id = " +
            "(SELECT user_id FROM users WHERE phone_number = :phoneNumber))")
    suspend fun getGroupsForUserByPhone(phoneNumber: String): List<Group>


    @Transaction
    @Query("SELECT * FROM `groups` WHERE group_id IN " +
            "(SELECT group_id FROM user_groups WHERE user_id = :userId)")
    suspend fun getUserGroupsWithDetails(userId: String): List<Group>


    @Query("UPDATE user_groups SET is_synced = 1 WHERE user_id = :userId AND group_id = :groupId")
    suspend fun markAsSynced(userId: String, groupId: String)

    @Query("SELECT * FROM user_groups WHERE is_synced = 0")
    suspend fun getUnsyncedUserGroups(): List<UserGroup>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(userGroup: UserGroup)

    @Update
    suspend fun update(userGroup: UserGroup)



    // 🔹 Soft delete
    @Query("UPDATE user_groups SET is_deleted = 1, deleted_at = :timestamp WHERE user_id = :userId AND group_id = :groupId")
    suspend fun markAsDeleted(userId: String, groupId: String, timestamp: Long)

    // 🔹 Get all soft-deleted user groups
    @Query("SELECT * FROM user_groups WHERE is_deleted = 1")
    suspend fun getDeletedUserGroups(): List<UserGroup>

    // 🔹 Permanently delete
    @Query("DELETE FROM user_groups WHERE user_id = :userId AND group_id = :groupId")
    suspend fun permanentDelete(userId: String, groupId: String)

}