package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.GroupWithMembers
import com.example.chamabuddy.domain.model.Member

interface GroupRepository {
    suspend fun getGroup(groupId: String): Group?
    suspend fun getGroupsByIds(ids: List<String>): List<Group>
    suspend fun getAllGroups(): List<Group>
    suspend fun getUserGroups(userId: String): List<Group>
    suspend fun findGroupByName(name: String, userId: String): Group?

    suspend fun createGroup(name: String, adminId: String): Group


    suspend fun addMemberToGroup(groupId: String, member: Member)
    suspend fun getGroupWithMembers(groupId: String): GroupWithMembers?
}