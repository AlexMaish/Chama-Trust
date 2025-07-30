package com.example.chamabuddy.data.sync


import androidx.work.ListenableWorker.Result
import com.example.chamabuddy.data.sync.FirestoreSyncManager
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

    suspend fun performSync(): Result {
        return try {
            // Users
            syncManager.syncUsers(
                localFetch = { userRepository.getUnsyncedUsers() },
                updateLocal = { userRepository.markUserSynced(it) }
            )
            // Groups
            syncManager.syncGroups(
                localFetch = { groupRepository.getUnsyncedGroups() },
                updateLocal = { groupRepository.markGroupSynced(it) }
            )
            // Cycles
            syncManager.syncCycles(
                localFetch = { cycleRepository.getUnsyncedCycles() },
                updateLocal = { cycleRepository.markCycleSynced(it) }
            )
            // Members
            syncManager.syncMembers(
                localFetch = { memberRepository.getUnsyncedMembers() },
                updateLocal = { memberRepository.syncMember(it) }
            )
            // Group Members
            syncManager.syncGroupMembers(
                localFetch = { groupRepository.getUnsyncedGroupMembers() },
                updateLocal = { groupRepository.markGroupMemberSynced(it) }
            )
            // User Groups
            syncManager.syncUserGroups(
                localFetch = { userRepository.getUnsyncedUserGroups() },
                updateLocal = { userRepository.markUserGroupSynced(it) }
            )
            // Weekly Meetings
            syncManager.syncWeeklyMeetings(
                localFetch = { meetingRepository.getUnsyncedMeetings() },
                updateLocal = { meetingRepository.markMeetingSynced(it) }
            )
            // Monthly Savings
            syncManager.syncMonthlySavings(
                localFetch = { savingRepository.getUnsyncedSavings() },
                updateLocal = { savingRepository.markSavingSynced(it) }
            )
            // Monthly Saving Entries
            syncManager.syncMonthlySavingEntries(
                localFetch = { savingRepository.getUnsyncedEntries() },
                updateLocal = { savingRepository.markEntrySynced(it) }
            )
            // Member Contributions
            syncManager.syncMemberContributions(
                localFetch = { memberContributionRepository.getUnsyncedContributions() },
                updateLocal = { memberContributionRepository.markContributionSynced(it) }
            )
            // Beneficiaries
            syncManager.syncBeneficiaries(
                localFetch = { beneficiaryRepository.getUnsyncedBeneficiaries() },
                updateLocal = { beneficiaryRepository.markBeneficiarySynced(it) }
            )
            // Benefit Entities
            syncManager.syncBenefitEntities(
                localFetch = { benefitRepository.getUnsyncedBenefits() },
                updateLocal = { benefitRepository.markBenefitSynced(it) }
            )
            // Expense Entities
            syncManager.syncExpenseEntities(
                localFetch = { expenseRepository.getUnsyncedExpenses() },
                updateLocal = { expenseRepository.markExpenseSynced(it) }
            )
            // Penalties
            syncManager.syncPenalties(
                localFetch = { penaltyRepository.getUnsyncedPenalties() },
                updateLocal = { penaltyRepository.markPenaltySynced(it) }
            )

            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}