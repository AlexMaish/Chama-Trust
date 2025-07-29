package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class GroupFire(
    val groupId: String = "",
    val name: String = "",
    val adminId: String = "",
    val adminName: String? = null,
    val createdAt: Timestamp = Timestamp.now(),
    val totalSavings: Double = 0.0,
    val lastUpdated: Timestamp = Timestamp.now()
)
