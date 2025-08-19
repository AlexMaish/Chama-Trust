package com.example.chamabuddy.data.remote

import com.example.chamabuddy.domain.Firebase.BeneficiaryFire
import com.example.chamabuddy.domain.Firebase.BenefitEntityFire
import com.example.chamabuddy.domain.Firebase.CycleFire
import com.example.chamabuddy.domain.Firebase.ExpenseEntityFire
import com.example.chamabuddy.domain.Firebase.GroupFire
import com.example.chamabuddy.domain.Firebase.GroupMemberFire
import com.example.chamabuddy.domain.Firebase.MemberContributionFire
import com.example.chamabuddy.domain.Firebase.MemberFire
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

import com.example.chamabuddy.domain.Firebase.MemberWelfareContributionFire
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
    isSynced = true
)

fun BenefitEntityFire.toLocal() = BenefitEntity(
    benefitId = id.toString(),
    groupId = groupId,
    name = name,
    description = description,
    amount = amount,
    date = date.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
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
    isSynced = true
)

fun ExpenseEntityFire.toLocal() = ExpenseEntity(
    expenseId = id.toString(),
    groupId = groupId,
    title = title,
    description = description,
    amount = amount,
    date = date.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
)

fun GroupMemberFire.toLocal() = GroupMember(
    groupId = groupId,
    userId = userId,
    isAdmin = isAdmin,
    joinedAt = joinedAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
)

fun MemberContributionFire.toLocal(): MemberContribution = MemberContribution(
    contributionId = contributionId,
    meetingId = meetingId,
    memberId = memberId,
    amountContributed = amountContributed,
    contributionDate = contributionDate.toDate().time, // ✅ Timestamp → Long
    isLate = isLate,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time, // ✅ Timestamp → Long
    isSynced = true
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
    isSynced = true
)

fun MonthlySavingEntryFire.toLocal(): MonthlySavingEntry {
    return MonthlySavingEntry(
        entryId = entryId,
        savingId = savingId,
        memberId = memberId,
        amount = amount,
        entryDate = entryDate.toDate().time, // ✅ Convert Timestamp to Long
        recordedBy = recordedBy,
        groupId = groupId,
        isPlaceholder = isPlaceholder,
        monthYear = monthYear,
        lastUpdated = lastUpdated.toDate().time, // ✅ Timestamp to Long
        isSynced = true
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
    isSynced = true
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
    isSynced = true
)

fun UserFire.toLocal() = User(
    userId = userId,
    username = username,
    password = password,
    phoneNumber = phoneNumber,
    createdAt = createdAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
)

fun UserGroupFire.toLocal() = UserGroup(
    userId = userId,
    groupId = groupId,
    isOwner = isOwner,
    joinedAt = joinedAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
)

fun WeeklyMeetingFire.toLocal() = WeeklyMeeting(
    meetingId = meetingId,
    cycleId = cycleId,
    meetingDate = meetingDate.toDate().time,
    totalCollected = totalCollected,
    recordedBy = recordedBy,
    groupId = groupId,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
)

fun GroupFire.toLocal() = Group(
    groupId = groupId,
    name = name,
    adminId = adminId,
    adminName = adminName,
    createdAt = createdAt.toDate().time,
    totalSavings = totalSavings,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
)
// MemberWelfareContributionFire -> MemberWelfareContribution
fun MemberWelfareContributionFire.toLocal() = com.example.chamabuddy.domain.model.MemberWelfareContribution(
    contributionId = contributionId,
    meetingId = meetingId,
    memberId = memberId,
    amountContributed = amountContributed,
    // contributionDate is Long in the Fire DTO, domain expects Long -> use directly
    contributionDate = contributionDate,
    isLate = isLate,
    groupId = groupId
)

// WelfareBeneficiaryFire -> WelfareBeneficiary
fun WelfareBeneficiaryFire.toLocal() = com.example.chamabuddy.domain.model.WelfareBeneficiary(
    beneficiaryId = beneficiaryId,
    meetingId = meetingId,
    memberId = memberId,
    amountReceived = amountReceived,
    // dateAwarded is Long in the Fire DTO -> use directly
    dateAwarded = dateAwarded,
    groupId = groupId
)

// WelfareFire -> Welfare
fun WelfareFire.toLocal() = com.example.chamabuddy.domain.model.Welfare(
    welfareId = welfareId,
    groupId = groupId,
    name = name,
    amount = amount,
    createdBy = createdBy,
    // createdAt and lastUpdated are Timestamp in WelfareFire -> convert to epoch millis
    createdAt = createdAt.toDate().time,
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true
)

// WelfareMeetingFire -> WelfareMeeting
fun WelfareMeetingFire.toLocal() = com.example.chamabuddy.domain.model.WelfareMeeting(
    meetingId = meetingId,
    welfareId = welfareId,
    // meetingDate is Long in the Fire DTO -> use directly
    meetingDate = meetingDate,
    welfareAmount = welfareAmount,
    totalCollected = totalCollected,
    recordedBy = recordedBy,
    groupId = groupId,
    // lastUpdated is Timestamp in WelfareMeetingFire -> convert to epoch millis
    lastUpdated = lastUpdated.toDate().time,
    isSynced = true,
    beneficiaryNames = beneficiaryNames,
    contributorSummaries = contributorSummaries
)
