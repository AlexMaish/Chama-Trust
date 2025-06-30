package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.GroupDao
import com.example.chamabuddy.data.local.UserDao
import com.example.chamabuddy.data.local.UserGroupDao
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.model.UserGroup
import com.example.chamabuddy.domain.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

import android.content.Context
import com.example.chamabuddy.domain.repository.GroupRepository
import dagger.hilt.android.qualifiers.ApplicationContext

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userGroupDao: UserGroupDao,
    @ApplicationContext private val context: Context, // Inject context
) : UserRepository {

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    private val sharedPrefs = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    override suspend fun getCurrentUserId(): String? = withContext(dispatcher) {
        sharedPrefs.getString("CURRENT_USER_ID", null)
    }


    override suspend fun setCurrentUserId(userId: String) = withContext(dispatcher) {
        sharedPrefs.edit()
            .putString("CURRENT_USER_ID", userId)
            .apply()
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

    override suspend fun getUserById(userId: String): User? =
        withContext(Dispatchers.IO) {
            userDao.getUserById(userId)
        }
    override suspend fun getUserGroups(userId: String): Result<List<String>> =
        withContext(dispatcher) {
            runCatching {
                userGroupDao.getUserGroups(userId)
            }
        }

}
