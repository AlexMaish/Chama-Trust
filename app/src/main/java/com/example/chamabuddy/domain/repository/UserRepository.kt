package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.model.UserGroup

interface UserRepository {

    suspend fun registerUser(username: String, password: String, phoneNumber: String): Result<User>

    suspend fun loginUser(identifier: String, password: String): Result<User>

    suspend fun joinGroup(userId: String, groupId: String, isOwner: Boolean = false): Result<Unit>

    suspend fun getUserGroups(userId: String): Result<List<String>>

    suspend fun getUserById(userId: String): User?

    suspend fun getUserByPhone(phoneNumber: String): User?

    suspend fun getUserName(userId: String): String?

    suspend fun getCurrentUserId(): String?

    suspend fun setCurrentUserId(userId: String)

    suspend fun clearCurrentUser()

    suspend fun getCurrentUser(): User?

    suspend fun getCurrentUserMemberId(groupId: String): String?

    suspend fun getUnsyncedUsers(): List<User>

    suspend fun markUserSynced(user: User)

    suspend fun getUnsyncedUserGroups(): List<UserGroup>

    suspend fun markUserGroupSynced(userGroup: UserGroup)

    suspend fun insertUser(user: User)
    suspend fun updateUser(user: User)

    suspend fun upsertUser(user: User)
    suspend fun syncUser(user: User)

    suspend fun loginWithFirebase(identifier: String, password: String): Result<User>

    suspend fun insertUserGroup(userGroup: UserGroup)
    suspend fun updateUserGroup(userGroup: UserGroup)
    suspend fun getUserGroup(userId: String, groupId: String): UserGroup?
    suspend fun isUserInGroup(userId: String, groupId: String): Boolean

    suspend fun markUserAsDeleted(userId: String, timestamp: Long)
    suspend fun getDeletedUsers(): List<User>
    suspend fun permanentDeleteUser(userId: String)


    suspend fun markUserGroupAsDeleted(userId: String, groupId: String, timestamp: Long)
    suspend fun getDeletedUserGroups(): List<UserGroup>
    suspend fun permanentDeleteUserGroup(userId: String, groupId: String)
    suspend fun ensureFirebaseAuthSignIn(identifier: String, password: String)

    suspend fun changePassword(userId: String, oldPassword: String, newPassword: String): Result<Unit>
}
