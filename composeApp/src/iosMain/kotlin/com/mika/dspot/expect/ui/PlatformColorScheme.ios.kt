package com.mika.dspot.expect.ui

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

@Composable
actual fun platformDynamicColorScheme(isDark: Boolean): ColorScheme? = null

actual fun isWallpaperDynamicColorSupported(): Boolean = false

@Composable
actual fun SystemBarAppearanceEffect(isDark: Boolean) {}

actual fun platformBackgroundColor(): Color = Color(0xFF000000)
actual fun platformSurfaceColor(): Color = Color(0xFF0A0A0A)
actual fun platformSurfaceVariantColor(): Color = Color(0xFF0D0D0D)
actual fun platformDividerColor(): Color = Color(0xFF111111)
actual fun platformNavIndicatorColor(): Color = Color.Transparent
actual fun isOledTheme(): Boolean = true

