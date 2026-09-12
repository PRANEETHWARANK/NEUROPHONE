package com.neurophone.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = NeuroPrimary,
    onPrimary = NeuroOnPrimary,
    primaryContainer = Color(0xFF4C1D95),
    onPrimaryContainer = Color(0xFFEDE9FE),
    secondary = NeuroSecondary,
    onSecondary = Color(0xFF003C4A),
    background = NeuroBackground,
    onBackground = NeuroOnBackground,
    surface = NeuroSurface,
    onSurface = NeuroOnSurface,
    surfaceVariant = NeuroSurfaceVariant,
    error = NeuroError,
    outline = Color(0xFF3A3A55)
)

private val LightColorScheme = lightColorScheme(
    primary = NeuroPrimaryLight,
    onPrimary = NeuroOnPrimary,
    primaryContainer = Color(0xFFEDE9FE),
    onPrimaryContainer = Color(0xFF4C1D95),
    secondary = Color(0xFF0284C7),
    onSecondary = Color(0xFFFFFFFF),
    background = NeuroBackgroundLight,
    onBackground = NeuroOnBackgroundLight,
    surface = NeuroSurfaceLight,
    onSurface = NeuroOnBackgroundLight,
    surfaceVariant = NeuroCardLight,
    error = NeuroError,
    outline = Color(0xFFD1D5DB)
)

@Composable
fun NeuroPhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}