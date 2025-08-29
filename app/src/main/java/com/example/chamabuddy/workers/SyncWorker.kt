package com.example.chamabuddy.workers

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.chamabuddy.data.local.preferences.SyncPreferences
import com.example.chamabuddy.data.remote.toFirebase
import com.example.chamabuddy.data.remote.toLocal
import com.example.chamabuddy.domain.Firebase.UserFire
import com.example.chamabuddy.domain.Firebase.GroupFire
import com.example.chamabuddy.domain.Firebase.UserGroupFire
import com.example.chamabuddy.domain.repository.UserRepository
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.util.SyncLogger
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import dagger.assisted.AssistedFactory

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val userRepository: UserRepository,
    private val groupRepository: GroupRepository,
    private val firestore: FirebaseFirestore,
    private val preferences: SyncPreferences,
    private val syncHelper: SyncHelper
) : CoroutineWorker(appContext, workerParams) {

    companion object {
        val syncStatus = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    }

    sealed class SyncStatus {
        object Idle : SyncStatus()
        object InProgress : SyncStatus()
        data class InProgressWithProgress(val current: Int, val total: Int) : SyncStatus()
        data class Success(val timestamp: Long) : SyncStatus()
        data class Failed(val message: String) : SyncStatus()
    }

    override suspend fun doWork(): Result {
        SyncLogger.d("✅✅✅ SYNC WORKER STARTED EXECUTION ✅✅✅")
        syncStatus.value = SyncStatus.InProgress

        return try {
            SyncLogger.d("⏳ Beginning sync process...")
            SyncLogger.d("User ID: ${preferences.getCurrentUserId()}")
            SyncLogger.d("Last sync timestamp: ${preferences.getLastSyncTimestamp()}")

            val now = System.currentTimeMillis()
            val userId = preferences.getCurrentUserId()
                ?: return Result.failure().also {
                    syncStatus.value = SyncStatus.Failed("User not logged in")
                    SyncLogger.e("❌ Sync aborted — no logged in user.")
                }

            var lastSync = preferences.getLastSyncTimestamp()
            val isInitialSync = lastSync == 0L
            if (isInitialSync) {
                // Trigger full sync for all groups
                val userGroups = preferences.getUserGroups()
                syncHelper.triggerGroupSync(userGroups)
            }

            // ----------- USERS SYNC (UPLOAD UNSYNCED & PULL UPDATES) ------------
            val unsyncedUsers = userRepository.getUnsyncedUsers()
            SyncLogger.d("Unsynced users count: ${unsyncedUsers.size}")
            unsyncedUsers.forEachIndexed { index, user ->
                firestore.collection("users").document(user.userId)
                    .set(user.toFirebase()).await()
                userRepository.markUserSynced(user)
                syncStatus.value = SyncStatus.InProgressWithProgress(index + 1, unsyncedUsers.size)
            }

            // Build user query properly (apply whereGreaterThan only when needed)
            val userQuery = if (!isInitialSync) {
                firestore.collection("users")
                    .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
            } else {
                firestore.collection("users")
            }

            // Replaced user sync: use insert-or-update with SQLiteConstraintException handling
            val firebaseUsers = userQuery.get().await()
            SyncLogger.d("Fetched ${firebaseUsers.size()} users from Firestore for update.")
            for (doc in firebaseUsers) {
                val firebaseUser = doc.toObject(UserFire::class.java)
                val localUser = userRepository.getUserById(firebaseUser.userId)

                // Use insertOrUpdate approach instead of separate insert/update
                if (localUser == null) {
                    try {
                        userRepository.insertUser(firebaseUser.toLocal())
                    } catch (e: SQLiteConstraintException) {
                        // If user already exists (race condition), update instead
                        SyncLogger.d("User ${firebaseUser.userId} already exists, updating")
                        userRepository.updateUser(firebaseUser.toLocal())
                    }
                } else if (firebaseUser.lastUpdated.toDate().time > localUser.lastUpdated) {
                    userRepository.updateUser(firebaseUser.toLocal())
                }
            }

            // ----------- USER GROUPS SYNC (CRITICAL SECTION WITH FIXES) ------------
            val userGroupsQuery = firestore.collection("user_groups")
                .whereEqualTo("userId", userId)
            val userGroupsSnapshot = userGroupsQuery.get().await()

            val userGroupIds = mutableSetOf<String>()
            for (doc in userGroupsSnapshot) {
                val userGroup = doc.toObject(UserGroupFire::class.java)
                userGroupIds.add(userGroup.groupId)

                // Check if group exists before inserting user-group relationship
                val groupExists = groupRepository.getGroupById(userGroup.groupId) != null

                if (!groupExists) {
                    SyncLogger.d("Group ${userGroup.groupId} missing for user-group relationship")

                    // Fetch missing group directly from Firestore
                    val groupDoc = firestore.collection("groups")
                        .document(userGroup.groupId)
                        .get()
                        .await()

                    groupDoc.toObject(GroupFire::class.java)?.let { firebaseGroup ->
                        // Insert group into local database
                        groupRepository.insertGroup(firebaseGroup.toLocal())
                        SyncLogger.d("Inserted missing group: ${firebaseGroup.groupId}")
                    } ?: run {
                        SyncLogger.e("Group document ${userGroup.groupId} not found in Firestore")
                    }
                }

                // Now safe to insert/update user-group relationship
                val localUserGroup = userRepository.getUserGroup(userId, userGroup.groupId)
                if (localUserGroup == null) {
                    userRepository.insertUserGroup(userGroup.toLocal())
                    SyncLogger.d("Inserted new user-group relationship for group: ${userGroup.groupId}")
                } else if (userGroup.lastUpdated.toDate().time > localUserGroup.lastUpdated) {
                    userRepository.updateUserGroup(userGroup.toLocal())
                    SyncLogger.d("Updated existing user-group relationship for group: ${userGroup.groupId}")
                }
            }
            preferences.setUserGroups(userGroupIds)
            SyncLogger.d("User belongs to ${userGroupIds.size} groups.")

            // ----------- GROUP SYNC MUST HAPPEN BEFORE UPLOADING USER-GROUP RELATIONSHIPS ------------
            // Upload local unsynced groups first so that when we upload user-group links, the groups exist on server
            val unsyncedGroups = groupRepository.getUnsyncedGroups()
            SyncLogger.d("Unsynced groups count: ${unsyncedGroups.size}")
            unsyncedGroups.forEachIndexed { index, group ->
                firestore.collection("groups").document(group.groupId)
                    .set(group.toFirebase()).await()
                groupRepository.markGroupSynced(group)
                syncStatus.value = SyncStatus.InProgressWithProgress(index + 1, unsyncedGroups.size)
            }

            // Now fetch group metadata from server for groups the user belongs to (in batches to respect whereIn limits)
            if (userGroupIds.isNotEmpty()) {
                val batches = userGroupIds.chunked(10)
                for (batch in batches) {
                    val groupQuery = if (!isInitialSync) {
                        firestore.collection("groups")
                            .whereIn("groupId", batch)
                            .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                    } else {
                        firestore.collection("groups")
                            .whereIn("groupId", batch)
                    }

                    val firebaseGroups = groupQuery.get().await()
                    SyncLogger.d("Fetched ${firebaseGroups.size()} groups from Firestore for update.")
                    for (doc in firebaseGroups) {
                        val firebaseGroup = doc.toObject(GroupFire::class.java)
                        val localGroup = groupRepository.getGroupById(firebaseGroup.groupId)
                        // Always insert group if missing
                        if (localGroup == null) {
                            groupRepository.insertGroup(firebaseGroup.toLocal())
                        } else if (firebaseGroup.lastUpdated.toDate().time > localGroup.lastUpdated) {
                            groupRepository.updateGroup(firebaseGroup.toLocal())
                        }
                    }
                }
            }

            // NEW: Trigger any group-level sync helper logic (e.g., sync cycles/members for newly discovered groups)
            if (userGroupIds.isNotEmpty()) {
                syncHelper.triggerGroupSync(userGroupIds)
                SyncLogger.d("Triggered group sync for ${userGroupIds.size} groups.")
            }

            // ----------- UPLOAD USER-GROUP RELATIONSHIPS (NOW THAT GROUPS ARE PRESENT) ------------
            val unsyncedUserGroups = userRepository.getUnsyncedUserGroups()
            SyncLogger.d("Unsynced user-group relationships count: ${unsyncedUserGroups.size}")
            unsyncedUserGroups.forEachIndexed { index, userGroup ->
                firestore.collection("user_groups")
                    .document("${userGroup.userId}_${userGroup.groupId}")
                    .set(userGroup.toFirebase()).await()
                userRepository.markUserGroupSynced(userGroup)
                syncStatus.value =
                    SyncStatus.InProgressWithProgress(index + 1, unsyncedUserGroups.size)
            }

            // ----------- FINALIZE ------------
            preferences.saveSyncTimestamp(now)
            preferences.setInitialSyncComplete()
            syncStatus.value = SyncStatus.Success(now)

            SyncLogger.d("🟢 Sync completed successfully")
            Result.success()

        } catch (e: Exception) {
            syncStatus.value = SyncStatus.Failed(e.message ?: "Unknown error")
            SyncLogger.e("🔴 Sync FAILED: ${e.message}", e)
            Result.failure(workDataOf("error" to e.message))
        }
    }
}
