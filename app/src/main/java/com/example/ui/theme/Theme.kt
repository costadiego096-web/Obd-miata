package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Mx5Amber,
    onPrimary = Color.Black,
    primaryContainer = Mx5AmberDark,
    onPrimaryContainer = Color.White,
    secondary = RaceRed,
    onSecondary = Color.White,
    secondaryContainer = CockpitSurfaceVariant,
    onSecondaryContainer = Mx5AmberLight,
    tertiary = TrackGreen,
    onTertiary = Color.Black,
    background = CockpitBlack,
    onBackground = TextPrimary,
    surface = CockpitSurface,
    onSurface = TextPrimary,
    surfaceVariant = CockpitSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = CockpitOutline,
    error = RaceRed,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // OBD and race dashboards are optimized for dark cockpit aesthetics
    dynamicColor: Boolean = false, // Keep high-contrast automotive color scheme
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
