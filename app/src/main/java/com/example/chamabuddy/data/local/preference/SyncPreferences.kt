package com.example.chamabuddy.data.local.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
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
    private val GROUP_SYNC_PREFIX = "last_sync_group_"
    private val USER_GROUPS_KEY = stringPreferencesKey("user_groups")

    private val CURRENT_USER_ID = stringPreferencesKey("current_user_id")

    suspend fun setCurrentUserId(userId: String) {
        dataStore.edit { prefs ->
            prefs[CURRENT_USER_ID] = userId
        }
    }

    suspend fun getCurrentUserId(): String? {
        return dataStore.data.firstOrNull()?.get(CURRENT_USER_ID)
    }

    private val INITIAL_SYNC_COMPLETE_KEY = booleanPreferencesKey("initial_sync_complete")

    suspend fun setInitialSyncComplete() {
        dataStore.edit { prefs ->
            prefs[INITIAL_SYNC_COMPLETE_KEY] = true
        }
    }

    suspend fun isInitialSyncComplete(): Boolean {
        return dataStore.data.firstOrNull()?.get(INITIAL_SYNC_COMPLETE_KEY) ?: false
    }


    suspend fun getLastSyncTimestamp(): Long {
        return dataStore.data.firstOrNull()?.get(LAST_SYNC_KEY) ?: 0L
    }

    suspend fun saveSyncTimestamp(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[LAST_SYNC_KEY] = timestamp
        }
    }



    suspend fun setUserGroups(groupIds: Set<String>) {
        dataStore.edit { prefs ->
            prefs[USER_GROUPS_KEY] = groupIds.joinToString(",")
        }
    }

    suspend fun getUserGroups(): Set<String> {
        return dataStore.data.firstOrNull()
            ?.get(USER_GROUPS_KEY)
            ?.split(",")
            ?.filter { it.isNotEmpty() }
            ?.toSet() ?: emptySet()
    }


    suspend fun getLastGroupSync(groupId: String): Long {
        return dataStore.data.firstOrNull()
            ?.get(longPreferencesKey("last_sync_group_$groupId")) ?: 0L
    }

    suspend fun setLastGroupSync(groupId: String, timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[longPreferencesKey("last_sync_group_$groupId")] = timestamp
        }
    }

}
