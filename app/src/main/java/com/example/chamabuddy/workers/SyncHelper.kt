package com.example.chamabuddy.workers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.work.*
import com.example.chamabuddy.util.SyncLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import androidx.work.await
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Singleton
class SyncHelper @Inject constructor(
    @ApplicationContext private val context: Context,
    private val workManager: WorkManager
) {

    fun triggerGroupSync(groupIds: Set<String>) {
        if (!isNetworkAvailable()) {
            SyncLogger.d("Network unavailable - skipping group sync")
            return
        }

        SyncLogger.d("Triggering sync for groups: $groupIds")

        if (groupIds.isEmpty()) {
            SyncLogger.d("No groups to sync - skipping")
            return
        }

        val data = Data.Builder()
            .putStringArray("group_ids", groupIds.toTypedArray())
            .build()

        val request = OneTimeWorkRequestBuilder<GroupSyncWorker>()
            .setInputData(data)
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



    fun triggerFullSync() {
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
            ExistingWorkPolicy.REPLACE,
            request
        )

        SyncLogger.d("📝 Work enqueued with ID: ${request.id}")
    }

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network)
        return capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
    }
}
