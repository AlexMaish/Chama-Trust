package com.example.chamabuddy.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.chamabuddy.domain.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Date

@Database(
    entities = [
        Member::class,
        Cycle::class,
        WeeklyMeeting::class,
        Beneficiary::class,
        MemberContribution::class,
        MonthlySaving::class,
        MonthlySavingEntry::class,
        Group::class,
        User::class,
        UserGroup::class,
        GroupMember::class
    ],
    version = 12,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun memberDao(): MemberDao
    abstract fun cycleDao(): CycleDao
    abstract fun meetingDao(): WeeklyMeetingDao
    abstract fun beneficiaryDao(): BeneficiaryDao
    abstract fun contributionDao(): MemberContributionDao
    abstract fun monthlySavingDao(): MonthlySavingDao
    abstract fun savingEntryDao(): MonthlySavingEntryDao
    abstract fun groupDao(): GroupDao
    abstract fun userDao(): UserDao
    abstract fun userGroupDao(): UserGroupDao
    abstract fun groupMemberDao(): GroupMemberDao

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            runInTransaction(block)
        }
    }

}

class Converters {
    @TypeConverter fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }
    @TypeConverter fun dateToTimestamp(date: Date?): Long? = date?.time
}

