package com.example.chamabuddy.domain.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "MemberWelfareContribution")
data class MemberWelfareContribution(
    @PrimaryKey val contributionId: String = UUID.randomUUID().toString(),
    val meetingId: String,
    val memberId: String,
    val amountContributed: Int,
    val contributionDate: Long = Date().time,
    val isLate: Boolean = false,
    val groupId: String,
   val lastUpdated: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false,
    val isDeleted: Boolean = false,
    val deletedAt: Long = 0
)
