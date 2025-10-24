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
            syncManager.syncUsers(
                { userRepository.getUnsyncedUsers() },
                userRepository::markUserSynced
            )

            syncManager.syncGroups(
                { groupRepository.getUnsyncedGroups() },
                groupRepository::markGroupSynced
            )

            syncManager.syncUserGroups(
                { userRepository.getUnsyncedUserGroups() },
                userRepository::markUserGroupSynced
            )

            syncManager.syncGroupMembers(
                { groupRepository.getUnsyncedGroupMembers() },
                groupRepository::markGroupMemberSynced
            )

            syncManager.syncMembers(
                { memberRepository.getUnsyncedMembers() },
                memberRepository::syncMember
            )

            syncManager.syncCycles(
                { cycleRepository.getUnsyncedCycles() },
                cycleRepository::markCycleSynced
            )

            syncManager.syncWeeklyMeetings(
                { meetingRepository.getUnsyncedMeetings() },
                meetingRepository::markMeetingSynced
            )

            syncManager.syncMemberContributions(
                { memberContributionRepository.getUnsyncedContributions() },
                memberContributionRepository::markContributionSynced
            )

            syncManager.syncBeneficiaries(
                { beneficiaryRepository.getUnsyncedBeneficiaries() },
                beneficiaryRepository::markBeneficiarySynced
            )

            syncManager.syncMonthlySavings(
                { savingRepository.getUnsyncedSavings() },
                savingRepository::markSavingSynced
            )

            syncManager.syncBenefitEntities(
                { benefitRepository.getUnsyncedBenefits() },
                benefitRepository::markBenefitSynced
            )

            syncManager.syncExpenseEntities(
                { expenseRepository.getUnsyncedExpenses() },
                expenseRepository::markExpenseSynced
            )

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
