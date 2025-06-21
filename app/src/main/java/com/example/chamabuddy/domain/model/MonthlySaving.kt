package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "MonthlySaving",
    foreignKeys = [
        ForeignKey(
            entity = Cycle::class,
            parentColumns = ["cycle_id"],
            childColumns = ["cycle_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["cycle_id"])
    ]
)
data class MonthlySaving(
    @PrimaryKey
    @ColumnInfo(name = "saving_id")
    val savingId: String,

    @ColumnInfo(name = "cycle_id")
    val cycleId: String,

    @ColumnInfo(name = "month_year")
    val monthYear: String,

    @ColumnInfo(name = "target_amount")
    val targetAmount: Int,

    @ColumnInfo(name = "actual_amount")
    val actualAmount: Int = 0
)