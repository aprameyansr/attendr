package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.screens.*
import com.example.ui.theme.*
import com.example.ui.viewmodel.AttendanceViewModel
import com.example.ui.viewmodel.ViewModelFactory
import com.example.worker.NotificationScheduler

enum class AppTab(val route: String) {
    TIMETABLE("timetable"),
    ATTENDANCE("attendance"),
    HOME("home"),
    CALCULATOR("calculator"),
    SETTINGS("settings")
}

class MainActivity : ComponentActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            Toast.makeText(this, "Attendance reminders enabled!", Toast.LENGTH_SHORT).show()
            NotificationScheduler.scheduleTodayNotifications(this)
        } else {
            Toast.makeText(this, "Reminders may arrive late without permissions.", Toast.LENGTH_LONG).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Request POST_NOTIFICATIONS runtime permission dynamically on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        // Schedule notifications for today on startup
        NotificationScheduler.scheduleTodayNotifications(this)

        setContent {
            val app = application as AttendanceApplication
            val factory = ViewModelFactory(app, app.repository)
            val viewModel: AttendanceViewModel = viewModel(factory = factory)

            val settingsState by viewModel.settingsState.collectAsState()
            val themeColorHex = settingsState["theme_color"] ?: "#A78BFA"

            LaunchedEffect(themeColorHex) {
                try {
                    val color = Color(android.graphics.Color.parseColor(themeColorHex))
                    PrimaryColor = color
                    val hsv = FloatArray(3)
                    android.graphics.Color.colorToHSV(android.graphics.Color.parseColor(themeColorHex), hsv)
                    hsv[2] *= 0.8f // Reduce brightness by 20% to derive PrimaryDark
                    val darkColorInt = android.graphics.Color.HSVToColor(hsv)
                    PrimaryDark = Color(darkColorInt)
                } catch (e: Exception) {
                    PrimaryColor = Color(0xFFA78BFA)
                    PrimaryDark = Color(0xFF8B5CF6)
                }
            }

            MyApplicationTheme {
                var currentTab by remember { mutableStateOf(AppTab.HOME) }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    bottomBar = {
                        CustomBottomNavigationBar(
                            selectedTab = currentTab,
                            onTabSelected = { currentTab = it }
                        )
                    },
                    containerColor = BackgroundColor,
                    contentWindowInsets = WindowInsets.systemBars // Protect system-level safe zones including status bar and navigation bars
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = currentTab,
                            transitionSpec = {
                                fadeIn() togetherWith fadeOut()
                            },
                            label = "screen_transition"
                        ) { tab ->
                            when (tab) {
                                AppTab.TIMETABLE -> TimetableScreen(viewModel = viewModel)
                                AppTab.ATTENDANCE -> AttendanceScreen(viewModel = viewModel)
                                AppTab.HOME -> HomeScreen(viewModel = viewModel)
                                AppTab.CALCULATOR -> CalculatorScreen(viewModel = viewModel)
                                AppTab.SETTINGS -> SettingsScreen(viewModel = viewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CustomBottomNavigationBar(
    selectedTab: AppTab,
    onTabSelected: (AppTab) -> Unit
) {
    Surface(
        color = SurfaceColor,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier
            .fillMaxWidth()
            .border(1.dp, BorderColor, RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars) // Protect notch gesture pills on raw screens
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            // Render 5 items. Center (HOME) is visually highlighted
            NavBarItem(
                tab = AppTab.TIMETABLE,
                icon = Icons.Filled.DateRange,
                label = "Timetable",
                selected = selectedTab == AppTab.TIMETABLE,
                onSelected = onTabSelected
            )

            NavBarItem(
                tab = AppTab.ATTENDANCE,
                icon = Icons.Filled.HowToReg,
                label = "Attendance",
                selected = selectedTab == AppTab.ATTENDANCE,
                onSelected = onTabSelected
            )

            // Center highlighted FAB action
            NavBarEmphasizedItem(
                tab = AppTab.HOME,
                icon = Icons.Filled.Home,
                selected = selectedTab == AppTab.HOME,
                onSelected = onTabSelected
            )

            NavBarItem(
                tab = AppTab.CALCULATOR,
                icon = Icons.Filled.Calculate,
                label = "Calculator",
                selected = selectedTab == AppTab.CALCULATOR,
                onSelected = onTabSelected
            )

            NavBarItem(
                tab = AppTab.SETTINGS,
                icon = Icons.Filled.Settings,
                label = "Settings",
                selected = selectedTab == AppTab.SETTINGS,
                onSelected = onTabSelected
            )
        }
    }
}

@Composable
fun RowScope.NavBarItem(
    tab: AppTab,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onSelected: (AppTab) -> Unit
) {
    val iconColor = if (selected) PrimaryColor else TextSecondaryColor
    val textColor = if (selected) Color.White else TextSecondaryColor
    
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onSelected(tab) }
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            color = textColor,
            fontSize = 10.sp,
            fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Medium
        )
    }
}

@Composable
fun NavBarEmphasizedItem(
    tab: AppTab,
    icon: ImageVector,
    selected: Boolean,
    onSelected: (AppTab) -> Unit
) {
    Box(
        modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(if (selected) PrimaryColor else CardSurfaceColor)
            .border(
                width = 2.dp,
                color = if (selected) Color.White else BorderColor,
                shape = CircleShape
            )
            .clickable { onSelected(tab) }
            .testTag("home_navigation_tab"),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = "Home tab",
            tint = if (selected) Color.Black else PrimaryColor,
            modifier = Modifier.size(24.dp)
        )
    }
}
