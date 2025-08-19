package com.example.chamabuddy.domain.model


import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.*

@Entity(tableName = "welfare_beneficiaries")
data class WelfareBeneficiary(
    @PrimaryKey val beneficiaryId: String = UUID.randomUUID().toString(),
    val meetingId: String,
    val memberId: String,
    val amountReceived: Int,
    val dateAwarded: Long = Date().time,
    val groupId: String
)