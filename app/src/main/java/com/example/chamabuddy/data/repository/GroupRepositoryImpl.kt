package com.example.chamabuddy.data.repository

import androidx.room.Transaction
import com.example.chamabuddy.data.local.GroupDao
import com.example.chamabuddy.data.local.GroupMemberDao
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.UserGroupDao
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.GroupMember
import com.example.chamabuddy.domain.model.GroupWithMembers
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.UserGroup
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.UserRepository
import java.util.UUID

class GroupRepositoryImpl(
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    private val userGroupDao: UserGroupDao,
    private val userRepository : UserRepository

) : GroupRepository {


    override suspend fun createGroup(name: String, adminId: String): Group {
        val user = userRepository.getUserById(adminId)
            ?: throw Exception("User not found")

        val group = Group(
            name = name,
            adminId = adminId,
            adminName = user.username
        )
        groupDao.insertGroup(group)

        // Add creator as admin member
        val adminMember = Member(
            memberId = UUID.randomUUID().toString(),
            name = user.username,
            phoneNumber = user.phoneNumber,
            isAdmin = true,
            userId = adminId,
            groupId = group.groupId
        )
        memberDao.insertMember(adminMember)

        // Add user-group association
        userGroupDao.insertUserGroup(
            UserGroup(
                userId = adminId,
                groupId = group.groupId,
                isOwner = true
            )
        )

        return group
    }
    // In GroupRepositoryImpl
    override suspend fun addMemberToGroup(groupId: String, phoneNumber: String) {
        // 1. Check if user exists in the global user database (simulated)
        val user = userRepository.getUserByPhone(phoneNumber)
            ?: throw Exception("User not found")

        // 2. Create member entry
        val member = Member(
            memberId = UUID.randomUUID().toString(),
            name = user.username,
            phoneNumber = phoneNumber,
            isAdmin = false,
            userId = user.userId,
            groupId = groupId
        )

        // 3. Add to both tables
        memberDao.insertMember(member)
        userGroupDao.insertUserGroup(
            UserGroup(
                userId = user.userId,
                groupId = groupId,
                isOwner = false
            )
        )
    }

    override suspend fun getGroup(groupId: String): Group? {
        return groupDao.getGroup(groupId)
    }


    override suspend fun getGroupsByIds(ids: List<String>): List<Group> {
        return groupDao.getGroupsByIds(ids)
    }


    override suspend fun getAllGroups(): List<Group> {
        return groupDao.getAllGroups()
    }


    override suspend fun getUserGroups(userId: String): List<Group> {
        // Get group IDs for the user
        val groupIds = userGroupDao.getGroupsForUser(userId)

        // Get groups by their IDs
        return groupDao.getGroupsByIds(groupIds)
    }


    @Transaction
    override suspend fun getGroupWithMembers(groupId: String): GroupWithMembers? {
        return groupDao.getGroupWithMembers(groupId)
    }

    override suspend fun findGroupByName(name: String, userId: String): Group? {
        return groupDao.findGroupByName(name, userId)
    }

    override suspend fun getUserGroupsWithDetails(userId: String): List<Group> {
        return userGroupDao.getUserGroupsWithDetails(userId)
    }

    override suspend fun getUserGroupsByPhone(phoneNumber: String): List<Group> {
        return userGroupDao.getGroupsForUserByPhone(phoneNumber)
    }
}