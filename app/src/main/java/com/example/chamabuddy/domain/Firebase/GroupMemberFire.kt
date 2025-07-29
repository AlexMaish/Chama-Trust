package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class GroupMemberFire(
    val groupId: String = "",
    val userId: String = "",
    val isAdmin: Boolean = false,
    val joinedAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now()
)
