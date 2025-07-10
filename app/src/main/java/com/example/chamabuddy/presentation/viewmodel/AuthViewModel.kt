package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.Common.Resource
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
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
            userRepository.getCurrentUserId()?.let { userId ->
                userRepository.getUserById(userId)?.let { user ->
                    _currentUser.value = user
                    _authState.value = AuthState.Authenticated(userId)
                }
            }
        }
    }

    suspend fun loadCurrentMemberId(groupId: String) {
        _currentMemberId.value = userRepository.getCurrentUserMemberId(groupId)
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