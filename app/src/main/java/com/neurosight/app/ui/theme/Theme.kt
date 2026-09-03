package com.neurosight.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NeuroSightColors = darkColorScheme(
    primary = Color(0xFF38BDF8),
    secondary = Color(0xFF22D3EE),
    background = Color(0xFF0B0F14),
    surface = Color(0xFF0B0F14),
    onPrimary = Color(0xFF0B0F14),
    onBackground = Color(0xFFE2E8F0),
    onSurface = Color(0xFFE2E8F0)
)

/** Simple always-dark theme -- fits a camera-overlay UI best and keeps things demo-simple. */
@Composable
fun NeuroSightTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NeuroSightColors,
        content = content
    )
}
