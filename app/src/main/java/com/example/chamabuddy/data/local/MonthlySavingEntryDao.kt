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

    @Query("""
        SELECT * 
        FROM MonthlySavingEntry 
        WHERE member_id = :memberId 
        AND saving_id IN (
            SELECT saving_id 
            FROM MonthlySaving 
            WHERE cycle_id = :cycleId
        )
    """)
    suspend fun getSavingsForMemberInCycle(memberId: String, cycleId: String): List<MonthlySavingEntry>


    @Query("""
        SELECT COALESCE(SUM(amount), 0)
        FROM MonthlySavingEntry
        WHERE member_id = :memberId
        AND group_id = :groupId
        AND saving_id IN (
            SELECT saving_id FROM MonthlySaving 
            WHERE cycle_id = :cycleId
        )
    """)
    suspend fun getTotalForMemberInGroupCycle(
        memberId: String,
        groupId: String,
        cycleId: String
    ): Int


    @Query("SELECT COALESCE(SUM(amount), 0) FROM MonthlySavingEntry WHERE group_id = :groupId")
    suspend fun getTotalSavingsByGroup(groupId: String): Int



    @Query("""
        SELECT COALESCE(SUM(amount), 0) 
        FROM MonthlySavingEntry 
        WHERE member_id = :memberId 
        AND group_id = :groupId
    """)
    suspend fun getMemberSavingsTotalInGroup(groupId: String, memberId: String): Int

    @Query("""
    SELECT COALESCE(SUM(amount), 0) 
    FROM MonthlySavingEntry 
    WHERE saving_id IN (
        SELECT saving_id 
        FROM MonthlySaving 
        WHERE cycle_id = :cycleId AND month_year = :monthYear
    )
    AND member_id = :memberId
""")
    suspend fun getCurrentTotalForMemberMonth(
        cycleId: String,
        monthYear: String,
        memberId: String
    ): Int

    @Query("DELETE FROM MonthlySavingEntry WHERE entry_id = :entryId")
    suspend fun deleteEntry(entryId: String)


    @Query("UPDATE MonthlySavingEntry SET is_synced = 1 WHERE entry_id = :entryId")
    suspend fun markAsSynced(entryId: String)

    @Query("SELECT * FROM MonthlySavingEntry WHERE is_synced = 0")
    suspend fun getUnsyncedEntries(): List<MonthlySavingEntry>


    // ✅ NEW METHODS
    @Query("SELECT * FROM MonthlySavingEntry WHERE entry_id = :entryId LIMIT 1")
    suspend fun getEntryById(entryId: String): MonthlySavingEntry?

    @androidx.room.Update
    suspend fun updateEntry(entry: MonthlySavingEntry)
}