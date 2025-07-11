package com.example.chamabuddy.domain.model


data class CycleWithBeneficiaries(
    val cycle: Cycle,
    val beneficiaries: List<BeneficiaryWithMember>
)
