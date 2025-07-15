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


    @Query("SELECT * FROM Cycle WHERE group_id = :groupId AND is_active = 1")
    suspend fun getActiveCycleForGroup(groupId: String): Cycle?

    @Query("SELECT * FROM Cycle WHERE group_id = :groupId")
    suspend fun getCyclesForGroup(groupId: String): List<Cycle>

    @Query("SELECT SUM(total_savings) FROM Cycle")

    suspend fun getTotalSavings(): Int?

    @Query("SELECT * FROM Cycle ORDER BY start_date DESC")
    suspend fun getCycleHistory(): List<Cycle>


    @Query("SELECT * FROM Cycle WHERE group_id = :groupId AND is_active = 1 LIMIT 1")
    suspend fun getActiveCycleByGroupId(groupId: String): Cycle?


    @Query("SELECT * FROM Cycle WHERE group_id = :groupId")
    suspend fun getCyclesByGroupId(groupId: String): List<Cycle>

    @Query("SELECT * FROM Cycle WHERE cycle_id IN (:cycleIds)")
    suspend fun getCyclesByIds(cycleIds: List<String>): List<Cycle>

    @Query("SELECT * FROM cycle WHERE group_id = :groupId")
    suspend fun getCyclesByGroup(groupId: String): List<Cycle>


    @Query("SELECT * FROM cycle WHERE group_id= :groupId ORDER BY start_date DESC LIMIT 1")
    suspend fun getLastCycleByGroupId(groupId: String): Cycle?
}

