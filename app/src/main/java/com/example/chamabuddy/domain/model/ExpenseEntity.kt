package com.example.chamabuddy.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class ExpenseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: String,
    val title: String,
    val description: String,
    val amount: Double,
    val date: Long = System.currentTimeMillis()
)