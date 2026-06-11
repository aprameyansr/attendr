package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AttendlyColorScheme = darkColorScheme(
    primary = PrimaryColor,
    onPrimary = Color.Black,
    primaryContainer = PrimaryDark,
    onPrimaryContainer = Color.White,
    background = BackgroundColor,
    onBackground = TextPrimaryColor,
    surface = SurfaceColor,
    onSurface = TextPrimaryColor,
    surfaceVariant = CardSurfaceColor,
    onSurfaceVariant = TextSecondaryColor,
    outline = BorderColor,
    error = ErrorColor,
    secondary = PrimaryColor,
    onSecondary = Color.Black
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Locked to dark-mode-first Attendly experience
    dynamicColor: Boolean = false, // Preserve strict cohesive branding palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = AttendlyColorScheme,
        typography = Typography,
        content = content
    )
}
