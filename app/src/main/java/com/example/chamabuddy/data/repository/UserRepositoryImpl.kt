package com.example.chamabuddy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
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
            // Check for existing user
            if (userDao.getUserByUsername(username) != null) {
                throw Exception("Username already taken")
            }
            if (userDao.getUserByPhone(phoneNumber) != null) {
                throw Exception("Phone number already registered")
            }

            // Create new user
            val user = User(
                username = username,
                password = password,
                phoneNumber = phoneNumber
            )
            userDao.insertUser(user)
            setCurrentUserId(user.userId)
            user
        }
    }
    override suspend fun getUserByPhone(phoneNumber: String): User? {
        val normalizedPhone = phoneNumber.replace(Regex("[^0-9]"), "")
        return withContext(Dispatchers.IO) {
            userDao.getUserByPhone(normalizedPhone)
        }
    }
    override suspend fun loginUser(
        identifier: String,
        password: String
    ): Result<User> = withContext(dispatcher) {
        runCatching {
            val user = userDao.getUserByUsername(identifier)
                ?: userDao.getUserByPhone(identifier)
                ?: throw IllegalArgumentException("User not found")

            if (user.password != password) throw IllegalArgumentException("Invalid credentials")
            setCurrentUserId(user.userId) // Set as current user
            user
        }
    }

    override suspend fun getUserName(userId: String): String? {
        return userDao.getUserByUsername(userId)?.username
    }


    override suspend fun joinGroup(
        userId: String,
        groupId: String,
        isOwner: Boolean
    ): Result<Unit> = withContext(dispatcher) {
        runCatching {
            val ug = UserGroup(userId = userId, groupId = groupId, isOwner = isOwner)
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
                userGroupDao.getUserGroups(userId)
            }
        }

    override suspend fun getCurrentUserMemberId(groupId: String): String? {
        val userId = getCurrentUserId() ?: return null
        val user = getCurrentUser() ?: return null

        return withContext(Dispatchers.IO) {
            // 1. Try by user ID
            memberRepository.getMemberByUserId(userId, groupId)?.memberId
            // 2. Try by phone number with normalization
                ?: memberRepository.getMemberByPhoneForGroup(user.phoneNumber, groupId)?.memberId
        }
    }
    fun String.normalizePhone(): String {
        return this.replace(Regex("[^0-9]"), "").trim()
    }
    override suspend fun getCurrentUser(): User? {
        val userId = getCurrentUserId()
        return userId?.let { getUserById(it) }
    }

}

