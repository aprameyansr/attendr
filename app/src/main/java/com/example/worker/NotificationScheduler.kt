package com.example.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.example.data.database.AppDatabase
import com.example.data.model.Course
import com.example.data.model.PeriodDefinition
import com.example.data.model.TimetableSlot
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

data class MergedSlot(
    val periodId: Int, // PeriodDefinition periodNumber or ID
    val periodNumber: Int,
    val startTime: String, // HH:MM
    val endTime: String, // HH:MM
    val courseId: Int?,
    val courseName: String,
    val courseColor: String,
    val courseIcon: String,
    val isExtra: Boolean,
    val isCancelled: Boolean,
    val isChanged: Boolean = false,
    val slotId: Int? = null, // Original timetable slot id if any
    val periods: Int = 1,
    val units: Int = 1
)

object NotificationScheduler {

    private const val TAG = "NotificationScheduler"

    suspend fun getDailySlots(db: AppDatabase, dateStr: String, dayOfWeek: Int): List<MergedSlot> {
        val slots = db.timetableDao().getSlotsForDay(dayOfWeek)
        val overrides = db.overrideDao().getOverridesForDate(dateStr)
        val periods = db.periodDao().getAllPeriods()
        val courses = db.courseDao().getAllCourses().associateBy { it.id }

        val merged = mutableListOf<MergedSlot>()

        for (s in slots) {
            val p = periods.find { it.periodNumber == s.periodId } ?: continue
            val ov = overrides.find { it.periodId == s.periodId }

            if (ov != null) {
                when (ov.status) {
                    "CANCELLED" -> {
                        // Keep it but mark as cancelled so UI shows it,
                        // and we don't schedule notifications for it
                        val c = courses[s.courseId]
                        merged.add(MergedSlot(
                            periodId = s.periodId,
                            periodNumber = p.periodNumber,
                            startTime = p.startTime,
                            endTime = p.endTime,
                            courseId = s.courseId,
                            courseName = c?.name ?: "No Subject",
                            courseColor = c?.color ?: "#20202B",
                            courseIcon = c?.icon ?: "book",
                            isExtra = false,
                            isCancelled = true,
                            slotId = s.id,
                            periods = c?.periodsPerSession ?: 1,
                            units = c?.attendanceUnitsPerSession ?: 1
                        ))
                    }
                    "CHANGED" -> {
                        val c = courses[ov.courseId]
                        merged.add(MergedSlot(
                            periodId = s.periodId,
                            periodNumber = p.periodNumber,
                            startTime = p.startTime,
                            endTime = p.endTime,
                            courseId = ov.courseId,
                            courseName = c?.name ?: "No Subject",
                            courseColor = c?.color ?: "#20202B",
                            courseIcon = c?.icon ?: "book",
                            isExtra = false,
                            isCancelled = false,
                            isChanged = true,
                            slotId = s.id,
                            periods = c?.periodsPerSession ?: 1,
                            units = c?.attendanceUnitsPerSession ?: 1
                        ))
                    }
                    else -> {
                        val c = courses[s.courseId]
                        merged.add(MergedSlot(
                            periodId = s.periodId,
                            periodNumber = p.periodNumber,
                            startTime = p.startTime,
                            endTime = p.endTime,
                            courseId = s.courseId,
                            courseName = c?.name ?: "No Subject",
                            courseColor = c?.color ?: "#20202B",
                            courseIcon = c?.icon ?: "book",
                            isExtra = false,
                            isCancelled = false,
                            slotId = s.id,
                            periods = c?.periodsPerSession ?: 1,
                            units = c?.attendanceUnitsPerSession ?: 1
                        ))
                    }
                }
            } else {
                val c = courses[s.courseId]
                merged.add(MergedSlot(
                    periodId = s.periodId,
                    periodNumber = p.periodNumber,
                    startTime = p.startTime,
                    endTime = p.endTime,
                    courseId = s.courseId,
                    courseName = c?.name ?: "No Subject",
                    courseColor = c?.color ?: "#20202B",
                    courseIcon = c?.icon ?: "book",
                    isExtra = false,
                    isCancelled = false,
                    slotId = s.id,
                    periods = c?.periodsPerSession ?: 1,
                    units = c?.attendanceUnitsPerSession ?: 1
                ))
            }
        }

        // Add extras
        for (ov in overrides) {
            if (ov.status == "EXTRA" && merged.none { it.periodId == ov.periodId }) {
                val p = periods.find { it.periodNumber == ov.periodId } ?: continue
                val c = courses[ov.courseId]
                merged.add(MergedSlot(
                    periodId = ov.periodId,
                    periodNumber = p.periodNumber,
                    startTime = p.startTime,
                    endTime = p.endTime,
                    courseId = ov.courseId,
                    courseName = c?.name ?: "No Subject",
                    courseColor = c?.color ?: "#20202B",
                    courseIcon = c?.icon ?: "book",
                    isExtra = true,
                    isCancelled = false,
                    slotId = null,
                    periods = c?.periodsPerSession ?: 1,
                    units = c?.attendanceUnitsPerSession ?: 1
                ))
            }
        }

        return merged.sortedBy { it.periodNumber }
    }

