package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.BenefitDao
import com.example.chamabuddy.domain.model.BenefitEntity
import com.example.chamabuddy.domain.repository.BenefitRepository
import java.util.UUID
import javax.inject.Inject

class BenefitRepositoryImpl @Inject constructor(
    private val benefitDao: BenefitDao
) : BenefitRepository {
    override suspend fun findSimilarBenefit(groupId: String, name: String, amount: Double, date: Long): BenefitEntity? {
        return benefitDao.findSimilarBenefit(groupId, name, amount, date)
    }


    override suspend fun addBenefit(benefit: BenefitEntity) {
        val newId = if (benefit.benefitId.isEmpty()) UUID.randomUUID().toString() else benefit.benefitId
        val updatedBenefit = benefit.copy(benefitId = newId, isSynced = false)
        benefitDao.insert(updatedBenefit)
    }


    override fun getBenefits(groupId: String) = benefitDao.getBenefits(groupId)
    override fun getTotal(groupId: String) = benefitDao.getTotal(groupId)

    override suspend fun getUnsyncedBenefits(): List<BenefitEntity> =
        benefitDao.getUnsyncedBenefits()

    override suspend fun markBenefitSynced(benefit: BenefitEntity) {
        benefitDao.markAsSynced(benefit.benefitId)
    }


    override suspend fun getBenefitById(benefitId: String): BenefitEntity? {
        return benefitDao.getBenefitById(benefitId)
    }

    override suspend fun insertBenefit(benefit: BenefitEntity) {
        benefitDao.insert(benefit)
    }


    override suspend fun markAsDeleted(benefitId: String, timestamp: Long) =
        benefitDao.markAsDeleted(benefitId, timestamp)

    override suspend fun getDeletedBenefits(): List<BenefitEntity> =
        benefitDao.getDeletedBenefits()

    override suspend fun permanentDelete(benefitId: String) =
        benefitDao.permanentDelete(benefitId)


    override suspend fun updateBenefit(benefit: BenefitEntity) {
        benefitDao.update(benefit)
    }
}