package com.example.chamabuddy.domain.model

import androidx.room.Index
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
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
    val entryDate: String = System.currentTimeMillis().toString(),

    @ColumnInfo(name = "recorded_by")
    val recordedBy: String?,

    @ColumnInfo(name = "group_id")
    val groupId: String,



    @ColumnInfo(name = "isPlaceholder")
    val isPlaceholder: Boolean = false


    )