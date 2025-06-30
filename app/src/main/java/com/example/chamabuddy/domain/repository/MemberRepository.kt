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
}