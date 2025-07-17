package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.BenefitDao
import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.domain.repository.BenefitRepository
import javax.inject.Inject

class BenefitRepositoryImpl @Inject constructor(
    private val benefitDao: BenefitDao
) : BenefitRepository {
    override suspend fun addBenefit(benefit: BenefitEntity) = benefitDao.insert(benefit)
    override fun getBenefits(groupId: String) = benefitDao.getBenefits(groupId)
    override fun getTotal(groupId: String) = benefitDao.getTotal(groupId)
}