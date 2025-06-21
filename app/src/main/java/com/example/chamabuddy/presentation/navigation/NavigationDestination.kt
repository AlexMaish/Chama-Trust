// Navigation.kt
package com.example.chamabuddy.presentation.navigation

import com.example.chamabuddy.presentation.navigation.MemberProfileDestination.memberIdArg

interface NavigationDestination {
    val route: String
    val title: String
}

object HomeDestination : NavigationDestination {
    override val route = "home"
    override val title = "Home"
}

object BeneficiaryDestination : NavigationDestination {
    override val route = "beneficiary"
    override val title = "Beneficiary"
}

object MembersDestination : NavigationDestination {
    override val route = "members"
    override val title = "Members"
}

object CycleDetailDestination : NavigationDestination {
    override val route = "cycle_detail"
    override val title = "Cycle Details"
    const val cycleIdArg = "cycleId"
    val routeWithArgs = "$route/{$cycleIdArg}"
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
    val routeWithArgs = "$route/{$memberIdArg}"
}

object ProfileDestination : NavigationDestination {
    override val route = "profile"
    override val title = "Member Profile"
    const val memberIdArg = "memberId"
    val routeWithArgs = "$route/{$memberIdArg}"
}
object SavingsDestination : NavigationDestination {
    override val route = "savings"
    override val title = "Savings"
}