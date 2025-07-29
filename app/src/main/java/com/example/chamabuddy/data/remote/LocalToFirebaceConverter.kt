package com.example.chamabuddy.data.remote

import com.example.chamabuddy.domain.Firebase.*
import com.example.chamabuddy.domain.model.*
import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.util.*

// Date formatter for string-based dates in Firebase models
private val dateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

// Beneficiary Conversions
fun Beneficiary.toFirebase() = BeneficiaryFire(
    beneficiaryId = beneficiaryId,
    meetingId = meetingId,
    memberId = memberId,
    amountReceived = amountReceived,
    paymentOrder = paymentOrder,
    dateAwarded = Timestamp(Date(dateAwarded)),
    cycleId = cycleId,
    groupId = groupId,
    lastUpdated = Timestamp.now()
)

// BenefitEntity Conversions
fun BenefitEntity.toFirebase() = BenefitEntityFire(
    id = benefitId.toLongOrNull() ?: 0L,
    groupId = groupId,
    name = name,
    description = description,
    amount = amount,
    date = Timestamp(Date(date)),
    lastUpdated = Timestamp.now()
)

// Cycle Conversions
fun Cycle.toFirebase() = CycleFire(
    cycleId = cycleId,
    startDate = Timestamp(Date(startDate)),
    endDate = endDate?.let { Timestamp(Date(it)) },
    weeklyAmount = weeklyAmount,
    monthlySavingsAmount = monthlySavingsAmount,
    isActive = isActive,
    totalMembers = totalMembers,
    totalSavings = totalSavings,
    groupId = groupId,
    beneficiariesPerMeeting = beneficiariesPerMeeting,
    cycleNumber = cycleNumber,
    lastUpdated = Timestamp.now()
)

// ExpenseEntity Conversions
fun ExpenseEntity.toFirebase() = ExpenseEntityFire(
    id = expenseId.toLongOrNull() ?: 0L,
    groupId = groupId,
    title = title,
    description = description,
    amount = amount,
    date = Timestamp(Date(date)),
    lastUpdated = Timestamp.now()
)

// GroupMember Conversions
fun GroupMember.toFirebase() = GroupMemberFire(
    groupId = groupId,
    userId = userId,
    isAdmin = isAdmin,
    joinedAt = Timestamp(Date(joinedAt)),
    lastUpdated = Timestamp.now()
)

// MemberContribution Conversions
fun MemberContribution.toFirebase() = MemberContributionFire(
    contributionId = contributionId,
    meetingId = meetingId,
    memberId = memberId,
    amountContributed = amountContributed,
    contributionDate = dateFormatter.format(Date(contributionDate)),
    isLate = isLate,
    groupId = groupId,
    lastUpdated = Timestamp.now()
)

// Member Conversions
fun Member.toFirebase() = MemberFire(
    memberId = memberId,
    name = name,
    nickname = nickname,
    phoneNumber = phoneNumber,
    profilePicture = profilePicture,
    isAdmin = isAdmin,
    isActive = isActive,
    joinDate = Timestamp(Date(joinDate)),
    userId = userId,
    groupId = groupId,
    isOwner = isOwner,
    lastUpdated = Timestamp.now()
)

// MonthlySavingEntry Conversions
fun MonthlySavingEntry.toFirebase() = MonthlySavingEntryFire(
    entryId = entryId,
    savingId = savingId,
    memberId = memberId,
    amount = amount,
    entryDate = dateFormatter.format(Date(entryDate)),
    recordedBy = recordedBy,
    groupId = groupId,
    isPlaceholder = isPlaceholder,
    monthYear = monthYear,
    lastUpdated = Timestamp.now()
)

// MonthlySaving Conversions
fun MonthlySaving.toFirebase() = MonthlySavingFire(
    savingId = savingId,
    cycleId = cycleId,
    monthYear = monthYear,
    targetAmount = targetAmount,
    actualAmount = actualAmount,
    groupId = groupId,
    lastUpdated = Timestamp.now()
)

// Penalty Conversions
fun Penalty.toFirebase() = PenaltyFire(
    id = penaltyId.toLongOrNull() ?: 0L,
    groupId = groupId,
    memberName = memberName,
    description = description,
    amount = amount,
    date = Timestamp(Date(date)),
    lastUpdated = Timestamp.now()
)

// User Conversions
fun User.toFirebase() = UserFire(
    userId = userId,
    username = username,
    password = password,
    phoneNumber = phoneNumber,
    createdAt = Timestamp(Date(createdAt)),
    lastUpdated = Timestamp.now()
)

// UserGroup Conversions
fun UserGroup.toFirebase() = UserGroupFire(
    userId = userId,
    groupId = groupId,
    isOwner = isOwner,
    joinedAt = Timestamp(Date(joinedAt)),
    lastUpdated = Timestamp.now()
)

// WeeklyMeeting Conversions
fun WeeklyMeeting.toFirebase() = WeeklyMeetingFire(
    meetingId = meetingId,
    cycleId = cycleId,
    meetingDate = Timestamp(Date(meetingDate)),
    totalCollected = totalCollected,
    recordedBy = recordedBy,
    groupId = groupId,
    lastUpdated = Timestamp.now()
)

// Group Conversions
fun Group.toFirebase() = GroupFire(
    groupId = groupId,
    name = name,
    adminId = adminId,
    adminName = adminName,
    createdAt = Timestamp(Date(createdAt)),
    totalSavings = totalSavings,
    lastUpdated = Timestamp.now()
)

fun Map<String, Any>.toMemberFire(): MemberFire {
    return MemberFire(
        memberId = this["memberId"] as? String ?: "",
        name = this["name"] as? String ?: "",
        nickname = this["nickname"] as? String,
        phoneNumber = this["phoneNumber"] as? String ?: "",
        profilePicture = this["profilePicture"] as? String,
        isAdmin = this["isAdmin"] as? Boolean ?: false,
        isActive = this["isActive"] as? Boolean ?: true,
        joinDate = this["joinDate"] as? Timestamp ?: Timestamp.now(),
        userId = this["userId"] as? String,
        groupId = this["groupId"] as? String ?: "",
        isOwner = this["isOwner"] as? Boolean ?: false,
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now()
    )
}