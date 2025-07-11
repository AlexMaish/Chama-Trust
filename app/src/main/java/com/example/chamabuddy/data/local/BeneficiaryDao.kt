package com.example.chamabuddy.data.local
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.Beneficiary
import com.example.chamabuddy.domain.model.Member
/**
 * Data Access Object (DAO) for interacting with the Beneficiaries table.
 */
@Dao
interface BeneficiaryDao {

    /**
     * Inserts a new beneficiary into the database.
     * If a beneficiary with the same primary key already exists, it will be replaced.
     *
     * @param beneficiary The beneficiary to insert.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: Beneficiary)

    /**
     * Retrieves the beneficiaries for a specific meeting, ordered by payment order.
     *
     * @param meetingId The ID of the meeting.
     * @return A list of members who are beneficiaries for the meeting.
     */
//    @Query("""
//        SELECT m.* FROM member m
//        JOIN beneficiaries b ON m.member_id = b.member_id
//        WHERE b.meeting_id = :meetingId
//        ORDER BY b.payment_order
//    """)
//    suspend fun getBeneficiariesForMeeting(meetingId: String): List<Member>

    /**
     * Checks if a member has received a benefit in a specific cycle.
     *
     * @param memberId The ID of the member.
     * @param cycleId The ID of the cycle.
     * @return The count of benefits received by the member in the cycle.
     */
    @Query("""
        SELECT COUNT(*) FROM beneficiaries b
        JOIN weeklymeeting w ON b.meeting_id = w.meeting_id
        WHERE b.member_id = :memberId AND w.cycle_id = :cycleId
    """)
    suspend fun hasReceivedInCycle(memberId: String, cycleId: String): Int

    /**
     * Retrieves members who are eligible for a specific cycle (i.e., members who haven't received a benefit in that cycle and are active).
     *
     * @param cycleId The ID of the cycle.
     * @return A list of eligible members.
     */
    @Query("""
        SELECT * FROM Member 
        WHERE member_id NOT IN (
            SELECT member_id FROM beneficiaries WHERE cycle_id = :cycleId
        )
        AND is_active = 1
    """)
    suspend fun getEligibleMembersForCycle(cycleId: String): List<Member>
    /**
     * Retrieves the direct beneficiaries for a specific meeting, ordered by payment order.
     *
     * @param meetingId The ID of the meeting.
     * @return A list of beneficiaries for the meeting.
     */
    @Query("SELECT * FROM beneficiaries WHERE meeting_id = :meetingId ORDER BY payment_order")
    suspend fun getDirectBeneficiariesForMeeting(meetingId: String): List<Beneficiary>

    /**
     * Retrieves all beneficiaries for a specific member.
     *
     * @param memberId The ID of the member.
     * @return A list of beneficiaries for the member.
     */
    @Query("SELECT * FROM beneficiaries WHERE member_id = :memberId")
    suspend fun getBeneficiariesByMember(memberId: String): List<Beneficiary>

    /**
     * Deletes a beneficiary by ID.
     *
     * @param beneficiaryId The ID of the beneficiary to delete.
     */
    @Query("DELETE FROM beneficiaries WHERE beneficiary_id = :beneficiaryId")
    suspend fun deleteBeneficiary(beneficiaryId: String)

    /**
     * Retrieves members who are eligible for a specific meeting (i.e., members who aren't already beneficiaries for that meeting and are active).
     *
     * @param meetingId The ID of the meeting.
     * @return A list of eligible members.
     */
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

//    @Query("SELECT COUNT(*) FROM beneficiaries WHERE member_id = :memberId AND cycle_id = :cycleId")
//    suspend fun hasReceivedInCycle(memberId: String, cycleId: String): Int
}