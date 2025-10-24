package com.example.chamabuddy.data.remote

import com.example.chamabuddy.domain.Firebase.*
import com.example.chamabuddy.domain.model.*
import com.google.firebase.Timestamp
import java.util.*

fun Beneficiary.toFirebase() = BeneficiaryFire(
    beneficiaryId = beneficiaryId,
    meetingId = meetingId,
    memberId = memberId,
    amountReceived = amountReceived,
    paymentOrder = paymentOrder,
    dateAwarded = Timestamp(Date(dateAwarded)),
    cycleId = cycleId,
    groupId = groupId,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun Welfare.toFirebase() = WelfareFire(
    welfareId = welfareId,
    groupId = groupId,
    name = name,
    amount = amount,
    createdBy = createdBy,
    createdAt = Timestamp(Date(createdAt)),
    lastUpdated = Timestamp(Date(lastUpdated)),
    isSynced = isSynced,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun WelfareBeneficiary.toFirebase() = WelfareBeneficiaryFire(
    beneficiaryId = beneficiaryId,
    meetingId = meetingId,
    memberId = memberId,
    amountReceived = amountReceived,
    dateAwarded = dateAwarded,
    groupId = groupId,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isSynced = isSynced,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun WelfareMeeting.toFirebase() = WelfareMeetingFire(
    meetingId = meetingId,
    welfareId = welfareId,
    meetingDate = meetingDate,
    welfareAmount = welfareAmount,
    totalCollected = totalCollected,
    recordedBy = recordedBy,
    groupId = groupId,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isSynced = isSynced,
    isDeleted = isDeleted,
    deletedAt = deletedAt,
    beneficiaryNames = beneficiaryNames,
    contributorSummaries = contributorSummaries
)


fun MemberWelfareContribution.toFirebase() = MemberWelfareContributionFire(
    contributionId = contributionId,
    meetingId = meetingId,
    memberId = memberId,
    amountContributed = amountContributed,
    contributionDate = contributionDate,
    isLate = isLate,
    groupId = groupId,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isSynced = isSynced,
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun BenefitEntity.toFirebase() = BenefitEntityFire(
    id = benefitId,
    groupId = groupId,
    name = name,
    description = description,
    amount = amount,
    date = Timestamp(Date(date)),
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


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
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun ExpenseEntity.toFirebase() = ExpenseEntityFire(
    id = expenseId,
    groupId = groupId,
    title = title,
    description = description,
    amount = amount,
    date = Timestamp(Date(date)),
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun GroupMember.toFirebase() = GroupMemberFire(
    groupId = groupId,
    userId = userId,
    isAdmin = isAdmin,
    joinedAt = Timestamp(Date(joinedAt)),
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun MemberContribution.toFirebase() = MemberContributionFire(
    contributionId = contributionId,
    meetingId = meetingId,
    memberId = memberId,
    amountContributed = amountContributed,
    contributionDate = Timestamp(Date(contributionDate)),
    isLate = isLate,
    groupId = groupId,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


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
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun MonthlySavingEntry.toFirebase() = MonthlySavingEntryFire(
    entryId = entryId,
    savingId = savingId,
    memberId = memberId,
    amount = amount,
    entryDate = Timestamp(Date(entryDate)),
    recordedBy = recordedBy,
    groupId = groupId,
    isPlaceholder = isPlaceholder,
    monthYear = monthYear,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun MonthlySaving.toFirebase() = MonthlySavingFire(
    savingId = savingId,
    cycleId = cycleId,
    monthYear = monthYear,
    targetAmount = targetAmount,
    actualAmount = actualAmount,
    groupId = groupId,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun Penalty.toFirebase() = PenaltyFire(
    id = penaltyId,
    groupId = groupId,
    memberId = memberId,
    memberName = memberName,
    description = description,
    amount = amount,
    date = Timestamp(Date(date)),
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun User.toFirebase() = UserFire(
    userId = userId,
    username = username,
    password = password,
    phoneNumber = phoneNumber,
    createdAt = Timestamp(Date(createdAt)),
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun UserGroup.toFirebase() = UserGroupFire(
    userId = userId,
    groupId = groupId,
    isOwner = isOwner,
    joinedAt = Timestamp(Date(joinedAt)),
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun WeeklyMeeting.toFirebase() = WeeklyMeetingFire(
    meetingId = meetingId,
    cycleId = cycleId,
    meetingDate = Timestamp(Date(meetingDate)),
    totalCollected = totalCollected,
    recordedBy = recordedBy,
    groupId = groupId,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)

fun Group.toFirebase() = GroupFire(
    groupId = groupId,
    name = name,
    adminId = adminId,
    adminName = adminName,
    createdAt = Timestamp(Date(createdAt)),
    totalSavings = totalSavings,
    lastUpdated = Timestamp(Date(lastUpdated)),
    isDeleted = isDeleted,
    deletedAt = deletedAt
)


fun Map<String, Any>.toBeneficiaryFire(): BeneficiaryFire {
    return BeneficiaryFire(
        beneficiaryId = this["beneficiaryId"] as? String ?: "",
        meetingId = this["meetingId"] as? String ?: "",
        memberId = this["memberId"] as? String ?: "",
        amountReceived = (this["amountReceived"] as? Number)?.toInt() ?: 0,
        paymentOrder = (this["paymentOrder"] as? Number)?.toInt() ?: 0,
        dateAwarded = this["dateAwarded"] as? Timestamp ?: Timestamp.now(),
        cycleId = this["cycleId"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toBenefitEntityFire(): BenefitEntityFire {
    return BenefitEntityFire(
        id = this["id"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        name = this["name"] as? String ?: "",
        description = this["description"] as? String ?: "",
        amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
        date = this["date"] as? Timestamp ?: Timestamp.now(),
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toCycleFire(): CycleFire {
    return CycleFire(
        cycleId = this["cycleId"] as? String ?: "",
        startDate = this["startDate"] as? Timestamp ?: Timestamp.now(),
        endDate = this["endDate"] as? Timestamp,
        weeklyAmount = (this["weeklyAmount"] as? Number)?.toInt() ?: 0,
        monthlySavingsAmount = (this["monthlySavingsAmount"] as? Number)?.toInt() ?: 0,
        isActive = this["isActive"] as? Boolean ?: true,
        totalMembers = (this["totalMembers"] as? Number)?.toInt() ?: 0,
        totalSavings = (this["totalSavings"] as? Number)?.toInt() ?: 0,
        groupId = this["groupId"] as? String ?: "",
        beneficiariesPerMeeting = (this["beneficiariesPerMeeting"] as? Number)?.toInt() ?: 2,
        cycleNumber = (this["cycleNumber"] as? Number)?.toInt() ?: 0,
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toExpenseEntityFire(): ExpenseEntityFire {
    return ExpenseEntityFire(
        id = this["id"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        title = this["title"] as? String ?: "",
        description = this["description"] as? String ?: "",
        amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
        date = this["date"] as? Timestamp ?: Timestamp.now(),
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toGroupMemberFire(): GroupMemberFire {
    return GroupMemberFire(
        groupId = this["groupId"] as? String ?: "",
        userId = this["userId"] as? String ?: "",
        isAdmin = this["isAdmin"] as? Boolean ?: false,
        joinedAt = this["joinedAt"] as? Timestamp ?: Timestamp.now(),
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toMemberContributionFire(): MemberContributionFire {
    return MemberContributionFire(
        contributionId = this["contributionId"] as? String ?: "",
        meetingId = this["meetingId"] as? String ?: "",
        memberId = this["memberId"] as? String ?: "",
        amountContributed = (this["amountContributed"] as? Number)?.toInt() ?: 0,
        contributionDate = this["contributionDate"] as? Timestamp ?: Timestamp.now(),
        isLate = this["isLate"] as? Boolean ?: false,
        groupId = this["groupId"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toMemberFire(): MemberFire {
    return MemberFire(
        memberId = this["memberId"] as? String ?: "",
        name = this["name"] as? String ?: "",
        nickname = this["nickname"] as? String?,
        phoneNumber = this["phoneNumber"] as? String ?: "",
        profilePicture = this["profilePicture"] as? String?,
        isAdmin = this["admin"] as? Boolean ?: false,
        isActive = this["active"] as? Boolean ?: true,
        joinDate = this["joinDate"] as? Timestamp ?: Timestamp.now(),
        userId = this["userId"] as? String?,
        groupId = this["groupId"] as? String ?: "",
        isOwner = this["owner"] as? Boolean ?: false,
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toMonthlySavingEntryFire(): MonthlySavingEntryFire {
    return MonthlySavingEntryFire(
        entryId = this["entryId"] as? String ?: "",
        savingId = this["savingId"] as? String ?: "",
        memberId = this["memberId"] as? String ?: "",
        amount = (this["amount"] as? Number)?.toInt() ?: 0,
        entryDate = this["entryDate"] as? Timestamp ?: Timestamp.now(),
        recordedBy = this["recordedBy"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        isPlaceholder = this["isPlaceholder"] as? Boolean ?: false,
        monthYear = this["monthYear"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toMonthlySavingFire(): MonthlySavingFire {
    return MonthlySavingFire(
        savingId = this["savingId"] as? String ?: "",
        cycleId = this["cycleId"] as? String ?: "",
        monthYear = this["monthYear"] as? String ?: "",
        targetAmount = (this["targetAmount"] as? Number)?.toInt() ?: 0,
        actualAmount = (this["actualAmount"] as? Number)?.toInt() ?: 0,
        groupId = this["groupId"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toPenaltyFire(): PenaltyFire {
    return PenaltyFire(
        id = this["id"] as? String ?: "",
        memberId = this["memberId"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        memberName = this["memberName"] as? String ?: "",
        description = this["description"] as? String ?: "",
        amount = (this["amount"] as? Number)?.toDouble() ?: 0.0,
        date = this["date"] as? Timestamp ?: Timestamp.now(),
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toUserFire(): UserFire {
    return UserFire(
        userId = this["userId"] as? String ?: "",
        username = this["username"] as? String ?: "",
        password = this["password"] as? String ?: "",
        phoneNumber = this["phoneNumber"] as? String ?: "",
        createdAt = this["createdAt"] as? Timestamp ?: Timestamp.now(),
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toUserGroupFire(): UserGroupFire {
    return UserGroupFire(
        userId = this["userId"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        isOwner = this["isOwner"] as? Boolean ?: false,
        joinedAt = this["joinedAt"] as? Timestamp ?: Timestamp.now(),
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toWeeklyMeetingFire(): WeeklyMeetingFire {
    return WeeklyMeetingFire(
        meetingId = this["meetingId"] as? String ?: "",
        cycleId = this["cycleId"] as? String ?: "",
        meetingDate = this["meetingDate"] as? Timestamp ?: Timestamp.now(),
        totalCollected = (this["totalCollected"] as? Number)?.toInt() ?: 0,
        recordedBy = this["recordedBy"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toGroupFire(): GroupFire {
    return GroupFire(
        groupId = this["groupId"] as? String ?: "",
        name = this["name"] as? String ?: "",
        adminId = this["adminId"] as? String ?: "",
        adminName = this["adminName"] as? String ?: "",
        createdAt = this["createdAt"] as? Timestamp ?: Timestamp.now(),
        totalSavings = (this["totalSavings"] as? Number)?.toDouble() ?: 0.0,
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toWelfareFire(): WelfareFire {
    return WelfareFire(
        welfareId = this["welfareId"] as? String ?: "",
        groupId = this["groupId"] as? String ?: "",
        name = this["name"] as? String ?: "",
        amount = (this["amount"] as? Number)?.toInt() ?: 0,
        createdBy = this["createdBy"] as? String ?: "",
        createdAt = this["createdAt"] as? Timestamp ?: Timestamp.now(),
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isSynced = this["isSynced"] as? Boolean ?: false,
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toWelfareBeneficiaryFire(): WelfareBeneficiaryFire {
    return WelfareBeneficiaryFire(
        beneficiaryId = this["beneficiaryId"] as? String ?: "",
        meetingId = this["meetingId"] as? String ?: "",
        memberId = this["memberId"] as? String ?: "",
        amountReceived = (this["amountReceived"] as? Number)?.toInt() ?: 0,
        dateAwarded = (this["dateAwarded"] as? Number)?.toLong() ?: 0,
        groupId = this["groupId"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isSynced = this["isSynced"] as? Boolean ?: false,
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}

fun Map<String, Any>.toWelfareMeetingFire(): WelfareMeetingFire {
    return WelfareMeetingFire(
        meetingId = this["meetingId"] as? String ?: "",
        welfareId = this["welfareId"] as? String ?: "",
        meetingDate = (this["meetingDate"] as? Number)?.toLong()
            ?: (this["meetingDate"] as? Timestamp)?.toDate()?.time
            ?: Timestamp.now().toDate().time,
        welfareAmount = (this["welfareAmount"] as? Number)?.toInt() ?: 0,
        totalCollected = (this["totalCollected"] as? Number)?.toInt() ?: 0,
        recordedBy = this["recordedBy"] as? String,
        groupId = this["groupId"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isSynced = this["isSynced"] as? Boolean ?: false,
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0 ,
        beneficiaryNames = this["beneficiaryNames"] as? List<String> ?: emptyList(),
        contributorSummaries = this["contributorSummaries"] as? List<String> ?: emptyList()
    )
}

fun Map<String, Any>.toMemberWelfareContributionFire(): MemberWelfareContributionFire {
    return MemberWelfareContributionFire(
        contributionId = this["contributionId"] as? String ?: "",
        meetingId = this["meetingId"] as? String ?: "",
        memberId = this["memberId"] as? String ?: "",
        amountContributed = (this["amountContributed"] as? Number)?.toInt() ?: 0,
        contributionDate = (this["contributionDate"] as? Number)?.toLong() ?: 0,
        isLate = this["isLate"] as? Boolean ?: false,
        groupId = this["groupId"] as? String ?: "",
        lastUpdated = this["lastUpdated"] as? Timestamp ?: Timestamp.now(),
        isSynced = this["isSynced"] as? Boolean ?: false,
        isDeleted = this["isDeleted"] as? Boolean ?: false,
        deletedAt = (this["deletedAt"] as? Timestamp)?.toDate()?.time ?: 0
    )
}