package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.*
import com.example.data.repository.AppRepository
import com.example.worker.MergedSlot
import com.example.worker.NotificationScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

data class CourseStats(
    val course: Course,
    val attended: Int,
    val held: Int,
    val percentage: Float,
    val statusMessage: String,
    val requiredToReach: Int,
    val allowedSkips: Int,
    val isSafe: Boolean
)

data class UndoAction(
    val prevRecord: AttendanceRecord?,
    val curRecord: AttendanceRecord,
    val isNew: Boolean
)

class AttendanceViewModel(
    application: Application,
    private val repository: AppRepository
) : AndroidViewModel(application) {

    private val context: Context get() = getApplication()

    // --- State Flows ---
    val courses: StateFlow<List<Course>> = repository.getAllCoursesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allRecords: StateFlow<List<AttendanceRecord>> = repository.getAllRecordsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val periods: StateFlow<List<PeriodDefinition>> = repository.getAllPeriodsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val timetableSlots: StateFlow<List<TimetableSlot>> = repository.getAllSlotsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val overrides: StateFlow<List<TemporaryScheduleOverride>> = repository.getAllOverridesFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _settingsList = repository.getAllSettingsFlow()
    val settingsState: StateFlow<Map<String, String>> = _settingsList
        .map { list -> list.associate { it.key to it.value } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    // --- Derived UI State: Course Stats (Computed Complex Equations) ---
    val courseStats: StateFlow<List<CourseStats>> = combine(courses, allRecords, settingsState) { list, records, settings ->
        val minPctStr = settings["min_attendance"] ?: "75"
        val minPct = minPctStr.toFloatOrNull() ?: 75f
        
        list.map { course ->
            val matchingRecords = records.filter { it.courseId == course.id }
            
            if (course.attendanceMode == "FIXED_TOTAL") {
                val presentCount = matchingRecords.count { it.status == "PRESENT" }
                val earnedHours = course.initialClassesAttended + (presentCount * course.hoursEarnedPerAttendance)
                val targetHours = course.targetHours
                val percentage = if (targetHours > 0) (earnedHours.toFloat() / targetHours) * 100f else 100f
                
                val requiredHours = (minPct / 100f) * targetHours
                val hoursNeeded = requiredHours - earnedHours
                val requiredToReach = if (hoursNeeded > 0) {
                    Math.ceil(hoursNeeded / course.hoursEarnedPerAttendance.toDouble()).toInt()
                } else 0
                
                // Allow skip calculation: since "skip" means missed opportunity to earn,
                // the question becomes "how many more classes can we afford to not make Present?"
                // Under simple hours, it's the extra hours above target we have divided by earnings
                val extraHours = earnedHours - requiredHours
                val allowedSkips = if (extraHours > 0) {
                    Math.floor(extraHours / course.hoursEarnedPerAttendance.toDouble()).toInt()
                } else 0
                
                val isSafe = earnedHours >= requiredHours
                
                val statusMessage = if (isSafe) {
                    "Requirement met! $earnedHours / $targetHours hours earned."
                } else {
                    "Earn $hoursNeeded hours to reach ${minPct.toInt()}%"
                }

                CourseStats(
                    course = course,
                    attended = earnedHours,
                    held = targetHours,
                    percentage = percentage,
                    statusMessage = statusMessage,
                    requiredToReach = requiredToReach,
                    allowedSkips = allowedSkips,
                    isSafe = isSafe
                )
            } else {
                // NORMAL MODE
                val presentUnits = matchingRecords.filter { it.status == "PRESENT" }.sumOf { it.units }
                val absentUnits = matchingRecords.filter { it.status == "ABSENT" }.sumOf { it.units }

                val attended = course.initialClassesAttended + presentUnits
                val held = course.initialClassesHeld + presentUnits + absentUnits
                val percentage = if (held > 0) (attended.toFloat() / held) * 100f else 100f

                val thresholdDecimal = minPct / 100f
                val isSafe = percentage >= minPct

                val requiredToReach = if (!isSafe) {
                    Math.max(0, Math.ceil(((thresholdDecimal * held - attended) / (1 - thresholdDecimal)).toDouble()).toInt())
                } else 0

                val allowedSkips = if (isSafe) {
                    Math.max(0, Math.floor(((attended - (thresholdDecimal * held)) / thresholdDecimal).toDouble()).toInt())
                } else 0

                val statusMessage = if (isSafe) {
                    if (allowedSkips > 0) "You can skip $allowedSkips classes" else "Limit reached. Do not skip!"
                } else {
                    "Attend $requiredToReach classes to reach ${minPct.toInt()}%"
                }

                CourseStats(
                    course = course,
                    attended = attended,
                    held = held,
                    percentage = percentage,
                    statusMessage = statusMessage,
                    requiredToReach = requiredToReach,
                    allowedSkips = allowedSkips,
                    isSafe = isSafe
                )
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Live Today Timetable Slots Flow ---
    private val _selectedDate = MutableStateFlow(getTodayDateString())
    val selectedDate: StateFlow<String> = _selectedDate.asStateFlow()

    val todaySlots: StateFlow<List<MergedSlot>> = combine(selectedDate, timetableSlots, overrides, periods, courses) { date, _, _, _, _ ->
        val calendar = Calendar.getInstance()
        val parts = date.split("-")
        if (parts.size == 3) {
            val yr = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
            val m = (parts[1].toIntOrNull() ?: 1) - 1
            val d = parts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
            calendar.set(yr, m, d)
        }
        val dayIndex = when (calendar.get(Calendar.DAY_OF_WEEK)) {
            Calendar.MONDAY -> 1
            Calendar.TUESDAY -> 2
            Calendar.WEDNESDAY -> 3
            Calendar.THURSDAY -> 4
            Calendar.FRIDAY -> 5
            Calendar.SATURDAY -> 6
            else -> -1
        }
        if (dayIndex == -1) {
            emptyList()
        } else {
            NotificationScheduler.getDailySlots(repository.database, date, dayIndex)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // --- Undo Buffer ---
    private val _undoBuffer = MutableStateFlow<UndoAction?>(null)
    val undoBuffer: StateFlow<UndoAction?> = _undoBuffer.asStateFlow()

    // --- Live Calculator State ---
    private val _calculatorCourseId = MutableStateFlow<Int?>(null)
    val calculatorCourseId: StateFlow<Int?> = _calculatorCourseId.asStateFlow()

    private val _calculatorSkipCount = MutableStateFlow(0)
    val calculatorSkipCount: StateFlow<Int> = _calculatorSkipCount.asStateFlow()

    val calculatorResult: StateFlow<String> = combine(calculatorCourseId, calculatorSkipCount, courseStats, settingsState) { cId, skips, statsList, settings ->
        val minPct = (settings["min_attendance"] ?: "75").toFloatOrNull() ?: 75f
        val stat = statsList.find { it.course.id == cId }
        
        if (stat == null) {
            "Select a course above to compute."
        } else {
            if (stat.course.attendanceMode == "FIXED_TOTAL") {
                val newEarned = stat.attended // skips do not change hours earned, but is a missed chance.
                // In fixed total hours, each skip is literally "doing nothing", hours earned doesn't decrease, hours held is unchanged
                val targetHours = stat.course.targetHours
                val newPct = if (targetHours > 0) (newEarned.toFloat() / targetHours) * 100f else 100f
                val difference = newPct - stat.percentage
                
                val reqHours = (minPct / 100f) * targetHours
                val isSafe = newEarned >= reqHours
                if (isSafe) {
                    val currentSurplusHours = newEarned - reqHours
                    val potentialSkips = Math.floor(currentSurplusHours / stat.course.hoursEarnedPerAttendance.toDouble()).toInt()
                    "With fixed targets, your percentage remains ${newPct.toInt()}%.\nYou have a surplus of ${currentSurplusHours.toInt()} hours (can afford to miss $potentialSkips classes)."
                } else {
                    val hoursNeeded = reqHours - newEarned
                    val sessionsNeeded = Math.ceil(hoursNeeded / stat.course.hoursEarnedPerAttendance.toDouble()).toInt()
                    "Below threshold! You need ${hoursNeeded.toInt()} more hours.\nAttend $sessionsNeeded classes to recover."
                }
            } else {
                // NORMAL MODE
                val newAttended = stat.attended
                val newHeld = stat.held + (skips * stat.course.attendanceUnitsPerSession)
                val newPct = if (newHeld > 0) (newAttended.toFloat() / newHeld) * 100f else 100f
                val statusTextStr = String.format(Locale.getDefault(), "Simulated Percentage: %.1f%%\n", newPct)
                
                val isSafe = newPct >= minPct
                if (isSafe) {
                    val thresholdDecimal = minPct / 100f
                    val allowedSkips = Math.max(0, Math.floor(((newAttended - (thresholdDecimal * newHeld)) / thresholdDecimal).toDouble()).toInt())
                    statusTextStr + "You can still skip $allowedSkips classes while remaining above ${minPct.toInt()}%."
                } else {
                    val thresholdDecimal = minPct / 100f
                    val requiredToReach = Math.max(0, Math.ceil(((thresholdDecimal * newHeld - newAttended) / (1 - thresholdDecimal)).toDouble()).toInt())
                    statusTextStr + "Below requirement of ${minPct.toInt()}%! Attend $requiredToReach classes consecutively to recover."
                }
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Select a course to begin.")

    // --- Timetable Range Header Helper ---
    val timetableWeekRange: StateFlow<String> = selectedDate.map { date ->
        calculateWeekRange(date)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    init {
        // Automatically set first course as calculator default when course list loads
        viewModelScope.launch {
            courses.collect { list ->
                if (_calculatorCourseId.value == null && list.isNotEmpty()) {
                    _calculatorCourseId.value = list.first().id
                }
            }
        }
    }

    // --- Attendance Operations ---
    fun markAttendance(
        courseId: Int,
        date: String,
        status: String,
        periodIndex: Int = 1,
        customPeriods: Int? = null,
        customUnits: Int? = null
    ) {
        viewModelScope.launch {
            // Find course to get units/periods
            val course = repository.getCourseById(courseId) ?: return@launch
            val periods = customPeriods ?: course.periodsPerSession
            val units = customUnits ?: course.attendanceUnitsPerSession

            // Backup existing record
            val prev = repository.getRecordForSlotAndDate(periodIndex, date)
            val isNew = prev == null

            val cur = AttendanceRecord(
                id = prev?.id ?: 0,
                courseId = courseId,
                date = date,
                status = status,
                periodIndex = periodIndex,
                slotId = periodIndex,
                periods = periods,
                units = units,
                isExtra = false
            )

            // Save record
            val id = repository.insertRecord(cur)
            val savedRecord = cur.copy(id = if (cur.id == 0) id.toInt() else cur.id)

            // Push into rollback buffer
            _undoBuffer.value = UndoAction(prevRecord = prev, curRecord = savedRecord, isNew = isNew)

            // Clear buffer automatically after 5 seconds
            viewModelScope.launch {
                kotlinx.coroutines.delay(5000)
                if (_undoBuffer.value?.curRecord?.id == savedRecord.id) {
                    _undoBuffer.value = null
                }
            }
            
            // Also refresh notifications since database updated
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun updateRecordPeriodsUnits(record: AttendanceRecord, periods: Int, units: Int) {
        viewModelScope.launch {
            val updated = record.copy(periods = periods, units = units)
            repository.updateRecord(updated)
        }
    }

    fun undoLastAction() {
        val undo = _undoBuffer.value ?: return
        viewModelScope.launch {
            if (undo.isNew) {
                // Delete the record we just created
                repository.deleteRecord(undo.curRecord)
            } else {
                // Restore previous record
                undo.prevRecord?.let { repository.insertRecord(it) }
            }
            _undoBuffer.value = null
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun dismissUndo() {
        _undoBuffer.value = null
    }

    fun deleteAttendanceRecord(record: AttendanceRecord) {
        viewModelScope.launch {
            repository.deleteRecord(record)
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun addExtraClassRecord(courseId: Int, date: String, status: String) {
        viewModelScope.launch {
            val course = repository.getCourseById(courseId) ?: return@launch
            val record = AttendanceRecord(
                courseId = courseId,
                date = date,
                status = status,
                isExtra = true,
                periods = course.periodsPerSession,
                units = course.attendanceUnitsPerSession
            )
            repository.insertRecord(record)
        }
    }

    // --- Course Direct Operations ---
    fun createCourse(
        name: String,
        color: String,
        icon: String,
        mode: String,
        periods: Int,
        units: Int,
        initialHeld: Int,
        initialAttended: Int,
        targetHours: Int = 80,
        hoursPerAttendance: Int = 2
    ) {
        viewModelScope.launch {
            val size = courses.value.size
            val course = Course(
                name = name,
                color = color,
                icon = icon,
                attendanceMode = mode,
                periodsPerSession = periods,
                attendanceUnitsPerSession = units,
                initialClassesHeld = initialHeld,
                initialClassesAttended = initialAttended,
                targetHours = targetHours,
                hoursEarnedPerAttendance = hoursPerAttendance,
                orderIndex = size
            )
            repository.insertCourse(course)
        }
    }

    fun updateCourseStats(course: Course) {
        viewModelScope.launch {
            repository.updateCourse(course)
        }
    }

    fun deleteCourse(course: Course) {
        viewModelScope.launch {
            repository.deleteCourse(course)
            // Cleanup related attendance records
            val records = allRecords.value.filter { it.courseId == course.id }
            records.forEach { repository.deleteRecord(it) }
        }
    }

    // --- Timetable Operations ---
    fun updateTimetableSlot(dayOfWeek: Int, periodId: Int, courseId: Int?) {
        viewModelScope.launch {
            val slots = timetableSlots.value.filter { it.dayOfWeek == dayOfWeek }
            val existing = slots.find { it.periodId == periodId }
            if (existing != null) {
                if (courseId == null) {
                    repository.deleteSlot(existing)
                } else {
                    repository.updateSlot(existing.copy(courseId = courseId))
                }
            } else if (courseId != null) {
                val newSlot = TimetableSlot(
                    dayOfWeek = dayOfWeek,
                    periodId = periodId,
                    courseId = courseId,
                    orderIndex = slots.size
                )
                repository.insertSlot(newSlot)
            }
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun reorderTimetableSlot(dayOfWeek: Int, fromIndex: Int, toIndex: Int) {
        viewModelScope.launch {
            val list = repository.getSlotsForDay(dayOfWeek).toMutableList()
            if (fromIndex in list.indices && toIndex in list.indices) {
                val target = list.removeAt(fromIndex)
                list.add(toIndex, target)
                // update indexes
                list.forEachIndexed { idx, slot ->
                    repository.updateSlot(slot.copy(orderIndex = idx))
                }
            }
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun copyTimetableDay(fromDay: Int, toDay: Int) {
        viewModelScope.launch {
            val source = repository.getSlotsForDay(fromDay)
            val targets = repository.getSlotsForDay(toDay)
            
            // Delete targets
            targets.forEach { repository.deleteSlot(it) }
            
            // Insert duplicates
            source.forEach { slot ->
                repository.insertSlot(
                    TimetableSlot(
                        dayOfWeek = toDay,
                        periodId = slot.periodId,
                        courseId = slot.courseId,
                        orderIndex = slot.orderIndex
                    )
                )
            }
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    // --- Temporary Overrides for Date ---
    fun addTemporaryOverride(date: String, periodId: Int, status: String, courseId: Int?) {
        viewModelScope.launch {
            val calendar = Calendar.getInstance()
            val parts = date.split("-")
            var dow = 1
            if (parts.size == 3) {
                val yr = parts[0].toIntOrNull() ?: calendar.get(Calendar.YEAR)
                val m = (parts[1].toIntOrNull() ?: 1) - 1
                val d = parts[2].toIntOrNull() ?: calendar.get(Calendar.DAY_OF_MONTH)
                calendar.set(yr, m, d)
                dow = when (calendar.get(Calendar.DAY_OF_WEEK)) {
                    Calendar.MONDAY -> 1
                    Calendar.TUESDAY -> 2
                    Calendar.WEDNESDAY -> 3
                    Calendar.THURSDAY -> 4
                    Calendar.FRIDAY -> 5
                    Calendar.SATURDAY -> 6
                    else -> 1
                }
            }

            val ov = TemporaryScheduleOverride(
                date = date,
                periodId = periodId,
                dayOfWeek = dow,
                status = status,
                courseId = courseId
            )
            repository.insertOverride(ov)
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun deleteTemporaryOverride(override: TemporaryScheduleOverride) {
        viewModelScope.launch {
            repository.deleteOverride(override)
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun clearOverridesForDate(date: String) {
        viewModelScope.launch {
            repository.clearOverridesForDate(date)
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    // --- Period Operations ---
    fun addNewPeriod(startTime: String, endTime: String) {
        viewModelScope.launch {
            val pList = periods.value
            val nextNum = (pList.maxOfOrNull { it.periodNumber } ?: 0) + 1
            repository.insertPeriod(
                PeriodDefinition(
                    periodNumber = nextNum,
                    startTime = startTime,
                    endTime = endTime
                )
            )
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    fun updatePeriodTiming(id: Int, pNum: Int, startTime: String, endTime: String) {
        viewModelScope.launch {
            repository.updatePeriod(
                PeriodDefinition(
                    id = id,
                    periodNumber = pNum,
                    startTime = startTime,
                    endTime = endTime
                )
            )
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    // --- Settings Directly Saved ---
    fun saveSetting(key: String, value: String) {
        viewModelScope.launch {
            repository.saveSetting(key, value)
            if (key == "notifications_enabled" || key == "saturday_enabled") {
                if (value == "true") {
                    NotificationScheduler.scheduleTodayNotifications(context)
                } else {
                    NotificationScheduler.cancelAllScheduledNotifications(context)
                }
            }
        }
    }

    // --- Calculator Inputs ---
    fun setCalculatorParams(courseId: Int, skips: Int) {
        _calculatorCourseId.value = courseId
        _calculatorSkipCount.value = skips
    }

    // --- Navigation Date Helpers ---
    fun selectDate(date: String) {
        _selectedDate.value = date
    }

    fun shiftDate(days: Int) {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        try {
            val current = format.parse(_selectedDate.value) ?: Date()
            val calendar = Calendar.getInstance().apply {
                time = current
                add(Calendar.DAY_OF_YEAR, days)
            }
            // Skip Sunday
            if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
                calendar.add(Calendar.DAY_OF_YEAR, if (days > 0) 1 else -1)
            }
            _selectedDate.value = format.format(calendar.time)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // --- JSON Backup Trigger ---
    suspend fun exportData(): String {
        return repository.exportDatabaseToJson()
    }

    suspend fun importData(json: String): Boolean {
        val success = repository.importDatabaseFromJson(json)
        if (success) {
            NotificationScheduler.scheduleTodayNotifications(context)
        }
        return success
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            _selectedDate.value = getTodayDateString()
            NotificationScheduler.scheduleTodayNotifications(context)
        }
    }

    // --- Shared Utilities ---
    private fun getTodayDateString(): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val calendar = Calendar.getInstance()
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
            calendar.add(Calendar.DAY_OF_YEAR, 1) // default to Monday if today is Sunday
        }
        return format.format(calendar.time)
    }

    private fun calculateWeekRange(dateStr: String): String {
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        val outFormat = SimpleDateFormat("MMM d", Locale.getDefault())
        try {
            val date = format.parse(dateStr) ?: Date()
            val calendar = Calendar.getInstance().apply {
                time = date
            }
            // Align calendar to Monday of this week
            val currentDay = calendar.get(Calendar.DAY_OF_WEEK)
            val diff = Calendar.MONDAY - currentDay
            calendar.add(Calendar.DAY_OF_YEAR, if (diff > 0) diff - 7 else diff)
            
            val start = outFormat.format(calendar.time)
            calendar.add(Calendar.DAY_OF_YEAR, 5) // add 5 days for Mon-Sat range
            val end = outFormat.format(calendar.time)
            return "$start – $end"
        } catch (e: Exception) {
            return ""
        }
    }
}
