package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class MemberContributionFire(
    val contributionId: String = "",
    val meetingId: String = "",
    val memberId: String = "",
    val amountContributed: Int = 0,
    val contributionDate: Timestamp = Timestamp.now(), // Changed from String
    val isLate: Boolean = false,
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now(),

    val isDeleted: Boolean = false,

    val deletedAt: Long = 0
)