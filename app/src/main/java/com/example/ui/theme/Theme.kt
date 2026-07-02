package com.example.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val CosmicDarkColorScheme = darkColorScheme(
    primary = CyberCrimson,
    secondary = NeonCyan,
    tertiary = BrightYellow,
    background = TheaterBlack,
    surface = SlateGrayElevation,
    onPrimary = TextWhitePrimary,
    onSecondary = TheaterBlack,
    onBackground = TextWhitePrimary,
    onSurface = TextWhitePrimary,
    surfaceVariant = GlassOverlay,
    outline = BorderDark
)

// A high-contrast safe fallback light scheme matching dark tones and highlights
private val CinematicLightColorScheme = lightColorScheme(
    primary = Color(0xFF00BAD6), // Vivid Cyan-Blue from screenshot
    secondary = Color(0xFFFF3850), // Hot Red
    tertiary = Color(0xFFFF9800), // Orange/Gold
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF1A1A1A), // Dark charcoal text
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F6F8), // Warm light gray for tags/background inputs
    outline = Color(0xFFE2E4E8) // Soft outline
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = false, // Default to false (Cinematic Light Theme) for a clean, bright Light Mode experience
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CosmicDarkColorScheme else CinematicLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
