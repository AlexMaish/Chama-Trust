package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "MonthlySavingEntry",
    foreignKeys = [
        ForeignKey(
            entity = MonthlySaving::class,
            parentColumns = ["saving_id"],
            childColumns = ["saving_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Member::class,
            parentColumns = ["member_id"],
            childColumns = ["member_id"],
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
        Index(value = ["saving_id"]),
        Index(value = ["member_id"]),
        Index(value = ["recorded_by"])
    ]
)
data class MonthlySavingEntry(
    @PrimaryKey
    @ColumnInfo(name = "entry_id")
    val entryId: String,

    @ColumnInfo(name = "saving_id")
    val savingId: String,

    @ColumnInfo(name = "member_id")
    val memberId: String,

    @ColumnInfo(name = "amount")
    val amount: Int,

    @ColumnInfo(name = "entry_date")
    val entryDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "recorded_by")
    val recordedBy: String?,

    @ColumnInfo(name = "group_id")
    val groupId: String,

    @ColumnInfo(name = "isPlaceholder")
    val isPlaceholder: Boolean = false,

    @ColumnInfo(name = "month_year")
    val monthYear: String,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_synced")
    val isSynced: Boolean = false,

    @ColumnInfo(name = "is_deleted")
    val isDeleted: Boolean = false,

    @ColumnInfo(name = "deleted_at")
    val deletedAt: Long = 0
)