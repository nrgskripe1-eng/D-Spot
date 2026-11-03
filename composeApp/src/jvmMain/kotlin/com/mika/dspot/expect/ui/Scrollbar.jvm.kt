package com.mika.dspot.expect.ui

import androidx.compose.foundation.HorizontalScrollbar
import androidx.compose.foundation.ScrollbarStyle
import androidx.compose.foundation.gestures.FlingBehavior
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.rememberScrollbarAdapter
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.mika.dspot.Platform
import com.mika.dspot.getPlatform

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip

private val miniCapsuleStyle =
    ScrollbarStyle(
        minimalHeight = 16.dp,
        thickness = 3.dp,
        shape = RoundedCornerShape(10.dp),
        hoverDurationMillis = 200,
        unhoverColor = Color.White.copy(alpha = 0.5f),
        hoverColor = Color(0xFF8ECAE6),
    )

/**
 * Dragging the thumb sets the scroll position directly — no fling — so a snapping list or grid would
 * stop between items. The thumb reports its drag on the returned source; when it is let go, a
 * zero-velocity run of the container's own snap fling settles on the nearest item.
 */
@Composable
private fun rememberSnapOnThumbRelease(
    scrollState: ScrollableState,
    flingBehavior: FlingBehavior?,
): MutableInteractionSource {
    val interactionSource = remember { MutableInteractionSource() }
    if (flingBehavior != null) {
        LaunchedEffect(interactionSource, scrollState, flingBehavior) {
            interactionSource.interactions.collect { interaction ->
                if (interaction is DragInteraction.Stop || interaction is DragInteraction.Cancel) {
                    scrollState.scroll { with(flingBehavior) { performFling(0f) } }
                }
            }
        }
    }
    return interactionSource
}

@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyListState,
    flingBehavior: FlingBehavior?,
) {
    val interactionSource = rememberSnapOnThumbRelease(scrollState, flingBehavior)
    if (getPlatform() == Platform.Desktop) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 2.dp, vertical = 2.dp),
            ) {
                HorizontalScrollbar(
                    modifier = Modifier.width(64.dp),
                    style = miniCapsuleStyle,
                    adapter =
                        rememberScrollbarAdapter(
                            scrollState = scrollState,
                        ),
                    interactionSource = interactionSource,
                )
            }
        }
    }
}

@Composable
actual fun HorizontalScrollBar(
    modifier: Modifier,
    scrollState: LazyGridState,
    flingBehavior: FlingBehavior?,
) {
    val measuredHorizontal by remember(scrollState) {
        derivedStateOf { scrollState.layoutInfo.orientation == Orientation.Horizontal }
    }
    val interactionSource = rememberSnapOnThumbRelease(scrollState, flingBehavior)
    if (getPlatform() == Platform.Desktop && measuredHorizontal) {
        Box(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 4.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .width(68.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 2.dp, vertical = 2.dp),
            ) {
                HorizontalScrollbar(
                    modifier = Modifier.width(64.dp),
                    style = miniCapsuleStyle,
                    adapter = rememberScrollbarAdapter(scrollState = scrollState),
                    interactionSource = interactionSource,
                )
            }
        }
    }
}
