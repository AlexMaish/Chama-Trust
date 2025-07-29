package com.example.chamabuddy.domain.model


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*


@Entity(tableName = "Member")
data class Member(
    @PrimaryKey
    @ColumnInfo(name = "member_id")
    val memberId: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "nickname")
    val nickname: String? = null,

    @ColumnInfo(name = "phone_number")
    val phoneNumber: String,

    @ColumnInfo(name = "profile_picture")
    val profilePicture: String? = null,

    @ColumnInfo(name = "is_admin")
    val isAdmin: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "join_date")
    val joinDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "user_id")
    val userId: String? = null, // Tracks associated user account

    @ColumnInfo(name = "group_id")
    val groupId: String,


    @ColumnInfo(name = "is_owner")
    val isOwner: Boolean = false,


    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false

)
