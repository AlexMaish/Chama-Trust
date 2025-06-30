package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "user_groups",
    primaryKeys = ["user_id", "group_id"],
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["user_id"],
            childColumns = ["user_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Group::class,
            parentColumns = ["group_id"],
            childColumns = ["group_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("user_id"), Index("group_id")]
)
data class UserGroup(
    @ColumnInfo(name = "user_id") val userId: String,
    @ColumnInfo(name = "group_id") val groupId: String,
    @ColumnInfo(name = "is_owner") val isOwner: Boolean = false,
    @ColumnInfo(name = "joined_at") val joinedAt: Long = System.currentTimeMillis()
)