    fun scheduleTodayNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        Log.d(TAG, "Request to schedule today's class period notifications...")

        // Offload database queries to a global thread
        val db = AppDatabase.getDatabase(context)
        
        // We use simple asynchronous launch via standard global coroutines or quick execution thread pool.
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val satEnabledStr = db.settingDao().getSetting("saturday_enabled")?.value ?: "false"
                val satEnabled = satEnabledStr.toBoolean()

                val calendar = Calendar.getInstance()
                val dayOfWeekNumber = calendar.get(Calendar.DAY_OF_WEEK)
                val dayIndex = when (dayOfWeekNumber) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    else -> -1
                }

                if (dayIndex == -1 || (dayIndex == 6 && !satEnabled)) {
                    Log.d(TAG, "Today is Sunday or disabled Saturday. Skipping scheduling.")
                    return@launch
                }

                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val todayStr = dateFormat.format(Date())

                // Fetch today's resolved slots
                val todaySlots = getDailySlots(db, todayStr, dayIndex)

                val currentMillis = System.currentTimeMillis()

                for (slot in todaySlots) {
                    if (slot.courseId == null || slot.isCancelled) continue

                    // Parse slot end time
                    val endParts = slot.endTime.split(":")
                    if (endParts.size != 2) continue
                    val endHour = endParts[0].toIntOrNull() ?: continue
                    val endMin = endParts[1].toIntOrNull() ?: continue

                    val targetCalendar = Calendar.getInstance().apply {
                        set(Calendar.HOUR_OF_DAY, endHour)
                        set(Calendar.MINUTE, endMin)
                        set(Calendar.SECOND, 0)
                        set(Calendar.MILLISECOND, 0)
                    }

                    val targetMillis = targetCalendar.timeInMillis
                    if (targetMillis > currentMillis) {
                        val initialDelayMs = targetMillis - currentMillis
                        
                        // Build Work
                        val data = Data.Builder()
                            .putInt("courseId", slot.courseId)
                            .putString("courseName", slot.courseName)
                            .putInt("periodId", slot.periodId)
                            .putInt("periods", slot.periods)
                            .putInt("units", slot.units)
                            .build()

                        val uniqueWorkName = "PERIOD_NOTIF_${todayStr}_${slot.periodId}"

                        val workRequest = OneTimeWorkRequestBuilder<PeriodNotificationWorker>()
                            .setInitialDelay(initialDelayMs, TimeUnit.MILLISECONDS)
                            .setInputData(data)
                            .addTag("PERIOD_NOTIF")
                            .build()

                        workManager.enqueueUniqueWork(
                            uniqueWorkName,
                            ExistingWorkPolicy.REPLACE,
                            workRequest
                        )
                        Log.d(TAG, "Scheduled period notification for ${slot.courseName} in ${initialDelayMs/1000/60} mins")
                    } else {
                        Log.d(TAG, "Period end time ${slot.endTime} is in the past, skipping setting.")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Fatal error while scheduling notifications", e)
            }
        }
    }

    fun cancelAllScheduledNotifications(context: Context) {
        val workManager = WorkManager.getInstance(context)
        workManager.cancelAllWorkByTag("PERIOD_NOTIF")
        Log.d(TAG, "Cancelled all scheduled notifications.")
    }
}
