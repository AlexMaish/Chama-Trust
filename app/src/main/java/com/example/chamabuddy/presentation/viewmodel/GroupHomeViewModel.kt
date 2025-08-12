package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.WorkManager
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.ExistingWorkPolicy
import androidx.work.WorkInfo
import com.example.chamabuddy.data.local.preferences.SyncPreferences
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.UserRepository
import com.example.chamabuddy.workers.SyncHelper
import com.example.chamabuddy.workers.SyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupHomeViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository,
    private val syncHelper: SyncHelper,
    private val syncPreferences: SyncPreferences,
    private val workManager: WorkManager // Added this dependency
) : ViewModel() {

    // Add sync state tracking
    sealed class SyncState {
        object Idle : SyncState()
        object LoadingGroups : SyncState()
        object SyncingData : SyncState()
        object Complete : SyncState()
        data class Error(val message: String) : SyncState()
    }

    private val _syncState = MutableStateFlow<SyncState>(SyncState.Idle)
    val syncState: StateFlow<SyncState> = _syncState.asStateFlow()

    private val _uiState = MutableStateFlow(GroupHomeUiState())
    val uiState: StateFlow<GroupHomeUiState> = _uiState.asStateFlow()

    private var hasPerformedInitialSync = false
    init {
        loadUserGroups()
        triggerInitialSync()
    }

    private fun triggerInitialSync() {
        viewModelScope.launch {
            syncHelper.triggerFullSync()
        }
    }

    private fun triggerDataSync(groupIds: Set<String>) {
        viewModelScope.launch {
            syncHelper.triggerGroupSync(groupIds)
        }
    }

    private fun loadUserGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            _syncState.value = SyncState.LoadingGroups

            try {
                val userId = userRepository.getCurrentUserId() ?: return@launch
                val groups = groupRepository.getUserGroups(userId)

                if (groups.isEmpty()) {
                    triggerFullUserSync(userId)
                } else {
                    val groupIds = groups.map { it.groupId }.toSet()
                    syncPreferences.setUserGroups(groupIds)

                    // Replace deep sync with generic sync
                    triggerDataSync(groupIds)

                    _uiState.value = _uiState.value.copy(
                        groups = groups,
                        isLoading = false,
                        isSyncComplete = true
                    )
                    _syncState.value = SyncState.Complete
                }
            } catch (e: Exception) {
                _syncState.value = SyncState.Error("Failed to load groups: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    snackbarMessage = "Failed to load groups"
                )
            }
        }
    }

    private fun triggerGroupSync(groupIds: Set<String>) {
        viewModelScope.launch {
            syncHelper.triggerGroupSync(groupIds)
        }
    }
    private fun triggerFullUserSync(userId: String) {
        _syncState.value = SyncState.SyncingData

        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        workManager.enqueueUniqueWork(
            "full_user_sync_$userId",
            ExistingWorkPolicy.KEEP,
            request
        )

        workManager.getWorkInfoByIdLiveData(request.id).observeForever { info ->
            when (info?.state) {
                WorkInfo.State.SUCCEEDED -> {
                    viewModelScope.launch {
                        val groups = groupRepository.getUserGroups(userId)
                        val groupIds = groups.map { it.groupId }.toSet()
                        syncPreferences.setUserGroups(groupIds)

                        if (groupIds.isNotEmpty()) {
                            syncHelper.triggerGroupSync(groupIds)
                        }

                        _uiState.value = _uiState.value.copy(
                            groups = groups,
                            isLoading = false,
                            isSyncComplete = true
                        )
                        _syncState.value = SyncState.Complete
                    }
                }

                WorkInfo.State.FAILED -> {
                    val errorMsg = info.outputData.getString("error")
                        ?: "Unknown sync error"
                    _syncState.value = SyncState.Error("Full sync failed: $errorMsg")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        snackbarMessage = "Sync failed: $errorMsg"
                    )
                }
                else -> { /* Optional loading state */ }
            }
        }
    }


    fun showCreateGroupDialog() {
        _uiState.value = _uiState.value.copy(showCreateGroupDialog = true)
    }

    fun hideCreateGroupDialog() {
        _uiState.value = _uiState.value.copy(
            showCreateGroupDialog = false,
            nameValidationError = null
        )
    }

    fun validateAndCreateGroup(name: String) {
        viewModelScope.launch {
            if (name.isBlank()) {
                _uiState.value = _uiState.value.copy(
                    nameValidationError = "Group name cannot be empty"
                )
                return@launch
            }

            try {
                val userId = userRepository.getCurrentUserId()
                    ?: throw Exception("User not authenticated")

                groupRepository.createGroup(name, userId)
                loadUserGroups()
                hideCreateGroupDialog()
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Group created successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Failed to create group: ${e.message}"
                )
            }
        }
    }

    fun refreshGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncComplete = false)
            loadUserGroups()
        }
    }

    fun clearSnackbar() {
        _uiState.value = _uiState.value.copy(snackbarMessage = null)
    }
}


data class GroupHomeUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val nameValidationError: String? = null,
    val showCreateGroupDialog: Boolean = false,
    val isSyncComplete: Boolean = false,
    val isUpToDate: Boolean = false
)