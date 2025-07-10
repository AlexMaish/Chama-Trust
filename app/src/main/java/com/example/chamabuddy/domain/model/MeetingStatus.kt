package com.example.chamabuddy.domain.model

data class MeetingStatus(
    val totalExpected: Int,
    val totalContributed: Int,
    val beneficiariesSelected: Boolean,
    val beneficiaryCount: Int,
    val requiredBeneficiaryCount: Int,
    val fullyRecorded: Boolean
)