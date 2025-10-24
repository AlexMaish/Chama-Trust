package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySavingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMonthlySaving(saving: MonthlySaving)


        @Insert(onConflict = OnConflictStrategy.REPLACE)
        suspend fun insert(saving: MonthlySaving)


        @Update
        suspend fun update(saving: MonthlySaving)


    @Query("""
        SELECT DISTINCT m.cycle_id 
        FROM MonthlySavingEntry e
        INNER JOIN MonthlySaving m ON e.saving_id = m.saving_id
        WHERE e.member_id = :memberId
    """)
    suspend fun getDistinctCycleIdsForMember(memberId: String): List<String>

    @Query("""
        SELECT e.* 
        FROM MonthlySavingEntry e
        INNER JOIN MonthlySaving m ON e.saving_id = m.saving_id
        WHERE e.member_id = :memberId AND m.cycle_id = :cycleId
    """)
    suspend fun getSavingsForMemberInCycle(memberId: String, cycleId: String): List<MonthlySavingEntry>


    @Query("DELETE FROM MonthlySaving WHERE saving_id = :savingId")
    suspend fun deleteSaving(savingId: String)



    @Query("UPDATE MonthlySaving SET is_synced = 1 WHERE saving_id = :savingId")
    suspend fun markAsSynced(savingId: String)


    @Query("""
    SELECT COALESCE(SUM(actual_amount), 0) 
    FROM MonthlySaving 
    WHERE group_id = :groupId AND month_year = :monthYear AND is_deleted = 0
""")
    suspend fun getTotalSavingsForMonth(groupId: String, monthYear: String): Int?


    @Query("SELECT * FROM MonthlySaving WHERE is_deleted = 1")
    suspend fun getDeletedSavings(): List<MonthlySaving>








    // 🔹 Soft delete
    @Query("UPDATE MonthlySaving SET is_deleted = 1, deleted_at = :timestamp WHERE saving_id = :savingId")
    suspend fun markAsDeleted(savingId: String, timestamp: Long)


    // 🔹 Permanently delete
    @Query("DELETE FROM MonthlySaving WHERE saving_id = :savingId")
    suspend fun permanentDelete(savingId: String)



    @Query("SELECT * FROM MonthlySaving WHERE cycle_id = :cycleId AND month_year = :monthYear AND is_deleted = 0")
    suspend fun getSavingForMonth(cycleId: String, monthYear: String): MonthlySaving?

    @Query("SELECT * FROM MonthlySaving WHERE cycle_id = :cycleId AND is_deleted = 0 ORDER BY month_year DESC")
    fun getSavingsForCycle(cycleId: String): Flow<List<MonthlySaving>>

    @Query("SELECT * FROM MonthlySaving WHERE is_synced = 0 AND is_deleted = 0")
    suspend fun getUnsyncedSavings(): List<MonthlySaving>

    @Query("SELECT * FROM MonthlySaving WHERE saving_id = :savingId AND is_deleted = 0 LIMIT 1")
    suspend fun getSavingById(savingId: String): MonthlySaving?


}