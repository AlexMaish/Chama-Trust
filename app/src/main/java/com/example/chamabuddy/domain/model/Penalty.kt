package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "penalties")
data class Penalty(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val penaltyId: String = "",
    val groupId: String,
    val memberName: String,
    val memberId: String,
    val description: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "last_updated") val lastUpdated: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "is_synced") val isSynced: Boolean = false
)
