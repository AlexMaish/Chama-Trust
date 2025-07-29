package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.GroupWithMembers
import com.example.chamabuddy.domain.model.Member

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

}