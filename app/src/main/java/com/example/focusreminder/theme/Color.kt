package com.example.focusreminder.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================
// Focus & Reminder: Dual-Mode Neumorphic Design System
// Light: Porcelain (#E0E5EC) with "In-Between" Balanced Depth
// Dark: Obsidian Slate (#181C24) with Deep Physical Soft UI
// ============================================================

data class NeuColors(
    val isDark: Boolean,
    val background: Color,
    val surface: Color,
    val surfaceSunken: Color,
    val shadowLight: Color,
    val shadowDark: Color,
    val innerShadowTop: Color,
    val innerShadowBottom: Color,
    val bevelBorder: Brush,
    val insetBevelBorder: Brush,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val textOnEmerald: Color,
    val track: Color,
    val emeraldPrimary: Color,
    val emeraldDark: Color,
    val emeraldLight: Color,
    val forestContainer: Color,
    val forestContainerSubtle: Color,
    val warningAmber: Color,
    val errorRed: Color
)

// --- LIGHT PORCELAIN (Calibrated "In-Between" Depth) ---
val LightNeuColors = NeuColors(
    isDark = false,
    background = Color(0xFFE0E5EC),
    surface = Color(0xFFE0E5EC),
    surfaceSunken = Color(0xFFD6DDE7), // 10 units deeper than surface: visible step without gray mud
    shadowLight = Color(0xFFFFFFFF),
    shadowDark = Color(0x667D8FA6),
    innerShadowTop = Color(0x40506278), // 25% opacity: noticeable soft drop shadow
    innerShadowBottom = Color(0x55FFFFFF), // 33% ambient floor highlight bounce
    bevelBorder = Brush.linearGradient(
        colors = listOf(
            Color(0x90FFFFFF), // Top-left light reflection
            Color(0x209BAAC0)  // Bottom-right subtle edge
        )
    ),
    insetBevelBorder = Brush.linearGradient(
        colors = listOf(
            Color(0x35506278), // Top-left soft dark crease
            Color(0x10506278),
            Color(0x40FFFFFF)  // Bottom-right soft light floor rim
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    ),
    textPrimary = Color(0xFF1E293B),
    textSecondary = Color(0xFF475569),
    textMuted = Color(0xFF64748B),
    textOnEmerald = Color(0xFFFFFFFF),
    track = Color(0xFFCCD5E2),
    emeraldPrimary = Color(0xFF059669),
    emeraldDark = Color(0xFF047857),
    emeraldLight = Color(0xFF10B981),
    forestContainer = Color(0xFFD1FAE5),
    forestContainerSubtle = Color(0xFFE6F4EA),
    warningAmber = Color(0xFFD97706),
    errorRed = Color(0xFFE11D48)
)

// --- DARK OBSIDIAN (Lighter Surfaces with Pure Dark Shadows, Zero White Shadows) ---
val DarkNeuColors = NeuColors(
    isDark = true,
    background = Color(0xFF131720),      // Deep Charcoal Slate canvas
    surface = Color(0xFF202633),         // Lighter surface overall (natural layer elevation)
    surfaceSunken = Color(0xFF141821),   // Recessed sunken cavity (darker than card surface)
    shadowLight = Color.Transparent,     // ZERO white shadow in opposite corner!
    shadowDark = Color(0x99000000),      // Pure dark shadow, darker than the card as normal
    innerShadowTop = Color(0x80000000),  // Natural dark inner shadow for deboss
    innerShadowBottom = Color.Transparent, // ZERO white glare
    bevelBorder = Brush.linearGradient(
        colors = listOf(
            Color(0x22FFFFFF), // Subtle top-left hairline rim
            Color(0x08FFFFFF)  // Faint bottom-right rim
        )
    ),
    insetBevelBorder = Brush.linearGradient(
        colors = listOf(
            Color(0x50000000), // Top-left inner shadow crease
            Color(0x20000000),
            Color.Transparent  // Zero white in opposite corner
        ),
        start = androidx.compose.ui.geometry.Offset(0f, 0f),
        end = androidx.compose.ui.geometry.Offset(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY)
    ),
    textPrimary = Color(0xFFF1F5F9),     // Crisp off-white (Slate 100)
    textSecondary = Color(0xFF94A3B8),   // Slate 400
    textMuted = Color(0xFF64748B),       // Slate 500
    textOnEmerald = Color(0xFFFFFFFF),
    track = Color(0xFF10131A),
    emeraldPrimary = Color(0xFF10B981),   // Glowing Neon Emerald
    emeraldDark = Color(0xFF34D399),      // Mint accent for high readability on dark
    emeraldLight = Color(0xFF6EE7B7),
    forestContainer = Color(0xFF064E3B),
    forestContainerSubtle = Color(0xFF065F46),
    warningAmber = Color(0xFFF59E0B),
    errorRed = Color(0xFFFB7185)
)

val LocalNeuColors = staticCompositionLocalOf { LightNeuColors }

// Dynamic Composable Getters (Seamlessly support both Light & Dark mode everywhere)
val NeuBackground: Color @Composable get() = LocalNeuColors.current.background
val NeuSurface: Color @Composable get() = LocalNeuColors.current.surface
val NeuSurfaceSunken: Color @Composable get() = LocalNeuColors.current.surfaceSunken
val NeuShadowLight: Color @Composable get() = LocalNeuColors.current.shadowLight
val NeuShadowDark: Color @Composable get() = LocalNeuColors.current.shadowDark
val NeuSunkenTrack: Color @Composable get() = LocalNeuColors.current.track
val EmeraldPrimary: Color @Composable get() = LocalNeuColors.current.emeraldPrimary
val EmeraldDark: Color @Composable get() = LocalNeuColors.current.emeraldDark
val EmeraldLight: Color @Composable get() = LocalNeuColors.current.emeraldLight
val ForestContainer: Color @Composable get() = LocalNeuColors.current.forestContainer
val ForestContainerSubtle: Color @Composable get() = LocalNeuColors.current.forestContainerSubtle
val TextPrimary: Color @Composable get() = LocalNeuColors.current.textPrimary
val TextSecondary: Color @Composable get() = LocalNeuColors.current.textSecondary
val TextMuted: Color @Composable get() = LocalNeuColors.current.textMuted
val TextOnEmerald: Color @Composable get() = LocalNeuColors.current.textOnEmerald
val WarningAmber: Color @Composable get() = LocalNeuColors.current.warningAmber
val ErrorRed: Color @Composable get() = LocalNeuColors.current.errorRed
val NeuBevelBorder: Brush @Composable get() = LocalNeuColors.current.bevelBorder
val NeuInsetBevelBorder: Brush @Composable get() = LocalNeuColors.current.insetBevelBorder

// Legacy compatibility aliases
val ObsidianBackground: Color @Composable get() = NeuBackground
val ObsidianSurface: Color @Composable get() = NeuSurface
val GlassSurfaceDark: Color @Composable get() = NeuSurface
val GlassSurfaceCard: Color @Composable get() = NeuSurface
val GlassMaterialBackground: Color @Composable get() = NeuSurface
val GlassBorderEmerald: Color @Composable get() = Color(0x3310B981)
val GlassBorderSubtle: Color @Composable get() = Color(0x2210B981)
val WarningAmberContainer: Color @Composable get() = Color(0xFFFEF3C7)
