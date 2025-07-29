package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class ExpenseEntityFire(
    val id: Long = 0,
    val groupId: String = "",
    val title: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val date: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
)