package com.mika.dspot.expect.ui

import androidx.compose.runtime.Composable

@Composable
internal actual fun PlatformBackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
) {
    // iOS uses native swipe-to-go-back gestures.
}

