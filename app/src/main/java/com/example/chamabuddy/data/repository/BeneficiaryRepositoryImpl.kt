package com.example.chamabuddy.data.repository


import com.example.chamabuddy.data.local.BeneficiaryDao
import com.example.chamabuddy.domain.model.Beneficiary
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.BeneficiaryRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject

class BeneficiaryRepositoryImpl @Inject constructor(
    private val beneficiaryDao: BeneficiaryDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : BeneficiaryRepository {

    override suspend fun insertBeneficiary(beneficiary: Beneficiary) = withContext(dispatcher) {
        beneficiaryDao.insertBeneficiary(beneficiary)
    }

    override suspend fun getBeneficiariesByMeeting(meetingId: String): List<Beneficiary> =
        withContext(dispatcher) {
            beneficiaryDao.getBeneficiariesForMeeting(meetingId)
        }

//    override suspend fun getBeneficiaryById(memberId: String): List<Beneficiary> =
//        withContext(dispatcher) {
//            // This would require a new query in the DAO
//            emptyList() // Placeholder - implement based on your needs
//        }

    override suspend fun deleteBeneficiariesForMeeting(meetingId: String) {
        beneficiaryDao.deleteBeneficiariesForMeeting(meetingId)
    }
    override suspend fun getBeneficiariesForMeeting(meetingId: String): List<Beneficiary> =
        withContext(dispatcher) {
            beneficiaryDao.getBeneficiariesForMeeting(meetingId)
        }
    override suspend fun getBeneficiaryById(beneficiaryId: String): Beneficiary? =
        withContext(dispatcher) {
            beneficiaryDao.getBeneficiaryById(beneficiaryId)
        }

    override suspend fun deleteBeneficiary(beneficiaryId: String) = withContext(dispatcher) {
        // This would require a new query in the DAO
    }
    override suspend fun updateBeneficiaryAmount(beneficiaryId: String, newAmount: Int) {
        beneficiaryDao.updateBeneficiaryAmount(beneficiaryId, newAmount)
    }

    suspend fun getEligibleMembersForMeeting(meetingId: String): List<Member> =
        withContext(dispatcher) {
            beneficiaryDao.getEligibleMembersForMeeting(meetingId)
        }
    override suspend fun getBeneficiaryCountForMeeting(meetingId: String): Int {
        return beneficiaryDao.getBeneficiaryCountForMeeting(meetingId)
    }
    override suspend fun getBeneficiariesByCycle(cycleId: String): List<Beneficiary> =
        withContext(dispatcher) {
            beneficiaryDao.getBeneficiariesByCycle(cycleId)
        }


}