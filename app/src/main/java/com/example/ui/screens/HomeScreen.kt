package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Course
import com.example.ui.components.SubjectIconHelper
import com.example.ui.components.SwipeableCard
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.CourseStats
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val courseStatsList by viewModel.courseStats.collectAsState()
    val undoBuffer by viewModel.undoBuffer.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var expandedCourseId by remember { mutableStateOf<Int?>(null) }

    // Mean Overall Attendance computation
    val overallPercentage = remember(courseStatsList) {
        if (courseStatsList.isEmpty()) 0f
        else {
            courseStatsList.map { it.percentage }.average().toFloat()
        }
    }

    val studentName = settingsState["user_name"] ?: "Student"

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // --- HEADER ---
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Hello, $studentName",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Mark attendance instantly",
                            fontSize = 14.sp,
                            color = TextSecondaryColor
                        )
                    }
                    
                    IconButton(
                        onClick = { showCreateDialog = true },
                        modifier = Modifier
                            .background(CardSurfaceColor, CircleShape)
                            .testTag("add_course_button")
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Add Subject", tint = PrimaryColor)
                    }
                }
            }

            // --- OVERALL ATTENDANCE PROGRESS RING ---
            item {
                OverallAttendanceMeter(overallPercentage)
            }

            // --- SECTION HEADER ---
            item {
                Text(
                    text = "SUBJECTS & QUICK MARK",
                    color = TextSecondaryColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // --- EMPTY STATE ---
            if (courseStatsList.isEmpty()) {
                item {
                    EmptyStatePlaceholder(onAddClick = { showCreateDialog = true })
                }
            }

            // --- LIST OF SUBJECT CARDS ---
            items(courseStatsList, key = { it.course.id }) { stat ->
                val isExpanded = expandedCourseId == stat.course.id
                
                SwipeableSubjectCard(
                    stat = stat,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedCourseId = if (isExpanded) null else stat.course.id
                    },
                    onSwipePresent = {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val todayStr = dateFormat.format(Date())
                        viewModel.markAttendance(stat.course.id, todayStr, "PRESENT")
                    },
                    onSwipeAbsent = {
                        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                        val todayStr = dateFormat.format(Date())
                        viewModel.markAttendance(stat.course.id, todayStr, "ABSENT")
                    },
                    onUpdateCourse = { updatedCourse ->
                        viewModel.updateCourseStats(updatedCourse)
                    },
                    onDeleteCourse = {
                        viewModel.deleteCourse(stat.course)
                        if (isExpanded) expandedCourseId = null
                    }
                )
            }
        }

        // --- EXCELLENCE: UNDO SNACKBAR BAR PANEL ---
        AnimatedVisibility(
            visible = undoBuffer != null,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 80.dp, start = 16.dp, end = 16.dp)
        ) {
            undoBuffer?.let { undo ->
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
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Marked ${undo.curRecord.status.lowercase().capitalize()} for slot ${undo.curRecord.periodIndex}",
                            color = Color.White,
                            fontSize = 14.sp
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = { viewModel.undoLastAction() }
                            ) {
                                Text("UNDO", color = PrimaryColor, fontWeight = FontWeight.Bold)
                            }
                            IconButton(onClick = { viewModel.dismissUndo() }) {
                                Icon(Icons.Filled.Close, contentDescription = "Dismiss", tint = TextSecondaryColor)
                            }
                        }
                    }
                }
            }
        }

        // --- SUBJECT CREATION DIALOG SHEET ---
        if (showCreateDialog) {
            SubjectCreateDialog(
                onDismiss = { showCreateDialog = false },
                onSave = { name, color, icon, mode, sessions, units, initialHeld, initialAttended, target, hoursPer ->
                    viewModel.createCourse(
                        name = name,
                        color = color,
                        icon = icon,
                        mode = mode,
                        periods = sessions,
                        units = units,
                        initialHeld = initialHeld,
                        initialAttended = initialAttended,
                        targetHours = target,
                        hoursPerAttendance = hoursPer
                    )
                    showCreateDialog = false
                    Toast.makeText(context, "Subject added!", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

// --- OVERALL METRIC PROGRESS CYCLES ---
@Composable
fun OverallAttendanceMeter(percentage: Float) {
    val animatedPercentage = animateFloatAsState(
        targetValue = percentage,
        animationSpec = tween(durationMillis = 1000)
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(100.dp)
            ) {
                // Background Track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = BorderColor,
                        startAngle = -90f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                
                // Color Active Ring
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(PrimaryColor, PrimaryDark, PrimaryColor)
                        ),
                        startAngle = -90f,
                        sweepAngle = animatedPercentage.value * 3.6f,
                        useCenter = false,
                        style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Text(
                    text = String.format(Locale.getDefault(), "%.0f%%", animatedPercentage.value),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Overall Attendance",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(4.dp))
                val descText = if (percentage >= 75f) {
                    "Great! Keep it above 75% to stay protected."
                } else {
                    "Attention! You are below recommended threshold."
                }
                Text(
                    text = descText,
                    fontSize = 12.sp,
                    color = if (percentage >= 75f) SuccessColor else ErrorColor
                )
            }
        }
    }
}

// --- COMPOSE CORE LIST SWIPE EXPANDABLE CARD IMPLEMENTATION ---
@Composable
fun SwipeableSubjectCard(
    stat: CourseStats,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onSwipePresent: () -> Unit,
    onSwipeAbsent: () -> Unit,
    onUpdateCourse: (Course) -> Unit,
    onDeleteCourse: () -> Unit
) {
    SwipeableCard(
        onSwipeLeft = onSwipePresent,
        onSwipeRight = onSwipeAbsent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = CardSurfaceColor),
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .border(2.dp, BorderColor, RoundedCornerShape(24.dp))
                .clickable { onToggleExpand() }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Color strip bar
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(48.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color(android.graphics.Color.parseColor(stat.course.color)))
                    )

                    // Icon indicator
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(SurfaceColor, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = SubjectIconHelper.getIcon(stat.course.icon),
                            contentDescription = "Subject Icon",
                            tint = Color(android.graphics.Color.parseColor(stat.course.color))
                        )
                    }

                    // Name + attendance status message
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stat.course.name,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stat.statusMessage,
                            fontSize = 12.sp,
                            color = if (stat.isSafe) SuccessColor else ErrorColor
                        )
                    }

                    // Single Percent tag
                    Text(
                        text = String.format(Locale.getDefault(), "%.0f%%", stat.percentage),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (stat.isSafe) Color.White else ErrorColor
                    )
                }

                // Expandable details block inline
                AnimatedVisibility(
                    visible = isExpanded,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Classes Attended", color = TextSecondaryColor, fontSize = 13.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (stat.course.initialClassesAttended > 0) {
                                            onUpdateCourse(stat.course.copy(initialClassesAttended = stat.course.initialClassesAttended - 1))
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor)
                                ) {
                                    Text("-", color = Color.White)
                                }
                                Text(stat.course.initialClassesAttended.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                 Button(
                                    onClick = {
                                        if (stat.course.attendanceMode != "NORMAL" || stat.course.initialClassesAttended < stat.course.initialClassesHeld) {
                                            onUpdateCourse(stat.course.copy(initialClassesAttended = stat.course.initialClassesAttended + 1))
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor)
                                ) {
                                    Text("+", color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Classes Held So Far", color = TextSecondaryColor, fontSize = 13.sp)
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (stat.course.initialClassesHeld > 0) {
                                            val nextHeld = stat.course.initialClassesHeld - 1
                                            if (stat.course.attendanceMode != "NORMAL" || nextHeld >= stat.course.initialClassesAttended) {
                                                onUpdateCourse(stat.course.copy(initialClassesHeld = nextHeld))
                                            }
                                        }
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor)
                                ) {
                                    Text("-", color = Color.White)
                                }
                                Text(stat.course.initialClassesHeld.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                Button(
                                    onClick = {
                                        onUpdateCourse(stat.course.copy(initialClassesHeld = stat.course.initialClassesHeld + 1))
                                    },
                                    contentPadding = PaddingValues(0.dp),
                                    modifier = Modifier.size(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = SurfaceColor)
                                ) {
                                    Text("+", color = Color.White)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Mode: ${stat.course.attendanceMode.replace("_", " ")}", color = TextSecondaryColor, fontSize = 11.sp)
                                if (stat.course.attendanceMode == "NORMAL") {
                                    Text("${stat.course.periodsPerSession} periods, ${stat.course.attendanceUnitsPerSession} units", color = TextSecondaryColor, fontSize = 11.sp)
                                } else {
                                    Text("Target: ${stat.course.targetHours} hours (+${stat.course.hoursEarnedPerAttendance} hrs/pres)", color = TextSecondaryColor, fontSize = 11.sp)
                                }
                            }

                            IconButton(
                                onClick = onDeleteCourse,
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(SurfaceColor, RoundedCornerShape(12.dp))
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete course", tint = ErrorColor, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

// --- SUB-SCREEN EXCELLENCE DIAGS: ADD NEW SUBJECTS ---
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubjectCreateDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, color: String, icon: String, mode: String, sessions: Int, units: Int, initialHeld: Int, initialAttended: Int, target: Int, hoursPer: Int) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("NORMAL") } // "NORMAL" or "FIXED_TOTAL"
    
    // Auto-suggest logic inside state
    var selectedIcon by remember { mutableStateOf("book") }
    var userOverriddenIcon by remember { mutableStateOf(false) }

    var selectedColorIdx by remember { mutableStateOf(0) }
    
    val colorsList = listOf("#A78BFA", "#F43F5E", "#06B6D4", "#10B981", "#F59E0B", "#EC4899", "#8B5CF6")
    
    var periodsPerSession by remember { mutableStateOf(1) }
    var attendanceUnitsPerSession by remember { mutableStateOf(1) }
    
    var initialHeld by remember { mutableStateOf(0) }
    var initialAttended by remember { mutableStateOf(0) }
    
    var targetHours by remember { mutableStateOf(80) }
    var hoursEarnedPerAttendance by remember { mutableStateOf(2) }

    // Propose icons when name updates
    LaunchedEffect(name) {
        if (!userOverriddenIcon && name.isNotEmpty()) {
            selectedIcon = SubjectIconHelper.suggestIcon(name)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = SurfaceColor,
        title = {
            Text("Add New Subject", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 20.sp)
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Subject Name", color = TextSecondaryColor) },
                        modifier = Modifier.fillMaxWidth().testTag("add_subject_name_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = PrimaryColor,
                            unfocusedBorderColor = BorderColor
                        ),
                        singleLine = true
                    )
                }

                item {
                    Text("Subject Mode", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (mode == "NORMAL") 
                            "Standard (%): Tracks your attendance as a percentage of held classes (e.g., Target 75%)."
                            else "Fixed Hours: Tracks attendance by total credit hours earned toward a fixed syllabus target.",
                        color = TextSecondaryColor,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (mode == "NORMAL") PrimaryColor else CardSurfaceColor,
                            modifier = Modifier
                                .weight(1.dp.value)
                                .clickable { mode = "NORMAL" }
                        ) {
                            Text(
                                "Standard (%)",
                                color = if (mode == "NORMAL") Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (mode == "FIXED_TOTAL") PrimaryColor else CardSurfaceColor,
                            modifier = Modifier
                                .weight(1.dp.value)
                                .clickable { mode = "FIXED_TOTAL" }
                        ) {
                            Text(
                                "Fixed Hours",
                                color = if (mode == "FIXED_TOTAL") Color.Black else Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(vertical = 12.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                item {
                    Text("Color Palette", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        colorsList.forEachIndexed { index, hex ->
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(hex)))
                                    .border(
                                        width = if (selectedColorIdx == index) 3.dp else 0.dp,
                                        color = Color.White,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorIdx = index }
                            )
                        }
                    }
                }

                item {
                    Text("Icon (Auto-suggested)", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SubjectIconHelper.iconList.forEach { (iconId, imageVector) ->
                            val isSelected = selectedIcon == iconId
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .background(
                                        if (isSelected) Color(android.graphics.Color.parseColor(colorsList[selectedColorIdx])) else CardSurfaceColor,
                                        CircleShape
                                    )
                                    .clickable {
                                        selectedIcon = iconId
                                        userOverriddenIcon = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = imageVector,
                                    contentDescription = iconId,
                                    tint = if (isSelected) Color.Black else Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }

                if (mode == "NORMAL") {
                    item {
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Periods/Session", color = Color.White, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { if (periodsPerSession > 1) periodsPerSession-- },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) { Text("-") }
                                        Text(periodsPerSession.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                                        Button(
                                            onClick = { periodsPerSession++ },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) { Text("+") }
                                    }
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text("Units/Session", color = Color.White, fontSize = 12.sp)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Button(
                                            onClick = { if (attendanceUnitsPerSession > 1) attendanceUnitsPerSession-- },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) { Text("-") }
                                        Text(attendanceUnitsPerSession.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                                        Button(
                                            onClick = { attendanceUnitsPerSession++ },
                                            contentPadding = PaddingValues(0.dp),
                                            modifier = Modifier.size(36.dp)
                                        ) { Text("+") }
                                    }
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "• Periods/Session: Number of consecutive schedule blocks this class normally takes (typically 1 or 2 back-to-back).\n• Units/Session: Weight counted toward your attendance statistics per session (normally 1).",
                                color = TextSecondaryColor,
                                fontSize = 11.sp,
                                lineHeight = 14.sp
                            )
                        }
                    }
                } else {
                    // FIXED_TOTAL
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Required Target Hours", color = Color.White, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { if (targetHours > 10) targetHours -= 10 }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) { Text("-") }
                                    Text(targetHours.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                    Button(onClick = { targetHours += 10 }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
                                }
                            }

                            Column(modifier = Modifier.weight(1f)) {
                                Text("Hour/Present Class", color = Color.White, fontSize = 12.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Button(onClick = { if (hoursEarnedPerAttendance > 1) hoursEarnedPerAttendance-- }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) { Text("-") }
                                    Text(hoursEarnedPerAttendance.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                    Button(onClick = { hoursEarnedPerAttendance++ }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
                                }
                            }
                        }
                    }
                }

                // Mid-Semester Initializer inputs
                item {
                    Divider(color = BorderColor, modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Starting Midway? Enter Initial Classes Below:",
                        color = PrimaryColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                                       Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (mode == "NORMAL") "Initial Classes Held" else "Initial Hours Conducted", color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(
                                    onClick = {
                                        if (initialHeld > 0) {
                                            initialHeld--
                                            if (mode == "NORMAL" && initialAttended > initialHeld) {
                                                initialAttended = initialHeld
                                            }
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("-") }
                                Text(initialHeld.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                Button(onClick = { initialHeld++ }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) { Text("+") }
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(if (mode == "NORMAL") "Initial Attended" else "Initial Hours Earned", color = Color.White, fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Button(onClick = { if (initialAttended > 0) initialAttended-- }, modifier = Modifier.size(36.dp), contentPadding = PaddingValues(0.dp)) { Text("-") }
                                Text(initialAttended.toString(), color = Color.White, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                                Button(
                                    onClick = {
                                        if (mode != "NORMAL" || initialAttended < initialHeld) {
                                            initialAttended++
                                        }
                                    },
                                    modifier = Modifier.size(36.dp),
                                    contentPadding = PaddingValues(0.dp)
                                ) { Text("+") }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.trim().isNotEmpty()) {
                        onSave(
                            name.trim(),
                            colorsList[selectedColorIdx],
                            selectedIcon,
                            mode,
                            periodsPerSession,
                            attendanceUnitsPerSession,
                            initialHeld,
                            initialAttended,
                            targetHours,
                            hoursEarnedPerAttendance
                        )
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
            ) {
                Text("Launch Subject", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Nevermind", color = TextSecondaryColor)
            }
        }
    )
}

@Composable
fun EmptyStatePlaceholder(onAddClick: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .background(CardSurfaceColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Filled.School, contentDescription = "School placeholder", tint = PrimaryColor, modifier = Modifier.size(32.dp))
            }

            Text(
                "Welcome to Attendr",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Text(
                "Keep track of academic semesters without friction. Add your first subject to get going.",
                fontSize = 12.sp,
                color = TextSecondaryColor,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Button(
                onClick = onAddClick,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Icon", tint = Color.Black, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add Subject", color = Color.Black, fontWeight = FontWeight.Bold)
            }
        }
    }
}
