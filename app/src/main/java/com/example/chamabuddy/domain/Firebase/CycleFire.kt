package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class CycleFire(
    val cycleId: String = "",
    val startDate: Timestamp = Timestamp.now(),
    val endDate: Timestamp? = null,
    val weeklyAmount: Int = 0,
    val monthlySavingsAmount: Int = 0,
    val isActive: Boolean = true,
    val totalMembers: Int = 0,
    val totalSavings: Int = 0,
    val groupId: String = "",
    val beneficiariesPerMeeting: Int = 2,
    val cycleNumber: Int = 0,
    val lastUpdated: Timestamp = Timestamp.now(),

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)