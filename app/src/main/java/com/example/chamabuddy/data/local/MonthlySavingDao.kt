package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.MonthlySaving
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySavingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlySaving(saving: MonthlySaving)

    @Query(
        """
        SELECT * FROM MonthlySaving
        WHERE cycle_id = :cycleId
        ORDER BY month_year DESC
    """
    )
    fun getSavingsForCycle(cycleId: String): Flow<List<MonthlySaving>>

    @Query(
        """
        SELECT * FROM MonthlySaving
        WHERE cycle_id = :cycleId AND month_year = :monthYear
    """
    )
    suspend fun getSavingForMonth(cycleId: String, monthYear: String): MonthlySaving?


        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(saving: MonthlySaving)

//        @Query("SELECT * FROM MonthlySaving WHERE cycle_id = :cycleId AND month_year = :monthYear")
//        suspend fun getSavingForMonth(cycleId: String, monthYear: String): MonthlySaving?

        @Update
        suspend fun update(saving: MonthlySaving)

//        @Query("SELECT * FROM MonthlySaving WHERE cycle_id = :cycleId")
//        fun getSavingsForCycle(cycleId: String): Flow<List<MonthlySaving>>
//
}