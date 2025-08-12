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
    private val userRepository: UserRepository
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

        // MUST run sequentially in this exact order
        syncGroupDetails(groupId, lastSync)
        syncMembers(groupId, lastSync)
        syncCycles(groupId, lastSync)
        syncMeetings(groupId, lastSync)
        syncBeneficiaries(groupId, lastSync)  // Now after cycles/members
        syncContributions(groupId, lastSync)   // Now after cycles/members
        syncSavings(groupId, lastSync)
        syncMonthlySavingEntries(groupId, lastSync)
        syncBenefitEntities(groupId, lastSync)
        syncExpenseEntities(groupId, lastSync)
        syncPenalties(groupId, lastSync)
        syncUserGroups(groupId, lastSync)

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
        } catch (e: Exception) {
            SyncLogger.e("Error syncing contributions: ${e.message}", e)
        }
    }

    private suspend fun verifyContributionReferences(contribution: MemberContributionFire): Boolean {
        val memberExists = memberRepository.getMemberById(contribution.memberId) != null

        val cycleExists = if (contribution.meetingId != null) {
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
        } catch (e: Exception) {
            SyncLogger.e("Error syncing user groups: ${e.message}", e)
        }
    }
    // Add to GroupSyncWorker class
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
        } catch (e: Exception) {
            SyncLogger.e("Error syncing saving entries: ${e.message}", e)
        }
    }

    private suspend fun verifyMonthlySavingEntryReferences(entry: MonthlySavingEntryFire): Boolean {
        return savingRepository.getSavingById(entry.savingId) != null &&
                memberRepository.getMemberById(entry.memberId) != null
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