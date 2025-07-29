package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp


data class WeeklyMeetingFire(
    val meetingId: String = "",
    val cycleId: String = "",
    val meetingDate: Timestamp = Timestamp.now(),
    val totalCollected: Int = 0,
    val recordedBy: String? = null,
    val groupId: String = "",
    val lastUpdated: Timestamp = Timestamp.now()
)