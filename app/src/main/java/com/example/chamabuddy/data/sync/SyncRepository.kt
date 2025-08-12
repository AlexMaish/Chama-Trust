package com.example.chamabuddy.data.sync

import androidx.work.ListenableWorker.Result
import com.example.chamabuddy.domain.repository.*
import javax.inject.Inject

class SyncRepository @Inject constructor(
    private val syncManager: FirestoreSyncManager,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val cycleRepository: CycleRepository,
    private val memberRepository: MemberRepository,
    private val meetingRepository: MeetingRepository,
    private val memberContributionRepository: MemberContributionRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val savingRepository: SavingsRepository,
    private val benefitRepository: BenefitRepository,
    private val expenseRepository: ExpenseRepository,
    private val penaltyRepository: PenaltyRepository
) {

    suspend fun performFullSync(): Result {
        return try {
            // Users
            syncManager.syncUsers(
                { userRepository.getUnsyncedUsers() },
                userRepository::markUserSynced
            )

            // Groups
            syncManager.syncGroups(
                { groupRepository.getUnsyncedGroups() },
                groupRepository::markGroupSynced
            )

            // User Groups
            syncManager.syncUserGroups(
                { userRepository.getUnsyncedUserGroups() },
                userRepository::markUserGroupSynced
            )

            // Group Members
            syncManager.syncGroupMembers(
                { groupRepository.getUnsyncedGroupMembers() },
                groupRepository::markGroupMemberSynced
            )

            // Members
            syncManager.syncMembers(
                { memberRepository.getUnsyncedMembers() },
                memberRepository::syncMember
            )

            // Cycles
            syncManager.syncCycles(
                { cycleRepository.getUnsyncedCycles() },
                cycleRepository::markCycleSynced
            )

            // Weekly Meetings
            syncManager.syncWeeklyMeetings(
                { meetingRepository.getUnsyncedMeetings() },
                meetingRepository::markMeetingSynced
            )

            // Member Contributions
            syncManager.syncMemberContributions(
                { memberContributionRepository.getUnsyncedContributions() },
                memberContributionRepository::markContributionSynced
            )

            // Beneficiaries
            syncManager.syncBeneficiaries(
                { beneficiaryRepository.getUnsyncedBeneficiaries() },
                beneficiaryRepository::markBeneficiarySynced
            )

            // Monthly Savings
            syncManager.syncMonthlySavings(
                { savingRepository.getUnsyncedSavings() },
                savingRepository::markSavingSynced
            )

            // Benefit Entities
            syncManager.syncBenefitEntities(
                { benefitRepository.getUnsyncedBenefits() },
                benefitRepository::markBenefitSynced
            )

            // Expense Entities
            syncManager.syncExpenseEntities(
                { expenseRepository.getUnsyncedExpenses() },
                expenseRepository::markExpenseSynced
            )

            // Penalties
            syncManager.syncPenalties(
                { penaltyRepository.getUnsyncedPenalties() },
                penaltyRepository::markPenaltySynced
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
