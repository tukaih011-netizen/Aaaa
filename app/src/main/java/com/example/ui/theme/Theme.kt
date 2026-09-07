package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val TimePassDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = VoidDark,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = CyanPrimary,
    secondary = CyanSecondary,
    onSecondary = VoidDark,
    secondaryContainer = SurfaceHighlight,
    onSecondaryContainer = TextPrimary,
    tertiary = NeonGreen,
    onTertiary = VoidDark,
    background = VoidDark,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = CardBorder,
    outlineVariant = GridLine,
    error = LaserCrimson,
    onError = Color.White
)

private val TimePassLightColorScheme = darkColorScheme( // Keep default aesthetic futuristic dark even in light mode, with slight adjustment
    primary = CyanPrimary,
    onPrimary = VoidDark,
    primaryContainer = SurfaceElevated,
    onPrimaryContainer = CyanPrimary,
    secondary = CyanSecondary,
    onSecondary = VoidDark,
    background = VoidDark,
    surface = SurfaceDark,
    onSurface = TextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = TimePassDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                it.statusBarColor = VoidDark.toArgb()
                it.navigationBarColor = VoidDark.toArgb()
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = false
                    isAppearanceLightNavigationBars = false
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

