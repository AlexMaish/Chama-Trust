package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class PenaltyFire(
    val id: String = "",
    val groupId: String = "",
    val memberName: String = "",
    val description: String = "",
    val amount: Double = 0.0,
    val memberId : String= "",
    val date: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
)
