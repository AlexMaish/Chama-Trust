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
    val contributionDate: String = System.currentTimeMillis().toString(),

    @ColumnInfo(name = "is_late")
    val isLate: Boolean = false
)