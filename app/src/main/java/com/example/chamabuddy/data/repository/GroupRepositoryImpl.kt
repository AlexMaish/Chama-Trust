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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
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
            adminName = user.username,
            isSynced = false
        )
        groupDao.insertGroup(group)

        val groupMember = GroupMember(
            groupId = group.groupId,
            userId = adminId,
            isAdmin = true,
            isSynced = false
        )
        groupMemberDao.insert(groupMember)

        val adminMember = Member(
            memberId = UUID.randomUUID().toString(),
            name = user.username,
            phoneNumber = user.phoneNumber,
            isAdmin = true,
            userId = adminId,
            groupId = group.groupId,
            isSynced = false
        )
        memberDao.insertMember(adminMember)

        userGroupDao.insertUserGroup(
            UserGroup(
                userId = adminId,
                groupId = group.groupId,
                isOwner = true,
                isSynced = false
            )
        )

        return group
    }

    @Transaction
    override suspend fun addMemberToGroup(groupId: String, phoneNumber: String) {
        val normalizedPhone = phoneNumber.normalizePhone()
        val user = userRepository.getUserByPhone(normalizedPhone)
            ?: throw Exception("User with phone $normalizedPhone not found")

        userGroupDao.insertUserGroup(
            UserGroup(
                userId = user.userId,
                groupId = groupId,
                isOwner = false,
                isSynced = false
            )
        )

        val groupMember = GroupMember(
            groupId = groupId,
            userId = user.userId,
            isAdmin = false,
            isSynced = false
        )
        groupMemberDao.insert(groupMember)

        val member = Member(
            memberId = UUID.randomUUID().toString(),
            name = user.username,
            phoneNumber = user.phoneNumber,
            isAdmin = false,
            userId = user.userId,
            groupId = groupId,
            isSynced = false
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

override suspend fun getUserGroups(userId: String): List<Group> {
    return withContext(Dispatchers.IO) {
        val groupIds = userGroupDao.getGroupIdsForUser(userId)

        if (groupIds.isNotEmpty()) {
            groupDao.getGroupsByIds(groupIds)
        } else {
            emptyList()
        }
    }
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

    override suspend fun getUnsyncedGroups(): List<Group> = withContext(Dispatchers.IO) {
        groupDao.getUnsyncedGroups()
    }

    override suspend fun markGroupSynced(group: Group) = withContext(Dispatchers.IO) {
        groupDao.markAsSynced(group.groupId)
    }



    override suspend fun getUnsyncedGroupMembers(): List<GroupMember> = withContext(Dispatchers.IO) {
        groupMemberDao.getUnsyncedGroupMembers()
    }

    override suspend fun markGroupMemberSynced(groupMember: GroupMember) = withContext(Dispatchers.IO) {
        groupMemberDao.markAsSynced(groupMember.groupId, groupMember.userId)
    }

    override suspend fun insertGroup(group: Group) = withContext(Dispatchers.IO) {
        groupDao.insertGroup(group)
    }

    override suspend fun updateGroup(group: Group) = withContext(Dispatchers.IO) {
        groupDao.insertGroup(group)
    }


    override suspend fun getUnsyncedGroup(groupId: String): Group? {
        return groupDao.getUnsyncedGroup(groupId)
    }


    override fun getUserGroupsFlow(userId: String): Flow<List<Group>> {
        return groupDao.observeUserGroups(userId)
    }


    override suspend fun markGroupsAsDeleted(groupId: String, timestamp: Long) =
        groupDao.markAsDeleted(groupId, timestamp)

    override suspend fun getDeletedGroups(): List<Group> =
        groupDao.getDeletedGroups()

    override suspend fun permanentDeleteGroups(groupId: String) =
        groupDao.permanentDelete(groupId)

    override suspend fun markAsDeleted(groupId: String, userId: String, timestamp: Long) =
        groupMemberDao.markAsDeleted(groupId, userId, timestamp)

    override suspend fun getDeletedGroupMembers(): List<GroupMember> =
        groupMemberDao.getDeletedGroupMembers()

    override suspend fun permanentDelete(groupId: String, userId: String) =
        groupMemberDao.permanentDelete(groupId, userId)


}