package com.example.data.repository

import android.content.Context
import com.example.data.database.AppDatabase
import com.example.data.model.*
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@JsonClass(generateAdapter = true)
data class DatabaseBackup(
    val courses: List<Course>,
    val attendance: List<AttendanceRecord>,
    val timetable: List<TimetableSlot>,
    val settings: List<SettingEntity>,
    val periods: List<PeriodDefinition>
)

class AppRepository(val db: AppDatabase) {

    val database = db

    private val courseDao = db.courseDao()
    private val attendanceDao = db.attendanceDao()
    private val periodDao = db.periodDao()
    private val timetableDao = db.timetableDao()
    private val overrideDao = db.overrideDao()
    private val settingDao = db.settingDao()
    private val notificationLogDao = db.notificationLogDao()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    // --- initialization ---
    suspend fun initializeDefaultsIfEmpty() = withContext(Dispatchers.IO) {
        // 1. Setup default periods if none exist
        val periods = periodDao.getAllPeriods()
        if (periods.isEmpty()) {
            val defaultPeriods = listOf(
                PeriodDefinition(periodNumber = 1, startTime = "08:10", endTime = "09:00"),
                PeriodDefinition(periodNumber = 2, startTime = "09:00", endTime = "09:50"),
                PeriodDefinition(periodNumber = 3, startTime = "10:10", endTime = "11:00"),
                PeriodDefinition(periodNumber = 4, startTime = "11:00", endTime = "11:50"),
                PeriodDefinition(periodNumber = 5, startTime = "12:50", endTime = "13:40"),
                PeriodDefinition(periodNumber = 6, startTime = "13:40", endTime = "14:30"),
                PeriodDefinition(periodNumber = 7, startTime = "14:30", endTime = "15:20")
            )
            periodDao.insertPeriods(defaultPeriods)
        }

        // 2. Setup default settings if none exist
        if (settingDao.getSetting("user_name") == null) {
            settingDao.insertSetting(SettingEntity("user_name", "Student"))
        }
        if (settingDao.getSetting("semester") == null) {
            settingDao.insertSetting(SettingEntity("semester", "1"))
        }
        if (settingDao.getSetting("min_attendance") == null) {
            settingDao.insertSetting(SettingEntity("min_attendance", "75"))
        }
        if (settingDao.getSetting("saturday_enabled") == null) {
            settingDao.insertSetting(SettingEntity("saturday_enabled", "false"))
        }
        if (settingDao.getSetting("notifications_enabled") == null) {
            settingDao.insertSetting(SettingEntity("notifications_enabled", "true"))
        }
    }

    suspend fun resetAllData() = withContext(Dispatchers.IO) {
        db.clearAllTables()
        initializeDefaultsIfEmpty()
    }

    // --- Courses ---
    fun getAllCoursesFlow(): Flow<List<Course>> = courseDao.getAllCoursesFlow().flowOn(Dispatchers.IO)
    suspend fun getAllCourses(): List<Course> = withContext(Dispatchers.IO) { courseDao.getAllCourses() }
    suspend fun getCourseById(id: Int): Course? = withContext(Dispatchers.IO) { courseDao.getCourseById(id) }
    suspend fun insertCourse(course: Course): Long = withContext(Dispatchers.IO) { courseDao.insertCourse(course) }
    suspend fun updateCourse(course: Course) = withContext(Dispatchers.IO) { courseDao.updateCourse(course) }
    suspend fun deleteCourse(course: Course) = withContext(Dispatchers.IO) { courseDao.deleteCourse(course) }
    suspend fun deleteCourseById(id: Int) = withContext(Dispatchers.IO) { courseDao.deleteCourseById(id) }

