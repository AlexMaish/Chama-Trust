package com.example.chamabuddy.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "penalties")
data class Penalty(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val groupId: String,
    val memberName: String,
    val description: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis()
)