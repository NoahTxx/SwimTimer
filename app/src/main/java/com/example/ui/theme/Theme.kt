package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PoolsideDarkColorScheme = darkColorScheme(
    primary = AquaPrimary,
    onPrimary = AquaOnPrimary,
    primaryContainer = AquaPrimaryContainer,
    onPrimaryContainer = AquaOnPrimaryContainer,
    secondary = AquaPrimary,
    onSecondary = Color.Black,
    secondaryContainer = OceanSurfaceVariant,
    onSecondaryContainer = TextWhite,
    tertiary = PaceGreen,
    onTertiary = Color.Black,
    tertiaryContainer = PaceGreenContainer,
    onTertiaryContainer = PaceGreen,
    background = DeepOceanBackground,
    onBackground = TextWhite,
    surface = OceanSurface,
    onSurface = TextWhite,
    surfaceVariant = OceanSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OceanBorder,
    error = PaceRed,
    onError = Color.White,
    errorContainer = PaceRedContainer,
    onErrorContainer = PaceRed,
)

private val PoolsideLightColorScheme = lightColorScheme(
    primary = Color(0xFF006874),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF97F0FF),
    onPrimaryContainer = Color(0xFF001F24),
    secondary = Color(0xFF006590),
    onSecondary = Color.White,
    background = Color(0xFFF6FAFD),
    onBackground = Color(0xFF191C1E),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C1E),
    surfaceVariant = Color(0xFFDEE4E7),
    onSurfaceVariant = Color(0xFF3F484B),
    outline = Color(0xFF70797C),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
)

@Composable
fun SwimTimerTheme(
    darkTheme: Boolean = true, // Default to High-Contrast Dark for pool coaching
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) PoolsideDarkColorScheme else PoolsideLightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = DeepOceanBackground.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = DeepOceanBackground.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
