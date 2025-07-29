package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class UserGroupFire(
    val userId: String = "",
    val groupId: String = "",
    val isOwner: Boolean = false,
    val joinedAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
)