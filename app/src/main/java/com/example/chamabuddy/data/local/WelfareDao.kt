package com.example.chamabuddy.data.local


import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.chamabuddy.domain.model.Welfare
import kotlinx.coroutines.flow.Flow

@Dao
interface WelfareDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(welfare: Welfare)

    @Query("SELECT * FROM welfares WHERE groupId = :groupId AND isDeleted = 0")
    fun getWelfaresByGroup(groupId: String): Flow<List<Welfare>>

    @Query("SELECT * FROM welfares WHERE welfareId = :welfareId")
    suspend fun getWelfareById(welfareId: String): Welfare?

    @Query("SELECT * FROM welfares WHERE isSynced = 0")
    suspend fun getUnsyncedWelfares(): List<Welfare>

    @Query("UPDATE welfares SET isSynced = 1 WHERE welfareId = :welfareId")
    suspend fun markAsSynced(welfareId: String)

    @androidx.room.Update
    suspend fun updateWelfare(welfare: Welfare)

    @Query("UPDATE welfares SET isDeleted = 1, deletedAt = :timestamp WHERE welfareId = :welfareId")
    suspend fun markAsDeleted(welfareId: String, timestamp: Long)

    @Query("SELECT * FROM welfares WHERE isDeleted = 1")
    suspend fun getDeletedWelfares(): List<Welfare>

    @Query("DELETE FROM welfares WHERE welfareId = :welfareId")
    suspend fun permanentDelete(welfareId: String)
}
