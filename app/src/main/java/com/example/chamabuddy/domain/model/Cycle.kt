package com.example.chamabuddy.domain.model


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Cycle")
data class Cycle(
    @PrimaryKey
    @ColumnInfo(name = "cycle_id")
    val cycleId: String,

    @ColumnInfo(name = "start_date")
    val startDate: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "end_date")
    val endDate: Long? = null,


    @ColumnInfo(name = "weekly_amount")
    val weeklyAmount: Int,

    @ColumnInfo(name = "monthly_savings_amount")
    val monthlySavingsAmount: Int,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "total_members")
    val totalMembers: Int,
    var totalSavings: Int
)