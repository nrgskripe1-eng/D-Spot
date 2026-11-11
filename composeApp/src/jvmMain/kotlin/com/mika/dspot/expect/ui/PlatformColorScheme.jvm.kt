package com.mika.dspot.expect.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? = null

actual fun isWallpaperDynamicColorSupported(): Boolean = false

@Composable
actual fun SystemBarAppearanceEffect(isDark: Boolean) {
    // No system bars to adapt on desktop.
}

actual fun platformBackgroundColor(): Color = Color.Unspecified
actual fun platformSurfaceColor(): Color = Color.Unspecified
actual fun platformSurfaceVariantColor(): Color = Color.Unspecified
actual fun platformDividerColor(): Color = Color.Unspecified
actual fun platformNavIndicatorColor(): Color = Color.Unspecified
actual fun isOledTheme(): Boolean = false
