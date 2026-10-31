package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val IptvDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = Color.Black,
    primaryContainer = CyanPrimaryVariant.copy(alpha = 0.2f),
    onPrimaryContainer = CyanPrimary,
    secondary = VioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = VioletSecondary.copy(alpha = 0.25f),
    onSecondaryContainer = Color.White,
    tertiary = AmberAccent,
    onTertiary = Color.Black,
    background = DarkBg,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = DarkCardBorder,
    error = RedAccent,
    onError = Color.White
)

@Composable
fun NovaIptvTheme(
    darkTheme: Boolean = true, // Force dark theme for IPTV cinematic experience
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = IptvDarkColorScheme,
        typography = Typography,
        content = content
    )
}
