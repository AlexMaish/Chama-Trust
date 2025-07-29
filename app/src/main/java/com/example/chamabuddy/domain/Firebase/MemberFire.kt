package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class MemberFire(
    val memberId: String = "",
    val name: String = "",
    val nickname: String? = null,
    val phoneNumber: String = "",
    val profilePicture: String? = null,
    val isAdmin: Boolean = false,
    val isActive: Boolean = true,
    val joinDate: Timestamp = Timestamp.now(),
    val userId: String? = null,
    val groupId: String = "",
    val isOwner: Boolean = false,
    val lastUpdated: Timestamp = Timestamp.now()
)