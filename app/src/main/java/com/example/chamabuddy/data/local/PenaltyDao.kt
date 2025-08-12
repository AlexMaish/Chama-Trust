package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.Penalty
import kotlinx.coroutines.flow.Flow

@Dao
interface PenaltyDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(penalty: Penalty)

    @Query("SELECT * FROM penalties WHERE groupId = :groupId")
    fun getPenaltiesForGroup(groupId: String): kotlinx.coroutines.flow.Flow<List<Penalty>>

    @Query("SELECT SUM(amount) FROM penalties WHERE groupId = :groupId")
    fun getTotalForGroup(groupId: String): kotlinx.coroutines.flow.Flow<Double>

    @Query("UPDATE penalties SET is_synced = 1 WHERE penaltyId = :penaltyId")
    suspend fun markAsSynced(penaltyId: String)

    @Query("SELECT * FROM penalties WHERE is_synced = 0")
    suspend fun getUnsyncedPenalties(): List<Penalty>

    @Query("SELECT * FROM penalties WHERE penaltyId = :penaltyId LIMIT 1")
    suspend fun getPenaltyById(penaltyId: String): Penalty?


}