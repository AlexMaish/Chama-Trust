package com.example.chamabuddy.presentation.viewmodel



import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.MemberContribution
import com.example.chamabuddy.domain.repository.MemberContributionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ContributionViewModel @Inject constructor(
    private val repository: MemberContributionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ContributionUiState())
    val uiState: StateFlow<ContributionUiState> = _uiState.asStateFlow()

    private val _eventFlow = MutableSharedFlow<ContributionUiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    fun loadContributions(meetingId: String) {
        viewModelScope.launch {
            repository.getAllContributionsForMeeting(meetingId)
                .onStart { _uiState.update { it.copy(isLoading = true) } }
                .catch { e ->
                    _uiState.update { it.copy(error = e.message, isLoading = false) }
                    _eventFlow.emit(ContributionUiEvent.ShowSnackbar("Error loading: ${e.message}"))
                }
                .collect { contributions ->
                    val total = contributions.sumOf { it.amountContributed }
                    _uiState.update {
                        it.copy(
                            contributions = contributions,
                            totalContributed = total,
                            isLoading = false
                        )
                    }
                }
        }
    }

    fun addContribution(contribution: MemberContribution) {
        viewModelScope.launch {
            try {
                repository.insertContribution(contribution)
                _eventFlow.emit(ContributionUiEvent.ShowSnackbar("Contribution added."))
            } catch (e: Exception) {
                _eventFlow.emit(ContributionUiEvent.ShowSnackbar("Failed: ${e.message}"))
            }
        }
    }
}
data class ContributionUiState(
    val isLoading: Boolean = false,
    val contributions: List<MemberContribution> = emptyList(),
    val error: String? = null,
    val totalContributed: Int = 0
)



sealed class ContributionUiEvent {
    data class ShowSnackbar(val message: String) : ContributionUiEvent()
}
