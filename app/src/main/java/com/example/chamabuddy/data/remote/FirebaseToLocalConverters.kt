package com.example.chamabuddy.data.remote

import com.example.chamabuddy.domain.Firebase.BeneficiaryFire
import com.example.chamabuddy.domain.Firebase.BenefitEntityFire
import com.example.chamabuddy.domain.Firebase.CycleFire
import com.example.chamabuddy.domain.Firebase.ExpenseEntityFire
import com.example.chamabuddy.domain.Firebase.GroupFire
import com.example.chamabuddy.domain.Firebase.GroupMemberFire
import com.example.chamabuddy.domain.Firebase.MemberContributionFire
import com.example.chamabuddy.domain.Firebase.MemberFire
import com.example.chamabuddy.domain.Firebase.MemberWelfareContributionFire
import com.example.chamabuddy.domain.Firebase.MonthlySavingEntryFire
import com.example.chamabuddy.domain.Firebase.MonthlySavingFire
import com.example.chamabuddy.domain.Firebase.PenaltyFire
import com.example.chamabuddy.domain.Firebase.UserFire
import com.example.chamabuddy.domain.Firebase.UserGroupFire
import com.example.chamabuddy.domain.Firebase.WeeklyMeetingFire
import com.example.chamabuddy.domain.model.Beneficiary
import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.ExpenseEntity
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.model.GroupMember
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.MemberContribution
import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.domain.model.Penalty
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.model.UserGroup
import com.example.chamabuddy.domain.model.WeeklyMeeting

import com.example.chamabuddy.domain.Firebase.WelfareBeneficiaryFire
import com.example.chamabuddy.domain.Firebase.WelfareFire
import com.example.chamabuddy.domain.Firebase.WelfareMeetingFire
import com.google.firebase.Timestamp

import java.text.SimpleDateFormat
import java.util.Locale


fun BeneficiaryFire.toLocal() = Beneficiary(
    beneficiaryId = beneficiaryId,
    meetingId = meetingId,
    memberId = memberId,
    amountReceived = amountReceived,
    paymentOrder = paymentOrder,
    dateAwarded = dateAwarded.toDate().time,
    cycleId = cycleId,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun BenefitEntityFire.toLocal() = BenefitEntity(
    benefitId = id,
    groupId = groupId,
    name = name,
    description = description,
    amount = amount,
    date = date.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun CycleFire.toLocal() = Cycle(
    cycleId = cycleId,
    startDate = startDate.toDate().time,
    endDate = endDate?.toDate()?.time,
    weeklyAmount = weeklyAmount,
    monthlySavingsAmount = monthlySavingsAmount,
    isActive = isActive,
    totalMembers = totalMembers,
    totalSavings = totalSavings,
    groupId = groupId,
    beneficiariesPerMeeting = beneficiariesPerMeeting,
    cycleNumber = cycleNumber,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun ExpenseEntityFire.toLocal() = ExpenseEntity(
    expenseId = id.toString(),
    groupId = groupId,
    title = title,
    description = description,
    amount = amount,
    date = date.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun GroupMemberFire.toLocal() = GroupMember(
    groupId = groupId,
    userId = userId,
    isAdmin = isAdmin,
    joinedAt = joinedAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun MemberContributionFire.toLocal(): MemberContribution = MemberContribution(
    contributionId = contributionId,
    meetingId = meetingId,
    memberId = memberId,
    amountContributed = amountContributed,
    contributionDate = contributionDate.toDate().time,
    isLate = isLate,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun MemberFire.toLocal() = Member(
    memberId = memberId,
    name = name,
    nickname = nickname,
    phoneNumber = phoneNumber,
    profilePicture = profilePicture,
    isAdmin = isAdmin,
    isActive = isActive,
    joinDate = joinDate.toDate().time,
    userId = userId,
    groupId = groupId,
    isOwner = isOwner,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun MonthlySavingEntryFire.toLocal(): MonthlySavingEntry {
    return MonthlySavingEntry(
        entryId = entryId,
        savingId = savingId,
        memberId = memberId,
        amount = amount,
        entryDate = entryDate.toDate().time,
        recordedBy = recordedBy,
        groupId = groupId,
        isPlaceholder = isPlaceholder,
        monthYear = monthYear,
        lastUpdated = lastUpdated.toDate().time,
        isSynced = true,
        isDeleted = isDeleted,
        deletedAt = deletedAt
    )
}

fun MonthlySavingFire.toLocal() = MonthlySaving(
    savingId = savingId,
    cycleId = cycleId,
    monthYear = monthYear,
    targetAmount = targetAmount,
    actualAmount = actualAmount,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun PenaltyFire.toLocal() = Penalty(
    penaltyId = id.toString(),
    groupId = groupId,
    memberName = memberName,
    description = description,
    amount = amount,
    memberId = memberId,
    date = date.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun UserFire.toLocal() = User(
    userId = userId,
    username = username,
    password = password,
    phoneNumber = phoneNumber,
    createdAt = createdAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun UserGroupFire.toLocal() = UserGroup(
    userId = userId,
    groupId = groupId,
    isOwner = isOwner,
    joinedAt = joinedAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun WeeklyMeetingFire.toLocal() = WeeklyMeeting(
    meetingId = meetingId,
    cycleId = cycleId,
    meetingDate = meetingDate.toDate().time,
    totalCollected = totalCollected,
    recordedBy = recordedBy,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun GroupFire.toLocal() = Group(
    groupId = groupId,
    name = name,
    adminId = adminId,
    adminName = adminName,
    createdAt = createdAt.toDate().time,
    totalSavings = totalSavings,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun MemberWelfareContributionFire.toLocal() = com.example.chamabuddy.domain.model.MemberWelfareContribution(
    contributionId = contributionId,
    meetingId = meetingId,
    memberId = memberId,
    amountContributed = amountContributed,
    contributionDate = contributionDate,
    isLate = isLate,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = isSynced,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun WelfareBeneficiaryFire.toLocal() = com.example.chamabuddy.domain.model.WelfareBeneficiary(
    beneficiaryId = beneficiaryId,
    meetingId = meetingId,
    memberId = memberId,
    amountReceived = amountReceived,
    dateAwarded = dateAwarded,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = isSynced,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun WelfareFire.toLocal() = com.example.chamabuddy.domain.model.Welfare(
    welfareId = welfareId,
    groupId = groupId,
    name = name,
    amount = amount,
    createdBy = createdBy,
    createdAt = createdAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun WelfareMeetingFire.toLocal() = com.example.chamabuddy.domain.model.WelfareMeeting(
    meetingId = meetingId,
    welfareId = welfareId,
    meetingDate = meetingDate,
    welfareAmount = welfareAmount,
    totalCollected = totalCollected,
    recordedBy = recordedBy,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    beneficiaryNames = beneficiaryNames,
    contributorSummaries = contributorSummaries
)