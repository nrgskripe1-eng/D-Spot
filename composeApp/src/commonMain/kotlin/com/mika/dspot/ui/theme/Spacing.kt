package com.mika.dspot.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 4/8dp baseline spacing design tokens.
 * Strict system tokens for layout and padding.
 */
@Immutable
data class Spacing(
    val none: Dp = 0.dp,
    val extraSmall: Dp = 4.dp,    // 4dp micro-spacing
    val small: Dp = 8.dp,         // 8dp baseline tight
    val medium: Dp = 12.dp,       // 12dp standard component gap
    val large: Dp = 16.dp,        // 16dp screen gutter & card padding
    val extraLarge: Dp = 24.dp,   // 24dp section spacing
    val huge: Dp = 32.dp,         // 32dp major section rhythm
    val massive: Dp = 48.dp,      // 48dp touch targets & hero spacing
)

val LocalSpacing = staticCompositionLocalOf { Spacing() }