    // --- Attendance Records ---
    fun getAllRecordsFlow(): Flow<List<AttendanceRecord>> = attendanceDao.getAllRecordsFlow().flowOn(Dispatchers.IO)
    fun getRecordsForDateFlow(date: String): Flow<List<AttendanceRecord>> = attendanceDao.getRecordsForDateFlow(date).flowOn(Dispatchers.IO)
    suspend fun getAllRecords(): List<AttendanceRecord> = withContext(Dispatchers.IO) { attendanceDao.getAllRecords() }
    suspend fun getRecordsForDate(date: String): List<AttendanceRecord> = withContext(Dispatchers.IO) { attendanceDao.getRecordsForDate(date) }
    suspend fun insertRecord(record: AttendanceRecord): Long = withContext(Dispatchers.IO) { attendanceDao.insertRecord(record) }
    suspend fun updateRecord(record: AttendanceRecord) = withContext(Dispatchers.IO) { attendanceDao.updateRecord(record) }
    suspend fun deleteRecord(record: AttendanceRecord) = withContext(Dispatchers.IO) { attendanceDao.deleteRecord(record) }
    suspend fun deleteRecordById(id: Int) = withContext(Dispatchers.IO) { attendanceDao.deleteRecordById(id) }
    suspend fun deleteRecordBySlotAndDate(slotId: Int, date: String) = withContext(Dispatchers.IO) { attendanceDao.deleteRecordBySlotAndDate(slotId, date) }
    suspend fun getRecordForSlotAndDate(slotId: Int, date: String): AttendanceRecord? = withContext(Dispatchers.IO) { attendanceDao.getRecordForSlotAndDate(slotId, date) }

    // --- Period Definitions ---
    fun getAllPeriodsFlow(): Flow<List<PeriodDefinition>> = periodDao.getAllPeriodsFlow().flowOn(Dispatchers.IO)
    suspend fun getAllPeriods(): List<PeriodDefinition> = withContext(Dispatchers.IO) { periodDao.getAllPeriods() }
    suspend fun insertPeriod(period: PeriodDefinition): Long = withContext(Dispatchers.IO) { periodDao.insertPeriod(period) }
    suspend fun updatePeriod(period: PeriodDefinition) = withContext(Dispatchers.IO) { periodDao.updatePeriod(period) }
    suspend fun deletePeriod(period: PeriodDefinition) = withContext(Dispatchers.IO) { periodDao.deletePeriod(period) }
    suspend fun clearPeriods() = withContext(Dispatchers.IO) { periodDao.clearPeriods() }

    // --- Timetable Slots ---
    fun getAllSlotsFlow(): Flow<List<TimetableSlot>> = timetableDao.getAllSlotsFlow().flowOn(Dispatchers.IO)
    fun getSlotsForDayFlow(dayOfWeek: Int): Flow<List<TimetableSlot>> = timetableDao.getSlotsForDayFlow(dayOfWeek).flowOn(Dispatchers.IO)
    suspend fun getAllSlots(): List<TimetableSlot> = withContext(Dispatchers.IO) { timetableDao.getAllSlots() }
    suspend fun getSlotsForDay(dayOfWeek: Int): List<TimetableSlot> = withContext(Dispatchers.IO) { timetableDao.getSlotsForDay(dayOfWeek) }
    suspend fun insertSlot(slot: TimetableSlot): Long = withContext(Dispatchers.IO) { timetableDao.insertSlot(slot) }
    suspend fun insertSlots(slots: List<TimetableSlot>) = withContext(Dispatchers.IO) { timetableDao.insertSlots(slots) }
    suspend fun updateSlot(slot: TimetableSlot) = withContext(Dispatchers.IO) { timetableDao.updateSlot(slot) }
    suspend fun deleteSlot(slot: TimetableSlot) = withContext(Dispatchers.IO) { timetableDao.deleteSlot(slot) }
    suspend fun deleteSlotById(id: Int) = withContext(Dispatchers.IO) { timetableDao.deleteSlotById(id) }
    suspend fun clearTimetable() = withContext(Dispatchers.IO) { timetableDao.clearTimetable() }

