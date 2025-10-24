//package com.example.chamabuddy.workers

import android.content.Context
import androidx.work.WorkerParameters
import com.example.chamabuddy.domain.repository.GroupRepository
import com.example.chamabuddy.domain.repository.UserRepository
import com.google.firebase.firestore.FirebaseFirestore
import com.example.chamabuddy.data.local.preferences.SyncPreferences

//class SyncWorkerFactory(
//    private val userRepository: UserRepository,
//    private val groupRepository: GroupRepository,
//    private val firestore: FirebaseFirestore,
//    private val preferences: SyncPreferences,
//    private val syncHelper: SyncHelper // add this
//) : ChildWorkerFactory {
//    override fun create(
//        context: Context,
//        workerParams: WorkerParameters
//    ) = SyncWorker(
//        context,
//        workerParams,
//        userRepository,
//        groupRepository,
//        firestore,
//        preferences,
//        syncHelper // pass it here
//    )
//}
