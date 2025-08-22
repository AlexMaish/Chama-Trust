// Navigation.kt
package com.example.chamabuddy.presentation.navigation

import com.example.chamabuddy.presentation.navigation.MemberProfileDestination.memberIdArg

interface NavigationDestination {
    val route: String
    val title: String
}

object HomeDestination : NavigationDestination {
    override val route = "home"
    override val title = "ChamaBuddy" // Changed from "Home" to "ChamaBuddy"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}



object MembersDestination : NavigationDestination {
    override val route = "members"
    override val title = "Members"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"  // Correct format
}
object CycleDetailDestination : NavigationDestination {
    override val route = "cycle_detail"
    override val title = "Cycle Details"
    const val groupIdArg = "groupId"
    const val cycleIdArg = "cycleId"
    val routeWithArgs = "$route/{$groupIdArg}/{$cycleIdArg}"
}

object MeetingDetailDestination : NavigationDestination {
    override val route = "meeting_detail"
    override val title = "Meeting Details"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$meetingIdArg}"
}

object CreateCycleDestination : NavigationDestination {
    override val route = "create_cycle"
    override val title = "New Cycle"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}

object BeneficiaryDetailDestination : NavigationDestination {
    override val route = "beneficiary_detail"
    override val title = "Beneficiary Details"
    const val beneficiaryIdArg = "beneficiaryId"
    val routeWithArgs = "$route/{$beneficiaryIdArg}"
}

object MemberProfileDestination : NavigationDestination {
    override val route = "member_profile"
    override val title = "Member Profile"
    const val memberIdArg = "memberId"
}

object ProfileDestination : NavigationDestination {
    override val route = "profile"
    override val title = "Member Profile"
    const val groupIdArg = "groupId"
    const val memberIdArg = "memberId"
    val routeWithArgs = "$route/{$groupIdArg}/{$memberIdArg}"
}

object SavingsDestination : NavigationDestination {
    override val route = "savings"
    override val title = "Savings"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"  // Add this
}


object LoginDestination : NavigationDestination {
    override val route = "login"
    override val title = "Login"
}

object RegisterDestination : NavigationDestination {
    override val route = "register"
    override val title = "Register"
}

object AuthDestination : NavigationDestination {
    override val route = "auth"
    override val title = "Authentication"
}


object GroupsHomeDestination : NavigationDestination {
    override val route = "groups_home"
    override val title = "Your Groups"
}


object BeneficiaryGroupDestination : NavigationDestination {
    override val route = "beneficiary_group"
    override val title = "Beneficiaries"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}

object PenaltyDestination : NavigationDestination {
    override val route = "penalty"
    override val title = "Penalties"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}

object ExpenseDestination : NavigationDestination {
    override val route = "expense"
    override val title = "Expenses"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}

object BenefitDestination : NavigationDestination {
    override val route = "benefit"
    override val title = "Benefits"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}


object ContributionSummaryDestination : NavigationDestination {
    override val route = "contribution-summary"
    override val title = "Contribution Summary"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$meetingIdArg}"
}
object ContributionDestination : NavigationDestination {
    override val route = "contribution"
    override val title = "Contribution"
    const val groupIdArg = "groupId"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$groupIdArg}/{$meetingIdArg}"
}
object WelfareDestination : NavigationDestination {
    override val route = "welfare"
    override val title = "Welfare Details"
    const val welfareIdArg = "welfareId"
    val routeWithArgs = "$route/{$welfareIdArg}"
}

object WelfareMeetingDestination : NavigationDestination {
    override val route = "welfare_meeting"
    override val title = "Welfare Meeting"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$meetingIdArg}"
}

object WelfareBeneficiarySelectionDestination : NavigationDestination {
    override val route = "welfare_beneficiary_selection"
    override val title = "Select Beneficiaries"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$meetingIdArg}"
}

object WelfareContributionDestination : NavigationDestination {
    override val route = "welfare_contribution"
    override val title = "Welfare Contribution"
    const val meetingIdArg = "meetingId"
    val routeWithArgs = "$route/{$meetingIdArg}"
}

object CreateWelfareDestination : NavigationDestination {
    override val route = "create_welfare"
    override val title = "Create Welfare"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}

object SavingsFilterDestination : NavigationDestination {
    override val route = "savings_filter"
    override val title = "Savings Filter"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
}
