package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.PenaltyDao
import com.example.chamabuddy.domain.model.Penalty
import com.example.chamabuddy.domain.repository.PenaltyRepository
import kotlinx.coroutines.flow.Flow

class PenaltyRepositoryImpl(
    private val penaltyDao: PenaltyDao
) : PenaltyRepository {

    override suspend fun addPenalty(penalty: Penalty) {
        penaltyDao.insert(penalty)
    }

    override fun getPenalties(groupId: String): Flow<List<Penalty>> {
        return penaltyDao.getPenaltiesForGroup(groupId)
    }

    override fun getTotalAmount(groupId: String): Flow<Double> {
        return penaltyDao.getTotalForGroup(groupId)
    }


    override suspend fun getUnsyncedPenalties(): List<Penalty> =
        penaltyDao.getUnsyncedPenalties()

    override suspend fun markPenaltySynced(penalty: Penalty) {
        penaltyDao.markAsSynced(penalty.penaltyId)
    }


}