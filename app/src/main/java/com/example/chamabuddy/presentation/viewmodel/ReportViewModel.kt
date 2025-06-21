//package com.example.chamabuddy.presentation.viewmodel
//
//class ReportViewModel(
//    private val cycleRepository: CycleRepository,
//    private val meetingRepository: MeetingRepository,
//    private val savingsRepository: SavingsRepository,
//    private val memberRepository: MemberRepository
//) : ViewModel() {
//
//    private val _state = MutableStateFlow<ReportState>(ReportState.Idle)
//    val state: StateFlow<ReportState> = _state.asStateFlow()
//
//    fun handleEvent(event: ReportEvent) {
//        when (event) {
//            is ReportEvent.GenerateGroupFinancialReport -> generateGroupFinancialReport(event.cycleId)
//            is ReportEvent.GenerateMemberStatement -> generateMemberStatement(event.memberId, event.cycleId)
//            is ReportEvent.GenerateContributionHistory -> generateContributionHistory(event.cycleId)
//            is ReportEvent.GenerateSavingsReport -> generateSavingsReport(event.cycleId)
//            ReportEvent.ResetReportState -> resetState()
//        }
//    }
//
//    private fun generateGroupFinancialReport(cycleId: String) {
//        viewModelScope.launch {
//            _state.value = ReportState.Loading
//            try {
//                val report = cycleRepository.generateGroupFinancialReport(cycleId)
//                _state.value = ReportState.FinancialReportGenerated(report)
//            } catch (e: Exception) {
//                _state.value = ReportState.Error(e.message ?: "Failed to generate financial report")
//            }
//        }
//    }
//
//    private fun generateMemberStatement(memberId: String, cycleId: String) {
//        viewModelScope.launch {
//            _state.value = ReportState.Loading
//            try {
//                val statement = memberRepository.generateMemberStatement(memberId, cycleId)
//                _state.value = ReportState.MemberStatementGenerated(statement)
//            } catch (e: Exception) {
//                _state.value = ReportState.Error(e.message ?: "Failed to generate member statement")
//            }
//        }
//    }
//
//    private fun generateContributionHistory(cycleId: String) {
//        viewModelScope.launch {
//            _state.value = ReportState.Loading
//            try {
//                val history = meetingRepository.generateContributionHistory(cycleId)
//                _state.value = ReportState.ContributionHistoryGenerated(history)
//            } catch (e: Exception) {
//                _state.value = ReportState.Error(e.message ?: "Failed to generate contribution history")
//            }
//        }
//    }
//
//    private fun generateSavingsReport(cycleId: String) {
//        viewModelScope.launch {
//            _state.value = ReportState.Loading
//            try {
//                val report = savingsRepository.generateSavingsReport(cycleId)
//                _state.value = ReportState.SavingsReportGenerated(report)
//            } catch (e: Exception) {
//                _state.value = ReportState.Error(e.message ?: "Failed to generate savings report")
//            }
//        }
//    }
//
//    private fun resetState() {
//        _state.value = ReportState.Idle
//    }
//}