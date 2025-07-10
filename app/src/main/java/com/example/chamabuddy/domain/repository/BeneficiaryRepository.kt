package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Beneficiary


interface BeneficiaryRepository {
    suspend fun insertBeneficiary(beneficiary: Beneficiary)
    suspend fun getBeneficiariesByMeeting(meetingId: String): List<Beneficiary>
//    suspend fun getBeneficiaryById(memberId: String): List<Beneficiary>
    suspend fun deleteBeneficiary(beneficiaryId: String)
    suspend fun getBeneficiariesByCycle(cycleId: String): List<Beneficiary>
    suspend fun getBeneficiaryById(beneficiaryId: String): Beneficiary?
    suspend fun deleteBeneficiariesForMeeting(meetingId: String)

    suspend fun getBeneficiariesForMeeting(meetingId: String): List<Beneficiary>
}