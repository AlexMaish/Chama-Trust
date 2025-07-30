package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.model.UserGroup
import kotlinx.coroutines.withContext

interface UserRepository {
    suspend fun registerUser(username: String, password: String, phoneNumber: String): Result<User>
    suspend fun loginUser(identifier: String, password: String): Result<User>
    suspend fun joinGroup(userId: String, groupId: String, isOwner: Boolean = false): Result<Unit>
    suspend fun getUserGroups(userId: String): Result<List<String>>
    suspend fun getUserById(userId: String): User?
    suspend fun getCurrentUserId(): String?
    suspend fun setCurrentUserId(userId: String)
    suspend fun getUserName(userId: String): String?
    suspend fun clearCurrentUser()
    suspend fun getUserByPhone(phoneNumber: String): User?

    suspend fun getCurrentUser(): User? {
        val userId = getCurrentUserId()
        return userId?.let { getUserById(it) }
    }

    suspend fun getCurrentUserMemberId(groupId: String): String?

    suspend fun getUnsyncedUsers(): List<User>
    suspend fun markUserSynced(user: User)

    suspend fun getUnsyncedUserGroups(): List<UserGroup>
    suspend fun markUserGroupSynced(userGroup: UserGroup)
}

