package com.example.focusreminder.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*

/**
 * Global Theme Manager for switching between Light Porcelain and Dark Obsidian.
 */
object ThemeManager {
    var isDarkTheme by mutableStateOf(false)

    fun toggleTheme() {
        isDarkTheme = !isDarkTheme
    }
}

private val LightColorScheme = lightColorScheme(
    primary = LightNeuColors.emeraldPrimary,
    onPrimary = LightNeuColors.textOnEmerald,
    primaryContainer = LightNeuColors.forestContainer,
    onPrimaryContainer = LightNeuColors.emeraldDark,
    secondary = LightNeuColors.emeraldDark,
    onSecondary = LightNeuColors.textOnEmerald,
    secondaryContainer = LightNeuColors.forestContainerSubtle,
    onSecondaryContainer = LightNeuColors.emeraldDark,
    tertiary = LightNeuColors.emeraldLight,
    onTertiary = LightNeuColors.textPrimary,
    background = LightNeuColors.background,
    onBackground = LightNeuColors.textPrimary,
    surface = LightNeuColors.surface,
    onSurface = LightNeuColors.textPrimary,
    surfaceVariant = LightNeuColors.surface,
    onSurfaceVariant = LightNeuColors.textSecondary,
    outline = LightNeuColors.shadowDark,
    outlineVariant = LightNeuColors.track,
    error = LightNeuColors.errorRed,
    onError = LightNeuColors.textOnEmerald
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkNeuColors.emeraldPrimary,
    onPrimary = DarkNeuColors.textOnEmerald,
    primaryContainer = DarkNeuColors.forestContainer,
    onPrimaryContainer = DarkNeuColors.emeraldDark,
    secondary = DarkNeuColors.emeraldDark,
    onSecondary = DarkNeuColors.textOnEmerald,
    secondaryContainer = DarkNeuColors.forestContainerSubtle,
    onSecondaryContainer = DarkNeuColors.emeraldDark,
    tertiary = DarkNeuColors.emeraldLight,
    onTertiary = DarkNeuColors.textPrimary,
    background = DarkNeuColors.background,
    onBackground = DarkNeuColors.textPrimary,
    surface = DarkNeuColors.surface,
    onSurface = DarkNeuColors.textPrimary,
    surfaceVariant = DarkNeuColors.surface,
    onSurfaceVariant = DarkNeuColors.textSecondary,
    outline = DarkNeuColors.shadowDark,
    outlineVariant = DarkNeuColors.track,
    error = DarkNeuColors.errorRed,
    onError = DarkNeuColors.textOnEmerald
)

@Composable
fun FocusReminderTheme(
    darkTheme: Boolean = ThemeManager.isDarkTheme,
    content: @Composable () -> Unit,
) {
    val neuColors = if (darkTheme) DarkNeuColors else LightNeuColors
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    CompositionLocalProvider(LocalNeuColors provides neuColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
