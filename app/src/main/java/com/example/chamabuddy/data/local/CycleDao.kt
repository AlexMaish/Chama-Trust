package com.example.chamabuddy.data.local

import androidx.room.*
import com.example.chamabuddy.domain.model.Cycle
import kotlinx.coroutines.flow.Flow

@Dao
interface CycleDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCycle(cycle: Cycle)

    @Update
    suspend fun updateCycle(cycle: Cycle)

    @Query("SELECT * FROM Cycle WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveCycle(): Cycle?

    @Query("SELECT * FROM Cycle WHERE is_active = 1 LIMIT 1")
    fun observeActiveCycle(): Flow<Cycle?>

    @Query("SELECT * FROM Cycle ORDER BY start_date DESC")
    fun getAllCycles(): Flow<List<Cycle>>

    @Query("SELECT * FROM Cycle WHERE cycle_id = :cycleId LIMIT 1")
    suspend fun getCycleById(cycleId: String): Cycle?

    @Query("UPDATE Cycle SET is_active = 0 WHERE cycle_id = :cycleId")
    suspend fun endCycle(cycleId: String)

    @Query("SELECT * FROM Cycle WHERE is_active = 1 LIMIT 1")
    suspend fun getCurrentCycle(): Cycle?
}
