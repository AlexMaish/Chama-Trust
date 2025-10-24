package com.example.chamabuddy.domain.model


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index


@Entity(
    tableName = "MemberContribution",
    foreignKeys = [
        ForeignKey(
            entity = WeeklyMeeting::class,
            parentColumns = ["meeting_id"],
            childColumns = ["meeting_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["member_id"],
            childColumns = ["member_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["meeting_id"]),
        Index(value = ["member_id"])
    ]
)
data class MemberContribution(
    @PrimaryKey
    @ColumnInfo(name = "contribution_id")
    val contributionId: String,

    @ColumnInfo(name = "meeting_id")
    val meetingId: String,

    @ColumnInfo(name = "member_id")
    val memberId: String,

    @ColumnInfo(name = "amount_contributed")
    val amountContributed: Int,

    @ColumnInfo(name = "contribution_date")
    val contributionDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_late")
    val isLate: Boolean = false,

    @ColumnInfo(name = "group_id")
    val groupId: String,


    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long = 0
)