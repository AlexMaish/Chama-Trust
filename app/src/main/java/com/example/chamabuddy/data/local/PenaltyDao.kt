package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.chamabuddy.domain.model.Penalty
import kotlinx.coroutines.flow.Flow

@Dao
interface PenaltyDao {
    @Insert
    suspend fun insert(penalty: Penalty)

    @Query("SELECT * FROM penalties WHERE groupId = :groupId")
    fun getPenaltiesForGroup(groupId: String): Flow<List<Penalty>>

    @Query("SELECT SUM(amount) FROM penalties WHERE groupId = :groupId")
    fun getTotalForGroup(groupId: String): Flow<Double>
}
