package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

import java.util.*

data class WelfareMeetingFire(
    val meetingId: String = UUID.randomUUID().toString(),
    val welfareId: String = "",
    val meetingDate: Long = 0L,
    val welfareAmount: Int = 0,
    val totalCollected: Int = 0,
    val recordedBy: String? = null,
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now(),
    val isSynced: Boolean = false,
    val beneficiaryNames: List<String> = emptyList(),
    val contributorSummaries: List<String> = emptyList(),

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)
