package com.solanki.myapplication.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import android.os.Build

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF64B5F6),        // Richer, more vibrant blue
    onPrimary = Color(0xFF0D1117),      // Contrast against primary
    secondary = Color(0xFF90CAF9),
    onSecondary = Color(0xFF0D1117),
    tertiary = Color(0xFF80CBC4),
    background = Color(0xFF0D1117),     // Premium deep navy-grey (Midnight)
    onBackground = Color(0xFFE6EDF3),   // Clean off-white for text
    surface = Color(0xFF161B22),        // Subtle elevation for cards
    onSurface = Color(0xFFE6EDF3),
    surfaceVariant = Color(0xFF21262D),  // For input fields/secondary surfaces
    onSurfaceVariant = Color(0xFF8B949E), // Soft muted text
    outline = Color(0xFF30363D),        // Subtle borders
    primaryContainer = Color(0xFF1F6FEB),
    onPrimaryContainer = Color.White
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1976D2),
    onPrimary = Color.White,
    secondary = Color(0xFF42A5F5),
    onSecondary = Color.White,
    tertiary = Color(0xFF00796B),
    background = Color(0xFFF0F2F5),
    onBackground = Color(0xFF1C1E21),
    surface = Color.White,
    onSurface = Color.Black,
    surfaceVariant = Color(0xFFE4E6EB),
    onSurfaceVariant = Color(0xFF4B4B4B),
    outline = Color(0xFFCCD0D5)
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, 
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}