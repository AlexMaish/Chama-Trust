package com.example.chamabuddy.data.repository

import com.example.chamabuddy.data.local.PenaltyDao
import com.example.chamabuddy.domain.model.Penalty
import com.example.chamabuddy.domain.repository.PenaltyRepository
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class PenaltyRepositoryImpl(
    private val penaltyDao: PenaltyDao
) : PenaltyRepository {

    override suspend fun addPenalty(penalty: Penalty) {
        val newId = UUID.randomUUID().toString()
        val updatedPenalty = penalty.copy(
            penaltyId = newId,
            isSynced = false
        )
        penaltyDao.insert(updatedPenalty)
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


    override suspend fun getPenaltyById(penaltyId: String): Penalty? {
        return penaltyDao.getPenaltyById(penaltyId)
    }

    override suspend fun insertPenalty(penalty: Penalty) {
        penaltyDao.insert(penalty)
    }

    override suspend fun markAsDeleted(penaltyId: String, timestamp: Long) =
        penaltyDao.markAsDeleted(penaltyId, timestamp)

    override suspend fun getDeletedPenalties(): List<Penalty> =
        penaltyDao.getDeletedPenalties()

    override suspend fun permanentDelete(penaltyId: String) =
        penaltyDao.permanentDelete(penaltyId)
}
