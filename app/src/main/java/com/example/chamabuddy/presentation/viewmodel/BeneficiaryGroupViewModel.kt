package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Beneficiary
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.CycleWithBeneficiaries
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.BeneficiaryRepository
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeneficiaryGroupViewModel @Inject constructor(
    private val cycleRepository: CycleRepository,
    private val memberRepository: MemberRepository,
    private val beneficiaryRepository: BeneficiaryRepository
) : ViewModel() {

    private val _state = MutableStateFlow(BeneficiaryGroupState())
    val state: StateFlow<BeneficiaryGroupState> = _state.asStateFlow()

    fun loadData(groupId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                val cyclesWithBeneficiaries = cycleRepository.getCyclesWithBeneficiaries(groupId)
                    .sortedByDescending { it.cycle.cycleNumber }
                val activeMembers = memberRepository.getActiveMembersByGroup(groupId)

                _state.value = _state.value.copy(
                    isLoading = false,
                    cycles = cyclesWithBeneficiaries,
                    activeMembers = activeMembers
                )
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Unknown error"
                )
            }
        }
    }
}

data class BeneficiaryGroupState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val cycles: List<CycleWithBeneficiaries> = emptyList(),
    val activeMembers: List<Member> = emptyList()
)


