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

    @Query("SELECT * FROM 'groups' WHERE name = :name AND admin_id = :userId")
    suspend fun findGroupByName(name: String, userId: String): Group?



    @Transaction
    @Query("SELECT * FROM 'groups'  WHERE group_id = :groupId")
    suspend fun getGroupWithMembers(groupId: String): GroupWithMembers?
}

