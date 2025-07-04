package com.example.chamabuddy.data.repository

import android.net.Uri
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.UserDao
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.UUID
import javax.inject.Inject

class MemberRepositoryImpl @Inject constructor(
    private val memberDao: MemberDao,
    private val userDao: UserDao
) : MemberRepository {

    override suspend fun getMemberNameById(memberId: String): String? {
        return withContext(Dispatchers.IO) {
            memberDao.getMemberById(memberId)?.name
        }
    }

    override suspend fun updateMember(member: Member) = withContext(Dispatchers.IO) {
        memberDao.updateMember(member)
    }

    override suspend fun deleteMember(member: Member) = withContext(Dispatchers.IO) {
        memberDao.deleteMember(member)
    }

    override suspend fun getMemberById(memberId: String): Member? = withContext(Dispatchers.IO) {
        memberDao.getMemberById(memberId)
    }

    override suspend fun updateProfilePicture(memberId: String, imageUri: Uri) =
        withContext(Dispatchers.IO) {
            val member = memberDao.getMemberById(memberId)
                ?: throw NoSuchElementException("Member not found")
            memberDao.updateMember(member.copy(profilePicture = imageUri.toString()))
        }

    override suspend fun changePhoneNumber(memberId: String, newNumber: String) =
        withContext(Dispatchers.IO) {
            val member = memberDao.getMemberById(memberId)
                ?: throw NoSuchElementException("Member not found")
            memberDao.updateMember(member.copy(phoneNumber = newNumber))
        }

    override suspend fun getMembersByGroup(groupId: String): List<Member> =
        withContext(Dispatchers.IO) {
            memberDao.getMembersByGroup(groupId).map { member ->
                member.copy(
                    name = member.name,
                    phoneNumber = member.phoneNumber
                )
            }
        }

    override suspend fun addMember(member: Member) {
        withContext(Dispatchers.IO) {
            // Check for duplicate phone number in the same group
            val existingMember = memberDao.getMemberByPhoneInGroup(
                member.groupId,
                member.phoneNumber.normalizePhone()
            )

            if (existingMember != null) {
                throw IllegalStateException("Member with this number already exists")
            }

            // Validate if phone number belongs to a registered user
            val user = userDao.getUserByPhone(member.phoneNumber)

            if (user == null) {
                throw IllegalStateException("Member must be a registered user")
            }

            val memberWithUserId = member.copy(userId = user.userId)
            val memberId = UUID.randomUUID().toString()
            memberDao.insertMember(memberWithUserId.copy(memberId = memberId))
        }
    }

    override suspend fun getActiveMembersCount(): Int =
        withContext(Dispatchers.IO) {
            memberDao.getActiveMembersCount()
        }

    override fun getAllMembers(): Flow<List<Member>> = memberDao.getAllMembers()

    override fun getActiveMembers(): Flow<List<Member>> = memberDao.getActiveMembers()


    override fun getMembersByGroupFlow(groupId: String): Flow<List<Member>> {
        return memberDao.getMembersByGroupFlow(groupId)
    }


    override suspend fun getMemberByUserId(userId: String, groupId: String): Member? {
        return withContext(Dispatchers.IO) {
            memberDao.getMemberByUserId(userId, groupId)
        }
    }

    override suspend fun getMemberByPhoneForGroup(phone: String, groupId: String): Member? {
        val normalized = phone.normalizePhone()
        return withContext(Dispatchers.IO) {
            memberDao.getMemberByNormalizedPhone(groupId, normalized)
        }
    }

    fun String.normalizePhone(): String {
        return this.replace(Regex("[^0-9]"), "").trim()
    }


}