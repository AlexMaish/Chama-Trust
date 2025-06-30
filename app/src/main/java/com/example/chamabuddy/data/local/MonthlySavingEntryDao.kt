package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import kotlinx.coroutines.flow.Flow

@Dao
interface MonthlySavingEntryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSavingEntry(entry: MonthlySavingEntry)

    @Query("""
        SELECT * FROM MonthlySavingEntry
        WHERE saving_id = :savingId
        ORDER BY entry_date DESC
    """)
    fun getEntriesForSaving(savingId: String): Flow<List<MonthlySavingEntry>>

//    @Query("""
//        SELECT SUM(amount) FROM MonthlySavingEntry
//        WHERE saving_id = :savingId AND member_id = :memberId
//    """)
//    suspend fun getMemberSavingsTotal(savingId: String, memberId: String): Int

    @Query("""
        SELECT mse.* FROM MonthlySavingEntry mse
        JOIN MonthlySaving ms ON mse.saving_id = ms.saving_id
        WHERE ms.cycle_id = :cycleId AND mse.member_id = :memberId
        ORDER BY mse.entry_date DESC
    """)
    fun getMemberSavingsForCycle(cycleId: String, memberId: String): Flow<List<MonthlySavingEntry>>

    @Query("""
    SELECT SUM(amount) FROM MonthlySavingEntry
    WHERE member_id = :memberId 
    AND saving_id IN (SELECT saving_id FROM MonthlySaving WHERE cycle_id = :cycleId)
""")
    suspend fun getMemberSavingsTotalByCycle(cycleId: String, memberId: String): Int


    @Query("SELECT SUM(amount) FROM MonthlySavingEntry WHERE member_id = :memberId")
    suspend fun getMemberSavingsTotal(memberId: String): Int?

    @Query("""
    SELECT SUM(amount) FROM MonthlySavingEntry
    WHERE saving_id IN (SELECT saving_id FROM MonthlySaving WHERE cycle_id = :cycleId)
""")
    suspend fun getTotalSavingsByCycle(cycleId: String): Int?

    @Query("SELECT SUM(amount) FROM MonthlySavingEntry")
    suspend fun getTotalSavings(): Int?
//    @Insert
//    suspend fun insertSavingEntry(entry: MonthlySavingEntry)
//
//    @Query("SELECT * FROM MonthlySavingEntry WHERE saving_id = :savingId")
//    fun getEntriesForSaving(savingId: String): Flow<List<MonthlySavingEntry>>


}