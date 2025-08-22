package com.example.chamabuddy.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "welfares")
data class Welfare(
    @PrimaryKey val welfareId: String = UUID.randomUUID().toString(),
    val groupId: String,
    val name: String,
    val amount: Int,
    val createdBy: String,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)