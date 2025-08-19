package com.example.chamabuddy.domain.Firebase


import java.util.*

data class MemberWelfareContributionFire(
    val contributionId: String = UUID.randomUUID().toString(),
    val meetingId: String = "",
    val memberId: String = "",
    val amountContributed: Int = 0,
    val contributionDate: Long = Date().time,
    val isLate: Boolean = false,
    val groupId: String = ""
)
