package com.example.chamabuddy.presentation.viewmodel

import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.data.local.UserGroupDao
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class UserGroupViewModel @Inject constructor(
    private val userGroupDao: UserGroupDao,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _userGroups = mutableStateListOf<String>()
    val userGroups: List<String> = _userGroups

    private val userId: String = savedStateHandle["USER_ID"] ?: ""

    init {
        loadUserGroups()
    }

    private fun loadUserGroups() {
        viewModelScope.launch {
            val groupIds = userGroupDao.getUserGroups(userId)
            _userGroups.clear()
            _userGroups.addAll(groupIds)
        }
    }
}
