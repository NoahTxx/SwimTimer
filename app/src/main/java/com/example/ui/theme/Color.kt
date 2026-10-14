package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// High-contrast poolside dark theme palette
val AquaPrimary = Color(0xFF00E5FF)       // Electric Cyan - highest contrast in sunlight/halls
val AquaOnPrimary = Color(0xFF00363A)
val AquaPrimaryContainer = Color(0xFF004D40)
val AquaOnPrimaryContainer = Color(0xFF80DEEA)

val DeepOceanBackground = Color(0xFF070E18) // Ultra-deep navy black
val OceanSurface = Color(0xFF101B2B)        // High contrast card surface
val OceanSurfaceVariant = Color(0xFF182940) // Secondary container
val OceanBorder = Color(0xFF264060)

val TextWhite = Color(0xFFF0F6FC)
val TextSecondary = Color(0xFF90A4AE)
val TextMuted = Color(0xFF607D8B)

// Timing & Pace performance indicators
val PaceGreen = Color(0xFF00E676)           // Faster than target pace
val PaceGreenContainer = Color(0xFF003816)
val PaceRed = Color(0xFFFF1744)             // Slower than target pace
val PaceRedContainer = Color(0xFF420B13)
val PaceNeutral = Color(0xFFFFD600)         // On pace / yellow accent
val PaceNeutralContainer = Color(0xFF382E00)

// Water lock accent
val WaterLockBlue = Color(0xFF00B0FF)
val WaterLockDark = Color(0xEB040B14)

// Lane distinct color tags (for up to 15 swimmers/lanes)
val LaneColors = listOf(
    Color(0xFF00E5FF), // Lane 1: Cyan
    Color(0xFFFF5252), // Lane 2: Coral
    Color(0xFFFFEB3B), // Lane 3: Yellow
    Color(0xFF69F0AE), // Lane 4: Mint
    Color(0xFFE040FB), // Lane 5: Magenta
    Color(0xFFFF9100), // Lane 6: Orange
    Color(0xFF40C4FF), // Lane 7: Sky
    Color(0xFFB388FF), // Lane 8: Lavender
    Color(0xFFAEEA00), // Lane 9: Lime
    Color(0xFFFF4081), // Lane 10: Rose Pink
    Color(0xFF00E676), // Lane 11: Emerald
    Color(0xFFFFD600), // Lane 12: Amber
    Color(0xFF7C4DFF), // Lane 13: Electric Violet
    Color(0xFFFF6E40), // Lane 14: Sunset Coral
    Color(0xFF1DE9B6)  // Lane 15: Bright Teal
)
