package com.example.chamabuddy.domain.Firebase


import java.util.*

data class WelfareBeneficiaryFire(
    val beneficiaryId: String = UUID.randomUUID().toString(),
    val meetingId: String = "",
    val memberId: String = "",
    val amountReceived: Int = 0,
    val dateAwarded: Long = Date().time,
    val groupId: String = ""
)
