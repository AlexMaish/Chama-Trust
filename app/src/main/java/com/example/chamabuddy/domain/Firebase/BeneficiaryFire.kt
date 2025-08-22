package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class BeneficiaryFire(
    val beneficiaryId: String = "",
    val meetingId: String = "",
    val memberId: String = "",
    val amountReceived: Int = 0,
    val paymentOrder: Int = 0,
    val dateAwarded: Timestamp = Timestamp.now(),
    val cycleId: String = "",
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now(),

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)