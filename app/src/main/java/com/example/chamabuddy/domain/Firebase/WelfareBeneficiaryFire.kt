package com.example.chamabuddy.domain.Firebase


import com.google.firebase.Timestamp
import java.util.*

data class WelfareBeneficiaryFire(
    val beneficiaryId: String = "",
    val meetingId: String = "",
    val memberId: String = "",
    val amountReceived: Int = 0,
    val dateAwarded: Long = 0,
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now(), // Add this
    val isSynced: Boolean = false ,

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)
