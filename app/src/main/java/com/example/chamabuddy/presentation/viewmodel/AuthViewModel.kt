package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.Common.Resource
import com.example.chamabuddy.domain.model.User
import com.example.chamabuddy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val userRepository: UserRepository
) : ViewModel() {

    // nullable state: null = idle, Resource.Loading, Success, or Error
    private val _registrationState = MutableStateFlow<Resource<User>?>(null)
    val registrationState: StateFlow<Resource<User>?> = _registrationState

    private val _loginState = MutableStateFlow<Resource<User>?>(null)
    val loginState: StateFlow<Resource<User>?> = _loginState

    fun registerUser(username: String, password: String, phoneNumber: String) {
        viewModelScope.launch {
            _registrationState.value = Resource.Loading()
            userRepository.registerUser(username, password, phoneNumber).fold(
                onSuccess = { user -> _registrationState.value = Resource.Success(user) },
                onFailure = { e -> _registrationState.value = Resource.Error(e.message ?: "Registration failed") }
            )
        }
    }

    fun loginUser(identifier: String, password: String) {
        viewModelScope.launch {
            _loginState.value = Resource.Loading()
            userRepository.loginUser(identifier, password).fold(
                onSuccess = { user -> _loginState.value = Resource.Success(user) },
                onFailure = { e -> _loginState.value = Resource.Error(e.message ?: "Login failed") }
            )
        }
    }

    fun logout() {
        _loginState.value = null
        _registrationState.value = null
    }
}