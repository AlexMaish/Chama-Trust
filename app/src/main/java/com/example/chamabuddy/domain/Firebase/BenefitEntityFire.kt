package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class BenefitEntityFire(
    val id: String = "",
    val groupId: String = "",
    val name: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now(),

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)