package com.example.ui.screens

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import java.util.*

@Composable
fun CalculatorScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val courses by viewModel.courses.collectAsState()
    val courseStatsList by viewModel.courseStats.collectAsState()
    val calculatorCourseId by viewModel.calculatorCourseId.collectAsState()
    val calculatorSkipCount by viewModel.calculatorSkipCount.collectAsState()
    val calculatorResultText by viewModel.calculatorResult.collectAsState()
    val settingsState by viewModel.settingsState.collectAsState()

    val minPct = (settingsState["min_attendance"] ?: "75").toFloatOrNull() ?: 75f

    var selectedSubTab by remember { mutableStateOf(0) } // 0: Subject Skip, 1: Overall Dip, 2: Friend Check
    var showCourseDropdown by remember { mutableStateOf(false) }
    
    // --- STATE FOR TAB 2: OVERALL DIP ---
    var overallSkipsMap by remember { mutableStateOf(mapOf<Int, Int>()) }
    
    LaunchedEffect(courseStatsList) {
        val currentMap = overallSkipsMap.toMutableMap()
        courseStatsList.forEach { stat ->
            if (!currentMap.containsKey(stat.course.id)) {
                currentMap[stat.course.id] = 0
            }
        }
        overallSkipsMap = currentMap
    }

    // --- STATE FOR TAB 3: FRIEND MANUAL CHECKER ---
    var friendAttendedStr by remember { mutableStateOf("15") }
    var friendHeldStr by remember { mutableStateOf("20") }
    var friendTargetStr by remember { mutableStateOf(minPct.toInt().toString()) }

    // Synchronize inputs dynamically to prevent attended > held
    val parsedFriendHeld = friendHeldStr.toIntOrNull() ?: 0
    val parsedFriendAttended = friendAttendedStr.toIntOrNull() ?: 0
    val parsedFriendTarget = (friendTargetStr.toFloatOrNull() ?: minPct) / 100f

    val activeStat = remember(calculatorCourseId, courseStatsList) {
        courseStatsList.find { it.course.id == calculatorCourseId }
    }

    // Tab 1: Simulated Single Percentage
    val simulatedPct = remember(activeStat, calculatorSkipCount) {
        if (activeStat == null) 100f
        else {
            if (activeStat.course.attendanceMode == "FIXED_TOTAL") {
                activeStat.percentage
            } else {
                val newAttended = activeStat.attended
                val newHeld = activeStat.held + (calculatorSkipCount * activeStat.course.attendanceUnitsPerSession)
                if (newHeld > 0) (newAttended.toFloat() / newHeld) * 100f else 100f
            }
        }
    }

    val animatedPct = animateFloatAsState(
        targetValue = simulatedPct,
        animationSpec = tween(durationMillis = 600),
        label = "tab1_anim"
    )

    // Calculations for Tab 2: Overall Simulated Percentage
    val simulatedStats = remember(courseStatsList, overallSkipsMap) {
        courseStatsList.map { stat ->
            val skips = overallSkipsMap[stat.course.id] ?: 0
            val simCoursePct = if (stat.course.attendanceMode == "FIXED_TOTAL") {
                stat.percentage
            } else {
                val simulatedHeld = stat.held + (skips * stat.course.attendanceUnitsPerSession)
                val simulatedAttended = stat.attended
                if (simulatedHeld > 0) (simulatedAttended.toFloat() / simulatedHeld) * 100f else 100f
            }
            stat.copy(percentage = simCoursePct)
        }
    }

    val overallSimulatedPct = remember(simulatedStats) {
        if (simulatedStats.isEmpty()) 0f else {
            simulatedStats.map { it.percentage }.average().toFloat()
        }
    }

    val overallAnimatedPct = animateFloatAsState(
        targetValue = overallSimulatedPct,
        animationSpec = tween(600),
        label = "tab2_overall_pct"
    )

    // Calculations for Tab 3: Friend Quick Checker
    val friendPct = remember(parsedFriendHeld, parsedFriendAttended) {
        if (parsedFriendHeld > 0) (parsedFriendAttended.toFloat() / parsedFriendHeld) * 100f else 100f
    }
    
    val animatedFriendPct = animateFloatAsState(
        targetValue = friendPct,
        animationSpec = tween(600),
        label = "tab3_friend_pct"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(BackgroundColor)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 100.dp, top = 16.dp, start = 16.dp, end = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // --- HEADER ---
            item {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Smart Calculator",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Simulate attendance cuts or check a friend's metrics",
                        fontSize = 12.sp,
                        color = TextSecondaryColor
                    )
                }
            }

            // --- PILLED NAVIGATION SUBTABS ---
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(SurfaceColor)
                        .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val subTabs = listOf(
                        "Subject Skip",
                        "Overall Dip",
                        "Friend Check"
                    )
                    subTabs.forEachIndexed { index, title ->
                        val isSelected = selectedSubTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) PrimaryColor else Color.Transparent)
                                .clickable { selectedSubTab = index }
                                .padding(vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                color = if (isSelected) Color.Black else Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            // --- RENDERING TABS ---
            when (selectedSubTab) {
                0 -> {
                    // TAB 1: SUBJECT SKIP (ORIGINAL FEATURE)
                    if (courses.isEmpty()) {
                        item {
                            EmptyStateCard()
                        }
                    } else {
                        // Dropdown Selector Card
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(24.dp))
                                    .background(SurfaceColor)
                                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                                    .padding(16.dp)
                            ) {
                                Text("SELECT SUBJECT TO SIMULATE", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                Spacer(modifier = Modifier.height(10.dp))
                                
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(CardSurfaceColor)
                                        .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                        .clickable { showCourseDropdown = true }
                                        .padding(16.dp)
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                                        ) {
                                            val activeCourse = courses.find { it.id == calculatorCourseId }
                                            if (activeCourse != null) {
                                                Box(
                                                    modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(activeCourse.color)))
                                                )
                                                Text(text = activeCourse.name, color = Color.White, fontWeight = FontWeight.SemiBold)
                                            } else {
                                                Text("Select Course...", color = Color.Gray)
                                            }
                                        }

                                        Icon(Icons.Filled.ArrowDropDown, contentDescription = "Dropdown", tint = Color.White)
                                    }
                                }

                                DropdownMenu(
                                    expanded = showCourseDropdown,
                                    onDismissRequest = { showCourseDropdown = false },
                                    modifier = Modifier.fillMaxWidth(0.85f).background(SurfaceColor)
                                ) {
                                    courses.forEach { course ->
                                        DropdownMenuItem(
                                            text = { Text(course.name, color = Color.White) },
                                            onClick = {
                                                viewModel.setCalculatorParams(course.id, calculatorSkipCount)
                                                showCourseDropdown = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Simulation Meter Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(24.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("SIMULATED FUTURE OUTCOME", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(160.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(
                                                color = BorderColor,
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }

                                        val isSafeSimulated = simulatedPct >= minPct
                                        val activeColor = if (isSafeSimulated) SuccessColor else ErrorColor

                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(
                                                color = activeColor,
                                                startAngle = -90f,
                                                sweepAngle = animatedPct.value * 3.6f,
                                                useCenter = false,
                                                style = Stroke(width = 16.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = String.format(Locale.getDefault(), "%.1f%%", animatedPct.value),
                                                fontSize = 28.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = "Simulated Pct",
                                                fontSize = 11.sp,
                                                color = TextSecondaryColor
                                            )
                                        }
                                    }

                                    Text(
                                        text = calculatorResultText,
                                        fontSize = 14.sp,
                                        color = Color.White,
                                        fontWeight = FontWeight.Medium,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }

                        // Steppers
                        item {
                            val activeCourse = courses.find { it.id == calculatorCourseId }
                            if (activeCourse?.attendanceMode != "FIXED_TOTAL") {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(20.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Text("PROPOSED SKIPPED LECTURES", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                        
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(24.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (calculatorSkipCount > 0) {
                                                        viewModel.setCalculatorParams(calculatorCourseId ?: -1, calculatorSkipCount - 1)
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(CardSurfaceColor, CircleShape)
                                            ) {
                                                Icon(Icons.Filled.Remove, contentDescription = "Decrease skips", tint = Color.White)
                                            }

                                            Text(
                                                text = calculatorSkipCount.toString(),
                                                fontSize = 32.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )

                                            IconButton(
                                                onClick = {
                                                    viewModel.setCalculatorParams(calculatorCourseId ?: -1, calculatorSkipCount + 1)
                                                },
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .background(CardSurfaceColor, CircleShape)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = "Increase skips", tint = Color.White)
                                            }
                                        }
                                        
                                        Text(
                                            "Each skip session reduces attendance by ${activeCourse?.attendanceUnitsPerSession ?: 1} units.",
                                            fontSize = 11.sp,
                                            color = TextSecondaryColor
                                        )
                                    }
                                }
                            } else {
                                Card(
                                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                    shape = RoundedCornerShape(24.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                                        .padding(20.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Icon(Icons.Filled.Info, contentDescription = "Info", tint = PrimaryColor)
                                        Text(
                                            "This subject uses Fixed Total Mode. Skipping classes does not decrease your earned hours, but decreases opportunities to reach your absolute total hours of ${activeCourse.targetHours} hours.",
                                            color = Color.White,
                                            fontSize = 12.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                1 -> {
                    // TAB 2: OVERALL DIP SUMMARY (MULTI-SUBJECT INTEGRATED SIMULATOR)
                    if (courses.isEmpty()) {
                        item {
                            EmptyStateCard()
                        }
                    } else {
                        // 1. Overall Simulated Percentage Card
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                                shape = RoundedCornerShape(24.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                            ) {
                                Column(
                                    modifier = Modifier.padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    Text("SIMULATED OVERALL ATTENDANCE", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                    
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.size(130.dp)
                                    ) {
                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(
                                                color = BorderColor,
                                                startAngle = -90f,
                                                sweepAngle = 360f,
                                                useCenter = false,
                                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }

                                        val isOverallSafe = overallSimulatedPct >= minPct
                                        val activeColor = if (isOverallSafe) SuccessColor else ErrorColor

                                        Canvas(modifier = Modifier.fillMaxSize()) {
                                            drawArc(
                                                color = activeColor,
                                                startAngle = -90f,
                                                sweepAngle = overallAnimatedPct.value * 3.6f,
                                                useCenter = false,
                                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                            )
                                        }

                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                            Text(
                                                text = String.format(Locale.getDefault(), "%.1f%%", overallAnimatedPct.value),
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color.White
                                            )
                                            Text(
                                                text = if (isOverallSafe) "Target Met" else "Under Target",
                                                fontSize = 11.sp,
                                                color = if (isOverallSafe) SuccessColor else ErrorColor,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    Text(
                                        text = if (overallSimulatedPct >= minPct) {
                                            "Excellent! Your curriculum remains protected above the required minimum of ${minPct.toInt()}%."
                                        } else {
                                            "Warning! Skipping too many sessions drops your overall average below the mandatory ${minPct.toInt()}% threshold."
                                        },
                                        fontSize = 13.sp,
                                        color = Color.White,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(horizontal = 8.dp)
                                    )
                                }
                            }
                        }

                        // Section Title
                        item {
                            Text(
                                text = "SIMULATE COHORT SKIPS",
                                color = TextSecondaryColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                textAlign = TextAlign.Left
                            )
                        }

                        // List of course rows with skip adjusters
                        items(simulatedStats, key = { it.course.id }) { stat ->
                            val currentSkips = overallSkipsMap[stat.course.id] ?: 0
                            Card(
                                colors = CardDefaults.cardColors(containerColor = SurfaceColor),
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
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color(android.graphics.Color.parseColor(stat.course.color))))
                                            Text(stat.course.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Simulated: " + String.format(Locale.getDefault(), "%.1f%%", stat.percentage) + " (Actual: " + String.format(Locale.getDefault(), "%.1f%%", stat.course.initialClassesAttended.toFloat() / Math.max(1, stat.course.initialClassesHeld) * 100f) + ")",
                                            fontSize = 11.sp,
                                            color = TextSecondaryColor
                                        )
                                    }

                                    if (stat.course.attendanceMode != "FIXED_TOTAL") {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            IconButton(
                                                onClick = {
                                                    if (currentSkips > 0) {
                                                        val nextMap = overallSkipsMap.toMutableMap()
                                                        nextMap[stat.course.id] = currentSkips - 1
                                                        overallSkipsMap = nextMap
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(CardSurfaceColor)
                                            ) {
                                                Icon(Icons.Filled.Remove, contentDescription = "Decrease course skips", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }

                                            Text(
                                                text = currentSkips.toString(),
                                                color = Color.White,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold
                                            )

                                            IconButton(
                                                onClick = {
                                                    val nextMap = overallSkipsMap.toMutableMap()
                                                    nextMap[stat.course.id] = currentSkips + 1
                                                    overallSkipsMap = nextMap
                                                },
                                                modifier = Modifier
                                                    .size(32.dp)
                                                    .clip(CircleShape)
                                                    .background(CardSurfaceColor)
                                            ) {
                                                Icon(Icons.Filled.Add, contentDescription = "Increase course skips", tint = Color.White, modifier = Modifier.size(14.dp))
                                            }
                                        }
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .background(CardSurfaceColor, RoundedCornerShape(8.dp))
                                                .padding(horizontal = 10.dp, vertical = 6.dp)
                                        ) {
                                            Text("Fixed Target", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 3: FRIEND QUICK CHECKER (MANUAL OVERRIDE SPEC)
                    
                    // Target percentage fraction
                    val thresholdFraction = parsedFriendTarget

                    val isFriendSafe = friendPct >= (thresholdFraction * 100f)

                    val friendAllowedSkips = if (isFriendSafe && thresholdFraction > 0) {
                        try {
                            Math.max(0, Math.floor(((parsedFriendAttended - (thresholdFraction * parsedFriendHeld)) / thresholdFraction).toDouble()).toInt())
                        } catch (e: Exception) { 0 }
                    } else 0

                    val friendRequiredToReach = if (!isFriendSafe && thresholdFraction < 1f) {
                        try {
                            Math.max(0, Math.ceil(((thresholdFraction * parsedFriendHeld - parsedFriendAttended) / (1 - thresholdFraction)).toDouble()).toInt())
                        } catch (e: Exception) { 0 }
                    } else 0

                    // 1. Friend Percentage Meter
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("FRIEND\\'S ATTENDANCE STATUS", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(130.dp)
                                ) {
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = BorderColor,
                                            startAngle = -90f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }

                                    val activeColor = if (isFriendSafe) SuccessColor else ErrorColor

                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = activeColor,
                                            startAngle = -90f,
                                            sweepAngle = animatedFriendPct.value * 3.6f,
                                            useCenter = false,
                                            style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }

                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text(
                                            text = String.format(Locale.getDefault(), "%.1f%%", animatedFriendPct.value),
                                            fontSize = 24.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = Color.White
                                        )
                                        Text(
                                            text = if (isFriendSafe) "Protected" else "Below Safe",
                                            fontSize = 11.sp,
                                            color = if (isFriendSafe) SuccessColor else ErrorColor,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Interactive Card Summary
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(if (isFriendSafe) SuccessColor.copy(alpha = 0.12f) else ErrorColor.copy(alpha = 0.12f))
                                        .border(1.dp, if (isFriendSafe) SuccessColor else ErrorColor, RoundedCornerShape(16.dp))
                                        .padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Icon(
                                        imageVector = if (isFriendSafe) Icons.Filled.CheckCircle else Icons.Filled.Warning,
                                        contentDescription = "Status Icon",
                                        tint = if (isFriendSafe) SuccessColor else ErrorColor,
                                        modifier = Modifier.size(24.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = if (isFriendSafe) "Conscious Swerver" else "Rebound Recommended",
                                            color = if (isFriendSafe) SuccessColor else ErrorColor,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp
                                        )
                                        Spacer(modifier = Modifier.height(2.dp))
                                        Text(
                                            text = if (isFriendSafe) {
                                                "Can skip **$friendAllowedSkips** consecutive sessions and remain above ${(parsedFriendTarget * 100).toInt()}%."
                                            } else {
                                                "Must attend **$friendRequiredToReach** successive held classes consecutively to recover back to ${(parsedFriendTarget * 100).toInt()}%."
                                            },
                                            color = Color.White,
                                            fontSize = 12.sp,
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // 2. Friend Numeric Input Controls with Stepper & Text Field Support
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                            shape = RoundedCornerShape(24.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        ) {
                            Column(
                                modifier = Modifier.padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text("FRIEND\\'S RAW METRICS", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                                
                                // Target requirement slider/stepper
                                Column {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Minimum Attendance Target", color = Color.White, fontSize = 13.sp)
                                        Text("${(parsedFriendTarget * 100).toInt()}%", color = PrimaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    }
                                    
                                    Slider(
                                        value = friendTargetStr.toFloatOrNull() ?: minPct,
                                        onValueChange = { friendTargetStr = it.toInt().toString() },
                                        valueRange = 50f..100f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = PrimaryColor,
                                            activeTrackColor = PrimaryColor,
                                            inactiveTrackColor = BorderColor
                                        )
                                    )
                                }

                                Divider(color = BorderColor)

                                // Classes Attended Stepper + Input
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Classes Attended (Present)", color = Color.White, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (parsedFriendAttended > 0) {
                                                    friendAttendedStr = (parsedFriendAttended - 1).toString()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp).background(CardSurfaceColor, CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Minus", tint = Color.White)
                                        }

                                        OutlinedTextField(
                                            value = friendAttendedStr,
                                            onValueChange = { input ->
                                                val filtered = input.filter { it.isDigit() }
                                                val newVal = filtered.toIntOrNull() ?: 0
                                                friendAttendedStr = filtered
                                                
                                                // Dynamic safety constraint
                                                if (newVal > parsedFriendHeld) {
                                                    friendHeldStr = filtered // Push held up to maintain attended <= held
                                                }
                                            },
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryColor,
                                                unfocusedBorderColor = BorderColor
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                val nextAttended = parsedFriendAttended + 1
                                                friendAttendedStr = nextAttended.toString()
                                                // Dynamic safety constraint
                                                if (nextAttended > parsedFriendHeld) {
                                                    friendHeldStr = nextAttended.toString()
                                                }
                                            },
                                            modifier = Modifier.size(36.dp).background(CardSurfaceColor, CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Plus", tint = Color.White)
                                        }
                                    }
                                }

                                // Classes Held Stepper + Input
                                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text("Classes Conducted (Held So Far)", color = Color.White, fontSize = 13.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        IconButton(
                                            onClick = {
                                                if (parsedFriendHeld > 0) {
                                                    val nextHeld = parsedFriendHeld - 1
                                                    friendHeldStr = nextHeld.toString()
                                                    // Dynamic safety constraint
                                                    if (parsedFriendAttended > nextHeld) {
                                                        friendAttendedStr = nextHeld.toString()
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(36.dp).background(CardSurfaceColor, CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Remove, contentDescription = "Minus", tint = Color.White)
                                        }

                                        OutlinedTextField(
                                            value = friendHeldStr,
                                            onValueChange = { input ->
                                                val filtered = input.filter { it.isDigit() }
                                                val newVal = filtered.toIntOrNull() ?: 0
                                                friendHeldStr = filtered
                                                
                                                // Dynamic safety constraint
                                                if (parsedFriendAttended > newVal) {
                                                    friendAttendedStr = filtered // Pull attended down to maintain attended <= held
                                                }
                                            },
                                            textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 14.sp, textAlign = TextAlign.Center),
                                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                            singleLine = true,
                                            modifier = Modifier.weight(1f),
                                            colors = OutlinedTextFieldDefaults.colors(
                                                focusedBorderColor = PrimaryColor,
                                                unfocusedBorderColor = BorderColor
                                            )
                                        )

                                        IconButton(
                                            onClick = {
                                                friendHeldStr = (parsedFriendHeld + 1).toString()
                                            },
                                            modifier = Modifier.size(36.dp).background(CardSurfaceColor, CircleShape)
                                        ) {
                                            Icon(Icons.Filled.Add, contentDescription = "Plus", tint = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyStateCard() {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
    ) {
        Text(
            text = "Please add subjects on the home screen first to run skipping simulations.",
            color = TextSecondaryColor,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(32.dp)
        )
    }
}

