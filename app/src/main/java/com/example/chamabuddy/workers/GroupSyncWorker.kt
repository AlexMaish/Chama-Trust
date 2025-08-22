package com.example.chamabuddy.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chamabuddy.data.local.preferences.SyncPreferences
import com.example.chamabuddy.data.remote.toFirebase
import com.example.chamabuddy.data.remote.toLocal
import com.example.chamabuddy.data.repository.*
import com.example.chamabuddy.domain.Firebase.*
import com.example.chamabuddy.domain.model.*
import com.example.chamabuddy.domain.repository.BeneficiaryRepository
import com.example.chamabuddy.domain.repository.BenefitRepository
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.ExpenseRepository
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.MeetingRepository
import com.example.chamabuddy.domain.repository.MemberContributionRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.PenaltyRepository
import com.example.chamabuddy.domain.repository.SavingsRepository
import com.example.chamabuddy.domain.repository.UserRepository
import com.example.chamabuddy.domain.repository.WelfareRepository
import com.example.chamabuddy.util.SyncLogger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.sql.SQLException
import java.util.*
import javax.inject.Inject
import kotlin.jvm.java

@HiltWorker
class GroupSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val cycleRepository: CycleRepository,
    private val meetingRepository: MeetingRepository,
    private val beneficiaryRepository: BeneficiaryRepository,
    private val contributionRepository: MemberContributionRepository,
    private val savingRepository: SavingsRepository,
    private val benefitRepository: BenefitRepository,
    private val expenseRepository: ExpenseRepository,
    private val penaltyRepository: PenaltyRepository,
    private val firestore: FirebaseFirestore,
    private val preferences: SyncPreferences,
    private val userRepository: UserRepository,
    private val welfareRepository: WelfareRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        try {
            SyncLogger.d("GroupSyncWorker STARTED")
            val groupIds = inputData.getStringArray("group_ids")?.toSet() ?: emptySet()
            SyncLogger.d("Processing groups: ${groupIds.joinToString()}")

            if (groupIds.isEmpty()) {
                SyncLogger.d("No groups to sync")
                return@withContext Result.success()
            }

            groupIds.forEach { groupId ->
                syncGroupData(groupId)
            }

            SyncLogger.d("Group sync completed successfully")
            Result.success()
        } catch (e: Exception) {
            SyncLogger.e("GroupSyncWorker FAILED: ${e.message}", e)
            Result.retry()
        }
    }

    private suspend fun syncGroupData(groupId: String) {
        SyncLogger.d("Syncing data for group: $groupId")
        val lastSync = preferences.getLastGroupSync(groupId)

        // Run sequentially in this exact order
        syncGroupDetails(groupId, lastSync)
        syncMembers(groupId, lastSync)
        syncCycles(groupId, lastSync)
        syncMeetings(groupId, lastSync)
        syncBeneficiaries(groupId, lastSync)
        syncContributions(groupId, lastSync)
        syncSavings(groupId, lastSync)
        syncMonthlySavingEntries(groupId, lastSync)
        syncBenefitEntities(groupId, lastSync)
        syncExpenseEntities(groupId, lastSync)
        syncPenalties(groupId, lastSync)
        syncUserGroups(groupId, lastSync)

        // Add welfare syncs - these should come after members are synced
        syncWelfares(groupId, lastSync)
        syncWelfareMeetings(groupId, lastSync)
        syncMemberWelfareContributions(groupId, lastSync)
        syncWelfareBeneficiaries(groupId, lastSync)

        preferences.setLastGroupSync(groupId, System.currentTimeMillis())
        SyncLogger.d("Sync completed for group: $groupId")
    }

    private suspend fun syncGroupDetails(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing group details for: $groupId")
        try {
            val doc = firestore.collection("groups").document(groupId).get().await()
            doc.toObject(GroupFire::class.java)?.let { firebaseGroup ->
                val localGroup = groupRepository.getGroupById(groupId)
                val firebaseTime = firebaseGroup.lastUpdated.toDate().time

                if (localGroup == null || firebaseTime > localGroup.lastUpdated) {
                    groupRepository.insertGroup(firebaseGroup.toLocal())
                }
            }

            groupRepository.getUnsyncedGroups().filter { it.groupId == groupId }.forEach { group ->
                firestore.collection("groups").document(groupId)
                    .set(group.toFirebase()).await()
                groupRepository.markGroupSynced(group)
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing group details: ${e.message}", e)
        }
    }

    private suspend fun syncMembers(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing members for: $groupId")
        try {
            val membersSnapshot = firestore.collection("groups/$groupId/members")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            membersSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(MemberFire::class.java)?.let { firebaseMember ->
                        val localMember = memberRepository.getMemberById(firebaseMember.memberId)
                        val firebaseTime = firebaseMember.lastUpdated.toDate().time

                        if (localMember == null || firebaseTime > localMember.lastUpdated) {
                            memberRepository.syncMember(firebaseMember.toLocal())
                        }
                    }
                }
            }

            memberRepository.getUnsyncedMembersForGroup(groupId).forEach { member ->
                firestore.collection("groups/$groupId/members").document(member.memberId)
                    .set(member.toFirebase()).await()
                memberRepository.markMemberSynced(member)
            }

            // Handle deleted members
            val deletedMembers = memberRepository.getDeletedMembers().filter { it.groupId == groupId }
            for (member in deletedMembers) {
                try {
                    firestore.collection("groups/$groupId/members")
                        .document(member.memberId)
                        .delete()
                        .await()
                    memberRepository.permanentDelete(member.memberId)
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting member ${member.memberId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing members: ${e.message}", e)
        }
    }

    private suspend fun syncCycles(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing cycles for: $groupId")
        try {
            val cyclesSnapshot = firestore.collection("groups/$groupId/cycles")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            cyclesSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(CycleFire::class.java)?.let { firebaseCycle ->
                        val localCycle = cycleRepository.getCycleById(firebaseCycle.cycleId)
                        val firebaseTime = firebaseCycle.lastUpdated.toDate().time

                        if (localCycle == null || firebaseTime > localCycle.lastUpdated) {
                            cycleRepository.insertCycle(firebaseCycle.toLocal())
                        }
                    }
                }
            }

            cycleRepository.getUnsyncedCyclesForGroup(groupId).forEach { cycle ->
                firestore.collection("groups/$groupId/cycles").document(cycle.cycleId)
                    .set(cycle.toFirebase()).await()
                cycleRepository.markCycleSynced(cycle)
            }

            // Handle deleted cycles
            val deletedCycles = cycleRepository.getDeletedCycles().filter { it.groupId == groupId }
            for (cycle in deletedCycles) {
                try {
                    firestore.collection("groups/$groupId/cycles").document(cycle.cycleId).delete().await()
                    cycleRepository.permanentDelete(cycle.cycleId)
                    SyncLogger.d("Deleted cycle ${cycle.cycleId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting cycle ${cycle.cycleId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing cycles: ${e.message}", e)
        }
    }


    private suspend fun syncMeetings(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing meetings for: $groupId")
        try {
            val meetingsSnapshot = firestore.collection("groups/$groupId/weekly_meetings")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            meetingsSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(WeeklyMeetingFire::class.java)?.let { firebaseMeeting ->
                        val localMeeting = meetingRepository.getMeetingById(firebaseMeeting.meetingId)
                        val firebaseTime = firebaseMeeting.lastUpdated.toDate().time

                        if (localMeeting == null || firebaseTime > localMeeting.lastUpdated) {
                            meetingRepository.insertMeeting(firebaseMeeting.toLocal())
                        }
                    }
                }
            }

            meetingRepository.getUnsyncedMeetings().filter { it.groupId == groupId }.forEach { meeting ->
                firestore.collection("groups/$groupId/weekly_meetings").document(meeting.meetingId)
                    .set(meeting.toFirebase()).await()
                meetingRepository.markMeetingSynced(meeting)
            }

            // Handle deleted meetings
            val deletedMeetings = meetingRepository.getDeletedMeetings().filter { it.groupId == groupId }
            for (meeting in deletedMeetings) {
                try {
                    firestore.collection("groups/$groupId/weekly_meetings").document(meeting.meetingId).delete().await()
                    meetingRepository.permanentDelete(meeting.meetingId)
                    SyncLogger.d("Deleted meeting ${meeting.meetingId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting meeting ${meeting.meetingId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing meetings: ${e.message}", e)
        }
    }

    private suspend fun syncContributions(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing contributions for: $groupId")
        try {
            val contributionsSnapshot = firestore.collection("groups/$groupId/member_contributions")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            contributionsSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(MemberContributionFire::class.java)?.let { firebaseContribution ->
                        if (!verifyContributionReferences(firebaseContribution)) {
                            SyncLogger.e("Skipping contribution with missing references: ${firebaseContribution.contributionId}")
                            return@let
                        }

                        val localContribution = contributionRepository.getContributionById(firebaseContribution.contributionId)
                        val firebaseTime = firebaseContribution.lastUpdated.toDate().time

                        if (localContribution == null || firebaseTime > localContribution.lastUpdated) {
                            contributionRepository.insertContribution(firebaseContribution.toLocal())
                        }
                    }
                }
            }

            contributionRepository.getUnsyncedContributions().filter { it.groupId == groupId }.forEach { contribution ->
                firestore.collection("groups/$groupId/member_contributions").document(contribution.contributionId)
                    .set(contribution.toFirebase()).await()
                contributionRepository.markContributionSynced(contribution)
            }

            // Handle deleted contributions
            val deletedContributions = contributionRepository.getDeletedContributions().filter { it.groupId == groupId }
            for (contribution in deletedContributions) {
                try {
                    firestore.collection("groups/$groupId/member_contributions").document(contribution.contributionId).delete().await()
                    contributionRepository.permanentDelete(contribution.contributionId)
                    SyncLogger.d("Deleted contribution ${contribution.contributionId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting contribution ${contribution.contributionId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing contributions: ${e.message}", e)
        }
    }

    private suspend fun verifyContributionReferences(contribution: MemberContributionFire): Boolean {
        val memberExists = memberRepository.getMemberById(contribution.memberId) != null

        val cycleExists = if (true) {
            val meeting = meetingRepository.getMeetingById(contribution.meetingId)
            meeting != null && cycleRepository.getCycleById(meeting.cycleId) != null
        } else {
            false // Handle case where meetingId is null
        }

        return memberExists && cycleExists
    }

    private suspend fun syncBeneficiaries(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing beneficiaries for: $groupId")
        try {
            val beneficiariesSnapshot = firestore.collection("groups/$groupId/beneficiaries")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            beneficiariesSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(BeneficiaryFire::class.java)?.let { firebaseBeneficiary ->
                        if (!verifyBeneficiaryReferences(firebaseBeneficiary)) {
                            SyncLogger.e("Skipping beneficiary with missing references: ${firebaseBeneficiary.beneficiaryId}")
                            return@let
                        }

                        val localBeneficiary = beneficiaryRepository.getBeneficiaryById(firebaseBeneficiary.beneficiaryId)
                        val firebaseTime = firebaseBeneficiary.lastUpdated.toDate().time

                        if (localBeneficiary == null || firebaseTime > localBeneficiary.lastUpdated) {
                            beneficiaryRepository.insertBeneficiary(firebaseBeneficiary.toLocal())
                        }
                    }
                }
            }

            beneficiaryRepository.getUnsyncedBeneficiaries().filter { it.groupId == groupId }.forEach { beneficiary ->
                firestore.collection("groups/$groupId/beneficiaries").document(beneficiary.beneficiaryId)
                    .set(beneficiary.toFirebase()).await()
                beneficiaryRepository.markBeneficiarySynced(beneficiary)
            }

            // Handle deleted beneficiaries
            val deletedBeneficiaries = beneficiaryRepository.getDeletedBeneficiaries().filter { it.groupId == groupId }
            for (beneficiary in deletedBeneficiaries) {
                try {
                    firestore.collection("groups/$groupId/beneficiaries").document(beneficiary.beneficiaryId).delete().await()
                    beneficiaryRepository.permanentDelete(beneficiary.beneficiaryId)
                    SyncLogger.d("Deleted beneficiary ${beneficiary.beneficiaryId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting beneficiary ${beneficiary.beneficiaryId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing beneficiaries: ${e.message}", e)
        }
    }

    private suspend fun verifyBeneficiaryReferences(beneficiary: BeneficiaryFire): Boolean {
        return memberRepository.getMemberById(beneficiary.memberId) != null &&
                cycleRepository.getCycleById(beneficiary.cycleId) != null
    }

    private suspend fun syncSavings(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing savings for: $groupId")
        try {
            val savingsSnapshot = firestore.collection("groups/$groupId/monthly_savings")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            savingsSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(MonthlySavingFire::class.java)?.let { firebaseSaving ->
                        val localSaving = savingRepository.getSavingById(firebaseSaving.savingId)
                        val firebaseTime = firebaseSaving.lastUpdated.toDate().time

                        if (localSaving == null || firebaseTime > localSaving.lastUpdated) {
                            savingRepository.insertSaving(firebaseSaving.toLocal())
                        }
                    }
                }
            }

            savingRepository.getUnsyncedSavings().filter { it.groupId == groupId }.forEach { saving ->
                firestore.collection("groups/$groupId/monthly_savings").document(saving.savingId)
                    .set(saving.toFirebase()).await()
                savingRepository.markSavingSynced(saving)
            }

            // Handle deleted savings
            val deletedSavings = savingRepository.getDeletedSavings().filter { it.groupId == groupId }
            for (saving in deletedSavings) {
                try {
                    firestore.collection("groups/$groupId/monthly_savings")
                        .document(saving.savingId)
                        .delete()
                        .await()
                    savingRepository.permanentDelete(saving.savingId)
                    SyncLogger.d("Deleted saving ${saving.savingId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting saving ${saving.savingId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing savings: ${e.message}", e)
        }
    }

    private suspend fun syncBenefitEntities(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing benefits for: $groupId")
        try {
            val benefitsSnapshot = firestore.collection("groups/$groupId/benefits")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            benefitsSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(BenefitEntityFire::class.java)?.let { firebaseBenefit ->
                        val localBenefit = benefitRepository.getBenefitById(firebaseBenefit.id)
                        val firebaseTime = firebaseBenefit.lastUpdated.toDate().time

                        if (localBenefit == null || firebaseTime > localBenefit.lastUpdated) {
                            benefitRepository.insertBenefit(firebaseBenefit.toLocal())
                        }
                    }
                }
            }

            benefitRepository.getUnsyncedBenefits().filter { it.groupId == groupId }.forEach { benefit ->
                firestore.collection("groups/$groupId/benefits").document(benefit.benefitId)
                    .set(benefit.toFirebase()).await()
                benefitRepository.markBenefitSynced(benefit)
            }

            // Handle deleted benefits
            val deletedBenefits = benefitRepository.getDeletedBenefits().filter { it.groupId == groupId }
            for (benefit in deletedBenefits) {
                try {
                    firestore.collection("groups/$groupId/benefits").document(benefit.benefitId).delete().await()
                    benefitRepository.permanentDelete(benefit.benefitId)
                    SyncLogger.d("Deleted benefit ${benefit.benefitId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting benefit ${benefit.benefitId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing benefits: ${e.message}", e)
        }
    }

    private suspend fun syncExpenseEntities(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing expenses for: $groupId")
        try {
            val expensesSnapshot = firestore.collection("groups/$groupId/expenses")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            expensesSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(ExpenseEntityFire::class.java)?.let { firebaseExpense ->
                        val localExpense = expenseRepository.getExpenseById(firebaseExpense.id)
                        val firebaseTime = firebaseExpense.lastUpdated.toDate().time

                        if (localExpense == null || firebaseTime > localExpense.lastUpdated) {
                            expenseRepository.insertExpense(firebaseExpense.toLocal())
                        }
                    }
                }
            }

            expenseRepository.getUnsyncedExpenses().filter { it.groupId == groupId }.forEach { expense ->
                firestore.collection("groups/$groupId/expenses").document(expense.expenseId)
                    .set(expense.toFirebase()).await()
                expenseRepository.markExpenseSynced(expense)
            }

            // Handle deleted expenses
            val deletedExpenses = expenseRepository.getDeletedExpenses().filter { it.groupId == groupId }
            for (expense in deletedExpenses) {
                try {
                    firestore.collection("groups/$groupId/expenses").document(expense.expenseId).delete().await()
                    expenseRepository.permanentDelete(expense.expenseId)
                    SyncLogger.d("Deleted expense ${expense.expenseId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting expense ${expense.expenseId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing expenses: ${e.message}", e)
        }
    }

    private suspend fun syncPenalties(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing penalties for: $groupId")
        try {
            val penaltiesSnapshot = firestore.collection("groups/$groupId/penalties")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            penaltiesSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(PenaltyFire::class.java)?.let { firebasePenalty ->
                        val localPenalty = penaltyRepository.getPenaltyById(firebasePenalty.id)
                        val firebaseTime = firebasePenalty.lastUpdated.toDate().time

                        if (localPenalty == null || firebaseTime > localPenalty.lastUpdated) {
                            penaltyRepository.insertPenalty(firebasePenalty.toLocal())
                        }
                    }
                }
            }

            penaltyRepository.getUnsyncedPenalties().filter { it.groupId == groupId }.forEach { penalty ->
                firestore.collection("groups/$groupId/penalties").document(penalty.penaltyId)
                    .set(penalty.toFirebase()).await()
                penaltyRepository.markPenaltySynced(penalty)
            }

            // Handle deleted penalties
            val deletedPenalties = penaltyRepository.getDeletedPenalties().filter { it.groupId == groupId }
            for (penalty in deletedPenalties) {
                try {
                    firestore.collection("groups/$groupId/penalties").document(penalty.penaltyId).delete().await()
                    penaltyRepository.permanentDelete(penalty.penaltyId)
                    SyncLogger.d("Deleted penalty ${penalty.penaltyId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting penalty ${penalty.penaltyId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing penalties: ${e.message}", e)
        }
    }

    private suspend fun syncUserGroups(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing user groups for: $groupId")
        try {
            val userGroupsSnapshot = firestore.collection("user_groups")
                .whereEqualTo("groupId", groupId)
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            userGroupsSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(UserGroupFire::class.java)?.let { firebaseUserGroup ->

                        if (groupRepository.getGroupById(groupId) == null) {
                            SyncLogger.d("Group $groupId missing in local DB")
                            syncGroupDetails(groupId, 0) // Force sync group details
                        }
                        val localUserGroup = userRepository.getUserGroup(
                            firebaseUserGroup.userId,
                            firebaseUserGroup.groupId
                        )
                        val firebaseTime = firebaseUserGroup.lastUpdated.toDate().time

                        if (localUserGroup == null) {
                            userRepository.insertUserGroup(firebaseUserGroup.toLocal())
                        } else if (firebaseTime > localUserGroup.lastUpdated) {
                            userRepository.updateUserGroup(firebaseUserGroup.toLocal())
                        }
                    }
                }
            }

            userRepository.getUnsyncedUserGroups().filter { it.groupId == groupId }.forEach { userGroup ->
                firestore.collection("user_groups")
                    .document("${userGroup.userId}_${userGroup.groupId}")
                    .set(userGroup.toFirebase()).await()
                userRepository.markUserGroupSynced(userGroup)
            }

            // Handle deleted user groups
            val deletedUserGroups = userRepository.getDeletedUserGroups().filter { it.groupId == groupId }
            for (userGroup in deletedUserGroups) {
                try {
                    firestore.collection("user_groups")
                        .document("${userGroup.userId}_${userGroup.groupId}")
                        .delete()
                        .await()
                    userRepository.permanentDeleteUserGroup(userGroup.userId, userGroup.groupId)
                    SyncLogger.d("Deleted user group ${userGroup.userId}_${userGroup.groupId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting user group: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing user groups: ${e.message}", e)
        }
    }

    private suspend fun syncMonthlySavingEntries(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing monthly saving entries for: $groupId")
        try {
            // Download entries from Firestore
            val entriesSnapshot = firestore.collection("groups/$groupId/monthly_saving_entries")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            entriesSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(MonthlySavingEntryFire::class.java)?.let { firebaseEntry ->
                        if (!verifyMonthlySavingEntryReferences(firebaseEntry)) {
                            SyncLogger.e("Skipping saving entry with missing references: ${firebaseEntry.entryId}")
                            return@let
                        }

                        val localEntry = savingRepository.getEntryById(firebaseEntry.entryId)
                        val firebaseTime = firebaseEntry.lastUpdated.toDate().time

                        if (localEntry == null) {
                            savingRepository.insertEntry(firebaseEntry.toLocal())
                        } else if (firebaseTime > localEntry.lastUpdated) {
                            savingRepository.updateEntry(firebaseEntry.toLocal())
                        }
                    }
                }
            }

            // Upload local unsynced entries
            savingRepository.getUnsyncedEntries()
                .filter { it.groupId == groupId }
                .forEach { entry ->
                    firestore.collection("groups/$groupId/monthly_saving_entries")
                        .document(entry.entryId)
                        .set(entry.toFirebase()).await()
                    savingRepository.markEntrySynced(entry)
                }

            // Handle deleted saving entries
            val deletedEntries = savingRepository.getDeletedEntries().filter { it.groupId == groupId }
            for (entry in deletedEntries) {
                try {
                    firestore.collection("groups/$groupId/monthly_saving_entries")
                        .document(entry.entryId)
                        .delete()
                        .await()
                    savingRepository.permanentDeleteEntry(entry.entryId)
                    SyncLogger.d("Deleted saving entry ${entry.entryId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting saving entry ${entry.entryId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing saving entries: ${e.message}", e)
        }
    }

    private suspend fun verifyMonthlySavingEntryReferences(entry: MonthlySavingEntryFire): Boolean {
        return savingRepository.getSavingById(entry.savingId) != null &&
                memberRepository.getMemberById(entry.memberId) != null
    }

    private suspend fun syncWelfares(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing welfares for: $groupId")
        try {
            val welfaresSnapshot = firestore.collection("groups/$groupId/welfares")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            welfaresSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(WelfareFire::class.java)?.let { firebaseWelfare ->
                        val localWelfare = welfareRepository.getWelfareById(firebaseWelfare.welfareId)
                        val firebaseTime = firebaseWelfare.lastUpdated.toDate().time

                        if (localWelfare == null || firebaseTime > localWelfare.lastUpdated) {
                            welfareRepository.insertWelfare(firebaseWelfare.toLocal())
                        }
                    }
                }
            }

            welfareRepository.getUnsyncedWelfares().filter { it.groupId == groupId }.forEach { welfare ->
                firestore.collection("groups/$groupId/welfares").document(welfare.welfareId)
                    .set(welfare.toFirebase()).await()
                welfareRepository.markWelfareAsSynced(welfare.welfareId)
            }

            // Handle deleted welfares
            val deletedWelfares = welfareRepository.getDeletedWelfares().filter { it.groupId == groupId }
            for (welfare in deletedWelfares) {
                try {
                    firestore.collection("groups/$groupId/welfares").document(welfare.welfareId).delete().await()
                    welfareRepository.permanentDeleteWelfare(welfare.welfareId)
                    SyncLogger.d("Deleted welfare ${welfare.welfareId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting welfare ${welfare.welfareId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing welfares: ${e.message}", e)
        }
    }

    private suspend fun syncWelfareMeetings(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing welfare meetings for: $groupId")
        try {
            val meetingsSnapshot = firestore.collection("groups/$groupId/welfare_meetings")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            meetingsSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(WelfareMeetingFire::class.java)?.let { firebaseMeeting ->
                        if (!verifyWelfareMeetingReferences(firebaseMeeting)) {
                            SyncLogger.e("Skipping welfare meeting with missing references: ${firebaseMeeting.meetingId}")
                            return@let
                        }

                        val localMeeting = welfareRepository.getMeetingById(firebaseMeeting.meetingId)
                        val firebaseTime = firebaseMeeting.lastUpdated.toDate().time

                        if (localMeeting == null || firebaseTime > localMeeting.lastUpdated) {
                            welfareRepository.insertMeeting(firebaseMeeting.toLocal())
                        }
                    }
                }
            }

            welfareRepository.getUnsyncedMeetings().filter { it.groupId == groupId }.forEach { meeting ->
                firestore.collection("groups/$groupId/welfare_meetings").document(meeting.meetingId)
                    .set(meeting.toFirebase()).await()
                welfareRepository.markMeetingAsSynced(meeting.meetingId)
            }

            // Handle deleted welfare meetings
            val deletedWelfareMeetings = welfareRepository.getDeletedMeetings().filter { it.groupId == groupId }
            for (meeting in deletedWelfareMeetings) {
                try {
                    firestore.collection("groups/$groupId/welfare_meetings").document(meeting.meetingId).delete().await()
                    welfareRepository.permanentDeleteWelfareMeetings(meeting.meetingId)
                    SyncLogger.d("Deleted welfare meeting ${meeting.meetingId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting welfare meeting ${meeting.meetingId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing welfare meetings: ${e.message}", e)
        }
    }

    private suspend fun verifyWelfareMeetingReferences(meeting: WelfareMeetingFire): Boolean {
        return welfareRepository.getWelfareById(meeting.welfareId) != null
    }

    private suspend fun syncMemberWelfareContributions(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing member welfare contributions for: $groupId")
        try {
            val contributionsSnapshot = firestore.collection("groups/$groupId/member_welfare_contributions")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            contributionsSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(MemberWelfareContributionFire::class.java)?.let { firebaseContribution ->
                        if (!verifyWelfareContributionReferences(firebaseContribution)) {
                            SyncLogger.e("Skipping welfare contribution with missing references: ${firebaseContribution.contributionId}")
                            return@let
                        }

                        val localContribution = welfareRepository.getContributionById(firebaseContribution.contributionId)
                        val firebaseTime = firebaseContribution.lastUpdated.toDate().time
                        if (localContribution == null || firebaseTime > localContribution.lastUpdated) {
                            welfareRepository.insertContribution(firebaseContribution.toLocal())
                        }
                    }
                }
            }

            welfareRepository.getUnsyncedContributions().filter { it.groupId == groupId }.forEach { contribution ->
                firestore.collection("groups/$groupId/member_welfare_contributions").document(contribution.contributionId)
                    .set(contribution.toFirebase()).await()
                welfareRepository.markContributionAsSynced(contribution.contributionId)
            }

            // Handle deleted welfare contributions
            val deletedWelfareContributions = welfareRepository.getDeletedContributions().filter { it.groupId == groupId }
            for (contribution in deletedWelfareContributions) {
                try {
                    firestore.collection("groups/$groupId/member_welfare_contributions").document(contribution.contributionId).delete().await()
                    welfareRepository.permanentDeleteContribution(contribution.contributionId)
                    SyncLogger.d("Deleted welfare contribution ${contribution.contributionId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting welfare contribution ${contribution.contributionId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing welfare contributions: ${e.message}", e)
        }
    }

    private suspend fun verifyWelfareContributionReferences(contribution: MemberWelfareContributionFire): Boolean {
        return memberRepository.getMemberById(contribution.memberId) != null &&
                welfareRepository.getMeetingById(contribution.meetingId) != null
    }

    private suspend fun syncWelfareBeneficiaries(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing welfare beneficiaries for: $groupId")
        try {
            val beneficiariesSnapshot = firestore.collection("groups/$groupId/welfare_beneficiaries")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            beneficiariesSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(WelfareBeneficiaryFire::class.java)?.let { firebaseBeneficiary ->
                        if (!verifyWelfareBeneficiaryReferences(firebaseBeneficiary)) {
                            SyncLogger.e("Skipping welfare beneficiary with missing references: ${firebaseBeneficiary.beneficiaryId}")
                            return@let
                        }

                        val localBeneficiary = welfareRepository.getBeneficiaryById(firebaseBeneficiary.beneficiaryId)
                        val firebaseTime = firebaseBeneficiary.lastUpdated.toDate().time
                        if (localBeneficiary == null || firebaseTime > localBeneficiary.lastUpdated) {
                            welfareRepository.insertBeneficiary(firebaseBeneficiary.toLocal())
                        }
                    }
                }
            }

            welfareRepository.getUnsyncedBeneficiaries().filter { it.groupId == groupId }.forEach { beneficiary ->
                firestore.collection("groups/$groupId/welfare_beneficiaries").document(beneficiary.beneficiaryId)
                    .set(beneficiary.toFirebase()).await()
                welfareRepository.markBeneficiaryAsSynced(beneficiary.beneficiaryId)
            }

            // Handle deleted welfare beneficiaries
            val deletedWelfareBeneficiaries = welfareRepository.getDeletedBeneficiaries().filter { it.groupId == groupId }
            for (beneficiary in deletedWelfareBeneficiaries) {
                try {
                    firestore.collection("groups/$groupId/welfare_beneficiaries").document(beneficiary.beneficiaryId).delete().await()
                    welfareRepository.permanentDeleteBeneficiary(beneficiary.beneficiaryId)
                    SyncLogger.d("Deleted welfare beneficiary ${beneficiary.beneficiaryId} from Firebase")
                } catch (e: Exception) {
                    SyncLogger.e("Error deleting welfare beneficiary ${beneficiary.beneficiaryId}: ${e.message}")
                }
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing welfare beneficiaries: ${e.message}", e)
        }
    }

    private suspend fun verifyWelfareBeneficiaryReferences(beneficiary: WelfareBeneficiaryFire): Boolean {
        return memberRepository.getMemberById(beneficiary.memberId) != null &&
                welfareRepository.getMeetingById(beneficiary.meetingId) != null
    }

    private suspend fun <T> withForeignKeyRetry(
        maxRetries: Int = 3,
        block: suspend () -> T
    ): T {
        var attempt = 0
        while (true) {
            try {
                return block()
            } catch (e: SQLException) {
                if (e.message?.contains("FOREIGN KEY") == true && attempt < maxRetries) {
                    attempt++
                    SyncLogger.d("Foreign key constraint detected, retrying ($attempt/$maxRetries)")
                    delay(100L * attempt)
                } else {
                    throw e
                }
            }
        }
    }
}