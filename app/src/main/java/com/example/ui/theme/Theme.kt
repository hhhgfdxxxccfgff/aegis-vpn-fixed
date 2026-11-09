package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AegisDarkColorScheme = darkColorScheme(
    primary = AegisCyanPrimary,
    onPrimary = AegisOnCyan,
    primaryContainer = AegisCyanContainer,
    onPrimaryContainer = AegisCyanSecondary,
    secondary = AegisCyanSecondary,
    onSecondary = Color(0xFF001F29),
    tertiary = AegisEmerald,
    onTertiary = Color.White,
    background = AegisDarkBg,
    onBackground = AegisTextPrimary,
    surface = AegisDarkSurface,
    onSurface = AegisTextPrimary,
    surfaceVariant = AegisDarkSurfaceVariant,
    onSurfaceVariant = AegisTextSecondary,
    outline = AegisDarkCardBorder,
    error = AegisRose,
    onError = Color.White
)

private val AegisLightColorScheme = lightColorScheme(
    primary = Color(0xFF0284C7),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE0F2FE),
    onPrimaryContainer = Color(0xFF0369A1),
    secondary = Color(0xFF0EA5E9),
    onSecondary = Color.White,
    tertiary = Color(0xFF059669),
    onTertiary = Color.White,
    background = AegisLightBg,
    onBackground = AegisLightTextPrimary,
    surface = AegisLightSurface,
    onSurface = AegisLightTextPrimary,
    surfaceVariant = AegisLightSurfaceVariant,
    onSurfaceVariant = AegisLightTextSecondary,
    outline = AegisLightCardBorder,
    error = AegisRose,
    onError = Color.White
)

@Composable
fun AegisVpnTheme(
    themePreference: String = "SYSTEM",
    content: @Composable () -> Unit
) {
    val darkTheme = when (themePreference) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = if (darkTheme) AegisDarkColorScheme else AegisLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