    // --- Temporary Schedule Overrides ---
    fun getAllOverridesFlow(): Flow<List<TemporaryScheduleOverride>> = overrideDao.getAllOverridesFlow().flowOn(Dispatchers.IO)
    fun getOverridesForDateFlow(date: String): Flow<List<TemporaryScheduleOverride>> = overrideDao.getOverridesForDateFlow(date).flowOn(Dispatchers.IO)
    suspend fun getAllOverrides(): List<TemporaryScheduleOverride> = withContext(Dispatchers.IO) { overrideDao.getAllOverrides() }
    suspend fun getOverridesForDate(date: String): List<TemporaryScheduleOverride> = withContext(Dispatchers.IO) { overrideDao.getOverridesForDate(date) }
    suspend fun insertOverride(override: TemporaryScheduleOverride): Long = withContext(Dispatchers.IO) { overrideDao.insertOverride(override) }
    suspend fun updateOverride(override: TemporaryScheduleOverride) = withContext(Dispatchers.IO) { overrideDao.updateOverride(override) }
    suspend fun deleteOverride(override: TemporaryScheduleOverride) = withContext(Dispatchers.IO) { overrideDao.deleteOverride(override) }
    suspend fun deleteOverrideById(id: Int) = withContext(Dispatchers.IO) { overrideDao.deleteOverrideById(id) }
    suspend fun clearOverridesForDate(date: String) = withContext(Dispatchers.IO) { overrideDao.clearOverridesForDate(date) }

    // --- Settings ---
    fun getAllSettingsFlow(): Flow<List<SettingEntity>> = settingDao.getAllSettingsFlow().flowOn(Dispatchers.IO)
    suspend fun getSetting(key: String, defaultValue: String): String = withContext(Dispatchers.IO) {
        settingDao.getSetting(key)?.value ?: defaultValue
    }
    suspend fun getSettingDirect(key: String): SettingEntity? = withContext(Dispatchers.IO) {
        settingDao.getSetting(key)
    }
    suspend fun saveSetting(key: String, value: String) = withContext(Dispatchers.IO) {
        settingDao.insertSetting(SettingEntity(key, value))
    }

    // --- Notification Logs ---
    fun getAllLogsFlow(): Flow<List<NotificationLog>> = notificationLogDao.getAllLogsFlow().flowOn(Dispatchers.IO)
    suspend fun getLogById(id: String): NotificationLog? = withContext(Dispatchers.IO) { notificationLogDao.getLogById(id) }
    suspend fun insertLog(log: NotificationLog) = withContext(Dispatchers.IO) { notificationLogDao.insertLog(log) }

    // --- JSON Backup / Export / Import ---
    suspend fun exportDatabaseToJson(): String = withContext(Dispatchers.IO) {
        val courses = courseDao.getAllCourses()
        val attendance = attendanceDao.getAllRecords()
        val timetable = timetableDao.getAllSlots()
        val settings = settingDao.getAllSettings()
        val periods = periodDao.getAllPeriods()

        val backup = DatabaseBackup(
            courses = courses,
            attendance = attendance,
            timetable = timetable,
            settings = settings,
            periods = periods
        )

        val adapter = moshi.adapter(DatabaseBackup::class.java)
        adapter.toJson(backup)
    }

    suspend fun importDatabaseFromJson(json: String): Boolean = withContext(Dispatchers.IO) {
        try {
            val adapter = moshi.adapter(DatabaseBackup::class.java)
            val backup = adapter.fromJson(json) ?: return@withContext false

            // Clear current database values
            db.clearAllTables() // Clears all Room tables safely in a single operation

            // Insert backup values
            backup.courses.forEach { courseDao.insertCourse(it) }
            backup.attendance.forEach { attendanceDao.insertRecord(it) }
            backup.timetable.forEach { timetableDao.insertSlot(it) }
            backup.settings.forEach { settingDao.insertSetting(it) }
            backup.periods.forEach { periodDao.insertPeriod(it) }

            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
}
