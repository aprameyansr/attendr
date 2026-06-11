package com.example.data.dao

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CourseDao {
    @Query("SELECT * FROM courses ORDER BY orderIndex ASC, id ASC")
    fun getAllCoursesFlow(): Flow<List<Course>>

    @Query("SELECT * FROM courses ORDER BY orderIndex ASC, id ASC")
    suspend fun getAllCourses(): List<Course>

    @Query("SELECT * FROM courses WHERE id = :id")
    suspend fun getCourseById(id: Int): Course?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCourse(course: Course): Long

    @Update
    suspend fun updateCourse(course: Course)

    @Delete
    suspend fun deleteCourse(course: Course)

    @Query("DELETE FROM courses WHERE id = :id")
    suspend fun deleteCourseById(id: Int)
}

@Dao
interface AttendanceDao {
    @Query("SELECT * FROM attendance_records ORDER BY date DESC, timestamp DESC")
    fun getAllRecordsFlow(): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY timestamp ASC")
    fun getRecordsForDateFlow(date: String): Flow<List<AttendanceRecord>>

    @Query("SELECT * FROM attendance_records ORDER BY date DESC, timestamp DESC")
    suspend fun getAllRecords(): List<AttendanceRecord>

    @Query("SELECT * FROM attendance_records WHERE date = :date ORDER BY timestamp ASC")
    suspend fun getRecordsForDate(date: String): List<AttendanceRecord>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecord(record: AttendanceRecord): Long

    @Update
    suspend fun updateRecord(record: AttendanceRecord)

    @Delete
    suspend fun deleteRecord(record: AttendanceRecord)

    @Query("DELETE FROM attendance_records WHERE id = :id")
    suspend fun deleteRecordById(id: Int)

    @Query("DELETE FROM attendance_records WHERE slotId = :slotId AND date = :date")
    suspend fun deleteRecordBySlotAndDate(slotId: Int, date: String)

    @Query("SELECT * FROM attendance_records WHERE slotId = :slotId AND date = :date LIMIT 1")
    suspend fun getRecordForSlotAndDate(slotId: Int, date: String): AttendanceRecord?
}

@Dao
interface PeriodDao {
    @Query("SELECT * FROM period_definitions ORDER BY periodNumber ASC")
    fun getAllPeriodsFlow(): Flow<List<PeriodDefinition>>

    @Query("SELECT * FROM period_definitions ORDER BY periodNumber ASC")
    suspend fun getAllPeriods(): List<PeriodDefinition>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriod(period: PeriodDefinition): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPeriods(periods: List<PeriodDefinition>)

    @Update
    suspend fun updatePeriod(period: PeriodDefinition)

    @Delete
    suspend fun deletePeriod(period: PeriodDefinition)

    @Query("DELETE FROM period_definitions")
    suspend fun clearPeriods()
}

@Dao
interface TimetableDao {
    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, orderIndex ASC")
    fun getAllSlotsFlow(): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY orderIndex ASC")
    fun getSlotsForDayFlow(dayOfWeek: Int): Flow<List<TimetableSlot>>

    @Query("SELECT * FROM timetable_slots ORDER BY dayOfWeek ASC, orderIndex ASC")
    suspend fun getAllSlots(): List<TimetableSlot>

    @Query("SELECT * FROM timetable_slots WHERE dayOfWeek = :dayOfWeek ORDER BY orderIndex ASC")
    suspend fun getSlotsForDay(dayOfWeek: Int): List<TimetableSlot>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlot(slot: TimetableSlot): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSlots(slots: List<TimetableSlot>)

    @Update
    suspend fun updateSlot(slot: TimetableSlot)

    @Delete
    suspend fun deleteSlot(slot: TimetableSlot)

    @Query("DELETE FROM timetable_slots WHERE id = :id")
    suspend fun deleteSlotById(id: Int)

    @Query("DELETE FROM timetable_slots")
    suspend fun clearTimetable()
}

@Dao
interface OverrideDao {
    @Query("SELECT * FROM temporary_schedule_overrides ORDER BY date DESC, orderIndex ASC")
    fun getAllOverridesFlow(): Flow<List<TemporaryScheduleOverride>>

    @Query("SELECT * FROM temporary_schedule_overrides WHERE date = :date ORDER BY orderIndex ASC")
    fun getOverridesForDateFlow(date: String): Flow<List<TemporaryScheduleOverride>>

    @Query("SELECT * FROM temporary_schedule_overrides ORDER BY date DESC, orderIndex ASC")
    suspend fun getAllOverrides(): List<TemporaryScheduleOverride>

    @Query("SELECT * FROM temporary_schedule_overrides WHERE date = :date ORDER BY orderIndex ASC")
    suspend fun getOverridesForDate(date: String): List<TemporaryScheduleOverride>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOverride(override: TemporaryScheduleOverride): Long

    @Update
    suspend fun updateOverride(override: TemporaryScheduleOverride)

    @Delete
    suspend fun deleteOverride(override: TemporaryScheduleOverride)

    @Query("DELETE FROM temporary_schedule_overrides WHERE id = :id")
    suspend fun deleteOverrideById(id: Int)

    @Query("DELETE FROM temporary_schedule_overrides WHERE date = :date")
    suspend fun clearOverridesForDate(date: String)
}

@Dao
interface SettingDao {
    @Query("SELECT * FROM settings")
    fun getAllSettingsFlow(): Flow<List<SettingEntity>>

    @Query("SELECT * FROM settings")
    suspend fun getAllSettings(): List<SettingEntity>

    @Query("SELECT * FROM settings WHERE `key` = :key LIMIT 1")
    suspend fun getSetting(key: String): SettingEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetting(setting: SettingEntity)

    @Query("DELETE FROM settings WHERE `key` = :key")
    suspend fun deleteSetting(key: String)
}

@Dao
interface NotificationLogDao {
    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    fun getAllLogsFlow(): Flow<List<NotificationLog>>

    @Query("SELECT * FROM notification_logs ORDER BY timestamp DESC")
    suspend fun getAllLogs(): List<NotificationLog>

    @Query("SELECT * FROM notification_logs WHERE id = :id LIMIT 1")
    suspend fun getLogById(id: String): NotificationLog?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: NotificationLog)
}
