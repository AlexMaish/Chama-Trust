//package com.alex.m_chama.presentation.viewmodel
//
//class NotificationViewModel(
//    private val notificationRepository: NotificationRepository,
//    private val meetingRepository: MeetingRepository,
//    private val memberRepository: MemberRepository
//) : ViewModel() {
//
//    private val _state = MutableStateFlow<NotificationState>(NotificationState.Idle)
//    val state: StateFlow<NotificationState> = _state.asStateFlow()
//
//    fun handleEvent(event: NotificationEvent) {
//        when (event) {
//            is NotificationEvent.ScheduleWeeklyReminder -> scheduleWeeklyReminder()
//            is NotificationEvent.NotifyBeneficiaries -> notifyBeneficiaries(event.meetingId)
//            is NotificationEvent.NotifyLateContributors -> notifyLateContributors(event.meetingId)
//            is NotificationEvent.NotifyMonthlySavingsDeadline -> notifyMonthlySavingsDeadline()
//            NotificationEvent.ResetNotificationState -> resetState()
//        }
//    }
//
//    private fun scheduleWeeklyReminder() {
//        viewModelScope.launch {
//            _state.value = NotificationState.Loading
//            try {
//                notificationRepository.scheduleWeeklyReminder()
//                _state.value = NotificationState.ReminderScheduled
//            } catch (e: Exception) {
//                _state.value = NotificationState.Error(e.message ?: "Failed to schedule reminder")
//            }
//        }
//    }
//
//    private fun notifyBeneficiaries(meetingId: String) {
//        viewModelScope.launch {
//            _state.value = NotificationState.Loading
//            try {
//                val meeting = meetingRepository.getMeetingById(meetingId)
//                if (meeting != null) {
//                    notificationRepository.notifyBeneficiaries(meetingId)
//                    _state.value = NotificationState.BeneficiariesNotified
//                } else {
//                    _state.value = NotificationState.Error("Meeting not found")
//                }
//            } catch (e: Exception) {
//                _state.value = NotificationState.Error(e.message ?: "Failed to notify beneficiaries")
//            }
//        }
//    }
//
//    private fun notifyLateContributors(meetingId: String) {
//        viewModelScope.launch {
//            _state.value = NotificationState.Loading
//            try {
//                val meeting = meetingRepository.getMeetingById(meetingId)
//                if (meeting != null) {
//                    notificationRepository.notifyLateContributors(meetingId)
//                    _state.value = NotificationState.LateContributorsNotified
//                } else {
//                    _state.value = NotificationState.Error("Meeting not found")
//                }
//            } catch (e: Exception) {
//                _state.value = NotificationState.Error(e.message ?: "Failed to notify late contributors")
//            }
//        }
//    }
//
//    private fun notifyMonthlySavingsDeadline() {
//        viewModelScope.launch {
//            _state.value = NotificationState.Loading
//            try {
//                notificationRepository.notifyMonthlySavingsDeadline()
//                _state.value = NotificationState.SavingsDeadlineNotified
//            } catch (e: Exception) {
//                _state.value = NotificationState.Error(e.message ?: "Failed to notify savings deadline")
//            }
//        }
//    }
//
//    private fun resetState() {
//        _state.value = NotificationState.Idle
//    }
//}