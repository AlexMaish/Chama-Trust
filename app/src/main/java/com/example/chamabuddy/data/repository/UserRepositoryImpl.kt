package com.example.chamabuddy.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.chamabuddy.data.local.UserDao
import com.example.chamabuddy.data.local.UserGroupDao
import com.example.chamabuddy.data.remote.toFirebase
import com.example.chamabuddy.data.remote.toLocal
import com.example.chamabuddy.domain.Firebase.UserFire
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.model.UserGroup
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.UserRepository
import com.example.chamabuddy.util.EncryptionHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.tasks.await

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_settings")

class UserRepositoryImpl @Inject constructor(
    private val userDao: UserDao,
    private val userGroupDao: UserGroupDao,
    private val memberRepository: MemberRepository,
    private val firebaseAuth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : UserRepository {
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
    private val dataStore = context.dataStore
    private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")
    private val encryptionHelper = EncryptionHelper(context)

    override suspend fun setCurrentUserId(userId: String) {
        dataStore.edit { preferences ->
            preferences[CURRENT_USER_ID] = encryptionHelper.encrypt(userId)
        }
    }

    override suspend fun getCurrentUserId(): String? {
        val encrypted = dataStore.data.map { it[CURRENT_USER_ID] }.firstOrNull()
        return encrypted?.let {
            encryptionHelper.decrypt(it)
        } ?: run {
            // Clear corrupted data
            dataStore.edit { it.remove(CURRENT_USER_ID) }
            null
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
        try {
            val normalizedPhone = phoneNumber.normalizePhone()

            if (isPhoneRegisteredInFirebase(normalizedPhone)) {
                throw Exception("Phone number already registered")
            }

            val authResult = firebaseAuth.createUserWithEmailAndPassword(
                "$normalizedPhone@chamabuddy.com", password
            ).await()

            val firebaseUser = authResult.user ?: throw Exception("Firebase user creation failed")

            val user = User(
                userId = firebaseUser.uid,
                username = username,
                password = password,
                phoneNumber = normalizedPhone,
                isSynced = true
            )

            userDao.insertUser(user)
            setCurrentUserId(user.userId)

            firestore.collection("users").document(user.userId)
                .set(user.toFirebase())
                .await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun loginUser(
        identifier: String,
        password: String
    ): Result<User> = withContext(dispatcher) {
        try {
            val normalizedIdentifier = identifier.normalizePhone()
            val user = userDao.getUserByUsername(identifier)
                ?: userDao.getUserByPhone(normalizedIdentifier)
                ?: throw IllegalArgumentException("User not found locally")

            if (user.password != password) throw IllegalArgumentException("Invalid credentials")
            setCurrentUserId(user.userId)
            Result.success(user)
        } catch (localError: Exception) {
            loginWithFirebase(identifier, password)
        }
    }

    override suspend fun loginWithFirebase(identifier: String, password: String): Result<User> {
        return try {
            val normalizedPhone = identifier.normalizePhone()
            val email = "$normalizedPhone@chamabuddy.com"

            val authResult = firebaseAuth.signInWithEmailAndPassword(email, password).await()
            val firebaseUser = authResult.user ?: throw Exception("Firebase authentication failed")

            var user = userDao.getUserById(firebaseUser.uid)

            if (user == null) {
                val snapshot = firestore.collection("users").document(firebaseUser.uid).get().await()
                user = snapshot.toObject(UserFire::class.java)?.toLocal()
                user?.let {
                    userDao.insertUser(it)
                } ?: throw Exception("User not found in Firestore")
            }

            setCurrentUserId(user.userId)
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun isPhoneRegisteredInFirebase(phone: String): Boolean {
        val query = firestore.collection("users")
            .whereEqualTo("phoneNumber", phone)
            .limit(1)
            .get()
            .await()
        return !query.isEmpty
    }

    override suspend fun getUserByPhone(phoneNumber: String): User? {
        return userDao.getUserByPhone(phoneNumber.normalizePhone())
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
            val ug = UserGroup(userId = userId, groupId = groupId, isOwner = isOwner, isSynced = false)
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

    private fun String.normalizePhone(): String {
        return this.replace(Regex("[^0-9]"), "").trim()
    }


    override suspend fun insertUser(user: User) = userDao.insertUser(user)
    override suspend fun updateUser(user: User) = userDao.updateUser(user)

}
