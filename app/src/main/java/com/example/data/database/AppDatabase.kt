package com.example.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.dao.*
import com.example.data.model.*

@Database(
    entities = [
        Course::class,
        AttendanceRecord::class,
        PeriodDefinition::class,
        TimetableSlot::class,
        TemporaryScheduleOverride::class,
        SettingEntity::class,
        NotificationLog::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun courseDao(): CourseDao
    abstract fun attendanceDao(): AttendanceDao
    abstract fun periodDao(): PeriodDao
    abstract fun timetableDao(): TimetableDao
    abstract fun overrideDao(): OverrideDao
    abstract fun settingDao(): SettingDao
    abstract fun notificationLogDao(): NotificationLogDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "attendly_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
