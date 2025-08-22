package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.BenefitEntity
import kotlinx.coroutines.flow.Flow

interface BenefitRepository {
    suspend fun addBenefit(benefit: BenefitEntity)
    fun getBenefits(groupId: String): Flow<List<BenefitEntity>>
    fun getTotal(groupId: String): Flow<Double>

    suspend fun getUnsyncedBenefits(): List<BenefitEntity>
    suspend fun markBenefitSynced(benefit: BenefitEntity)

    suspend fun getBenefitById(benefitId: String): BenefitEntity?
    suspend fun insertBenefit(benefit: BenefitEntity)


    suspend fun markAsDeleted(benefitId: String, timestamp: Long)
    suspend fun getDeletedBenefits(): List<BenefitEntity>
    suspend fun permanentDelete(benefitId: String)


}