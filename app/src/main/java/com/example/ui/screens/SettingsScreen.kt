package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(
    viewModel: AttendanceViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val coroutineScope = rememberCoroutineScope()

    val settingsState by viewModel.settingsState.collectAsState()

    // Read values from State Map with baseline fallbacks
    val userName = settingsState["user_name"] ?: "Student"
    val semesterStr = settingsState["semester"] ?: "1"
    val minAttendanceStr = settingsState["min_attendance"] ?: "75"
    val saturdayEnabledStr = settingsState["saturday_enabled"] ?: "false"
    val notificationsEnabledStr = settingsState["notifications_enabled"] ?: "true"

    val saturdayEnabled = saturdayEnabledStr.toBoolean()
    val notificationsEnabled = notificationsEnabledStr.toBoolean()

    // Temporary values for editing text fields cleanly
    var editName by remember(userName) { mutableStateOf(userName) }
    var editSemester by remember(semesterStr) { mutableStateOf(semesterStr) }

    // Dialog sheets states
    var showBackupDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }
    var showOnboardingPermissions by remember { mutableStateOf(false) }

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
                Column {
                    Text(
                        text = "Settings",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Customize baseline configs and data portability options",
                        fontSize = 12.sp,
                        color = TextSecondaryColor
                    )
                }
            }

            // --- USER IDENTITY CARD CARD ---
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
                        Text("USER IDENTITY", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        OutlinedTextField(
                            value = editName,
                            onValueChange = {
                                editName = it
                                viewModel.saveSetting("user_name", it)
                            },
                            label = { Text("Student Name", color = TextSecondaryColor) },
                            modifier = Modifier.fillMaxWidth().testTag("settings_username_input"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = BorderColor
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )

                        OutlinedTextField(
                            value = editSemester,
                            onValueChange = {
                                editSemester = it
                                viewModel.saveSetting("semester", it)
                            },
                            label = { Text("Active Semester", color = TextSecondaryColor) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = BorderColor
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                        )
                    }
                }
            }

            // --- THEME CUSTOMIZATION CARD ---
            item {
                val themeColorHex = settingsState["theme_color"] ?: "#A78BFA"
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
                        Text("APP THEME COLOR", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        
                        // Preset Circular Pills
                        Text("Accent Color Presets", color = Color.White, fontSize = 13.sp)
                        val presets = listOf(
                            "#A78BFA" to "Lavender",
                            "#60A5FA" to "Blue",
                            "#2DD4BF" to "Teal",
                            "#34D399" to "Mint",
                            "#FBBF24" to "Yellow",
                            "#FB923C" to "Orange",
                            "#F87171" to "Coral",
                            "#F472B6" to "Pink"
                        )
                        
                        androidx.compose.foundation.lazy.grid.LazyVerticalGrid(
                            columns = androidx.compose.foundation.lazy.grid.GridCells.Fixed(4),
                            modifier = Modifier.height(100.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(presets.size) { index ->
                                val (hex, name) = presets[index]
                                val isSelected = themeColorHex.lowercase() == hex.lowercase()
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(Color(android.graphics.Color.parseColor(hex)))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Transparent,
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            viewModel.saveSetting("theme_color", hex)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            Icons.Filled.Check,
                                            contentDescription = "Selected",
                                            tint = Color.Black,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }

                        Divider(color = BorderColor)

                        // Color Wheel (Hue Slider)
                        Column {
                            Text("Fine-Tune custom shade (0-360° Hue)", color = Color.White, fontSize = 13.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val currentHue = remember(themeColorHex) {
                                val hsv = FloatArray(3)
                                try {
                                    android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(themeColorHex), hsv)
                                    hsv[0]
                                } catch (e: Exception) {
                                    250f
                                }
                            }
                            
                            var tempHue by remember(currentHue) { mutableStateOf(currentHue) }
                            
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(12.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(
                                        brush = androidx.compose.ui.graphics.Brush.linearGradient(
                                            colors = listOf(
                                                Color(0xFFFF0000), Color(0xFFFF9900), Color(0xFFFFFF00),
                                                Color(0xFF33FF00), Color(0xFF00FFFF), Color(0xFF0000FF),
                                                Color(0xFF9900FF), Color(0xFFFF00FF), Color(0xFFFF0000)
                                            )
                                        )
                                    )
                            )
                            
                            Slider(
                                value = tempHue,
                                onValueChange = {
                                    tempHue = it
                                    val hexColor = String.format("#%06X", 0xFFFFFF and android.graphics.Color.HSVToColor(floatArrayOf(it, 0.7f, 0.9f)))
                                    viewModel.saveSetting("theme_color", hexColor)
                                },
                                valueRange = 0f..360f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color.Transparent,
                                    inactiveTrackColor = Color.Transparent
                                )
                            )
                            
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Selected Hue: ${tempHue.toInt()}°", color = TextSecondaryColor, fontSize = 11.sp)
                                Box(
                                    modifier = Modifier
                                        .size(32.dp, 16.dp)
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(Color(android.graphics.Color.parseColor(themeColorHex)))
                                        .border(1.dp, Color.White.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                )
                            }
                        }
                    }
                }
            }

            // --- SYSTEM PREFERENCES CARD ---
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
                        Text("PREFERENCES", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)

                        // Minimum percentage slider
                        Column {
                            val minVal = minAttendanceStr.toFloatOrNull() ?: 75f
                            var sliderValue by remember(minVal) { mutableStateOf(minVal) }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Requirement Minimum", color = Color.White, fontSize = 14.sp)
                                Text("${sliderValue.toInt()}%", color = PrimaryColor, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Slider(
                                value = sliderValue,
                                onValueChange = { sliderValue = it },
                                onValueChangeFinished = {
                                    viewModel.saveSetting("min_attendance", sliderValue.toInt().toString())
                                },
                                valueRange = 50f..100f,
                                colors = SliderDefaults.colors(
                                    thumbColor = PrimaryColor,
                                    activeTrackColor = PrimaryColor,
                                    inactiveTrackColor = BorderColor
                                )
                            )
                        }

                        Divider(color = BorderColor)

                        // Saturdays enabled switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Saturday Classes", color = Color.White, fontSize = 14.sp)
                                Text("Adds Saturdays into the timetable week grid schema.", color = TextSecondaryColor, fontSize = 11.sp)
                            }
                            Switch(
                                checked = saturdayEnabled,
                                onCheckedChange = { viewModel.saveSetting("saturday_enabled", it.toString()) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PrimaryColor
                                )
                            )
                        }

                        Divider(color = BorderColor)

                        // Notifications enabled switch
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Instant End Reminders", color = Color.White, fontSize = 14.sp)
                                Text("Triggers a push reminder at class end timings automatically.", color = TextSecondaryColor, fontSize = 11.sp)
                            }
                            Switch(
                                checked = notificationsEnabled,
                                onCheckedChange = { viewModel.saveSetting("notifications_enabled", it.toString()) },
                                colors = SwitchDefaults.colors(
                                    checkedThumbColor = Color.Black,
                                    checkedTrackColor = PrimaryColor
                                )
                            )
                        }

                        // Explanation link
                        Text(
                            text = "Learn about notification reliability permissions...",
                            color = PrimaryColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { showOnboardingPermissions = true }
                                .padding(vertical = 4.dp)
                        )
                    }
                }
            }

            // --- PORTABILITY & TRANSFER CONFIGS ---
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
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("DATA PORTABILITY (JSON BACKUP)", color = TextSecondaryColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Move your data (subjects, periods, results) cleanly between physical phones.", color = TextSecondaryColor, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Button(
                                onClick = { showBackupDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.Share, contentDescription = "Export", tint = PrimaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Export DB", color = Color.White, fontSize = 12.sp)
                            }

                            Button(
                                onClick = { showImportDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = CardSurfaceColor),
                                modifier = Modifier
                                    .weight(1f)
                                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp)),
                                shape = RoundedCornerShape(12.dp)
                            ) {
                                Icon(Icons.Filled.FileUpload, contentDescription = "Import", tint = PrimaryColor, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Import DB", color = Color.White, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // --- RESET SEMESTER DATA ---
            item {
                var showResetConfirmDialog by remember { mutableStateOf(false) }

                Card(
                    colors = CardDefaults.cardColors(containerColor = SurfaceColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        .testTag("reset_data_card")
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("RESET SEMESTER DATA", color = ErrorColor, fontSize = 11.sp, fontWeight = FontWeight.Bold, letterSpacing = 1.sp)
                        Text("Crucial for starting a fresh semester. This will permanently erase all subjects, attendance logs, and custom schedules to restore defaults.", color = TextSecondaryColor, fontSize = 11.sp)
                        Spacer(modifier = Modifier.height(4.dp))

                        Button(
                            onClick = { showResetConfirmDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                            modifier = Modifier.fillMaxWidth().testTag("reset_data_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Filled.DeleteForever, contentDescription = "Reset", tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Reset All App Data", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (showResetConfirmDialog) {
                    AlertDialog(
                        onDismissRequest = { showResetConfirmDialog = false },
                        containerColor = SurfaceColor,
                        title = { Text("Reset Entire Database?", color = Color.White, fontWeight = FontWeight.Bold) },
                        text = {
                            Text("This action is completely irreversible. You will lose all current course progression details, timetable allocations, and overridden days. Ensure you have exported a back-up if you wish to restore it. Do you want to proceed?", color = TextSecondaryColor, fontSize = 13.sp)
                        },
                        confirmButton = {
                            Button(
                                onClick = {
                                    viewModel.resetAllData()
                                    showResetConfirmDialog = false
                                    Toast.makeText(context, "Fresh semester initialized! All data cleared.", Toast.LENGTH_LONG).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = ErrorColor),
                                modifier = Modifier.testTag("reset_confirm_confirm")
                            ) {
                                Text("Delete Everything", color = Color.Black, fontWeight = FontWeight.Bold)
                            }
                        },
                        dismissButton = {
                            TextButton(
                                onClick = { showResetConfirmDialog = false },
                                modifier = Modifier.testTag("reset_confirm_cancel")
                            ) {
                                Text("Cancel", color = TextSecondaryColor)
                            }
                        }
                    )
                }
            }

            // --- BOTTOM BRANDING AREA ---
            item {
                Spacer(modifier = Modifier.height(24.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "For Students, By Students ❤",
                        color = Color.LightGray.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                try {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://www.github.com/aprameyansr/attendr"))
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    Toast.makeText(context, "Unable to open GitHub link", Toast.LENGTH_SHORT).show()
                                }
                            }
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            painter = painterResource(id = com.example.R.drawable.ic_github),
                            contentDescription = "GitHub repository",
                            tint = PrimaryColor,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "GitHub Repository",
                            color = PrimaryColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
        }

        // --- DIALOG DIALOG: EXPORT DATA CODE DIAL ---
        if (showBackupDialog) {
            var copiedJson by remember { mutableStateOf("") }
            
            LaunchedEffect(Unit) {
                copiedJson = viewModel.exportData()
            }

            AlertDialog(
                onDismissRequest = { showBackupDialog = false },
                containerColor = SurfaceColor,
                title = { Text("Export Database Backup", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Your database is successfully serialized to portable JSON. Copy the text below to transfer:", color = TextSecondaryColor, fontSize = 13.sp)
                        
                        OutlinedTextField(
                            value = copiedJson,
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = BorderColor,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Attendly DB Backup", copiedJson)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Copied backup to clipboard!", Toast.LENGTH_SHORT).show()
                            showBackupDialog = false
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("Copy String", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showBackupDialog = false }) {
                        Text("Dismiss", color = TextSecondaryColor)
                    }
                }
            )
        }

        // --- DIALOG DIALOG: IMPORT DATA CODE DIAL ---
        if (showImportDialog) {
            var inputJson by remember { mutableStateOf("") }

            AlertDialog(
                onDismissRequest = { showImportDialog = false },
                containerColor = SurfaceColor,
                title = { Text("Restore From JSON Backup", color = Color.White, fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("Paste your exported database JSON code string here to load all metrics instantly.", color = TextSecondaryColor, fontSize = 13.sp)
                        
                        OutlinedTextField(
                            value = inputJson,
                            onValueChange = { inputJson = it },
                            placeholder = { Text("Paste JSON backing code here...", color = Color.Gray) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 11.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = PrimaryColor,
                                unfocusedBorderColor = BorderColor
                            )
                        )
                        Text("WARNING: This destroys current local entries fully to populate transferred databases.", color = ErrorColor, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            coroutineScope.launch {
                                val success = viewModel.importData(inputJson.trim())
                                if (success) {
                                    Toast.makeText(context, "Database restored! App synced.", Toast.LENGTH_LONG).show()
                                    showImportDialog = false
                                } else {
                                    Toast.makeText(context, "Invalid JSON structure. Import failed.", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("Restore Database", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showImportDialog = false }) {
                        Text("Cancel", color = TextSecondaryColor)
                    }
                }
            )
        }

        // --- DIALOG DIALOG: ONBOARDING / BACKGROUND RELIABILITY ---
        if (showOnboardingPermissions) {
            AlertDialog(
                onDismissRequest = { showOnboardingPermissions = false },
                containerColor = SurfaceColor,
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Filled.BatteryChargingFull, contentDescription = null, tint = PrimaryColor)
                        Text("Notification Info", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(
                            text = "To ensure notifications arrive exactly when classes end, Android may require allowing unrestricted battery usage or background execution inside device settings.",
                            color = Color.White,
                            fontSize = 13.sp
                        )
                        Text(
                            text = "The app does not access location, personal files, contacts, messages, or private information. This permission is used only to improve notification reliability.",
                            color = TextSecondaryColor,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "If not granted, notifications will still work but may arrive late under deep sleep.",
                            color = TextSecondaryColor,
                            fontSize = 12.sp
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = { showOnboardingPermissions = false },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryColor)
                    ) {
                        Text("I Understand", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    }
}
