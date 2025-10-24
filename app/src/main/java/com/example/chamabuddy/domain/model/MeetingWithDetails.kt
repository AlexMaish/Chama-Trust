package com.example.chamabuddy.domain.model



data class MeetingWithDetails(
    val meeting: WeeklyMeeting,
    val beneficiaries: List<BeneficiaryMember>,
    val contributors: List<ContributorMember>
)

data class BeneficiaryMember(
    val memberId: String,
    val memberName: String,
    val amountReceived: Int
)

data class ContributorMember(
    val memberId: String,
    val memberName: String,
    val amountContributed: Int
)