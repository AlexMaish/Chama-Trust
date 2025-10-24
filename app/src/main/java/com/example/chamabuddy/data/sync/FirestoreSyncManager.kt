package com.example.chamabuddy.data.sync

import android.content.Context
import android.content.SharedPreferences
import com.example.chamabuddy.data.remote.*
import com.example.chamabuddy.domain.Firebase.*
import com.example.chamabuddy.domain.model.*
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.logging.Logger
import javax.inject.Inject

class FirestoreSyncManager @Inject constructor(
    private val context: Context
) {
    private val db = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)
    private val logger = Logger.getLogger("FirestoreSync")

    suspend fun syncUsers(localFetch: suspend () -> List<User>, updateLocal: suspend (User) -> Unit) =
        syncEntity("users", localFetch, User::toFirebase, ::toUser, updateLocal, User::userId)

    suspend fun syncGroups(localFetch: suspend () -> List<Group>, updateLocal: suspend (Group) -> Unit) =
        syncEntity("groups", localFetch, Group::toFirebase, ::toGroup, updateLocal, Group::groupId)

    suspend fun syncCycles(localFetch: suspend () -> List<Cycle>, updateLocal: suspend (Cycle) -> Unit) =
        syncEntity("cycles", localFetch, Cycle::toFirebase, ::toCycle, updateLocal, Cycle::cycleId)

    suspend fun syncMembers(localFetch: suspend () -> List<Member>, updateLocal: suspend (Member) -> Unit) =
        syncEntity("members", localFetch, Member::toFirebase, ::toMember, updateLocal, Member::memberId)

    suspend fun syncGroupMembers(localFetch: suspend () -> List<GroupMember>, updateLocal: suspend (GroupMember) -> Unit) =
        syncEntity("group_members", localFetch, GroupMember::toFirebase, ::toGroupMember, updateLocal) {
            "${it.groupId}_${it.userId}"
        }

    suspend fun syncUserGroups(localFetch: suspend () -> List<UserGroup>, updateLocal: suspend (UserGroup) -> Unit) =
        syncEntity("user_groups", localFetch, UserGroup::toFirebase, ::toUserGroup, updateLocal) {
            "${it.userId}_${it.groupId}"
        }

    suspend fun syncWeeklyMeetings(localFetch: suspend () -> List<WeeklyMeeting>, updateLocal: suspend (WeeklyMeeting) -> Unit) =
        syncEntity("weekly_meetings", localFetch, WeeklyMeeting::toFirebase, ::toWeeklyMeeting, updateLocal, WeeklyMeeting::meetingId)

    suspend fun syncMemberContributions(localFetch: suspend () -> List<MemberContribution>, updateLocal: suspend (MemberContribution) -> Unit) =
        syncEntity("member_contributions", localFetch, MemberContribution::toFirebase, ::toMemberContribution, updateLocal, MemberContribution::contributionId)

    suspend fun syncBeneficiaries(localFetch: suspend () -> List<Beneficiary>, updateLocal: suspend (Beneficiary) -> Unit) =
        syncEntity("beneficiaries", localFetch, Beneficiary::toFirebase, ::toBeneficiary, updateLocal, Beneficiary::beneficiaryId)

    suspend fun syncMonthlySavings(localFetch: suspend () -> List<MonthlySaving>, updateLocal: suspend (MonthlySaving) -> Unit) =
        syncEntity("monthly_savings", localFetch, MonthlySaving::toFirebase, ::toMonthlySaving, updateLocal, MonthlySaving::savingId)

    suspend fun syncMonthlySavingEntries(localFetch: suspend () -> List<MonthlySavingEntry>, updateLocal: suspend (MonthlySavingEntry) -> Unit) =
        syncEntity("monthly_saving_entries", localFetch, MonthlySavingEntry::toFirebase, ::toMonthlySavingEntry, updateLocal, MonthlySavingEntry::entryId)

    suspend fun syncBenefitEntities(localFetch: suspend () -> List<BenefitEntity>, updateLocal: suspend (BenefitEntity) -> Unit) =
        syncEntity("benefit_entities", localFetch, BenefitEntity::toFirebase, ::toBenefitEntity, updateLocal, BenefitEntity::benefitId)

    suspend fun syncExpenseEntities(localFetch: suspend () -> List<ExpenseEntity>, updateLocal: suspend (ExpenseEntity) -> Unit) =
        syncEntity("expense_entities", localFetch, ExpenseEntity::toFirebase, ::toExpenseEntity, updateLocal, ExpenseEntity::expenseId)

    suspend fun syncPenalties(localFetch: suspend () -> List<Penalty>, updateLocal: suspend (Penalty) -> Unit) =
        syncEntity("penalties", localFetch, Penalty::toFirebase, ::toPenalty, updateLocal, Penalty::penaltyId)

    suspend fun syncMemberWelfareContributions(localFetch: suspend () -> List<MemberWelfareContribution>, updateLocal: suspend (MemberWelfareContribution) -> Unit) =
        syncEntity("member_welfare_contributions", localFetch, MemberWelfareContribution::toFirebase, ::toMemberWelfareContribution, updateLocal, MemberWelfareContribution::contributionId)

    suspend fun syncWelfareBeneficiaries(localFetch: suspend () -> List<WelfareBeneficiary>, updateLocal: suspend (WelfareBeneficiary) -> Unit) =
        syncEntity("welfare_beneficiaries", localFetch, WelfareBeneficiary::toFirebase, ::toWelfareBeneficiary, updateLocal, WelfareBeneficiary::beneficiaryId)

    suspend fun syncWelfares(localFetch: suspend () -> List<Welfare>, updateLocal: suspend (Welfare) -> Unit) =
        syncEntity("welfares", localFetch, Welfare::toFirebase, ::toWelfare, updateLocal, Welfare::welfareId)

    suspend fun syncWelfareMeetings(localFetch: suspend () -> List<WelfareMeeting>, updateLocal: suspend (WelfareMeeting) -> Unit) =
        syncEntity("welfare_meetings", localFetch, WelfareMeeting::toFirebase, ::toWelfareMeeting, updateLocal, WelfareMeeting::meetingId)

    private suspend fun <T> syncEntity(
        collectionName: String,
        localFetch: suspend () -> List<T>,
        toFirebase: (T) -> Any,
        fromFirestore: (Map<String, Any>) -> T,
        updateLocal: suspend (T) -> Unit,
        getId: (T) -> String
    ) {
        try {
            val lastSync = prefs.getLong("last_sync_$collectionName", 0)
            val currentTime = System.currentTimeMillis()

            val unsyncedItems = localFetch()
            val uploadResults = unsyncedItems.map { entity ->
                try {
                    db.collection(collectionName)
                        .document(getId(entity))
                        .set(toFirebase(entity))
                        .await()
                    true
                } catch (e: Exception) {
                    logger.severe("Upload failed for ${getId(entity)}: ${e.message}")
                    false
                }
            }

            unsyncedItems.forEachIndexed { index, entity ->
                if (uploadResults[index]) {
                    updateLocal(entity)
                }
            }

            val snapshot = db.collection(collectionName)
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get()
                .await()

            snapshot.documents.forEach { doc ->
                try {
                    val data = doc.data ?: return@forEach
                    val remoteTime = (data["lastUpdated"] as? Timestamp)?.toDate()?.time ?: 0

                    if (remoteTime > lastSync) {
                        val entity = fromFirestore(data)
                        updateLocal(entity)
                    }
                } catch (e: Exception) {
                    logger.severe("Download error: ${e.message}")
                }
            }

            prefs.edit().putLong("last_sync_$collectionName", currentTime).apply()

        } catch (e: Exception) {
            logger.severe("Sync failed for $collectionName: ${e.stackTraceToString()}")
            throw e
        }
    }

    private fun toUser(data: Map<String, Any>) = data.toUserFire().toLocal()
    private fun toGroup(data: Map<String, Any>) = data.toGroupFire().toLocal()
    private fun toCycle(data: Map<String, Any>) = data.toCycleFire().toLocal()
    private fun toMember(data: Map<String, Any>) = data.toMemberFire().toLocal()
    private fun toGroupMember(data: Map<String, Any>) = data.toGroupMemberFire().toLocal()
    private fun toUserGroup(data: Map<String, Any>) = data.toUserGroupFire().toLocal()
    private fun toWeeklyMeeting(data: Map<String, Any>) = data.toWeeklyMeetingFire().toLocal()
    private fun toMemberContribution(data: Map<String, Any>) = data.toMemberContributionFire().toLocal()
    private fun toBeneficiary(data: Map<String, Any>) = data.toBeneficiaryFire().toLocal()
    private fun toMonthlySaving(data: Map<String, Any>) = data.toMonthlySavingFire().toLocal()
    private fun toMonthlySavingEntry(data: Map<String, Any>) = data.toMonthlySavingEntryFire().toLocal()
    private fun toBenefitEntity(data: Map<String, Any>) = data.toBenefitEntityFire().toLocal()
    private fun toExpenseEntity(data: Map<String, Any>) = data.toExpenseEntityFire().toLocal()
    private fun toPenalty(data: Map<String, Any>) = data.toPenaltyFire().toLocal()

    private fun toMemberWelfareContribution(data: Map<String, Any>) = data.toMemberWelfareContributionFire().toLocal()
    private fun toWelfareBeneficiary(data: Map<String, Any>) = data.toWelfareBeneficiaryFire().toLocal()
    private fun toWelfare(data: Map<String, Any>) = data.toWelfareFire().toLocal()
    private fun toWelfareMeeting(data: Map<String, Any>) = data.toWelfareMeetingFire().toLocal()
}
