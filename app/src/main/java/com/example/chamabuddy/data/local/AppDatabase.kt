package com.example.chamabuddy.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import com.example.chamabuddy.domain.model.Beneficiary
import com.example.chamabuddy.domain.model.Cycle
import com.example.chamabuddy.domain.model.Member
import com.example.chamabuddy.domain.model.MemberContribution
import com.example.chamabuddy.domain.model.MonthlySaving
import com.example.chamabuddy.domain.model.MonthlySavingEntry
import com.example.chamabuddy.domain.model.WeeklyMeeting
import java.util.Date

@Database(
    entities = [
        Member::class,
        Cycle::class,
        WeeklyMeeting::class,
        Beneficiary::class,
        MemberContribution::class,
        MonthlySaving::class,
        MonthlySavingEntry::class
    ],
    version = 4
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

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "mchama_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}


// ✅ Custom Room Type Converter for Date
class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}
