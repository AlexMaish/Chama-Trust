package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class MemberContributionFire(
    val contributionId: String = "",
    val meetingId: String = "",
    val memberId: String = "",
    val amountContributed: Int = 0,
    val contributionDate: String = "",
    val isLate: Boolean = false,
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now()
)