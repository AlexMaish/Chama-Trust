package com.example.chamabuddy.presentation.viewmodel

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.SavingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@HiltViewModel
class MemberViewModel  @Inject constructor(
    private val memberRepository: MemberRepository,
    private val savingsRepository: SavingsRepository
) : ViewModel() {


    private val _state = MutableStateFlow<MemberState>(MemberState.Idle)
    val state: StateFlow<MemberState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMember = MutableStateFlow<Member?>(null)
    val selectedMember: StateFlow<Member?> = _selectedMember.asStateFlow()

    fun handleEvent(event: MemberEvent) {
        when (event) {
            is MemberEvent.LoadAllMembers -> loadMembers()
            is MemberEvent.AddMember -> addMember(event.member)
            is MemberEvent.UpdateMember -> updateMember(event.member)
            is MemberEvent.DeleteMember -> deleteMember(event.member)
            is MemberEvent.GetMemberDetails -> getMemberDetails(event.memberId)
            is MemberEvent.UpdateProfilePicture -> updateProfilePicture(event.memberId, event.imageUri)
            is MemberEvent.ChangePhoneNumber -> changePhoneNumber(event.memberId, event.newNumber)
            MemberEvent.ResetMemberState -> resetState()
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun loadMembers() {
        viewModelScope.launch {
            try {
                val members =  memberRepository.getActiveMembers().first()
                _state.value = MemberState.MembersLoaded(members)
            } catch (e: Exception) {
                _state.value = MemberState.Error(e.message ?: "Failed to load members")
            }
        }
    }

    private fun addMember(member: Member) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            _state.value = MemberState.Loading
            try {
                memberRepository.addMember(member)
                loadMembers() // Refresh the list
            } catch (e: Exception) {
                _state.value = MemberState.Error(e.message ?: "Failed to add member")
            }
        }
    }

    private fun updateMember(member: Member) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.updateMember(member)
                _selectedMember.value = member
                loadMembers() // Refresh the list
            } catch (e: Exception) {
                _state.value = MemberState.Error(e.message ?: "Failed to update member")
            }
        }
    }

    private fun deleteMember(member: Member) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.deleteMember(member)
                loadMembers() // Refresh the list
            } catch (e: Exception) {
                _state.value = MemberState.Error(e.message ?: "Failed to delete member")
            }
        }
    }

    private fun getMemberDetails(memberId: String) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                val member = memberRepository.getMemberById(memberId)
                if (member != null) {
                    _selectedMember.value = member
                    _state.value = MemberState.MemberDetails(member)
                } else {
                    _state.value = MemberState.Error("Member not found")
                }
            } catch (e: Exception) {
                _state.value = MemberState.Error(e.message ?: "Failed to load member details")
            }
        }
    }

    fun updateProfilePicture(memberId: String, imageUri: Uri) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.updateProfilePicture(memberId, imageUri)
                getMemberDetails(memberId) // Refresh member details
            } catch (e: Exception) {
                _state.value = MemberState.Error(
                    e.message ?: "Failed to update profile picture"
                )
            }
        }
    }
    fun getMemberNameById(memberId: String): String? {
        return runBlocking {
            try {
                memberRepository.getMemberNameById(memberId)
            } catch (e: Exception) {
                null
            }
        }
    }
    fun changePhoneNumber(memberId: String, newNumber: String) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.changePhoneNumber(memberId, newNumber)
                getMemberDetails(memberId) // Refresh member details
            } catch (e: Exception) {
                _state.value = MemberState.Error(
                    e.message ?: "Failed to change phone number"
                )
            }
        }
    }

    private fun resetState() {
        _state.value = MemberState.Idle
    }
}

// Member State
sealed class MemberState {
    object Idle : MemberState()
    object Loading : MemberState()
    data class MembersLoaded(val members: List<Member>) : MemberState()
    data class MemberDetails(val member: Member) : MemberState()
    data class Error(val message: String) : MemberState()
}

// Member Events
sealed class MemberEvent {
    object LoadAllMembers : MemberEvent()
    data class AddMember(val member: Member) : MemberEvent()
    data class UpdateMember(val member: Member) : MemberEvent()
    data class DeleteMember(val member: Member) : MemberEvent()
    data class GetMemberDetails(val memberId: String) : MemberEvent()
    object ResetMemberState : MemberEvent()

    data class UpdateProfilePicture(
        val memberId: String,
        val imageUri: Uri
    ) : MemberEvent()

    data class ChangePhoneNumber(
        val memberId: String,
        val newNumber: String
    ) : MemberEvent()
}