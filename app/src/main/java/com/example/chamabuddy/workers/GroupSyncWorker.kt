package com.example.chamabuddy.workers

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
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

        syncGroupDetails(groupId, lastSync)
        syncMembers(groupId, lastSync)
        syncCycles(groupId, lastSync)
        syncMeetings(groupId, lastSync)
        syncSavings(groupId, lastSync)
        syncMonthlySavingEntries(groupId, lastSync)
        syncContributions(groupId, lastSync)
        syncBeneficiaries(groupId, lastSync)
        syncBenefitEntities(groupId, lastSync)
        syncExpenseEntities(groupId, lastSync)
        syncPenalties(groupId, lastSync)
        syncUserGroups(groupId, lastSync)
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
            // 1. Download updates from Firebase
            val membersSnapshot = firestore.collection("groups/$groupId/members")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            membersSnapshot.documents.forEach { doc ->
                withForeignKeyRetry {
                    doc.toObject(MemberFire::class.java)?.let { firebaseMember ->
                        val localMember = memberRepository.getMemberById(firebaseMember.memberId)
                        val firebaseTime = firebaseMember.lastUpdated.toDate().time

                        // Always update if Firebase has newer data
                        if (localMember == null || firebaseTime > localMember.lastUpdated) {
                            memberRepository.syncMember(firebaseMember.toLocal())
                        }
                    }
                }
            }

            // 2. Upload local changes to Firebase
            memberRepository.getUnsyncedMembersForGroup(groupId).forEach { member ->
                // Ensure isActive field is included in the upload
                val memberData = member.toFirebase().apply {
                    // Explicitly ensure isActive is included
                    this.isActive = member.isActive
                }

                firestore.collection("groups/$groupId/members")
                    .document(member.memberId)
                    .set(memberData)
                    .await()

                memberRepository.markMemberSynced(member)
            }

            // 3. Handle deleted members
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
            val beneficiariesQuery = if (lastSync == 0L) {
                firestore.collection("groups/$groupId/beneficiaries").get()
            } else {
                firestore.collection("groups/$groupId/beneficiaries")
                    .whereGreaterThanOrEqualTo("lastUpdated", Timestamp(Date(lastSync))).get()
            }
            val beneficiariesSnapshot = beneficiariesQuery.await()

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
        // Check if member exists, if not try to sync it
        if (memberRepository.getMemberById(beneficiary.memberId) == null) {
            SyncLogger.d("Member ${beneficiary.memberId} missing, attempting to sync")
            syncSingleMember(beneficiary.groupId, beneficiary.memberId)
        }

        // Check if cycle exists, if not try to sync it
        if (cycleRepository.getCycleById(beneficiary.cycleId) == null) {
            SyncLogger.d("Cycle ${beneficiary.cycleId} missing, attempting to sync")
            syncSingleCycle(beneficiary.groupId, beneficiary.cycleId)
        }

        // Final check after attempting to sync dependencies
        return memberRepository.getMemberById(beneficiary.memberId) != null &&
                cycleRepository.getCycleById(beneficiary.cycleId) != null
    }
    private suspend fun syncSingleCycle(groupId: String, cycleId: String) {
        try {
            val doc = firestore.collection("groups/$groupId/cycles")
                .document(cycleId)
                .get()
                .await()

            doc.toObject(CycleFire::class.java)?.let { firebaseCycle ->
                cycleRepository.insertCycle(firebaseCycle.toLocal())
                SyncLogger.d("Synced missing cycle: $cycleId")
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing single cycle $cycleId: ${e.message}")
        }
    }
    private suspend fun syncSavings(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing savings for: $groupId")
        try {
            // For initial sync, fetch all savings without timestamp filter
            val savingsQuery = if (lastSync == 0L) {
                firestore.collection("groups/$groupId/monthly_savings").get()
            } else {
                firestore.collection("groups/$groupId/monthly_savings")
                    .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync))).get()
            }
            val savingsSnapshot = savingsQuery.await()

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


    private suspend fun syncSingleSaving(groupId: String, savingId: String) {
        try {
            val doc = firestore.collection("groups/$groupId/monthly_savings")
                .document(savingId)
                .get()
                .await()

            doc.toObject(MonthlySavingFire::class.java)?.let { firebaseSaving ->
                savingRepository.insertSaving(firebaseSaving.toLocal())
                SyncLogger.d("Synced missing saving: $savingId")
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing single saving $savingId: ${e.message}")
        }
    }

    private suspend fun syncSingleMember(groupId: String, memberId: String) {
        try {
            val doc = firestore.collection("groups/$groupId/members")
                .document(memberId)
                .get()
                .await()

            doc.toObject(MemberFire::class.java)?.let { firebaseMember ->
                memberRepository.syncMember(firebaseMember.toLocal())
                SyncLogger.d("Synced missing member: $memberId")
            }
        } catch (e: Exception) {
            SyncLogger.e("Error syncing single member $memberId: ${e.message}")
        }
    }

    private suspend fun syncBenefitEntities(groupId: String, lastSync: Long) {
        SyncLogger.d("Syncing benefits for: $groupId")
        try {
            val benefitsSnapshot = firestore.collection("groups/$groupId/benefits")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get().await()

            val processedIds = mutableSetOf<String>()

            benefitsSnapshot.documents.forEach { doc ->
                try {
                    doc.toObject(BenefitEntityFire::class.java)?.let { firebaseBenefit ->
                        // Check for duplicate processing
                        if (processedIds.contains(firebaseBenefit.id)) {
                            SyncLogger.d("Skipping duplicate benefit: ${firebaseBenefit.id}")
                            return@let
                        }
                        processedIds.add(firebaseBenefit.id)

                        // Check if benefit already exists locally
                        val existingBenefit = benefitRepository.getBenefitById(firebaseBenefit.id)
                        if (existingBenefit == null) {
                            SyncLogger.d("Inserting new benefit: ${firebaseBenefit.id}")
                            benefitRepository.insertBenefit(firebaseBenefit.toLocal())
                        } else {
                            // Compare timestamps to determine which version is newer
                            val firebaseTime = firebaseBenefit.lastUpdated.toDate().time
                            if (firebaseTime > existingBenefit.lastUpdated) {
                                SyncLogger.d("Updating existing benefit: ${firebaseBenefit.id}")
                                benefitRepository.updateBenefit(firebaseBenefit.toLocal())
                            } else if (firebaseTime < existingBenefit.lastUpdated) {
                                // Local version is newer, will upload in next step
                                SyncLogger.d("Local benefit is newer: ${firebaseBenefit.id}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    SyncLogger.e("Error processing benefit document: ${e.message}")
                }
            }

            // Upload local unsynced benefits
            benefitRepository.getUnsyncedBenefits()
                .filter { it.groupId == groupId }
                .forEach { benefit ->
                    val firestoreDoc = firestore.collection("groups/$groupId/benefits")
                        .document(benefit.benefitId)

                    // Check if document exists and compare timestamps
                    val existingDoc = firestoreDoc.get().await()
                    if (!existingDoc.exists()) {
                        firestoreDoc.set(benefit.toFirebase()).await()
                        benefitRepository.markBenefitSynced(benefit)
                        SyncLogger.d("Created new benefit in Firebase: ${benefit.benefitId}")
                    } else {
                        val existingBenefit = existingDoc.toObject(BenefitEntityFire::class.java)
                        val existingTime = existingBenefit?.lastUpdated?.toDate()?.time ?: 0

                        if (benefit.lastUpdated > existingTime) {
                            // Local version is newer, update Firebase
                            firestoreDoc.set(benefit.toFirebase()).await()
                            benefitRepository.markBenefitSynced(benefit)
                            SyncLogger.d("Updated benefit in Firebase: ${benefit.benefitId}")
                        } else if (benefit.lastUpdated < existingTime) {
                            // Firebase version is newer, update local
                            existingBenefit?.let {
                                benefitRepository.updateBenefit(it.toLocal())
                                SyncLogger.d("Updated local benefit from Firebase: ${benefit.benefitId}")
                            }
                        }
                        // If timestamps are equal, no action needed
                    }
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

            val processedIds = mutableSetOf<String>()

            expensesSnapshot.documents.forEach { doc ->
                try {
                    doc.toObject(ExpenseEntityFire::class.java)?.let { firebaseExpense ->
                        // Check for duplicate processing
                        if (processedIds.contains(firebaseExpense.id)) {
                            SyncLogger.d("Skipping duplicate expense: ${firebaseExpense.id}")
                            return@let
                        }
                        processedIds.add(firebaseExpense.id)

                        // Check if expense already exists locally
                        val existingExpense = expenseRepository.getExpenseById(firebaseExpense.id)
                        if (existingExpense == null) {
                            SyncLogger.d("Inserting new expense: ${firebaseExpense.id}")
                            expenseRepository.insertExpense(firebaseExpense.toLocal())
                        } else {
                            // Compare timestamps to determine which version is newer
                            val firebaseTime = firebaseExpense.lastUpdated.toDate().time
                            if (firebaseTime > existingExpense.lastUpdated) {
                                SyncLogger.d("Updating existing expense: ${firebaseExpense.id}")
                                expenseRepository.updateExpense(firebaseExpense.toLocal())
                            } else if (firebaseTime < existingExpense.lastUpdated) {
                                // Local version is newer, will upload in next step
                                SyncLogger.d("Local expense is newer: ${firebaseExpense.id}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    SyncLogger.e("Error processing expense document: ${e.message}")
                }
            }

            // Upload local unsynced expenses
            expenseRepository.getUnsyncedExpenses()
                .filter { it.groupId == groupId }
                .forEach { expense ->
                    val firestoreDoc = firestore.collection("groups/$groupId/expenses")
                        .document(expense.expenseId)

                    // Check if document exists and compare timestamps
                    val existingDoc = firestoreDoc.get().await()
                    if (!existingDoc.exists()) {
                        firestoreDoc.set(expense.toFirebase()).await()
                        expenseRepository.markExpenseSynced(expense)
                        SyncLogger.d("Created new expense in Firebase: ${expense.expenseId}")
                    } else {
                        val existingExpense = existingDoc.toObject(ExpenseEntityFire::class.java)
                        val existingTime = existingExpense?.lastUpdated?.toDate()?.time ?: 0

                        if (expense.lastUpdated > existingTime) {
                            // Local version is newer, update Firebase
                            firestoreDoc.set(expense.toFirebase()).await()
                            expenseRepository.markExpenseSynced(expense)
                            SyncLogger.d("Updated expense in Firebase: ${expense.expenseId}")
                        } else if (expense.lastUpdated < existingTime) {
                            // Firebase version is newer, update local
                            existingExpense?.let {
                                expenseRepository.updateExpense(it.toLocal())
                                SyncLogger.d("Updated local expense from Firebase: ${expense.expenseId}")
                            }
                        }
                        // If timestamps are equal, no action needed
                    }
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

            val processedIds = mutableSetOf<String>()

            penaltiesSnapshot.documents.forEach { doc ->
                try {
                    doc.toObject(PenaltyFire::class.java)?.let { firebasePenalty ->
                        // Check for duplicate processing
                        if (processedIds.contains(firebasePenalty.id)) {
                            SyncLogger.d("Skipping duplicate penalty: ${firebasePenalty.id}")
                            return@let
                        }
                        processedIds.add(firebasePenalty.id)

                        // Check if penalty already exists locally
                        val existingPenalty = penaltyRepository.getPenaltyById(firebasePenalty.id)
                        if (existingPenalty == null) {
                            SyncLogger.d("Inserting new penalty: ${firebasePenalty.id}")
                            penaltyRepository.insertPenalty(firebasePenalty.toLocal())
                        } else {
                            // Compare timestamps to determine which version is newer
                            val firebaseTime = firebasePenalty.lastUpdated.toDate().time
                            if (firebaseTime > existingPenalty.lastUpdated) {
                                SyncLogger.d("Updating existing penalty: ${firebasePenalty.id}")
                                penaltyRepository.updatePenalty(firebasePenalty.toLocal())
                            } else if (firebaseTime < existingPenalty.lastUpdated) {
                                // Local version is newer, will upload in next step
                                SyncLogger.d("Local penalty is newer: ${firebasePenalty.id}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    SyncLogger.e("Error processing penalty document: ${e.message}")
                }
            }

            // Upload local unsynced penalties
            penaltyRepository.getUnsyncedPenalties().filter { it.groupId == groupId }.forEach { penalty ->
                val firestoreDoc = firestore.collection("groups/$groupId/penalties")
                    .document(penalty.penaltyId)

                // Check if document exists and compare timestamps
                val existingDoc = firestoreDoc.get().await()
                if (!existingDoc.exists()) {
                    firestoreDoc.set(penalty.toFirebase()).await()
                    penaltyRepository.markPenaltySynced(penalty)
                    SyncLogger.d("Created new penalty in Firebase: ${penalty.penaltyId}")
                } else {
                    val existingPenalty = existingDoc.toObject(PenaltyFire::class.java)
                    val existingTime = existingPenalty?.lastUpdated?.toDate()?.time ?: 0

                    if (penalty.lastUpdated > existingTime) {
                        // Local version is newer, update Firebase
                        firestoreDoc.set(penalty.toFirebase()).await()
                        penaltyRepository.markPenaltySynced(penalty)
                        SyncLogger.d("Updated penalty in Firebase: ${penalty.penaltyId}")
                    } else if (penalty.lastUpdated < existingTime) {
                        // Firebase version is newer, update local
                        existingPenalty?.let {
                            penaltyRepository.updatePenalty(it.toLocal())
                            SyncLogger.d("Updated local penalty from Firebase: ${penalty.penaltyId}")
                        }
                    }
                    // If timestamps are equal, no action needed
                }
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
        var retryCount = 0
        val maxRetries = 3

        while (retryCount < maxRetries) {
            try {
                SyncLogger.d("Syncing monthly saving entries for: $groupId, lastSync: $lastSync")
                try {
                    // For initial sync, fetch all entries without timestamp filter
                    val entriesQuery = if (lastSync == 0L) {
                        SyncLogger.d("Performing initial sync for monthly saving entries")
                        firestore.collection("groups/$groupId/monthly_saving_entries").get()
                    } else {
                        SyncLogger.d("Performing incremental sync for monthly saving entries")
                        firestore.collection("groups/$groupId/monthly_saving_entries")
                            .whereGreaterThanOrEqualTo("lastUpdated", Timestamp(Date(lastSync))).get()
                    }
                    val entriesSnapshot = entriesQuery.await()
                    SyncLogger.d("Found ${entriesSnapshot.size()} monthly saving entries in Firestore")

                    entriesSnapshot.documents.forEach { doc ->
                        withForeignKeyRetry {
                            doc.toObject(MonthlySavingEntryFire::class.java)?.let { firebaseEntry ->
                                SyncLogger.d("Processing entry: ${firebaseEntry.entryId}, lastUpdated: ${firebaseEntry.lastUpdated.toDate().time}")


                                if (!verifyMonthlySavingEntryReferences(firebaseEntry)) {
                                    SyncLogger.e("Skipping saving entry with missing references: ${firebaseEntry.entryId}")
                                    return@let
                                }
                                // Check if this entry is marked as deleted locally
                                val localEntry = savingRepository.getEntryById(firebaseEntry.entryId)
                                if (localEntry?.isDeleted == true) {
                                    SyncLogger.d("Skipping deleted entry: ${firebaseEntry.entryId}")
                                    return@let
                                }

                                // Verify references exist
                                if (!verifyMonthlySavingEntryReferences(firebaseEntry)) {
                                    SyncLogger.e("Skipping saving entry with missing references: ${firebaseEntry.entryId}")
                                    SyncLogger.e("Saving ID: ${firebaseEntry.savingId}, Member ID: ${firebaseEntry.memberId}")

                                    // Try to sync missing references
                                    if (savingRepository.getSavingById(firebaseEntry.savingId) == null) {
                                        SyncLogger.d("Attempting to sync missing saving: ${firebaseEntry.savingId}")
                                        syncSingleSaving(groupId, firebaseEntry.savingId)
                                    }
                                    if (memberRepository.getMemberById(firebaseEntry.memberId) == null) {
                                        SyncLogger.d("Attempting to sync missing member: ${firebaseEntry.memberId}")
                                        syncSingleMember(groupId, firebaseEntry.memberId)
                                    }
                                    return@let
                                }

                                val firebaseTime = firebaseEntry.lastUpdated.toDate().time
                                if (localEntry == null) {
                                    SyncLogger.d("Inserting new monthly saving entry: ${firebaseEntry.entryId}")
                                    savingRepository.insertEntry(firebaseEntry.toLocal())
                                } else if (firebaseTime > localEntry.lastUpdated) {
                                    SyncLogger.d("Updating existing monthly saving entry: ${firebaseEntry.entryId}")
                                    savingRepository.updateEntry(firebaseEntry.toLocal())
                                } else {
                                    SyncLogger.d("No update needed for entry: ${firebaseEntry.entryId}")
                                }
                            }
                        }
                    }

                    // Upload local unsynced entries (excluding deleted ones)
                    val unsyncedEntries = savingRepository.getUnsyncedEntries()
                        .filter { it.groupId == groupId && !it.isDeleted }
                    SyncLogger.d("Found ${unsyncedEntries.size} unsynced local entries")

                    unsyncedEntries.forEach { entry ->
                        firestore.collection("groups/$groupId/monthly_saving_entries")
                            .document(entry.entryId)
                            .set(entry.toFirebase()).await()
                        savingRepository.markEntrySynced(entry)
                        SyncLogger.d("Uploaded unsynced entry: ${entry.entryId}")
                    }

                    // Handle deleted saving entries
                    val deletedEntries = savingRepository.getDeletedEntries().filter { it.groupId == groupId }
                    SyncLogger.d("Found ${deletedEntries.size} deleted entries to process")

                    for (entry in deletedEntries) {
                        try {
                            firestore.collection("groups/$groupId/monthly_saving_entries")
                                .document(entry.entryId)
                                .delete()
                                .await()
                            savingRepository.permanentDeleteEntry(entry.entryId)
                            SyncLogger.d("Deleted entry from remote: ${entry.entryId}")
                        } catch (e: Exception) {
                            if (e.message?.contains("NOT_FOUND") == true) {
                                savingRepository.permanentDeleteEntry(entry.entryId)
                                SyncLogger.d("Entry already deleted remotely: ${entry.entryId}")
                            } else {
                                SyncLogger.e("Error deleting entry ${entry.entryId}: ${e.message}")
                            }
                        }
                    }
                } catch (e: Exception) {
                    SyncLogger.e("Error syncing saving entries: ${e.message}", e)
                }

                break // ✅ success, exit retry loop
            } catch (e: SQLiteConstraintException) {
                if (e.message?.contains("FOREIGN KEY") == true) {
                    retryCount++
                    SyncLogger.d("Foreign key constraint violation, retry $retryCount/$maxRetries")
                    delay(1000L * retryCount) // exponential backoff

                    // Force re-sync dependencies before retry
                    syncSavings(groupId, 0)
                    syncMembers(groupId, 0)
                } else {
                    throw e
                }
            }
        }
    }


    private suspend fun verifyMonthlySavingEntryReferences(entry: MonthlySavingEntryFire): Boolean {
        val savingExists = savingRepository.getSavingById(entry.savingId) != null
        val memberExists = memberRepository.getMemberById(entry.memberId) != null

        if (!savingExists) {
            SyncLogger.d("Missing saving reference: ${entry.savingId}")
            syncSingleSaving(entry.groupId, entry.savingId)
        }

        if (!memberExists) {
            SyncLogger.d("Missing member reference: ${entry.memberId}")
            syncSingleMember(entry.groupId, entry.memberId)
        }

        return savingExists && memberExists
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