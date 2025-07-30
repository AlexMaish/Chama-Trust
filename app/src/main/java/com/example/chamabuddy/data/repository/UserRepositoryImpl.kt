package com.example.chamabuddy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.room.Query
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.UserDao
import com.example.chamabuddy.data.local.UserGroupDao
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.model.UserGroup
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.UserRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userGroupDao: UserGroupDao,
    private val memberRepository: MemberRepository,
    @ApplicationContext private val context: Context
) : UserRepository {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val dataStore = context.dataStore
    private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")

    override suspend fun getCurrentUserId(): String? {
        return dataStore.data.map { preferences ->
            preferences[CURRENT_USER_ID]
        }.firstOrNull()
    }

    override suspend fun setCurrentUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID] = userId
        }
    }

    override suspend fun clearCurrentUser() {
        dataStore.edit { preferences ->
            preferences.remove(CURRENT_USER_ID)
        }
    }

    override suspend fun registerUser(
        username: String,
        password: String,
        phoneNumber: String
    ): Result<User> = withContext(dispatcher) {
        runCatching {
            // Normalize phone number for consistency
            val normalizedPhone = phoneNumber.normalizePhone()

            // Check for existing user using normalized phone
            if (userDao.getUserByUsername(username) != null) {
                throw Exception("Username already taken")
            }
            if (userDao.getUserByPhone(normalizedPhone) != null) {
                throw Exception("Phone number already registered")
            }

            // Create new user with normalized phone
            val user = User(
                username = username,
                password = password,
                phoneNumber = normalizedPhone,
                isSynced = false
            )
            userDao.insertUser(user)
            setCurrentUserId(user.userId)
            user
        }
    }

    override suspend fun getUserByPhone(phoneNumber: String): User? {
        val normalized = phoneNumber.normalizePhone()
        return userDao.getUserByPhone(normalized)
    }

    override suspend fun loginUser(
        identifier: String,
        password: String
    ): Result<User> = withContext(dispatcher) {
        runCatching {
            // Normalize identifier if it's a phone number
            val normalizedIdentifier = if (identifier.contains(Regex("[^0-9]"))) {
                identifier.normalizePhone()
            } else {
                identifier
            }

            val user = userDao.getUserByUsername(identifier)
                ?: userDao.getUserByPhone(normalizedIdentifier)
                ?: throw IllegalArgumentException("User not found")

            if (user.password != password) throw IllegalArgumentException("Invalid credentials")
            setCurrentUserId(user.userId)
            user
        }
    }

    override suspend fun getUserName(userId: String): String? {
        return userDao.getUserName(userId)
    }

    override suspend fun joinGroup(
        userId: String,
        groupId: String,
        isOwner: Boolean
    ): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val ug = UserGroup(userId = userId, groupId = groupId, isOwner = isOwner,    isSynced = false)
            userGroupDao.insertUserGroup(ug)
        }
    }

    override suspend fun getUserById(userId: String): User? {
        return withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    }

    override suspend fun getUserGroups(userId: String): Result<List<String>> =
        withContext(dispatcher) {
            runCatching {
                userGroupDao.getGroupIdsForUser(userId)
            }
        }

    override suspend fun getCurrentUserMemberId(groupId: String): String? {
        val userId = getCurrentUserId() ?: return null
        val user = getCurrentUser() ?: return null

        return withContext(Dispatchers.IO) {
            memberRepository.getMemberByUserId(userId, groupId)?.memberId
                ?: memberRepository.getMemberByPhoneForGroup(
                    user.phoneNumber.normalizePhone(),
                    groupId
                )?.memberId
        }
    }

    private fun String.normalizePhone(): String {
        return this.replace(Regex("[^0-9]"), "").trim()
    }

    override suspend fun getCurrentUser(): User? {
        val userId = getCurrentUserId()
        return userId?.let { getUserById(it) }
    }


    override suspend fun getUnsyncedUsers(): List<User> = withContext(dispatcher) {
        userDao.getUnsyncedUsers()
    }

    override suspend fun markUserSynced(user: User) = withContext(dispatcher) {
        userDao.markAsSynced(user.userId)
    }



    override suspend fun getUnsyncedUserGroups(): List<UserGroup> = withContext(dispatcher) {
        userGroupDao.getUnsyncedUserGroups()
    }

    override suspend fun markUserGroupSynced(userGroup: UserGroup) = withContext(dispatcher) {
        userGroupDao.markAsSynced(userGroup.userId, userGroup.groupId)
    }

}
