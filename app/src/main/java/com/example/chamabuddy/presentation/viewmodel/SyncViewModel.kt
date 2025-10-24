package com.example.chamabuddy.presentation.viewmodel

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.*
import com.example.chamabuddy.data.local.preferences.SyncPreferences
import com.example.chamabuddy.workers.SyncHelper
import com.example.chamabuddy.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SyncViewModel @Inject constructor(
    private val workManager: WorkManager,
    private val connectivityManager: ConnectivityManager,
    private val syncHelper: SyncHelper,
    private val syncPreferences: SyncPreferences
) : ViewModel() {
    private val _isOnline = MutableStateFlow(false)
    val isOnline: StateFlow<Boolean> = _isOnline

    val syncStatus = SyncWorker.syncStatus

    private val _showNetworkRestored = MutableStateFlow(false)
    val showNetworkRestored: StateFlow<Boolean> = _showNetworkRestored

    private val callback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            if (!_isOnline.value) {
                _showNetworkRestored.value = true
            }
            _isOnline.value = true
            triggerSync()
        }

        override fun onLost(network: Network) {
            _isOnline.value = false
        }
    }

    init {
        observeNetworkState()

        viewModelScope.launch {
            _isOnline.collect { online ->
                if (online) {
                    syncHelper.triggerFullSync()
                }
            }
        }
    }

    private fun observeNetworkState() {
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        connectivityManager.registerNetworkCallback(request, callback)
    }
    fun triggerSync() {
        viewModelScope.launch {
            try {
                val groupIds = syncPreferences.getUserGroups()
                if (groupIds.isNotEmpty()) {
                    syncHelper.triggerGroupSync(groupIds)
                } else {
                    println("No groups found for sync.")
                }
            } catch (e: Exception) {
                e.printStackTrace()

            }
        }
    }


    fun resetNetworkRestored() {
        _showNetworkRestored.value = false
    }

    override fun onCleared() {
        super.onCleared()
        connectivityManager.unregisterNetworkCallback(callback)
    }
}
