package com.example.focusreminder.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val NeumorphicColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = TextOnEmerald,
    primaryContainer = ForestContainer,
    onPrimaryContainer = EmeraldDark,
    secondary = EmeraldDark,
    onSecondary = TextOnEmerald,
    secondaryContainer = ForestContainerSubtle,
    onSecondaryContainer = EmeraldDark,
    tertiary = EmeraldLight,
    onTertiary = TextPrimary,
    background = NeuBackground,
    onBackground = TextPrimary,
    surface = NeuSurface,
    onSurface = TextPrimary,
    surfaceVariant = NeuSurface,
    onSurfaceVariant = TextSecondary,
    outline = NeuShadowDark,
    outlineVariant = NeuSunkenTrack,
    error = ErrorRed,
    onError = TextOnEmerald
)

@Composable
fun FocusReminderTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = NeumorphicColorScheme,
        typography = Typography,
        content = content
    )
}
