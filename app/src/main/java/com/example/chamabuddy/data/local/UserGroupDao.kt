package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.UserGroup

// UserGroupDao.kt
@Dao
interface UserGroupDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUserGroup(userGroup: UserGroup)


    @Query("SELECT * FROM user_groups WHERE user_id = :userId AND group_id = :groupId LIMIT 1")
    suspend fun getUserGroup(userId: String, groupId: String): UserGroup?

    @Query("SELECT group_id FROM user_groups WHERE user_id = :userId")
    suspend fun getGroupsForUser(userId: String): List<String>

    @Query("SELECT group_id FROM user_groups WHERE user_id = :userId")
    suspend fun getUserGroups(userId: String): List<String>

    @Transaction
    @Query("SELECT * FROM `groups` WHERE group_id IN (SELECT group_id FROM user_groups WHERE user_id = :userId)")
    suspend fun getUserGroupsWithDetails(userId: String): List<Group>


    @Query("SELECT * FROM `groups` WHERE group_id IN " +
            "(SELECT group_id FROM user_groups WHERE user_id = " +
            "(SELECT user_id FROM users WHERE phone_number = :phoneNumber))")
    suspend fun getGroupsForUserByPhone(phoneNumber: String): List<Group>
}