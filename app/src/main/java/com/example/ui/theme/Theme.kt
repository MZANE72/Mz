package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.local.PlaylistConfig

private val ZanaDarkColorScheme = darkColorScheme(
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

private val ZanaLightColorScheme = lightColorScheme(
    primary = LightCyanPrimary,
    onPrimary = Color.White,
    primaryContainer = LightCyanPrimary.copy(alpha = 0.15f),
    onPrimaryContainer = LightCyanPrimary,
    secondary = VioletSecondary,
    onSecondary = Color.White,
    secondaryContainer = VioletSecondary.copy(alpha = 0.15f),
    onSecondaryContainer = VioletSecondary,
    tertiary = AmberAccent,
    onTertiary = Color.Black,
    background = LightBg,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightCardBorder,
    error = RedAccent,
    onError = Color.White
)

@Composable
fun ZanaIptvTheme(
    themeMode: String = PlaylistConfig.THEME_DARK,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        PlaylistConfig.THEME_SYSTEM -> systemInDark
        else -> true // THEME_DARK by default
    }

    val colorScheme = if (isDark) ZanaDarkColorScheme else ZanaLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun NovaIptvTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    ZanaIptvTheme(themeMode = if (darkTheme) PlaylistConfig.THEME_DARK else PlaylistConfig.THEME_SYSTEM, content = content)
}
