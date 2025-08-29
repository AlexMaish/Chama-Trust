package com.example.chamabuddy.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.example.chamabuddy.util.SyncLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import java.util.concurrent.atomic.AtomicBoolean

@Singleton
class SyncHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {
    private var isSyncing = AtomicBoolean(false)

    fun triggerFullSync() {
        if (isSyncing.getAndSet(true)) {
            SyncLogger.d("Sync already in progress, skipping")
            return
        }

        SyncLogger.d("🚀 Enqueuing full sync work")
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .addTag("full_sync_work")
            .build()

        workManager.enqueueUniqueWork(
            "full_sync_work",
            ExistingWorkPolicy.KEEP, // Changed from REPLACE to KEEP
            request
        )

        // Reset sync flag when work completes
        workManager.getWorkInfoByIdLiveData(request.id)
            .observeForever { workInfo ->
                if (workInfo?.state?.isFinished == true) {
                    isSyncing.set(false)
                }
            }

        SyncLogger.d("📝 Work enqueued with ID: ${request.id}")
    }

    // Add similar coordination to triggerGroupSync
    fun triggerGroupSync(groupIds: Set<String>) {
        if (groupIds.isEmpty() || !isNetworkAvailable()) {
            if (groupIds.isEmpty()) SyncLogger.d("No groups to sync - skipping")
            else SyncLogger.d("Network unavailable - skipping group sync")
            return
        }

        val data = Data.Builder()
            .putStringArray("group_ids", groupIds.toTypedArray())
            .build()

        val request = OneTimeWorkRequestBuilder<GroupSyncWorker>()
            .setInputData(data)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "group_sync_${System.currentTimeMillis()}",
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    fun schedulePeriodicSync() {
        SyncLogger.d("Scheduling periodic sync")

        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            4, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        val groupSyncRequest = PeriodicWorkRequestBuilder<GroupSyncWorker>(
            6, TimeUnit.HOURS,
            30, TimeUnit.MINUTES
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "periodic_full_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )

        workManager.enqueueUniquePeriodicWork(
            "periodic_group_sync",
            ExistingPeriodicWorkPolicy.KEEP,
            groupSyncRequest
        )
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
