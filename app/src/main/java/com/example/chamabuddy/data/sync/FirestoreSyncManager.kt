package com.example.chamabuddy.data.sync

import android.content.Context
import android.content.SharedPreferences
import androidx.annotation.WorkerThread
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject

class FirestoreSyncManager @Inject constructor(
    private val context: Context
) {
    private val db = FirebaseFirestore.getInstance()
    private val prefs: SharedPreferences =
        context.getSharedPreferences("sync_prefs", Context.MODE_PRIVATE)

    suspend fun <T> syncEntity(
        collectionName: String,
        localFetch: suspend () -> List<T>,
        toFirebase: (T) -> Any,
        fromFirestore: (Map<String, Any>) -> T,
        updateLocal: suspend (T) -> Unit,
        getId: (T) -> String
    ) {
        // 1. Upload unsynced local records
        val unsyncedItems = localFetch()
        unsyncedItems.forEach { entity ->
            db.collection(collectionName)
                .document(getId(entity))
                .set(toFirebase(entity))
                .await()

            // Mark as synced in local DB
            updateLocal(entity)
        }

        // 2. Download updates from Firestore
        val lastSync = prefs.getLong("last_sync_$collectionName", 0)
        val snapshot = db.collection(collectionName)
            .whereGreaterThan("lastUpdated", Timestamp(Date(lastSync)))
            .get().await()

        for (doc in snapshot.documents) {
            val data = doc.data ?: continue
            val entity = fromFirestore(data)
            updateLocal(entity)
        }

        // 3. Update sync timestamp
        prefs.edit().putLong("last_sync_$collectionName", System.currentTimeMillis()).apply()
    }
}