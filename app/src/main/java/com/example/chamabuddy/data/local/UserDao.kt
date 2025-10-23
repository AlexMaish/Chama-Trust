package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Transaction
import com.example.chamabuddy.domain.model.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: User)

    @Update
    suspend fun updateUser(user: User)

    @Transaction
    suspend fun upsertUser(user: User) {
        if (getUserById(user.userId) != null) {
            updateUser(user)
        } else {
            insertUser(user)
        }
    }

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE phone_number = :phone")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT username FROM users WHERE user_id = :userId")
    suspend fun getUserName(userId: String): String?

    @Query("SELECT * FROM users WHERE user_id = :userId")
    suspend fun getUserById(userId: String): User?

    @Query("UPDATE users SET is_synced = 1 WHERE user_id = :userId")
    suspend fun markAsSynced(userId: String)

    @Query("SELECT * FROM users WHERE is_synced = 0")
    suspend fun getUnsyncedUsers(): List<User>


    @Query("UPDATE users SET is_deleted = 1, deleted_at = :timestamp WHERE user_id = :userId")
    suspend fun markAsDeleted(userId: String, timestamp: Long)

    @Query("SELECT * FROM users WHERE is_deleted = 1")
    suspend fun getDeletedUsers(): List<User>

    @Query("DELETE FROM users WHERE user_id = :userId")
    suspend fun permanentDelete(userId: String)

}
