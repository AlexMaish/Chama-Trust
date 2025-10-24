package com.example.chamabuddy.data.local

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.room.DatabaseConfiguration
import androidx.room.RoomDatabase
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
        GroupMember::class,
        Penalty::class,
        ExpenseEntity::class,
        BenefitEntity::class,

        Welfare::class,
        WelfareMeeting::class,
        WelfareBeneficiary::class,
        MemberWelfareContribution::class
    ],
    version = 341,
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
    abstract fun penaltyDao(): PenaltyDao
    abstract fun expenseDao(): ExpenseDao
    abstract fun benefitDao(): BenefitDao

    abstract fun welfareDao(): WelfareDao
    abstract fun welfareMeetingDao(): WelfareMeetingDao
    abstract fun welfareContributionDao(): WelfareContributionDao
    abstract fun welfareBeneficiaryDao(): WelfareBeneficiaryDao

    suspend fun <T> runInTransaction(block: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            runInTransaction(block)
        }
    }

    /**
     * Ensure SQLite foreign key constraints are enabled.
     * This forces PRAGMA foreign_keys = ON every time the DB is created or opened.
     */
    override fun createOpenHelper(config: DatabaseConfiguration): SupportSQLiteOpenHelper {
        val context = config.context
        val builder = Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "app-database"
        ).addCallback(object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                db.execSQL("PRAGMA foreign_keys = ON;")
            }

            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.execSQL("PRAGMA foreign_keys = ON;")
            }
        })

        val tempDb = builder.build()
        return tempDb.openHelper
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? = value?.let { Date(it) }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? = date?.time





    @TypeConverter
    fun fromStringList(value: List<String>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() }


    }


}
