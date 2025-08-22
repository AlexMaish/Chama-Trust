package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.GroupMember
import com.example.chamabuddy.domain.model.GroupWithMembers
import com.example.chamabuddy.domain.model.Member
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

interface GroupRepository {
    suspend fun getGroup(groupId: String): Group?
    suspend fun getAllGroups(): List<Group>
    suspend fun getUserGroups(userId: String): List<Group>
    suspend fun findGroupByName(name: String, userId: String): Group?

    suspend fun createGroup(name: String, adminId: String): Group


    suspend fun getGroupWithMembers(groupId: String): GroupWithMembers?


    suspend fun getGroupsByIds(groupIds: List<String>): List<Group>
    suspend fun getUserGroupsWithDetails(userId: String): List<Group>


    suspend fun getUserGroupsByPhone(phoneNumber: String): List<Group>

    suspend fun addMemberToGroup(groupId: String, phoneNumber: String)

    suspend fun getGroupById(groupId: String): Group?

    suspend fun getUnsyncedGroups(): List<Group>
    suspend fun markGroupSynced(group: Group)

    suspend fun getUnsyncedGroupMembers(): List<GroupMember>
    suspend fun markGroupMemberSynced(groupMember: GroupMember)

    suspend fun insertGroup(group: Group)
    suspend fun updateGroup(group: Group)

    suspend fun getUnsyncedGroup(groupId: String): Group?

    fun getUserGroupsFlow(userId: String): Flow<List<Group>>

    suspend fun markGroupsAsDeleted(groupId: String, timestamp: Long)
    suspend fun getDeletedGroups(): List<Group>
    suspend fun permanentDeleteGroups(groupId: String)


    suspend fun markAsDeleted(groupId: String, userId: String, timestamp: Long)
    suspend fun getDeletedGroupMembers(): List<GroupMember>
    suspend fun permanentDelete(groupId: String, userId: String)
}


