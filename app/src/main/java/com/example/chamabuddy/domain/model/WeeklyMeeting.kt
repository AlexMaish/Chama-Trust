package com.example.chamabuddy.domain.model


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


@Entity(
    tableName = "WeeklyMeeting",
    foreignKeys = [
        ForeignKey(
            entity = Cycle::class,
            parentColumns = ["cycle_id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["member_id"],
            childColumns = ["recorded_by"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["cycle_id"]),
        Index(value = ["recorded_by"])
    ]
)
data class WeeklyMeeting(
    @PrimaryKey
    @ColumnInfo(name = "meeting_id")
    val meetingId: String,

    @ColumnInfo(name = "cycle_id")
    val cycleId: String,

    @ColumnInfo(name = "meeting_date")
    val meetingDate: Long,

    @ColumnInfo(name = "total_collected")
    val totalCollected: Int,

    @ColumnInfo(name = "recorded_by")
    val recordedBy: String? = null,

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