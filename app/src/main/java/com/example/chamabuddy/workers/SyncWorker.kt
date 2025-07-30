package com.example.chamabuddy.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.ListenableWorker.Result
import com.example.chamabuddy.data.sync.SyncRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.MutableStateFlow

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val syncRepository: SyncRepository
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
        // Delegate sync logic to SyncRepository
        val result = syncRepository.performSync()
        if (result == Result.success()) {
            syncStatus.value = SyncStatus.Success(System.currentTimeMillis())
        } else if (result == Result.retry()) {
            syncStatus.value = SyncStatus.Failed("Retrying")
        }
        return result
    }
}
