package com.example.focusreminder.theme

import androidx.compose.ui.graphics.Color

// ==========================================
// Focus & Reminder: High-Contrast Neumorphic Palette
// Gold Standard #E0E5EC Canvas for Deep Dual Shadows
// ==========================================

// Neumorphic Surface & Background (Sweet-spot #E0E5EC)
val NeuBackground = Color(0xFFE0E5EC)
val NeuSurface = Color(0xFFE0E5EC) // Identical to background for true physical extrusion
val NeuShadowLight = Color(0xFFFFFFFF)
val NeuShadowDark = Color(0xFF9BAAC0)
val NeuSunkenTrack = Color(0xFFD1D9E6)

// Emerald Accents
val EmeraldPrimary = Color(0xFF059669)
val EmeraldLight = Color(0xFF10B981)
val EmeraldDark = Color(0xFF047857)
val EmeraldNeon = Color(0xFF10B981)
val ForestContainer = Color(0xFFD1FAE5)
val ForestContainerSubtle = Color(0xFFE6F4EA)

// Deep Charcoal / Slate Typography for High Legibility
val TextPrimary = Color(0xFF1E293B)     // Slate 800 - Crisp, clear, high contrast
val TextSecondary = Color(0xFF475569)   // Slate 600 - Readable secondary
val TextMuted = Color(0xFF64748B)       // Slate 500 - Metadata & timestamps
val TextOnEmerald = Color(0xFFFFFFFF)

// Legacy compatibility aliases
val ObsidianBackground = NeuBackground
val ObsidianSurface = NeuSurface
val GlassSurfaceDark = NeuSurface
val GlassSurfaceCard = NeuSurface
val GlassMaterialBackground = NeuSurface
val GlassBorderEmerald = Color(0x3310B981)
val GlassBorderSubtle = Color(0x2210B981)

// Status & Warnings
val WarningAmber = Color(0xFFD97706)
val WarningAmberContainer = Color(0xFFFEF3C7)
val ErrorRed = Color(0xFFE11D48)
