package com.example.chamabuddy.domain.repository


import android.net.Uri
import com.example.chamabuddy.domain.model.Member
import kotlinx.coroutines.flow.Flow



interface MemberRepository {
    suspend fun addMember(member: Member)
    suspend fun updateMember(member: Member)
    suspend fun deleteMember(member: Member)
    fun getAllMembers(): Flow<List<Member>>
    suspend fun getMemberById(memberId: String): Member?
    fun getActiveMembers(): Flow<List<Member>>
    fun getAllAdmins(): Flow<List<Member>>
    suspend fun activateMember(memberId: String)
    suspend fun deactivateMember(memberId: String)
    suspend fun promoteToAdmin(memberId: String)
    suspend fun demoteFromAdmin(memberId: String)
    fun searchMembers(query: String): Flow<List<Member>>
    suspend fun getActiveMembersCount(): Int
    // New function needed by ViewModel
    suspend fun getMemberByName(name: String): Member?


    suspend fun updateProfilePicture(memberId: String, imageUri: Uri)
    suspend fun changePhoneNumber(memberId: String, newNumber: String)
}





