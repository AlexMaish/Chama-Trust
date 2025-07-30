package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.BenefitDao
import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.domain.repository.BenefitRepository
import java.util.UUID
import javax.inject.Inject

class BenefitRepositoryImpl @Inject constructor(
    private val benefitDao: BenefitDao
) : BenefitRepository {
    override suspend fun addBenefit(benefit: BenefitEntity) {
        val newId = UUID.randomUUID().toString()
        benefitDao.insert(benefit.copy(benefitId = newId, isSynced = false))
    }
    override fun getBenefits(groupId: String) = benefitDao.getBenefits(groupId)
    override fun getTotal(groupId: String) = benefitDao.getTotal(groupId)

    override suspend fun getUnsyncedBenefits(): List<BenefitEntity> =
        benefitDao.getUnsyncedBenefits()

    override suspend fun markBenefitSynced(benefit: BenefitEntity) {
        benefitDao.markAsSynced(benefit.benefitId)
    }

}