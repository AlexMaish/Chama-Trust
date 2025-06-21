package com.example.chamabuddy.di

import android.content.Context
import androidx.room.Room
import com.example.chamabuddy.data.local.AppDatabase
import com.example.chamabuddy.data.local.BeneficiaryDao
import com.example.chamabuddy.data.local.CycleDao
import com.example.chamabuddy.data.local.MemberContributionDao
import com.example.chamabuddy.data.local.MemberDao
import com.example.chamabuddy.data.local.MonthlySavingDao
import com.example.chamabuddy.data.local.MonthlySavingEntryDao
import com.example.chamabuddy.data.local.WeeklyMeetingDao
import com.example.chamabuddy.data.repository.BeneficiaryRepositoryImpl
import com.example.chamabuddy.data.repository.CycleRepositoryImpl
import com.example.chamabuddy.data.repository.MemberContributionRepositoryImpl
import com.example.chamabuddy.data.repository.MemberRepositoryImpl
import com.example.chamabuddy.data.repository.MeetingRepositoryImpl
import com.example.chamabuddy.data.repository.SavingsRepositoryImpl
//import com.example.chamabuddy.data.repository.ChamaRepositoryImpl
import com.example.chamabuddy.domain.repository.BeneficiaryRepository
import com.example.chamabuddy.domain.repository.CycleRepository
import com.example.chamabuddy.domain.repository.MemberContributionRepository
import com.example.chamabuddy.domain.repository.MemberRepository
import com.example.chamabuddy.domain.repository.MeetingRepository
import com.example.chamabuddy.domain.repository.SavingsRepository
import com.example.chamabuddy.domain.repository.ChamaRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "chamabuddy_db"
        ).fallbackToDestructiveMigration()
            .build()
    }

    // DAOs
    @Provides fun provideMemberDao(db: AppDatabase): MemberDao = db.memberDao()
    @Provides fun provideCycleDao(db: AppDatabase): CycleDao = db.cycleDao()
    @Provides fun provideMeetingDao(db: AppDatabase): WeeklyMeetingDao = db.meetingDao()
    @Provides fun provideBeneficiaryDao(db: AppDatabase): BeneficiaryDao = db.beneficiaryDao()
    @Provides fun provideContributionDao(db: AppDatabase): MemberContributionDao = db.contributionDao()
    @Provides fun provideMonthlySavingDao(db: AppDatabase): MonthlySavingDao = db.monthlySavingDao()
    @Provides fun provideSavingEntryDao(db: AppDatabase): MonthlySavingEntryDao = db.savingEntryDao()


    // Repositories
    @Provides
    @Singleton
    fun provideMemberRepository(
        dao: MemberDao
    ): MemberRepository = MemberRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideCycleRepository(
        cycleDao: CycleDao,
        meetingDao: WeeklyMeetingDao,
        beneficiaryDao: BeneficiaryDao,
        memberDao: MemberDao
    ): CycleRepository = CycleRepositoryImpl(cycleDao, meetingDao, beneficiaryDao, memberDao)

    @Provides
    @Singleton
    fun provideMemberContributionRepository(
        dao: MemberContributionDao
    ): MemberContributionRepository = MemberContributionRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideBeneficiaryRepository(
        dao: BeneficiaryDao
    ): BeneficiaryRepository = BeneficiaryRepositoryImpl(dao)

    @Provides
    @Singleton
    fun provideMeetingRepository(
        meetingDao: WeeklyMeetingDao,
        contributionDao: MemberContributionDao,
        beneficiaryDao: BeneficiaryDao,
        memberDao: MemberDao,
        cycleDao: CycleDao
    ): MeetingRepository = MeetingRepositoryImpl(
        meetingDao,
        contributionDao,
        beneficiaryDao,
        memberDao,
        cycleDao
    )

    @Provides
    @Singleton
    fun provideSavingsRepository(
        savingDao: MonthlySavingDao,
        entryDao: MonthlySavingEntryDao,
        cycleDao: CycleDao,
        memberDao: MemberDao
    ): SavingsRepository = SavingsRepositoryImpl(savingDao, entryDao, cycleDao, memberDao)

//    @Provides
//    @Singleton
//    fun provideChamaRepository(
//        memberRepo: MemberRepository,
//        cycleRepo: CycleRepository,
//        meetingRepo: MeetingRepository,
//        savingsRepo: SavingsRepository
//    ): ChamaRepository = ChamaRepositoryImpl(memberRepo, cycleRepo, meetingRepo, savingsRepo)
//
    // Dispatcher
    @Provides
    fun provideIoDispatcher() = Dispatchers.IO
}
