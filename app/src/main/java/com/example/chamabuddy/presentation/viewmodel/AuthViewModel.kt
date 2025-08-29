package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.Common.Resource
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.repository.UserRepository
import com.example.chamabuddy.util.SyncLogger
import com.example.chamabuddy.workers.SyncHelper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val syncHelper: SyncHelper
) : ViewModel() {

    private val _currentUser = MutableStateFlow<User?>(null)
    val currentUser: StateFlow<User?> = _currentUser

    private val _registrationState = MutableStateFlow<Resource<User>?>(null)
    val registrationState: StateFlow<Resource<User>?> = _registrationState

    private val _loginState = MutableStateFlow<Resource<User>?>(null)
    val loginState: StateFlow<Resource<User>?> = _loginState

    private val _currentMemberId = MutableStateFlow<String?>(null)
    val currentMemberId: StateFlow<String?> = _currentMemberId.asStateFlow()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Unauthenticated)
    val authState: StateFlow<AuthState> = _authState



    init {
        viewModelScope.launch {
            val userId = userRepository.getCurrentUserId()
            if (userId != null) {
                val user = userRepository.getUserById(userId)
                if (user != null) {
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(userId)
                } else {
                    _authState.value = AuthState.Unauthenticated
                }
            } else {
                _authState.value = AuthState.Unauthenticated
            }
        }
    }

    private val _currentUserId = MutableStateFlow<String?>(null)
    val currentUserId: StateFlow<String?> = _currentUserId

    init {
        viewModelScope.launch {
            _currentUserId.value = userRepository.getCurrentUserId()
        }
    }

    suspend fun loadCurrentMemberId(groupId: String) {
        _currentMemberId.value = userRepository.getCurrentUserMemberId(groupId)
    }


    // Add to AuthViewModel.kt
    private val _changePasswordState = MutableStateFlow<Resource<Unit>?>(null)
    val changePasswordState: StateFlow<Resource<Unit>?> = _changePasswordState

    fun changePassword(oldPassword: String, newPassword: String) {
        viewModelScope.launch {
            _changePasswordState.value = Resource.Loading()
            val currentUserId = userRepository.getCurrentUserId()
            val currentUser = currentUserId?.let { userRepository.getUserById(it) }

            if (currentUser == null) {
                _changePasswordState.value = Resource.Error("User not found. Please log in again.")
                return@launch
            }

            userRepository.changePassword(currentUserId, oldPassword, newPassword).fold(
                onSuccess = {
                    _changePasswordState.value = Resource.Success(Unit)
                },
                onFailure = { e ->
                    val errorMessage = when {
                        e.message?.contains("incorrect", ignoreCase = true) == true ->
                            "Current password is incorrect"
                        else -> e.message ?: "Password change failed"
                    }
                    _changePasswordState.value = Resource.Error(errorMessage)
                }
            )
        }
    }

    fun clearChangePasswordState() {
        _changePasswordState.value = null
    }
    fun registerUser(username: String, password: String, phoneNumber: String) {
        viewModelScope.launch {
            _registrationState.value = Resource.Loading()
            userRepository.registerUser(username, password, phoneNumber).fold(
                onSuccess = { user ->
                    _registrationState.value = Resource.Success(user)
                },
                onFailure = { e ->
                    _registrationState.value = Resource.Error(e.message ?: "Registration failed")
                }
            )
        }
    }
    fun loginUser(identifier: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            userRepository.loginUser(identifier, password).fold(
                onSuccess = { user ->
                    userRepository.setCurrentUserId(user.userId)
                    _currentUser.value = user
                    _loginState.value = Resource.Success(user)
                    _authState.value = AuthState.Authenticated(user.userId)

                    // Force sign-in to Firebase Auth
                    userRepository.ensureFirebaseAuthSignIn(identifier, password)

                    syncHelper.triggerFullSync()
                },
                onFailure = { e ->
                    _loginState.value = Resource.Error(e.message ?: "Login failed")
                }
            )
        }
    }



    fun logout() {
        viewModelScope.launch {
            userRepository.clearCurrentUser()
            _currentUser.value = null
            _authState.value = AuthState.Unauthenticated
        }
    }
}

sealed class AuthState {
    object Unauthenticated : AuthState()
    data class Authenticated(val userId: String) : AuthState()
}