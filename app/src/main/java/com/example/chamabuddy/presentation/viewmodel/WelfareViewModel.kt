package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.repository.WelfareRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class WelfareViewModel @Inject constructor(
    private val repository: WelfareRepository
) : ViewModel() {

    private val _welfares = MutableStateFlow<List<com.example.chamabuddy.domain.model.Welfare>>(emptyList())
    val welfares: StateFlow<List<com.example.chamabuddy.domain.model.Welfare>> = _welfares.asStateFlow()

    private val _meetings = MutableStateFlow<List<com.example.chamabuddy.domain.model.WelfareMeeting>>(emptyList())
    val meetings: StateFlow<List<com.example.chamabuddy.domain.model.WelfareMeeting>> = _meetings.asStateFlow()

    fun loadWelfares(groupId: String) {
        viewModelScope.launch {
            repository.getWelfaresForGroup(groupId).collect { list ->
                _welfares.value = list
            }
        }
    }

    fun createWelfare(groupId: String, name: String, userId: String, amount: Int) {
        viewModelScope.launch {
            repository.createWelfare(groupId, name, userId, amount)
        }
    }

    fun loadMeetings(welfareId: String) {
        viewModelScope.launch {
            repository.getMeetingsForWelfare(welfareId).collect { list ->
                _meetings.value = list
            }
        }
    }

    fun getLatestWelfareId(groupId: String): String? {
        return _welfares.value
            .filter { it.groupId == groupId }
            .maxByOrNull { it.createdAt }?.welfareId
    }


    fun createWelfareMeeting(
        welfareId: String,
        meetingDate: Date,
        recordedBy: String?,
        groupId: String,
        welfareAmount: Int
    ): String? {
        var newMeetingId: String? = null
        viewModelScope.launch {
            newMeetingId = repository.createWelfareMeeting(
                welfareId,
                meetingDate,
                recordedBy,
                groupId,
                welfareAmount
            )
        }
        return newMeetingId
    }

    suspend fun deleteMeeting(meetingId: String) {
        repository.deleteMeeting(meetingId)
    }
}