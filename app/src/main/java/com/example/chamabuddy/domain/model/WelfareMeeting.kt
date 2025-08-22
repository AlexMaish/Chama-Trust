package com.example.chamabuddy.domain.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.chamabuddy.data.local.Converters
import java.util.UUID

@Entity(tableName = "welfare_meetings")
@TypeConverters(Converters::class)
data class WelfareMeeting(
    @PrimaryKey val meetingId: String = UUID.randomUUID().toString(),
    val welfareId: String,
    val meetingDate: Long,
    val welfareAmount: Int,
    val totalCollected: Int,
    val recordedBy: String? = null,
    val groupId: String,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val beneficiaryNames: List<String> = emptyList(),
    val contributorSummaries: List<String> = emptyList(),
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)