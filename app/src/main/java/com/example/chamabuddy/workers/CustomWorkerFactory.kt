// CustomWorkerFactory.kt
package com.example.chamabuddy.workers

import android.content.Context
import androidx.work.ListenableWorker
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import com.example.chamabuddy.data.local.preferences.SyncPreferences
import com.example.chamabuddy.data.sync.SyncRepository
import com.example.chamabuddy.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore

class CustomWorkerFactory(
    private val syncWorkerFactory: SyncWorkerFactory
) : WorkerFactory() {

    override fun createWorker(
        appContext: Context,
        workerClassName: String,
        workerParameters: WorkerParameters
    ): ListenableWorker? {
        return when (workerClassName) {
            SyncWorker::class.java.name -> {
                syncWorkerFactory.create(appContext, workerParameters)
            }
            else -> null
        }
    }
}


class SyncWorkerFactory(
    private val userRepository: UserRepository,
    private val firestore: FirebaseFirestore,
    private val preferences: SyncPreferences
) {
    fun create(
        context: Context,
        params: WorkerParameters
    ): SyncWorker = SyncWorker(context, params, userRepository, firestore, preferences)
}
