package com.example.chamabuddy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.WelfareBeneficiary

@Dao
interface WelfareBeneficiaryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBeneficiary(beneficiary: WelfareBeneficiary)

    @Query("DELETE FROM welfare_beneficiaries WHERE meetingId = :meetingId")
    suspend fun deleteBeneficiariesForMeeting(meetingId: String)

    @Query("SELECT * FROM welfare_beneficiaries WHERE meetingId = :meetingId")
    suspend fun getBeneficiariesForMeeting(meetingId: String): List<WelfareBeneficiary>
}