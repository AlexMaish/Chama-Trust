package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "group_members",
    primaryKeys = ["group_id", "user_id"],
    foreignKeys = [
        ForeignKey(
            entity = Group::class,
            parentColumns = ["group_id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class GroupMember(
    @ColumnInfo(name = "group_id")
    val groupId: String,

    @ColumnInfo(name = "user_id")
    val userId: String, // Changed from memberId to userId

    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean = false,

    @ColumnInfo(name = "joined_at")
    val joinedAt: Long = System.currentTimeMillis()
)