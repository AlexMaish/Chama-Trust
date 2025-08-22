package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class MonthlySavingFire(
    val savingId: String = "",
    val cycleId: String = "",
    val monthYear: String = "",
    val targetAmount: Int = 0,
    val actualAmount: Int = 0,
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now(),

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)