package com.example.chamabuddy.domain.Firebase


import com.google.firebase.Timestamp
import java.util.*

data class MemberWelfareContributionFire(
    val contributionId: String = "",
    val meetingId: String = "",
    val memberId: String = "",
    val amountContributed: Int = 0,
    val contributionDate: Long = 0,
    val isLate: Boolean = false,
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now(), // Add this
    val isSynced: Boolean = false ,

    val isDeleted: Boolean = false,

    val deletedAt: Long = 0
)