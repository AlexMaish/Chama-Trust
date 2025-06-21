package com.example.chamabuddy.data.repository


import android.net.Uri
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.MemberRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberDao: MemberDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) : MemberRepository {

    override suspend fun getMemberByName(name: String): Member? = withContext(dispatcher) {
        memberDao.getMemberByName(name)
    }


    override suspend fun addMember(member: Member) = withContext(dispatcher) {
        memberDao.insertMember(member)
    }

    override suspend fun updateMember(member: Member) = withContext(dispatcher) {
        memberDao.updateMember(member)
    }

    override suspend fun deleteMember(member: Member) = withContext(dispatcher) {
        memberDao.deleteMember(member)
    }

    override fun getAllMembers(): Flow<List<Member>> = memberDao.getAllMembers()

    override suspend fun getMemberById(memberId: String): Member? = withContext(dispatcher) {
        memberDao.getMemberById(memberId)
    }

    override fun getActiveMembers(): Flow<List<Member>> = memberDao.getActiveMembers()

    override fun getAllAdmins(): Flow<List<Member>> = memberDao.getAllAdmins()

    override suspend fun activateMember(memberId: String) =
        updateMemberStatus(memberId, status = "active")

    override suspend fun deactivateMember(memberId: String) =
        updateMemberStatus(memberId, status = "inactive")

    override suspend fun promoteToAdmin(memberId: String) =
        updateMemberRole(memberId, isAdmin = true)

    override suspend fun demoteFromAdmin(memberId: String) =
        updateMemberRole(memberId, isAdmin = false)

    override fun searchMembers(query: String): Flow<List<Member>> =
        memberDao.getAllMembers().map { members ->
            val lowerQuery = query.lowercase()
            members.filter { member ->
                member.name.contains(lowerQuery, ignoreCase = true) ||
                        member.nickname?.contains(lowerQuery, ignoreCase = true) == true ||
                        member.phoneNumber.contains(lowerQuery, ignoreCase = true)
            }
        }

    // --- Private helpers to avoid repetition ---
    private suspend fun updateMemberStatus(memberId: String, status: String) =
        withContext(dispatcher) {
            val member = memberDao.getMemberById(memberId)
                ?: throw NoSuchElementException("Member not found")
            memberDao.updateMember(member.copy(currentStatus = status))
        }

    private suspend fun updateMemberRole(memberId: String, isAdmin: Boolean) =
        withContext(dispatcher) {
            val member = memberDao.getMemberById(memberId)
                ?: throw NoSuchElementException("Member not found")
            memberDao.updateMember(member.copy(isAdmin = isAdmin))
        }

    override suspend fun getActiveMembersCount(): Int {
        return memberDao.getActiveMembersCount()
    }

    override suspend fun updateProfilePicture(memberId: String, imageUri: Uri) =
        withContext(dispatcher) {
            val member = memberDao.getMemberById(memberId)
                ?: throw NoSuchElementException("Member not found")
            memberDao.updateMember(
                member.copy(profilePicture = imageUri.toString())
            )
        }

    override suspend fun changePhoneNumber(memberId: String, newNumber: String) =
        withContext(dispatcher) {
            val member = memberDao.getMemberById(memberId)
                ?: throw NoSuchElementException("Member not found")
            memberDao.updateMember(
                member.copy(phoneNumber = newNumber)
            )
        }
}