package com.example.chamabuddy.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chamabuddy.data.local.preferences.SyncPreferences

import com.example.chamabuddy.data.remote.toFirebase
import com.example.chamabuddy.data.remote.toLocal

import com.example.chamabuddy.domain.Firebase.UserFire
import com.example.chamabuddy.domain.repository.UserRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.tasks.await
import java.util.Date

class SyncWorker(
    context: Context,
    params: WorkerParameters,
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val preferences: SyncPreferences
) : CoroutineWorker(context, params) {

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
        syncStatus.value = SyncStatus.InProgress
        return try {
            // Sync local unsynced users to Firebase
            val unsyncedUsers = userRepository.getUnsyncedUsers()
            val total = unsyncedUsers.size
            unsyncedUsers.forEachIndexed { index, user ->
                firestore.collection("users").document(user.userId)
                    .set(user.toFirebase())
                    .await()
                userRepository.markUserSynced(user)
                syncStatus.value = SyncStatus.InProgressWithProgress(index + 1, total)
            }

            // Pull updated users from Firebase
            val lastSync = preferences.getLastSyncTimestamp()
            val firebaseUsers = firestore.collection("users")
                .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
                .get()
                .await()

            for (doc in firebaseUsers) {
                val firebaseUser = doc.toObject(UserFire::class.java)
                val localUser = userRepository.getUserById(firebaseUser.userId)

                if (localUser == null) {
                    userRepository.insertUser(firebaseUser.toLocal())
                } else if (firebaseUser.lastUpdated.toDate().time > localUser.lastUpdated) {
                    userRepository.updateUser(firebaseUser.toLocal())
                }
            }

            // Save sync timestamp
            val now = System.currentTimeMillis()
            preferences.saveSyncTimestamp(now)
            syncStatus.value = SyncStatus.Success(now)
            Result.success()
        } catch (e: Exception) {
            syncStatus.value = SyncStatus.Failed(e.message ?: "Unknown error")
            Result.retry()
        }
    }
}
