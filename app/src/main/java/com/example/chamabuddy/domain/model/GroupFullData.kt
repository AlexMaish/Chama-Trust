package com.example.chamabuddy.domain.model

data class GroupFullData(
    val groupWithMembers: GroupWithMembers?,
    val cycles: List<Cycle>,
    val meetings: List<MeetingWithDetails>,
    val savings: List<MonthlySaving>,
    val beneficiaries: List<Beneficiary>
)