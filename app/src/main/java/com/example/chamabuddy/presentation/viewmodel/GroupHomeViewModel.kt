// GroupHomeViewModel.kt
package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Group
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GroupHomeViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GroupHomeUiState())
    val uiState: StateFlow<GroupHomeUiState> = _uiState.asStateFlow()

    init {
        loadUserGroups()
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
            try {
                // ... validation logic ...

                val userId = userRepository.getCurrentUserId()
                    ?: throw Exception("User not authenticated")

                // Create group
                groupRepository.createGroup(name, userId)

                // Refresh groups list
                loadUserGroups()

                // ... update UI state ...
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Failed to create group: ${e.message}"
                )
            }
        }
    }

    private fun loadUserGroups() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                // Get current user ID
                val userId = userRepository.getCurrentUserId()
                    ?: throw Exception("User not authenticated")

                // Get current user object
                val currentUser = userRepository.getUserById(userId)
                    ?: throw Exception("User data not found. Please re-login.")


                // Use phone number to fetch groups
                val groups = groupRepository.getUserGroupsByPhone(currentUser.phoneNumber)

                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    isLoading = false
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Failed to load groups: ${e.message}",
                    isLoading = false
                )
            }
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
    val showCreateGroupDialog: Boolean = false
)