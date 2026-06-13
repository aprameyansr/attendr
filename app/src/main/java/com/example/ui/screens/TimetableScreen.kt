package com.example.ui.screens

import android.text.format.DateFormat
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.data.model.PeriodDefinition
import com.example.data.model.TimetableSlot
import com.example.ui.components.SubjectIconHelper
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val weekRange by viewModel.timetableWeekRange.collectAsState()
    val courses by viewModel.courses.collectAsState()
    val periods by viewModel.periods.collectAsState()
    val slots by viewModel.timetableSlots.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()

    val saturdayEnabled = (settingsState["saturday_enabled"] ?: "false").toBoolean()

    // Screen State variables
    var isEditMode by remember { mutableStateOf(false) }
    var selectedDayIndex by remember { mutableStateOf(1) } // 1 = Monday, 5 = Friday, 6 = Saturday
    
    // Dialog state variables
    var showCopyDialog by remember { mutableStateOf(false) }
    var showAddPeriodDialog by remember { mutableStateOf(false) }
    var editingPeriodId by remember { mutableStateOf<Int?>(null) }

    val daysList = remember(saturdayEnabled) {
        if (saturdayEnabled) {
            listOf("MON" to 1, "TUE" to 2, "WED" to 3, "THU" to 4, "FRI" to 5, "SAT" to 6)
        } else {
            listOf("MON" to 1, "TUE" to 2, "WED" to 3, "THU" to 4, "FRI" to 5)
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
            // --- TIMETABLE HEADER & WEEK RANGE CONTROLS ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.shiftDate(-7) }) {
                    Icon(Icons.Filled.ChevronLeft, contentDescription = "Prev Week", tint = Color.White)
                }

                Text(
                    text = weekRange,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                IconButton(onClick = { viewModel.shiftDate(7) }) {
                    Icon(Icons.Filled.ChevronRight, contentDescription = "Next Week", tint = Color.White)
                }
            }

            // --- HORIZONTAL DAY SELECTOR SELECTION ---
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysList.forEach { (name, index) ->
                    val isSelected = selectedDayIndex == index
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) PrimaryColor else CardSurfaceColor)
                            .clickable { selectedDayIndex = index }
                            .padding(vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            color = if (isSelected) Color.Black else TextSecondaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // --- MAIN TIMETABLE GRID ROWS ---
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isEditMode) "TIMETABLE EDITOR" else "TODAY'S SCHEDULE GRID",
                            color = TextSecondaryColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        if (isEditMode) {
                            TextButton(onClick = { showCopyDialog = true }) {
                                Icon(Icons.Filled.CopyAll, contentDescription = "Copy Schedule", tint = PrimaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy To...", color = PrimaryColor, fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Render all slots in selected day matching period definitions
                items(periods, key = { it.id }) { period ->
                    val matchingSlot = slots.find { it.dayOfWeek == selectedDayIndex && it.periodId == period.periodNumber }
                    val activeCourse = courses.find { it.id == matchingSlot?.courseId }

                    TimetableRowCard(
                        period = period,
                        activeCourse = activeCourse,
                        isEditMode = isEditMode,
                        courses = courses,
                        onUpdateSlot = { courseId ->
                            viewModel.updateTimetableSlot(selectedDayIndex, period.periodNumber, courseId)
                        },
                        onEditPeriod = {
                            editingPeriodId = period.id
                        }
                    )
                }

                if (isEditMode) {
                    item {
                        Button(
                            onClick = { showAddPeriodDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 12.dp)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.AddAlarm, contentDescription = "Add period", tint = PrimaryColor)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Custom Timetable Period", color = Color.White)
                        }
                    }
                }
            }
        }

        // --- FLOATING PENCIL EDIT FAB ---
        FloatingActionButton(
            onClick = { isEditMode = !isEditMode },
            containerColor = if (isEditMode) SuccessColor else PrimaryColor,
            contentColor = Color.Black,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(bottom = 24.dp, end = 24.dp)
                .testTag("timetable_edit_fab"),
            shape = CircleShape
        ) {
            Icon(
                imageVector = if (isEditMode) Icons.Filled.Check else Icons.Filled.Edit,
                contentDescription = "Edit Schedule"
            )
        }

        // --- DIALOG: COPY TIMETABLE (COMPENSATION DAY) ---
        if (showCopyDialog) {
            CopyTimetableDialog(
                currentDay = selectedDayIndex,
                daysList = daysList,
                onDismiss = { showCopyDialog = false },
                onCopy = { targetDay ->
                    viewModel.copyTimetableDay(selectedDayIndex, targetDay)
                    showCopyDialog = false
                    Toast.makeText(context, "Schedule duplicated successfully!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // --- DIALOG: ADD PERIOD CONFIG ---
        if (showAddPeriodDialog) {
            AddPeriodDialog(
                onDismiss = { showAddPeriodDialog = false },
                onSave = { start, end ->
                    viewModel.addNewPeriod(start, end)
                    showAddPeriodDialog = false
                    Toast.makeText(context, "New period added!", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // --- DIALOG: EDIT PERIOD TIMES ---
        editingPeriodId?.let { id ->
            val p = periods.find { it.id == id }
            if (p != null) {
                EditPeriodDialog(
                    period = p,
                    onDismiss = { editingPeriodId = null },
                    onSave = { start, end ->
                        viewModel.updatePeriodTiming(p.id, p.periodNumber, start, end)
                        editingPeriodId = null
                        Toast.makeText(context, "Timings saved!", Toast.LENGTH_SHORT).show()
                    }
                )
            }
        }
    }
}

// --- SUB-ROW GRAPHICS FOR TIMETABLE ROW CELLS ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TimetableRowCard(
    period: PeriodDefinition,
    activeCourse: Course?,
    isEditMode: Boolean,
    courses: List<Course>,
    onUpdateSlot: (Int?) -> Unit,
    onEditPeriod: () -> Unit
) {
    var showSubjectDropdown by remember { mutableStateOf(false) }

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
            // Period Number + Timings
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.width(80.dp)
            ) {
                Text(
                    text = "Period ${period.periodNumber}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable(enabled = isEditMode) { onEditPeriod() }
                ) {
                    Text(
                        text = "${period.startTime}–${period.endTime}",
                        color = if (isEditMode) PrimaryColor else TextSecondaryColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (isEditMode) {
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(Icons.Filled.AccessTime, contentDescription = null, tint = PrimaryColor, modifier = Modifier.size(10.dp))
                    }
                }
            }

            // Grid Content Visualizer
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (activeCourse != null) Color(android.graphics.Color.parseColor(activeCourse.color)).copy(alpha = 0.15f)
                        else SurfaceColor
                    )
                    .border(
                        width = 1.dp,
                        color = if (activeCourse != null) Color(android.graphics.Color.parseColor(activeCourse.color)) else BorderColor,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .clickable(enabled = isEditMode) { showSubjectDropdown = true }
                    .padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (activeCourse != null) {
                        Icon(
                            imageVector = SubjectIconHelper.getIcon(activeCourse.icon),
                            contentDescription = null,
                            tint = Color(android.graphics.Color.parseColor(activeCourse.color)),
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = activeCourse.name,
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                    } else {
                        Text(
                            text = "Free Period / Gaps",
                            color = TextSecondaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Dropdown Select inside cell container
                DropdownMenu(
                    expanded = showSubjectDropdown,
                    onDismissRequest = { showSubjectDropdown = false },
                    modifier = Modifier.background(SurfaceColor).border(1.dp, BorderColor)
                ) {
                    DropdownMenuItem(
                        text = { Text("Set Free Period (None)", color = ErrorColor) },
                        onClick = {
                            onUpdateSlot(null)
                            showSubjectDropdown = false
                        }
                    )
                    courses.forEach { course ->
                        DropdownMenuItem(
                            text = { Text(course.name, color = Color.White) },
                            onClick = {
                                onUpdateSlot(course.id)
                                showSubjectDropdown = false
                            }
                        )
                    }
                }
            }
        }
    }
}

// --- DIALOG COMPONENT: DUPLICATE TIMETABLE ---
@Composable
fun CopyTimetableDialog(
    currentDay: Int,
    daysList: List<Pair<String, Int>>,
    onDismiss: () -> Unit,
    onCopy: (Int) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Duplicate Timetable", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Copy timetable from selected day into a target day. This replaces the target's current schedule.",
                    color = TextSecondaryColor,
                    fontSize = 13.sp
                )
                
                daysList.filter { it.second != currentDay }.forEach { (name, id) ->
                    Button(
                        onClick = { onCopy(id) },
                        colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceColor),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Duplicate to $name", color = Color.White)
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Dismiss", color = TextSecondaryColor)
            }
        }
    )
}

// --- DIALOG COMPONENT: ADD TIME PERIODS ---
@Composable
fun AddPeriodDialog(
    onDismiss: () -> Unit,
    onSave: (start: String, end: String) -> Unit
) {
    var start by remember { mutableStateOf("09:00") }
    var end by remember { mutableStateOf("09:50") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Add New Class Period", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start Time (HH:MM)", color = TextSecondaryColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End Time (HH:MM)", color = TextSecondaryColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(start.trim(), end.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Add Period", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryColor)
            }
        }
    )
}

// --- DIALOG COMPONENT: EDIT TIME REGIONS ---
@Composable
fun EditPeriodDialog(
    period: PeriodDefinition,
    onDismiss: () -> Unit,
    onSave: (start: String, end: String) -> Unit
) {
    var start by remember { mutableStateOf(period.startTime) }
    var end by remember { mutableStateOf(period.endTime) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = { Text("Edit Period ${period.periodNumber} Timings", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Start Time (HH:MM)", color = TextSecondaryColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor
                    )
                )

                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("End Time (HH:MM)", color = TextSecondaryColor) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = PrimaryColor,
                        unfocusedBorderColor = BorderColor
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(start.trim(), end.trim()) },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Save Timings", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondaryColor)
            }
        }
    )
}
