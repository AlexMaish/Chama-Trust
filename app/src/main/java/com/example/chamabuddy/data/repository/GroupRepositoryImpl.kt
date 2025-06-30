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
            adminName = user.username ?: "Admin"
        )
        groupDao.insertGroup(group)

        // Add creator as admin member
        val adminMember = Member(
            memberId = adminId,
            name = user.username ?: "Admin",
            phoneNumber = user.phoneNumber,
            isAdmin = true,
            groupId = group.groupId
        )
        memberDao.insertMember(adminMember)

        // Add user-group association - THIS IS CRITICAL
        userGroupDao.insertUserGroup(
            UserGroup(
                userId = adminId,
                groupId = group.groupId,
                isOwner = true
            )
        )

        return group
    }

    override suspend fun addMemberToGroup(groupId: String, member: Member) {
        // Set groupId directly in member
        val memberWithGroup = member.copy(groupId = groupId)
        memberDao.insertMember(memberWithGroup)
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
}