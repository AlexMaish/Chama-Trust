package com.example.chamabuddy.data.local

import kotlinx.coroutines.flow.Flow
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.WeeklyMeeting


@Dao
interface MemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMember(member: Member)

    @Update
    suspend fun updateMember(member: Member)



    @Delete
    suspend fun deleteMember(member: Member)

    @Query("SELECT * FROM Member WHERE member_id = :memberId")
    suspend fun getMemberById(memberId: String): Member?

    @Query("SELECT * FROM Member WHERE is_admin = 1")
    fun getAllAdmins(): Flow<List<Member>>

    @Query("SELECT * FROM Member WHERE is_active = 1 ORDER BY name ASC")
    fun getActiveMembers(): Flow<List<Member>>

    @Query("SELECT * FROM Member ORDER BY name ASC")
    fun getAllMembers(): Flow<List<Member>>

    @Query("SELECT * FROM Member WHERE name LIKE '%' || :query || '%' OR nickname LIKE '%' || :query || '%' OR phone_number LIKE '%' || :query || '%'")
    fun searchMembers(query: String): Flow<List<Member>>

    @Query("SELECT * FROM Member WHERE name = :name LIMIT 1")
    suspend fun getMemberByName(name: String): Member?

    @Query("SELECT COUNT(*) FROM member WHERE is_active = 1")
    suspend fun getActiveMembersCount(): Int


    @Query("""
        SELECT m.* FROM Member m
        INNER JOIN group_members gm ON m.user_id = gm.user_id
        WHERE gm.group_id = :groupId AND m.is_active = 1
        ORDER BY m.name ASC
    """)
    suspend fun getActiveMembersForGroup(groupId: String): List<Member>


    @Query("SELECT * FROM member WHERE group_id = :groupId")
    suspend fun getMembersByGroup(groupId: String): List<Member>



    @Query("SELECT * FROM member WHERE group_id = :groupId")
    fun getMembersByGroupFlow(groupId: String): Flow<List<Member>>

    @Query("SELECT * FROM member WHERE user_id = :userId AND group_id = :groupId")
    suspend fun getMemberByUserId(userId: String, groupId: String): Member?



    @Query("SELECT * FROM member WHERE group_id = :groupId AND phone_number = :phoneNumber")
    suspend fun getMemberByPhoneInGroup(groupId: String, phoneNumber: String): Member?


    @Query("SELECT * FROM member WHERE group_id = :groupId AND REPLACE(REPLACE(phone_number, ' ', ''), '-', '') = :normalizedPhone")
    suspend fun getMemberByNormalizedPhone(groupId: String, normalizedPhone: String): Member?

    @Query("SELECT * FROM member WHERE group_id = :groupId AND is_active = 1")
    suspend fun getActiveMembersByGroup(groupId: String): List<Member>
}
