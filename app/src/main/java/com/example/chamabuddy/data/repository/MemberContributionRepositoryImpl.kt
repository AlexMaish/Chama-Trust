package com.example.chamabuddy.data.repository


import com.example.chamabuddy.data.local.MemberContributionDao
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.MemberContribution
import com.example.chamabuddy.domain.repository.MemberContributionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class MemberContributionRepositoryImpl @Inject constructor(
    private val dao: MemberContributionDao
) : MemberContributionRepository {

    override suspend fun insertContribution(contribution: MemberContribution) {
        dao.insertContribution(contribution)
    }

    override suspend fun getContributorsForMeeting(meetingId: String): List<MemberContribution> {
        return dao.getContributorsForMeeting(meetingId)
    }

    override suspend fun hasContributed(meetingId: String, memberId: String): Boolean {
        return dao.hasContributed(meetingId, memberId) > 0
    }

    override suspend fun getTotalContributedForMeeting(meetingId: String): Int {
        return dao.getTotalContributedForMeeting(meetingId)
    }

    override fun getAllContributionsForMeeting(meetingId: String): Flow<List<MemberContribution>> {
        return dao.getAllContributionsForMeeting(meetingId)
    }

    override suspend fun getContributionsByMeeting(meetingId: String): List<MemberContribution> {
        return dao.getContributionsByMeeting(meetingId)
    }

    override suspend fun getContributionsByMember(memberId: String): List<MemberContribution> {
        return dao.getContributionsByMember(memberId)
    }

    override suspend fun deleteContribution(contributionId: String) {
        dao.deleteContribution(contributionId)
    }

    override suspend fun getContributionsForMeeting(meetingId: String): List<MemberContribution> {
        return dao.getContributionsByMeeting(meetingId)
    }


    override suspend fun getUnsyncedContributions(): List<MemberContribution> =
        dao.getUnsyncedContributions()

    override suspend fun markContributionSynced(contribution: MemberContribution) {
        dao.markAsSynced(contribution.contributionId)
    }

}
