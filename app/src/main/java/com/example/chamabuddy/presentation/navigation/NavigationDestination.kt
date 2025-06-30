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

object BeneficiaryDestination : NavigationDestination {
    override val route = "beneficiary"
    override val title = "Beneficiary"
}

object MembersDestination : NavigationDestination {
    override val route = "members"
    override val title = "Members"
    const val groupIdArg = "groupId"
    val routeWithArgs = "$route/{$groupIdArg}"
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

