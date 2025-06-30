package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.User

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: User)

    @Query("SELECT * FROM users WHERE username = :username")
    suspend fun getUserByUsername(username: String): User?

    @Query("SELECT * FROM users WHERE phone_number = :phone")
    suspend fun getUserByPhone(phone: String): User?

    @Query("SELECT username FROM users WHERE user_id = :userId")
    suspend fun getUserName(userId: String): String?


        @Query("SELECT * FROM users WHERE user_id = :userId")
        suspend fun getUserById(userId: String): User?
}