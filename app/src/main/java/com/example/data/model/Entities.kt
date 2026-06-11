package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.JsonClass

@Entity(tableName = "courses")
@JsonClass(generateAdapter = true)
data class Course(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val color: String, // Hex code like #A78BFA
    val icon: String, // String identifier for standard icons (e.g. "bolt")
    val attendanceMode: String, // "NORMAL" or "FIXED_TOTAL"
    val periodsPerSession: Int, // e.g. 1, 2, 3
    val attendanceUnitsPerSession: Int, // e.g. 1, 2
    val initialClassesHeld: Int = 0,
    val initialClassesAttended: Int = 0,
    val targetHours: Int = 80, // For Fixed Total Mode
    val hoursEarnedPerAttendance: Int = 2, // For Fixed Total Mode
    val orderIndex: Int = 0
)

@Entity(tableName = "attendance_records")
@JsonClass(generateAdapter = true)
data class AttendanceRecord(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val courseId: Int,
    val date: String, // YYYY-MM-DD
    val status: String, // "PRESENT", "ABSENT", "CANCELLED", "NOT_MARKED"
    val periodIndex: Int = 1, // Period number
    val slotId: Int? = null, // Linked timetable slot if any
    val isExtra: Boolean = false,
    val periods: Int = 1,
    val units: Int = 1,
    val timestamp: Long = System.currentTimeMillis()
)

@Entity(tableName = "period_definitions")
@JsonClass(generateAdapter = true)
data class PeriodDefinition(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val periodNumber: Int,
    val startTime: String, // HH:MM
    val endTime: String // HH:MM
)

@Entity(tableName = "timetable_slots")
@JsonClass(generateAdapter = true)
data class TimetableSlot(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val dayOfWeek: Int, // 1 = Monday, 5 = Friday, 6 = Saturday
    val periodId: Int, // Refers to PeriodDefinition id or periodNumber
    val courseId: Int?, // Refers to Course id, null if free
    val orderIndex: Int = 0, // For reordering classes in day view
    val isTempChanged: Boolean = false
)

@Entity(tableName = "temporary_schedule_overrides")
@JsonClass(generateAdapter = true)
data class TemporaryScheduleOverride(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val date: String, // YYYY-MM-DD
    val periodId: Int, // PeriodDefinition.id or periodNumber
    val dayOfWeek: Int,
    val courseId: Int?, // Null means cancelled, or different courseId
    val status: String, // "CANCELLED", "CHANGED", "EXTRA", "NORMAL"
    val originalCourseId: Int? = null,
    val orderIndex: Int = 0
)

@Entity(tableName = "settings")
@JsonClass(generateAdapter = true)
data class SettingEntity(
    @PrimaryKey val key: String,
    val value: String
)

@Entity(tableName = "notification_logs")
@JsonClass(generateAdapter = true)
data class NotificationLog(
    @PrimaryKey(autoGenerate = false) val id: String, // e.g. "date_periodId"
    val date: String,
    val periodId: Int,
    val courseId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String // "SENT", "RESPONDED_PRESENT", "RESPONDED_ABSENT", "RESPONDED_CANCELLED", "IGNORED"
)
