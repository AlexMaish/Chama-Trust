package com.example.chamabuddy.domain.repository

import android.net.Uri
import com.example.chamabuddy.domain.model.Member
import kotlinx.coroutines.flow.Flow

interface MemberRepository {
    suspend fun addMember(member: Member)
    suspend fun updateMember(member: Member)
    suspend fun deleteMember(member: Member)
    fun getAllMembers(): Flow<List<Member>>
    fun getActiveMembers(): Flow<List<Member>>
    suspend fun getMemberById(memberId: String): Member?
    suspend fun getMemberNameById(memberId: String): String?
    suspend fun updateProfilePicture(memberId: String, imageUri: Uri)
    suspend fun changePhoneNumber(memberId: String, newNumber: String)
    suspend fun getMembersByGroup(groupId: String): List<Member>
    suspend fun getActiveMembersCount(): Int
    fun getMembersByGroupFlow(groupId: String): Flow<List<Member>>

    suspend fun getMemberByUserId(userId: String, groupId: String): Member?

    suspend fun getMemberByPhoneForGroup(phone: String, groupId: String): Member?

    suspend fun getActiveMembersByGroup(groupId: String): List<Member>

    suspend fun getAdminCount(groupId: String): Int

    suspend fun updateAdminStatus(memberId: String, isAdmin: Boolean)

    suspend fun updateActiveStatus(memberId: String, isActive: Boolean)

    suspend fun getUnsyncedMembers(): List<Member>
    suspend fun markMemberSynced(member: Member)


    suspend fun syncMember(member: Member)

    suspend fun getUnsyncedMembersForGroup(groupId: String): List<Member>

}



