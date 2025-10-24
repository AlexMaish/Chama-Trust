package com.example.chamabuddy.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.chamabuddy.domain.model.Beneficiary
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.BeneficiaryRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BeneficiaryViewModel @Inject constructor(
    private val beneficiaryRepository: BeneficiaryRepository,
    private val memberRepository: MemberRepository
) : ViewModel() {

    private val _state = MutableStateFlow<BeneficiaryState>(BeneficiaryState.Loading)
    val state: StateFlow<BeneficiaryState> = _state.asStateFlow()

    fun loadBeneficiaryDetails(beneficiaryId: String) {
        viewModelScope.launch {
            _state.value = BeneficiaryState.Loading
            try {
                val beneficiary = beneficiaryRepository.getBeneficiaryById(beneficiaryId)
                val member = beneficiary?.let { memberRepository.getMemberById(it.memberId) }

                if (beneficiary != null && member != null) {
                    _state.value = BeneficiaryState.Success(beneficiary, member)
                } else {
                    _state.value = BeneficiaryState.Error("Beneficiary not found")
                }
            } catch (e: Exception) {
                _state.value = BeneficiaryState.Error(e.message ?: "Unknown error")
            }
        }
    }
}

sealed class BeneficiaryState {
    object Loading : BeneficiaryState()
    data class Success(val beneficiary: Beneficiary, val member: Member) : BeneficiaryState()
    data class Error(val message: String) : BeneficiaryState()
}