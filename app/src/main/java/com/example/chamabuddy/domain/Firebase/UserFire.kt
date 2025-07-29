package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class UserFire(
    val userId: String = "",
    val username: String = "",
    val password: String = "",
    val phoneNumber: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
)