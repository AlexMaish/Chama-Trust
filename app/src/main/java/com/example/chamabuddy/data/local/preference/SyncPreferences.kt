package com.example.chamabuddy.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

private val Context.syncDataStore: DataStore<Preferences> by preferencesDataStore(name = "sync_prefs")

@Singleton
class SyncPreferences @Inject constructor(
    @ApplicationContext context: Context
){

    private val dataStore = context.syncDataStore
    private val LAST_SYNC_KEY = longPreferencesKey("last_sync_timestamp")

    suspend fun getLastSyncTimestamp(): Long {
        return dataStore.data.firstOrNull()?.get(LAST_SYNC_KEY) ?: 0L
    }

    suspend fun saveSyncTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_KEY] = timestamp
        }
    }
}
