package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.data.model.PeriodDefinition
import com.example.ui.components.SubjectIconHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.worker.MergedSlot
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AttendanceScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedDate by viewModel.selectedDate.collectAsState()
    val todaySlotsList by viewModel.todaySlots.collectAsState()
    val allRecords by viewModel.allRecords.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val periods by viewModel.periods.collectAsState()

    var showAddExtraDialog by remember { mutableStateOf(false) }
    var isReorderMode by remember { mutableStateOf(false) }

    // Readable date title
    val readableDateTitle = remember(selectedDate) {
        try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
            val date = inputFormat.parse(selectedDate) ?: Date()
            outputFormat.format(date)
        } catch (e: Exception) {
            selectedDate
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // --- TOP CALENDAR SELECT ROW ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.shiftDate(-1) }) {
                    Icon(Icons.Filled.ArrowBackIosNew, contentDescription = "Prev Day", tint = Color.White)
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = readableDateTitle,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Mark Today's Timetable Classes",
                        fontSize = 11.sp,
                        color = TextSecondaryColor
                    )
                }

                IconButton(onClick = { viewModel.shiftDate(1) }) {
                    Icon(Icons.Filled.ArrowForwardIos, contentDescription = "Next Day", tint = Color.White)
                }
            }

            // --- MAIN LIST ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TIMELINE TRACKING",
                            color = TextSecondaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(
                                onClick = { isReorderMode = !isReorderMode },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = if (isReorderMode) Icons.Filled.Check else Icons.Filled.SwapVert,
                                    contentDescription = "Reorder",
                                    tint = if (isReorderMode) SuccessColor else PrimaryColor,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (todaySlotsList.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 40.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Filled.DateRange, contentDescription = "No Class", tint = TextSecondaryColor, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(12.dp))
                                Text("No classes scheduled for today.", color = TextSecondaryColor, fontSize = 14.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                TextButton(onClick = { showAddExtraDialog = true }) {
                                    Text("+ Add Extra Session", color = PrimaryColor, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }

                itemsIndexed(todaySlotsList, key = { _, item -> "${item.periodId}_${item.courseId}_${item.isExtra}" }) { index, slot ->
                    // Find actual record
                    val record = allRecords.find { it.date == selectedDate && it.periodIndex == slot.periodId }

                    AttendanceSlotRow(
                        viewModel = viewModel,
                        slot = slot,
                        record = record,
                        isReorderMode = isReorderMode,
                        isFirst = index == 0,
                        isLast = index == todaySlotsList.lastIndex,
                        selectedDate = selectedDate,
                        onMark = { status, periodsCount, unitsCount ->
                            viewModel.markAttendance(slot.courseId ?: -1, selectedDate, status, slot.periodId, periodsCount, unitsCount)
                        },
                        onClear = {
                            record?.let { viewModel.deleteAttendanceRecord(it) }
                        },
                        onMoveUp = {
                            if (slot.slotId != null) {
                                viewModel.reorderTimetableSlot(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), index, index - 1)
                            }
                        },
                        onMoveDown = {
                            if (slot.slotId != null) {
                                viewModel.reorderTimetableSlot(Calendar.getInstance().get(Calendar.DAY_OF_WEEK), index, index + 1)
                            }
                        }
                    )
                }
            }
        }

        // --- ADD EXTRA CLASS FAB ---
        FloatingActionButton(
            onClick = { showAddExtraDialog = true },
            containerColor = PrimaryColor,
            contentColor = Color.Black,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 90.dp, end = 24.dp)
                .testTag("attendance_extra_class_fab")
        ) {
            Icon(Icons.Filled.Add, contentDescription = "Add Extra Session")
        }

        // --- DIALOG: ADD EXTRA SESSION ---
        if (showAddExtraDialog) {
            AddExtraClassDialog(
                courses = courses,
                periods = periods,
                onDismiss = { showAddExtraDialog = false },
                onSave = { courseId, periodId, isOverride ->
                    if (isOverride) {
                        viewModel.addTemporaryOverride(selectedDate, periodId, "EXTRA", courseId)
                    } else {
                        // Directly record as extra attendance
                        viewModel.addExtraClassRecord(courseId, selectedDate, "PRESENT")
                    }
                    showAddExtraDialog = false
                    Toast.makeText(context, "Added extra session for today!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// --- SUB-ROW ROW VISUALIZER FOR TIMELINE CELL ---
@Composable
fun AttendanceSlotRow(
    viewModel: AttendanceViewModel,
    slot: MergedSlot,
    record: com.example.data.model.AttendanceRecord?,
    isReorderMode: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    selectedDate: String,
    onMark: (status: String, periods: Int, units: Int) -> Unit,
    onClear: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    var customPeriods by remember(slot.periodId, selectedDate, slot.periods) { mutableStateOf(record?.periods ?: slot.periods) }
    var customUnits by remember(slot.periodId, selectedDate, slot.units) { mutableStateOf(record?.units ?: slot.units) }

    Card(
        colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Reordering handles
            if (isReorderMode && slot.slotId != null) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.ArrowDropUp, contentDescription = "Move Up", tint = if (!isFirst) PrimaryColor else Color.Gray)
                    }
                    IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(24.dp)) {
                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Move Down", tint = if (!isLast) PrimaryColor else Color.Gray)
                    }
                }
            }

            // Subject Symbol
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(SurfaceColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = SubjectIconHelper.getIcon(slot.courseIcon),
                    contentDescription = null,
                    tint = Color(android.graphics.Color.parseColor(slot.courseColor)),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Main Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slot.courseName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    if (slot.isExtra) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(PrimaryColor.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("EXTRA", color = PrimaryColor, fontSize = 8.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                
                Text(
                    text = "${slot.startTime}–${slot.endTime} • $customPeriods Period" + (if (customPeriods > 1) "s" else "") + " • $customUnits Unit" + (if (customUnits > 1) "s" else ""),
                    fontSize = 11.sp,
                    color = TextSecondaryColor
                )
                
                // If not free slot, show inline adjusters
                if (slot.courseId != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Periods setting
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("P:", color = TextSecondaryColor, fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceColor)
                                    .clickable {
                                        if (customPeriods > 1) {
                                            customPeriods--
                                            if (record != null) {
                                                viewModel.updateRecordPeriodsUnits(record, customPeriods, customUnits)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(customPeriods.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceColor)
                                    .clickable {
                                        customPeriods++
                                        if (record != null) {
                                            viewModel.updateRecordPeriodsUnits(record, customPeriods, customUnits)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // Units setting
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text("U:", color = TextSecondaryColor, fontSize = 10.sp)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceColor)
                                    .clickable {
                                        if (customUnits > 1) {
                                            customUnits--
                                            if (record != null) {
                                                viewModel.updateRecordPeriodsUnits(record, customPeriods, customUnits)
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("-", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(customUnits.toString(), color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(SurfaceColor)
                                    .clickable {
                                        customUnits++
                                        if (record != null) {
                                            viewModel.updateRecordPeriodsUnits(record, customPeriods, customUnits)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text("+", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Interactive state buttons or badge
            if (slot.courseId == null) {
                Text("Free", color = TextSecondaryColor, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            } else if (record == null) {
                // UNMARKED ACTIONS (Visible, Zero clicks!)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = { onMark("PRESENT", customPeriods, customUnits) },
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessColor.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Present", color = SuccessColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = { onMark("ABSENT", customPeriods, customUnits) },
                        colors = ButtonDefaults.buttonColors(containerColor = ErrorColor.copy(alpha = 0.15f)),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("Absent", color = ErrorColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = { onMark("CANCELLED", customPeriods, customUnits) },
                        modifier = Modifier
                            .size(32.dp)
                            .background(SurfaceColor, RoundedCornerShape(8.dp))
                    ) {
                        Icon(Icons.Filled.Block, contentDescription = "Cancel Session", tint = CancelledColor, modifier = Modifier.size(14.dp))
                    }
                }
            } else {
                // ALREADY MARKED BADGE WITH RESET
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val color = when (record.status) {
                        "PRESENT" -> SuccessColor
                        "ABSENT" -> ErrorColor
                        else -> CancelledColor
                    }
                    Box(
                        modifier = Modifier
                            .background(color.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                            .border(1.dp, color, RoundedCornerShape(12.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = record.status,
                            color = color,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    IconButton(
                        onClick = onClear,
                        modifier = Modifier
                            .size(32.dp)
                            .background(SurfaceColor, CircleShape)
                    ) {
                        Icon(Icons.Filled.Delete, contentDescription = "Erase Marking", tint = TextSecondaryColor, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// --- DIALOG COMPONENT: ADD IMPROMTU SESSION ---
@Composable
fun AddExtraClassDialog(
    courses: List<Course>,
    periods: List<PeriodDefinition>,
    onDismiss: () -> Unit,
    onSave: (courseId: Int, periodId: Int, isOverride: Boolean) -> Unit
) {
    if (courses.isEmpty() || periods.isEmpty()) {
        AlertDialog(
            onDismissRequest = onDismiss,
            containerColor = SurfaceColor,
            title = { Text("No Subjects Available", color = Color.White) },
            text = { Text("Please add subjects and definitions in baseline configs before tracking extra.", color = TextSecondaryColor) },
            confirmButton = { Button(onClick = onDismiss) { Text("OK") } }
        )
        return
    }

    var selectedCourseId by remember { mutableStateOf(courses.first().id) }
    var selectedPeriodId by remember { mutableStateOf(periods.first().periodNumber) }
    var scheduleOnTimeline by remember { mutableStateOf(true) }

    var expandedCourse by remember { mutableStateOf(false) }
    var expandedPeriod by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Add Extra Session", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(
                    "Track single dynamic replacements, compensation, or extra periods on today's calendar.",
                    color = TextSecondaryColor,
                    fontSize = 12.sp
                )

                // Select Subject dropdown
                Column {
                    Text("Select Subject", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurfaceColor, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .clickable { expandedCourse = true }
                            .padding(12.dp)
                    ) {
                        Text(courses.find { it.id == selectedCourseId }?.name ?: "No Subject", color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedCourse,
                        onDismissRequest = { expandedCourse = false },
                        modifier = Modifier.background(SurfaceColor)
                    ) {
                        courses.forEach { course ->
                            DropdownMenuItem(
                                text = { Text(course.name, color = Color.White) },
                                onClick = {
                                    selectedCourseId = course.id
                                    expandedCourse = false
                                }
                            )
                        }
                    }
                }

                // Select Time Slot dropdown
                Column {
                    Text("Select Timetable Slot", color = Color.White, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CardSurfaceColor, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .clickable { expandedPeriod = true }
                            .padding(12.dp)
                    ) {
                        val p = periods.find { it.periodNumber == selectedPeriodId }
                        Text(if (p != null) "Period ${p.periodNumber} (${p.startTime}–${p.endTime})" else "No Slot", color = Color.White)
                    }
                    DropdownMenu(
                        expanded = expandedPeriod,
                        onDismissRequest = { expandedPeriod = false },
                        modifier = Modifier.background(SurfaceColor)
                    ) {
                        periods.forEach { p ->
                            DropdownMenuItem(
                                text = { Text("Period ${p.periodNumber} (${p.startTime}–${p.endTime})", color = Color.White) },
                                onClick = {
                                    selectedPeriodId = p.periodNumber
                                    expandedPeriod = false
                                }
                            )
                        }
                    }
                }

                // Schedule style toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Add to Today's Timeline", color = Color.White, fontSize = 13.sp)
                        Text("Displays the class slot on today's grid, allowing quick-marking swipe later.", color = TextSecondaryColor, fontSize = 10.sp)
                    }
                    Switch(
                        checked = scheduleOnTimeline,
                        onCheckedChange = { scheduleOnTimeline = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.Black,
                            checkedTrackColor = PrimaryColor
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(selectedCourseId, selectedPeriodId, scheduleOnTimeline) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Schedule Session", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = TextSecondaryColor)
            }
        }
    )
}
