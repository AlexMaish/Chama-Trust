package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp

data class MonthlySavingEntryFire(
    val entryId: String = "",
    val savingId: String = "",
    val memberId: String = "",
    val amount: Int = 0,
    val entryDate: Timestamp = Timestamp.now(),
    val recordedBy: String? = null,
    val groupId: String = "",
    val isPlaceholder: Boolean = false,
    val monthYear: String = "",
    val lastUpdated: Timestamp = Timestamp.now(),

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)