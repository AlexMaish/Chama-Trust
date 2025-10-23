package com.example.chamabuddy.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.Beneficiary
import com.example.chamabuddy.domain.model.Member

@Dao
interface BeneficiaryDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: Beneficiary)


    @Query("""
        SELECT COUNT(*) FROM beneficiaries b
        JOIN weeklymeeting w ON b.meeting_id = w.meeting_id
        WHERE b.member_id = :memberId AND w.cycle_id = :cycleId
    """)
    suspend fun hasReceivedInCycle(memberId: String, cycleId: String): Int


    @Query("""
        SELECT * FROM Member 
        WHERE member_id NOT IN (
            SELECT member_id FROM beneficiaries WHERE cycle_id = :cycleId
        )
        AND is_active = 1
    """)
    suspend fun getEligibleMembersForCycle(cycleId: String): List<Member>

    @Query("SELECT * FROM beneficiaries WHERE meeting_id = :meetingId ORDER BY payment_order")
    suspend fun getDirectBeneficiariesForMeeting(meetingId: String): List<Beneficiary>


    @Query("SELECT * FROM beneficiaries WHERE member_id = :memberId")
    suspend fun getBeneficiariesByMember(memberId: String): List<Beneficiary>


    @Query("DELETE FROM beneficiaries WHERE beneficiary_id = :beneficiaryId")
    suspend fun deleteBeneficiary(beneficiaryId: String)


    @Query("""
        SELECT * FROM Member 
        WHERE member_id NOT IN (
            SELECT member_id FROM beneficiaries WHERE meeting_id = :meetingId
        )
        AND is_active = 1
    """)
    suspend fun getEligibleMembersForMeeting(meetingId: String): List<Member>

    @Query("SELECT * FROM beneficiaries WHERE cycle_id = :cycleId")
    fun getBeneficiariesByCycle(cycleId: String): List<Beneficiary>

    @Query("SELECT * FROM beneficiaries WHERE beneficiary_id = :beneficiaryId LIMIT 1")
    suspend fun getBeneficiaryById(beneficiaryId: String): Beneficiary?



    @Query("SELECT * FROM beneficiaries WHERE meeting_id = :meetingId")
    suspend fun getBeneficiariesForMeeting(meetingId: String): List<Beneficiary>


    @Query("DELETE FROM beneficiaries WHERE meeting_id = :meetingId")
    suspend fun deleteBeneficiariesForMeeting(meetingId: String)



    @Query("UPDATE beneficiaries SET amount_received = :newAmount WHERE beneficiary_id = :beneficiaryId")
    suspend fun updateBeneficiaryAmount(beneficiaryId: String, newAmount: Int)


    @Query("SELECT COUNT(*) FROM beneficiaries WHERE meeting_id = :meetingId")
    suspend fun getBeneficiaryCountForMeeting(meetingId: String): Int



    @Query("UPDATE beneficiaries SET is_synced = 1 WHERE beneficiary_id = :beneficiaryId")
    suspend fun markAsSynced(beneficiaryId: String)

    @Query("SELECT * FROM beneficiaries WHERE is_synced = 0")
    suspend fun getUnsyncedBeneficiaries(): List<Beneficiary>



    @Query("UPDATE beneficiaries SET is_deleted = 1, deleted_at = :timestamp WHERE beneficiary_id = :beneficiaryId")
    suspend fun markAsDeleted(beneficiaryId: String, timestamp: Long)

    @Query("SELECT * FROM beneficiaries WHERE is_deleted = 1")
    suspend fun getDeletedBeneficiaries(): List<Beneficiary>

    @Query("DELETE FROM beneficiaries WHERE beneficiary_id = :beneficiaryId")
    suspend fun permanentDelete(beneficiaryId: String)



}