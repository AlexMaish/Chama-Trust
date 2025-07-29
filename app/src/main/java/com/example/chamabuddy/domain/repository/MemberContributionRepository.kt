package com.example.chamabuddy.domain.repository



import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.MemberContribution
import kotlinx.coroutines.flow.Flow

interface MemberContributionRepository {
    suspend fun insertContribution(contribution: MemberContribution)
    suspend fun getContributorsForMeeting(meetingId: String): List<MemberContribution>
    suspend fun hasContributed(meetingId: String, memberId: String): Boolean
    suspend fun getTotalContributedForMeeting(meetingId: String): Int
    fun getAllContributionsForMeeting(meetingId: String): Flow<List<MemberContribution>>
    suspend fun getContributionsByMeeting(meetingId: String): List<MemberContribution>
    suspend fun getContributionsByMember(memberId: String): List<MemberContribution>
    suspend fun deleteContribution(contributionId: String)






        suspend fun getContributionsForMeeting(meetingId: String): List<MemberContribution>


    suspend fun getUnsyncedContributions(): List<MemberContribution>
    suspend fun markContributionSynced(contribution: MemberContribution)
}
