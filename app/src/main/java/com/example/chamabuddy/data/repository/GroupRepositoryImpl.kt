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
import javax.inject.Inject

class GroupRepositoryImpl @Inject constructor(
    private val groupDao: GroupDao,
    private val memberDao: MemberDao,
    private val userGroupDao: UserGroupDao,
    private val userRepository: UserRepository,
    private val groupMemberDao: GroupMemberDao
) : GroupRepository {

    @Transaction
    override suspend fun createGroup(name: String, adminId: String): Group {
        val user = userRepository.getUserById(adminId)
            ?: throw Exception("User not found")

        val group = Group(
            name = name,
            adminId = adminId,
            adminName = user.username
        )
        groupDao.insertGroup(group)

        // Create GroupMember entry for admin
        val groupMember = GroupMember(
            groupId = group.groupId,
            userId = adminId,
            isAdmin = true
        )
        groupMemberDao.insert(groupMember)

        // Create regular member entry for admin
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

    @Transaction
    override suspend fun addMemberToGroup(groupId: String, phoneNumber: String) {
        val normalizedPhone = phoneNumber.normalizePhone()
        val user = userRepository.getUserByPhone(normalizedPhone)
            ?: throw Exception("User with phone $normalizedPhone not found")

        // Add user-group association FIRST
        userGroupDao.insertUserGroup(
            UserGroup(
                userId = user.userId,
                groupId = groupId,
                isOwner = false
            )
        )

        // Then create GroupMember entry
        val groupMember = GroupMember(
            groupId = groupId,
            userId = user.userId,
            isAdmin = false
        )
        groupMemberDao.insert(groupMember)

        // Create regular member entry
        val member = Member(
            memberId = UUID.randomUUID().toString(),
            name = user.username,
            phoneNumber = user.phoneNumber,
            isAdmin = false,
            userId = user.userId,
            groupId = groupId
        )
        memberDao.insertMember(member)
    }
    private fun String.normalizePhone(): String {
        return this.replace(Regex("[^0-9]"), "").trim()
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
//
//    override suspend fun getUserGroups(userId: String): List<Group> {
//        val groupIds = userGroupDao.getGroupIdsForUser(userId)
//        return groupDao.getGroupsByIds(groupIds)
//    }
override suspend fun getUserGroups(userId: String): List<Group> {
    return userGroupDao.getUserGroupsWithDetails(userId)
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
        return userGroupDao.getGroupsForUserByPhone(phoneNumber.normalizePhone())
    }

    override suspend fun getGroupById(groupId: String): Group? {
        return groupDao.getGroup(groupId)
    }
}