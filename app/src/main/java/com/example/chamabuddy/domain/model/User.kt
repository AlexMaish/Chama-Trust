package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "users")
data class User(
    @PrimaryKey
    @ColumnInfo(name = "user_id")
    val userId: String = UUID.randomUUID().toString(),

    val username: String,

    val password: String,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),


    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false
) {
    init {
        require(username.length >= 4) { "Username must be at least 4 characters" }
        require(password.length >= 6) { "Password must be at least 6 characters" }
        require(phoneNumber.matches(Regex("^[0-9]{10,15}\$"))) {
            "Invalid phone number (10-15 digits)"
        }
    }
}