// AppModule.kt
package com.example.chamabuddy.di

import android.content.Context
import android.net.ConnectivityManager
import androidx.room.Room
import androidx.work.WorkManager
import com.example.chamabuddy.data.local.*
import com.example.chamabuddy.data.repository.*
import com.example.chamabuddy.data.sync.FirestoreSyncManager
import com.example.chamabuddy.data.sync.SyncRepository
import com.example.chamabuddy.domain.repository.*
import com.example.chamabuddy.workers.SyncWorker
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton
import androidx.work.ListenableWorker // For ListenableWorker
import androidx.hilt.work.WorkerAssistedFactory // For WorkerAssistedFactory
import androidx.work.WorkerFactory
import com.example.chamabuddy.data.local.preferences.SyncPreferences
import com.example.chamabuddy.workers.CustomWorkerFactory
import com.example.chamabuddy.workers.SyncWorkerFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    // --- Database ---
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(
            context.applicationContext,
            AppDatabase::class.java,
            "mchama_database"
        )
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideMemberDao(db: AppDatabase): MemberDao = db.memberDao()
    @Provides fun provideCycleDao(db: AppDatabase): CycleDao = db.cycleDao()
    @Provides fun provideMeetingDao(db: AppDatabase): WeeklyMeetingDao = db.meetingDao()
    @Provides fun provideBeneficiaryDao(db: AppDatabase): BeneficiaryDao = db.beneficiaryDao()
    @Provides fun provideContributionDao(db: AppDatabase): MemberContributionDao = db.contributionDao()
    @Provides fun provideMonthlySavingDao(db: AppDatabase): MonthlySavingDao = db.monthlySavingDao()
    @Provides fun provideSavingEntryDao(db: AppDatabase): MonthlySavingEntryDao = db.savingEntryDao()
    @Provides fun provideGroupDao(db: AppDatabase): GroupDao = db.groupDao()
    @Provides fun provideUserDao(db: AppDatabase): UserDao = db.userDao()
    @Provides fun provideUserGroupDao(db: AppDatabase): UserGroupDao = db.userGroupDao()
    @Provides fun provideGroupMemberDao(db: AppDatabase): GroupMemberDao = db.groupMemberDao()
    @Provides fun providePenaltyDao(db: AppDatabase): PenaltyDao = db.penaltyDao()
    @Provides fun provideExpenseDao(db: AppDatabase): ExpenseDao = db.expenseDao()
    @Provides fun provideBenefitDao(db: AppDatabase): BenefitDao = db.benefitDao()

    // --- Dispatchers ---
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    // --- Repositories ---
    @Provides
    @Singleton
    fun provideUserRepository(
        userDao: UserDao,
        userGroupDao: UserGroupDao,
        memberRepository: MemberRepository,
        @ApplicationContext context: Context,
        firebaseAuth: FirebaseAuth,
        firestore: FirebaseFirestore
    ): UserRepository = UserRepositoryImpl(
        userDao = userDao,
        userGroupDao = userGroupDao,
        memberRepository = memberRepository,
        firebaseAuth = firebaseAuth,
        firestore = firestore,
        context = context
    )


    @Provides
    @Singleton
    fun provideMemberRepository(
        memberDao: MemberDao,
        userDao: UserDao,
        userGroupDao: UserGroupDao
    ): MemberRepository = MemberRepositoryImpl(
        memberDao,
        userDao,
        userGroupDao
    )

    @Provides
    @Singleton
    fun provideCycleRepository(
        cycleDao: CycleDao,
        meetingDao: WeeklyMeetingDao,
        beneficiaryDao: BeneficiaryDao,
        memberDao: MemberDao,
        groupRepository: GroupRepository,
        dispatcher: CoroutineDispatcher
    ): CycleRepository = CycleRepositoryImpl(
        cycleDao,
        meetingDao,
        beneficiaryDao,
        memberDao,
        groupRepository,
        dispatcher
    )

    @Provides
    @Singleton
    fun provideMeetingRepository(
        meetingDao: WeeklyMeetingDao,
        contributionDao: MemberContributionDao,
        beneficiaryDao: BeneficiaryDao,
        memberDao: MemberDao,
        cycleDao: CycleDao,
        weeklyMeetingDao: WeeklyMeetingDao,
        dispatcher: CoroutineDispatcher
    ): MeetingRepository = MeetingRepositoryImpl(
        meetingDao,
        contributionDao,
        beneficiaryDao,
        memberDao,
        cycleDao,
        weeklyMeetingDao,
        dispatcher
    )

    @Provides
    @Singleton
    fun provideBeneficiaryRepository(
        beneficiaryDao: BeneficiaryDao,
        dispatcher: CoroutineDispatcher
    ): BeneficiaryRepository = BeneficiaryRepositoryImpl(beneficiaryDao, dispatcher)

    @Provides
    @Singleton
    fun provideMemberContributionRepository(
        contributionDao: MemberContributionDao
    ): MemberContributionRepository = MemberContributionRepositoryImpl(contributionDao)

    @Provides
    @Singleton
    fun provideSavingsRepository(
        db: AppDatabase,
        savingDao: MonthlySavingDao,
        savingEntryDao: MonthlySavingEntryDao,
        cycleDao: CycleDao,
        memberDao: MemberDao,
        dispatcher: CoroutineDispatcher
    ): SavingsRepository = SavingsRepositoryImpl(
        db,
        savingDao,
        savingEntryDao,
        cycleDao,
        memberDao,
        dispatcher
    )

    @Provides
    @Singleton
    fun provideGroupRepository(
        groupDao: GroupDao,
        memberDao: MemberDao,
        userGroupDao: UserGroupDao,
        userRepository: UserRepository,
        groupMemberDao: GroupMemberDao
    ): GroupRepository = GroupRepositoryImpl(
        groupDao,
        memberDao,
        userGroupDao,
        userRepository,
        groupMemberDao
    )

    @Provides
    @Singleton
    fun provideBenefitRepository(
        benefitDao: BenefitDao
    ): BenefitRepository = BenefitRepositoryImpl(benefitDao)

    @Provides
    @Singleton
    fun providePenaltyRepository(
        penaltyDao: PenaltyDao
    ): PenaltyRepository = PenaltyRepositoryImpl(penaltyDao)

    @Provides
    @Singleton
    fun provideExpenseRepository(
        expenseDao: ExpenseDao
    ): ExpenseRepository = ExpenseRepositoryImpl(expenseDao)

    // --- Firestore Sync ---
    @Provides
    @Singleton
    fun provideFirestoreSyncManager(
        @ApplicationContext context: Context
    ): FirestoreSyncManager = FirestoreSyncManager(context)

    @Provides
    @Singleton
    fun provideSyncRepository(
        syncManager: FirestoreSyncManager,
        userRepository: UserRepository,
        groupRepository: GroupRepository,
        cycleRepository: CycleRepository,
        memberRepository: MemberRepository,
        meetingRepository: MeetingRepository,
        memberContributionRepository: MemberContributionRepository,
        beneficiaryRepository: BeneficiaryRepository,
        savingRepository: SavingsRepository,
        benefitRepository: BenefitRepository,
        expenseRepository: ExpenseRepository,
        penaltyRepository: PenaltyRepository
    ): SyncRepository = SyncRepository(
        syncManager,
        userRepository,
        groupRepository,
        cycleRepository,
        memberRepository,
        meetingRepository,
        memberContributionRepository,
        beneficiaryRepository,
        savingRepository,
        benefitRepository,
        expenseRepository,
        penaltyRepository
    )

    @Provides
    fun provideWorkManager(
        @ApplicationContext context: Context
    ): WorkManager = WorkManager.getInstance(context)

    @Provides
    @Singleton
    fun provideConnectivityManager(
        @ApplicationContext context: Context
    ): ConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager




    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()



}



@Module
@InstallIn(SingletonComponent::class)
object WorkerModule {

    @Provides
    fun provideSyncWorkerFactory(
        userRepository: UserRepository,
        firestore: FirebaseFirestore,
        preferences: SyncPreferences
    ): SyncWorkerFactory {
        return SyncWorkerFactory(userRepository, firestore, preferences)
    }

    @Provides
    fun provideCustomWorkerFactory(syncWorkerFactory: SyncWorkerFactory): WorkerFactory {
        return CustomWorkerFactory(syncWorkerFactory)
    }
}


