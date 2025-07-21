package com.example.chamabuddy.presentation.viewmodel

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MemberViewModel @Inject constructor(
    private val groupRepository: GroupRepository,
    private val memberRepository: MemberRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private var currentGroupId: String by mutableStateOf("")

    private val _state = MutableStateFlow<MemberState>(MemberState.Idle)
    val state: StateFlow<MemberState> = _state.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedMember = MutableStateFlow<Member?>(null)
    val selectedMember: StateFlow<Member?> = _selectedMember.asStateFlow()

    fun setGroupId(groupId: String) {
        currentGroupId = groupId
    }


    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private var currentUserId: String by mutableStateOf("")
//    private var currentUserIsAdmin: Boolean by mutableStateOf(false)
//    private var currentUserIsOwner: Boolean by mutableStateOf(false)

    fun setCurrentUser(userId: String) {
        currentUserId = userId
    }
    private val _currentUserIsAdmin = MutableStateFlow(false)
    val currentUserIsAdmin: StateFlow<Boolean> = _currentUserIsAdmin.asStateFlow()

    private val _currentUserIsOwner = MutableStateFlow(false)
    val currentUserIsOwner: StateFlow<Boolean> = _currentUserIsOwner.asStateFlow()

    fun loadCurrentUserRole(groupId: String, userId: String) {
        viewModelScope.launch {
            try {
                val member = memberRepository.getMemberByUserId(userId, groupId)
                _currentUserIsAdmin.value = member?.isAdmin ?: false
                _currentUserIsOwner.value = member?.isOwner ?: false
            } catch (e: Exception) {
                // Reset on error
                _currentUserIsAdmin.value = false
                _currentUserIsOwner.value = false
            }
        }
    }

    fun handleEvent(event: MemberEvent) {
        when (event) {
            is MemberEvent.LoadMembersForGroup -> loadMembersForGroup(event.groupId)
            is MemberEvent.AddMember -> addMember(event.member)
            is MemberEvent.UpdateMember -> updateMember(event.member)
            is MemberEvent.DeleteMember -> deleteMember(event.member)
            is MemberEvent.GetMemberDetails -> getMemberDetails(event.memberId)
            is MemberEvent.UpdateProfilePicture -> updateProfilePicture(
                event.memberId,
                event.imageUri
            )

            is MemberEvent.ChangePhoneNumber -> changePhoneNumber(event.memberId, event.newNumber)
            MemberEvent.ResetMemberState -> resetState()

            is MemberEvent.PromoteToAdmin -> promoteToAdmin(event.memberId)
            is MemberEvent.DemoteToMember -> demoteToMember(event.memberId)
            is MemberEvent.DeactivateMember -> deactivateMember(event.memberId)
            is MemberEvent.ReactivateMember -> reactivateMember(event.memberId)
        }
    }


    private fun promoteToAdmin(memberId: String) {
        viewModelScope.launch {
            try {
                memberRepository.updateAdminStatus(memberId, true)
                loadMembersForGroup(currentGroupId)
                _snackbarMessage.value = "Member promoted to admin"
            } catch (e: Exception) {
                _state.value = MemberState.Error("Promotion failed: ${e.localizedMessage}")
            }
        }
    }

    private fun demoteToMember(memberId: String) {
        viewModelScope.launch {
            try {
                // Check admin count
                val adminCount = memberRepository.getAdminCount(currentGroupId)
                if (adminCount <= 1) {
                    throw IllegalStateException("Cannot demote the last admin")
                }

                memberRepository.updateAdminStatus(memberId, false)
                loadMembersForGroup(currentGroupId)
                _snackbarMessage.value = "Admin demoted to member"
            } catch (e: Exception) {
                _state.value = MemberState.Error("Demotion failed: ${e.localizedMessage}")
            }
        }
    }

    private fun deactivateMember(memberId: String) {
        viewModelScope.launch {
            try {
                val member = memberRepository.getMemberById(memberId)
                    ?: throw Exception("Member not found")

                if (member.userId == currentUserId) {
                    throw IllegalStateException("You cannot deactivate yourself")
                }

                memberRepository.updateActiveStatus(memberId, false)
                loadMembersForGroup(currentGroupId)
                _snackbarMessage.value = "Member deactivated"
            } catch (e: Exception) {
                _state.value = MemberState.Error("Deactivation failed: ${e.localizedMessage}")
            }
        }
    }

    private fun reactivateMember(memberId: String) {
        viewModelScope.launch {
            try {
                memberRepository.updateActiveStatus(memberId, true)
                loadMembersForGroup(currentGroupId)
                _snackbarMessage.value = "Member reactivated"
            } catch (e: Exception) {
                _state.value = MemberState.Error("Reactivation failed: ${e.localizedMessage}")
            }
        }
    }







    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    private fun loadMembersForGroup(groupId: String) {
        currentGroupId = groupId
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                val members = memberRepository.getMembersByGroup(groupId)
                if (members.isNotEmpty()) {
                    _state.value = MemberState.MembersLoaded(members)
                } else {
                    _state.value = MemberState.Empty("No members found")
                }
            } catch (e: Exception) {
                _state.value = MemberState.Error("Failed to load members: ${e.localizedMessage}")
            }
        }
    }

    private fun addMember(member: Member) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                groupRepository.addMemberToGroup(currentGroupId, member.phoneNumber)
                loadMembersForGroup(currentGroupId)
            } catch (e: IllegalStateException) {
                // Specific error for user not registered
                _snackbarMessage.value = e.message
            } catch (e: Exception) {
                _state.value = MemberState.Error("Failed to add member: ${e.localizedMessage}")
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }


    private fun updateMember(member: Member) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.updateMember(member)
                _selectedMember.value = member
                loadMembersForGroup(currentGroupId) // Reload members
            } catch (e: Exception) {
                _state.value = MemberState.Error("Update failed: ${e.localizedMessage}")
            }
        }
    }

    private fun deleteMember(member: Member) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.deleteMember(member)
                loadMembersForGroup(currentGroupId) // Reload members
            } catch (e: Exception) {
                _state.value = MemberState.Error("Deletion failed: ${e.localizedMessage}")
            }
        }
    }

    private fun getMemberDetails(memberId: String) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.getMemberById(memberId)?.let { member ->
                    _selectedMember.value = member
                    _state.value = MemberState.MemberDetails(member)
                } ?: run {
                    _state.value = MemberState.Error("Member not found")
                }
            } catch (e: Exception) {
                _state.value = MemberState.Error("Details error: ${e.localizedMessage}")
            }
        }
    }

    private fun updateProfilePicture(memberId: String, imageUri: Uri) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.updateProfilePicture(memberId, imageUri)
                getMemberDetails(memberId) // Refresh details
            } catch (e: Exception) {
                _state.value = MemberState.Error("Profile update failed: ${e.localizedMessage}")
            }
        }
    }

    suspend fun getMemberNameById(memberId: String): String? {
        return memberRepository.getMemberNameById(memberId)
    }

    private fun changePhoneNumber(memberId: String, newNumber: String) {
        viewModelScope.launch {
            _state.value = MemberState.Loading
            try {
                memberRepository.changePhoneNumber(memberId, newNumber)
                getMemberDetails(memberId) // Refresh details
            } catch (e: Exception) {
                _state.value = MemberState.Error("Phone update failed: ${e.localizedMessage}")
            }
        }
    }

    private fun resetState() {
        _state.value = MemberState.Idle
    }
}

sealed class MemberState {
    object Idle : MemberState()
    object Loading : MemberState()
    data class MembersLoaded(val members: List<Member>) : MemberState()
    data class MemberDetails(val member: Member) : MemberState()
    data class Error(val message: String) : MemberState()
    data class Empty(val message: String) : MemberState()
}

sealed class MemberEvent {
    data class LoadMembersForGroup(val groupId: String) : MemberEvent()
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


    data class PromoteToAdmin(val memberId: String) : MemberEvent()
    data class DemoteToMember(val memberId: String) : MemberEvent()
    data class DeactivateMember(val memberId: String) : MemberEvent()
    data class ReactivateMember(val memberId: String) : MemberEvent()


}