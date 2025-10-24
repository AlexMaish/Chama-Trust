package com.example.chamabuddy.domain.Firebase

import com.google.firebase.Timestamp
import java.util.*

data class WelfareFire(
    val welfareId: String = UUID.randomUUID().toString(),
    val groupId: String = "",
    val name: String = "",
    val amount: Int = 0,
    val createdBy: String = "",
    val createdAt: Timestamp = Timestamp.now(),
    val lastUpdated: Timestamp = Timestamp.now(),
    val isSynced: Boolean = false,

    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)

