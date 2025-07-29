package com.example.chamabuddy


import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.chamabuddy.workers.SyncWorker
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ChamaBuddy : Application() {
    @Inject lateinit var workManager: WorkManager

    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        scheduleSync()
    }

    private fun scheduleSync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            6, // Repeat every 6 hours
            TimeUnit.HOURS
        ).setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
        ).build()

        workManager.enqueueUniquePeriodicWork(
            "member_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}