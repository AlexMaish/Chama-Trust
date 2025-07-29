package com.example.chamabuddy.workers

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.chamabuddy.data.remote.toFirebase // Add missing import
import com.example.chamabuddy.data.remote.toLocal    // Add missing import
import com.example.chamabuddy.data.sync.FirestoreSyncManager
import com.example.chamabuddy.domain.Firebase.MemberFire
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.repository.MemberRepository
import com.google.firebase.Timestamp // Add missing import
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val memberRepository: MemberRepository,
    private val syncManager: FirestoreSyncManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            // Explicitly specify type <Member> for syncEntity
            syncManager.syncEntity<Member>(
                collectionName = "members",
                localFetch = { memberRepository.getUnsyncedMembers() },
                toFirebase = { member -> member.toFirebase() },
                fromFirestore = { data ->
                    MemberFire(
                        memberId = data["memberId"] as? String ?: "",
                        name = data["name"] as? String ?: "",
                        nickname = data["nickname"] as? String,
                        phoneNumber = data["phoneNumber"] as? String ?: "",
                        profilePicture = data["profilePicture"] as? String,
                        isAdmin = data["isAdmin"] as? Boolean ?: false,
                        isActive = data["isActive"] as? Boolean ?: true,
                        joinDate = data["joinDate"] as? Timestamp ?: Timestamp.now(),
                        userId = data["userId"] as? String,
                        groupId = data["groupId"] as? String ?: "",
                        isOwner = data["isOwner"] as? Boolean ?: false,
                        lastUpdated = data["lastUpdated"] as? Timestamp ?: Timestamp.now()
                    ).toLocal()
                },
                updateLocal = { member ->
                    memberRepository.syncMember(member)
                },
                getId = { it.memberId }
            )

            Result.success()
        } catch (e: Exception) {
            Result.failure()
        }
    }
}