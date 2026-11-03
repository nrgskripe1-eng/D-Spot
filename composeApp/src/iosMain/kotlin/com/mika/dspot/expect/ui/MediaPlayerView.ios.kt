package com.mika.dspot.expect.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import com.mika.domain.data.model.metadata.Lyrics
import com.mika.domain.data.model.streams.TimeLine

@Composable
actual fun MediaPlayerView(
    url: String,
    modifier: Modifier,
    cropToBounds: Boolean,
    onFirstFrame: () -> Unit,
) {
    Box(modifier = modifier)
}

@Composable
actual fun MediaPlayerViewWithSubtitle(
    modifier: Modifier,
    playerName: String,
    shouldPip: Boolean,
    shouldShowSubtitle: Boolean,
    shouldScaleDownSubtitle: Boolean,
    isInPipMode: Boolean,
    timelineState: TimeLine,
    lyricsData: Lyrics?,
    translatedLyricsData: Lyrics?,
    mainTextStyle: TextStyle,
    translatedTextStyle: TextStyle,
) {
    Box(modifier = modifier)
}

@Composable
actual fun rememberVideoAspectRatio(playerName: String): Float? = 16f / 9f

