package com.example.chamabuddy

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.example.chamabuddy.util.setupLogging
import com.example.chamabuddy.workers.SyncWorker
import com.google.firebase.FirebaseApp
import dagger.hilt.android.HiltAndroidApp
import java.util.concurrent.TimeUnit
import javax.inject.Inject

@HiltAndroidApp
class ChamaBuddy : Application(), Configuration.Provider {

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override fun getWorkManagerConfiguration(): Configuration {
        return Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        setupLogging()
        FirebaseApp.initializeApp(this)
        scheduleSync()
    }

    private fun scheduleSync() {
        val workManager = WorkManager.getInstance(this)
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            6, TimeUnit.HOURS
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            "full_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }
}
