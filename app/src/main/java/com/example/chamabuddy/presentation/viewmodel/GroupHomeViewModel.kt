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

    fun validateAndCreateGroup(name: String) {
        viewModelScope.launch {
            try {
                // Clear previous errors
                _uiState.value = _uiState.value.copy(nameValidationError = null)

                // Validate input
                if (name.isBlank()) {
                    _uiState.value = _uiState.value.copy(
                        nameValidationError = "Group name cannot be empty"
                    )
                    return@launch
                }

                val userId = userRepository.getCurrentUserId() ?: throw Exception("User not authenticated")

                // Create group
                groupRepository.createGroup(name, userId)

                kotlinx.coroutines.delay(300)


                // Refresh groups list
                loadUserGroups()

                // Update UI state
                _uiState.value = _uiState.value.copy(
                    snackbarMessage = "Group '$name' created successfully!",
                    nameValidationError = null
                )

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
                val userId = userRepository.getCurrentUserId() ?: throw Exception("User not authenticated")
                val groups = groupRepository.getUserGroups(userId)

                _uiState.value = _uiState.value.copy(
                    groups = groups,
                    isLoading = false,
                    snackbarMessage = null
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

    fun clearValidationError() {
        _uiState.value = _uiState.value.copy(nameValidationError = null)
    }
}

data class GroupHomeUiState(
    val groups: List<Group> = emptyList(),
    val isLoading: Boolean = false,
    val snackbarMessage: String? = null,
    val nameValidationError: String? = null
)