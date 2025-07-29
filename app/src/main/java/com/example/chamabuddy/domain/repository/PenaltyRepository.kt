package com.example.chamabuddy.domain.repository

import com.example.chamabuddy.domain.model.Penalty
import kotlinx.coroutines.flow.Flow

interface PenaltyRepository {
    suspend fun addPenalty(penalty: Penalty)
    fun getPenalties(groupId: String): Flow<List<Penalty>>
    fun getTotalAmount(groupId: String): Flow<Double>

    suspend fun getUnsyncedPenalties(): List<Penalty>
    suspend fun markPenaltySynced(penalty: Penalty)

}