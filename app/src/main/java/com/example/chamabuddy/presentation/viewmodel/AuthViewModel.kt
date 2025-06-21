//package com.alex.m_chama.presentation.viewmodel
//class AuthViewModel(
//    private val memberRepository: MemberRepository
//) : ViewModel() {
//
//    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
//    val state: StateFlow<AuthState> = _state.asStateFlow()
//
//    fun handleEvent(event: AuthEvent) {
//        when (event) {
//            is AuthEvent.Login -> login(event.phoneNumber, event.password)
//            is AuthEvent.Register -> register(event.member)
//            is AuthEvent.ResetPassword -> resetPassword(event.email)
//            AuthEvent.Logout -> logout()
//            AuthEvent.ResetAuthState -> resetState()
//        }
//    }
//
//    private fun login(phoneNumber: String, password: String) {
//        viewModelScope.launch {
//            _state.value = AuthState.Loading
//            try {
//                val member = memberRepository.getMemberByPhoneNumber(phoneNumber)
//                if (member != null) {
//                    // In real app, verify password here
//                    _state.value = AuthState.Success(member)
//                } else {
//                    _state.value = AuthState.Error("Member not found")
//                }
//            } catch (e: Exception) {
//                _state.value = AuthState.Error(e.message ?: "Login failed")
//            }
//        }
//    }
//
//    private fun register(member: Member) {
//        viewModelScope.launch {
//            _state.value = AuthState.Loading
//            try {
//                memberRepository.addMember(member)
//                _state.value = AuthState.Success(member)
//            } catch (e: Exception) {
//                _state.value = AuthState.Error(e.message ?: "Registration failed")
//            }
//        }
//    }
//
//    private fun resetPassword(email: String) {
//        viewModelScope.launch {
//            _state.value = AuthState.Loading
//            try {
//                // Implement password reset logic
//                _state.value = AuthState.PasswordResetSent
//            } catch (e: Exception) {
//                _state.value = AuthState.Error(e.message ?: "Password reset failed")
//            }
//        }
//    }
//
//    private fun logout() {
//        viewModelScope.launch {
//            _state.value = AuthState.Loading
//            // Clear any session data
//            _state.value = AuthState.LogoutSuccess
//        }
//    }
//
//    private fun resetState() {
//        _state.value = AuthState.Idle
//    }
//}

// Authentication State

//
//sealed class AuthState {
//    object Idle : AuthState()
//    object Loading : AuthState()
//    data class Success(val member: Member) : AuthState()
//    data class Error(val message: String) : AuthState()
//    object LogoutSuccess : AuthState()
//}
//
//// Authentication Events
//sealed class AuthEvent {
//    data class Login(val phoneNumber: String, val password: String) : AuthEvent()
//    object Logout : AuthEvent()
//    object ResetAuthState : AuthEvent()
//}