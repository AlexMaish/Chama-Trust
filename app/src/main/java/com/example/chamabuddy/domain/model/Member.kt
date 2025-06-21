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

    @ColumnInfo(name = "current_status")
    val currentStatus: String = MemberStatus.ACTIVE.toString(),

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

//    @ColumnInfo(name = "join_date")
//    val joinDate: Date = Date(),

    @ColumnInfo(name = "join_date")
    val joinDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "total_contributions")
    val totalContributions: Double = 0.0,

    @ColumnInfo(name = "total_savings")
    val totalSavings: Double = 0.0,

    @ColumnInfo(name = "benefited")
    val benefited: Boolean = false
)

@ColumnInfo(name = "join_date")
val joinDate: Long = System.currentTimeMillis()


@ColumnInfo(name = "profile_picture")
val profilePicture: String? = null

enum class MemberStatus {
    ACTIVE, INACTIVE
}