package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Penalty
import kotlinx.coroutines.flow.Flow

interface PenaltyRepository {
    suspend fun addPenalty(penalty: Penalty)
    fun getPenalties(groupId: String): Flow<List<Penalty>>
    fun getTotalAmount(groupId: String): Flow<Double>

    suspend fun getUnsyncedPenalties(): List<Penalty>
    suspend fun markPenaltySynced(penalty: Penalty)

    suspend fun getPenaltyById(penaltyId: String): Penalty?
    suspend fun insertPenalty(penalty: Penalty)


    suspend fun markAsDeleted(penaltyId: String, timestamp: Long)
    suspend fun getDeletedPenalties(): List<Penalty>
    suspend fun permanentDelete(penaltyId: String)

    suspend fun updatePenalty(penalty: Penalty)

    suspend fun findSimilarPenalty(groupId: String, memberId: String, description: String, amount: Double, date: Long): Penalty?

}