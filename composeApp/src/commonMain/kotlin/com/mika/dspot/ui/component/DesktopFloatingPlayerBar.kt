package com.mika.dspot.ui.component

// ─────────────────────────────────────────────────────────────────────────────
// DesktopFloatingPlayerBar
//
// Apple-influenced floating player bar for D-Spot desktop.
// Solid #1C1C1E surface, 14dp corners, 1dp #3A3A3C border, 10dp elevation.
// Album art (52x52dp, 9dp corners) overflows 19dp above the bar top edge;
// tilts -2.5 degrees at rest, rotates to 0 on hover (250ms ease).
// Main row (56dp): 66dp spacer | title+artist | transport | right-zone.
// Progress strip (24dp): seekable, timestamps, 2dp track + 8dp scrubber on hover.
// Entry animation: translateY(18dp->0) + alpha(0->1), spring(0.7, 300).
// Color tokens: no orange, no gradient, no frosted glass.
// ─────────────────────────────────────────────────────────────────────────────

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mika.domain.data.entities.SongEntity
import com.mika.domain.data.model.streams.TimeLine
import com.mika.domain.mediaservice.handler.ControlState
import com.mika.domain.mediaservice.handler.RepeatState
import com.mika.domain.utils.connectArtists
import com.mika.dspot.extension.elapsedLabel
import com.mika.dspot.extension.lengthLabel
import com.mika.dspot.ui.icon.Close
import com.mika.dspot.ui.icon.Favorite
import com.mika.dspot.ui.icon.FavoriteBorder
import com.mika.dspot.ui.icon.Pause
import com.mika.dspot.ui.icon.PlayArrow
import com.mika.dspot.ui.icon.QueueMusic
import com.mika.dspot.ui.icon.Repeat
import com.mika.dspot.ui.icon.RepeatOne
import com.mika.dspot.ui.icon.Shuffle
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.icon.SkipNext
import com.mika.dspot.ui.icon.SkipPrevious
import com.mika.dspot.ui.icon.VolumeDown
import com.mika.dspot.ui.icon.VolumeOff
import com.mika.dspot.ui.icon.VolumeUp
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.mika.dspot.viewModel.UIEvent

// ── Color tokens ──────────────────────────────────────────────────────────────
private val FpBar    = Color(0xFF1C1C1E)
private val FpBar2   = Color(0xFF252527)
private val FpBorder = Color(0xFF3A3A3C)
private val FpFg     = Color(0xFFF2F2F7)
private val FpFg2    = Color(0xFF8E8E93)
private val FpFg3    = Color(0xFF48484A)
private val FpPlayBg = Color(0xFFF2F2F7)
private val FpPlayFg = Color(0xFF0E0E10)

// ─────────────────────────────────────────────────────────────────────────────
// Public entry point
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Floating player bar — place inside a Box(fillMaxSize()) aligned to BottomCenter.
 *
 * Caller must pass:
 *   Modifier.align(Alignment.BottomCenter).padding(bottom = 20.dp)
 *
 * The 20dp top padding inside this composable gives room for the overflowing album art.
 */
@Composable
fun DesktopFloatingPlayerBar(
    modifier:         Modifier = Modifier,
    songEntity:       SongEntity?,
    controllerState:  ControlState,
    timelineState:    TimeLine,
    onUIEvent:        (UIEvent) -> Unit,
    onClick:          () -> Unit = {},
    onClose:          () -> Unit = {},
    onQueueClick:     () -> Unit = {},
) {
    // ── Entry animation ────────────────────────────────────────────────────────
    val translateY = remember { Animatable(18f) }
    val alpha      = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        val spec = spring<Float>(dampingRatio = 0.7f, stiffness = 300f)
        translateY.animateTo(0f, animationSpec = spec)
        alpha.animateTo(1f, animationSpec = tween(450))
    }

    // ── Seek state ─────────────────────────────────────────────────────────────
    var isSliding   by remember { mutableStateOf(false) }
    var sliderValue by remember { mutableFloatStateOf(0f) }   // 0..1
    LaunchedEffect(timelineState, isSliding) {
        if (!isSliding) {
            sliderValue = if (timelineState.total > 0L)
                timelineState.current.toFloat() / timelineState.total.toFloat()
            else 0f
        }
    }

    val density = LocalDensity.current

    // ── Outer container: 20dp top padding for art overflow, no clip ────────────
    Box(
        modifier = modifier
            .widthIn(max = 660.dp)
            .padding(top = 20.dp)
            .graphicsLayer {
                this.alpha        = alpha.value
                this.translationY = with(density) { translateY.value.dp.toPx() }
            },
    ) {
        // ── Bar surface ────────────────────────────────────────────────────────
        val barShape = RoundedCornerShape(14.dp)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(elevation = 10.dp, shape = barShape, clip = false)
                .clip(barShape)
                .background(FpBar)
                .border(1.dp, FpBorder, barShape),
        ) {
            // Main row — clip = true (inner top row)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .clip(RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .padding(start = 12.dp, end = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 66dp spacer — album art zone
                    Spacer(modifier = Modifier.width(66.dp))

                    // Title + artist (clickable to open NowPlaying)
                    Column(
                        modifier            = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onClick() }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Text(
                            text     = songEntity?.title ?: "",
                            style    = TextStyle(
                                fontSize   = 12.5.sp,
                                fontWeight = FontWeight.W500,
                                color      = FpFg,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text     = songEntity?.artistName?.connectArtists() ?: "",
                            style    = TextStyle(fontSize = 11.sp, color = FpFg2),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Transport: Shuffle Prev Play Next Repeat
                    TransportControls(
                        controllerState = controllerState,
                        onUIEvent       = onUIEvent,
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    // Right zone: Waveform Like Queue Volume | Close
                    RightZone(
                        isPlaying    = controllerState.isPlaying,
                        isLiked      = controllerState.isLiked,
                        volume       = controllerState.volume,
                        onUIEvent    = onUIEvent,
                        onQueueClick = onQueueClick,
                        onClose      = onClose,
                    )
                }
            }

            // Progress strip
            ProgressStrip(
                sliderValue   = sliderValue,
                timelineState = timelineState,
                isSliding     = isSliding,
                onSlideStart  = { isSliding = true },
                onSlideEnd    = { pct ->
                    isSliding   = false
                    sliderValue = pct
                    onUIEvent(UIEvent.UpdateProgress(pct * 100f))
                },
            )
        }

        // Album art — overflows 19dp above bar top
        FloatingAlbumArt(
            thumbnailUrl = songEntity?.thumbnails,
            onClick      = onClick,
            modifier     = Modifier
                .size(52.dp)
                .offset(x = 14.dp, y = (-19).dp),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Album art  52x52dp, 9dp corners, tilts -2.5deg at rest -> 0 on hover
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun FloatingAlbumArt(
    thumbnailUrl: String?,
    onClick:      () -> Unit = {},
    modifier:     Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered   by interaction.collectIsHoveredAsState()

    val rotation by animateFloatAsState(
        targetValue   = if (isHovered) 0f else -2.5f,
        animationSpec = tween(250, easing = FastOutSlowInEasing),
        label         = "FpArtRotation",
    )

    Box(
        modifier = modifier
            .hoverable(interaction)
            .clickable { onClick() }
            .graphicsLayer {
                rotationZ       = rotation
                transformOrigin = TransformOrigin(0.5f, 1f)  // pivot: bottom-center
                shadowElevation = 12.dp.toPx()
                shape           = RoundedCornerShape(9.dp)
                clip            = true
            },
    ) {
        AsyncImage(
            model = ImageRequest.Builder(LocalPlatformContext.current)
                .data(thumbnailUrl)
                .crossfade(400)
                .build(),
            placeholder        = rememberHolderPainter(),
            error              = rememberHolderPainter(),
            contentDescription = null,
            contentScale       = ContentScale.Crop,
            modifier           = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(9.dp)),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Transport: Shuffle Prev Play Next Repeat
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TransportControls(
    controllerState: ControlState,
    onUIEvent:       (UIEvent) -> Unit,
) {
    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SecondaryButton(
            active  = controllerState.isShuffle,
            onClick = { onUIEvent(UIEvent.Shuffle) },
        ) {
            Icon(
                imageVector        = SimpIcons.Shuffle,
                contentDescription = "Shuffle",
                tint               = if (controllerState.isShuffle) FpFg else FpFg2,
                modifier           = Modifier.size(16.dp),
            )
        }

        SecondaryButton(onClick = { onUIEvent(UIEvent.Previous) }) {
            Icon(
                imageVector        = SimpIcons.SkipPrevious,
                contentDescription = "Previous",
                tint               = FpFg2,
                modifier           = Modifier.size(16.dp),
            )
        }

        PlayButton(
            isPlaying = controllerState.isPlaying,
            onClick   = { onUIEvent(UIEvent.PlayPause) },
        )

        SecondaryButton(onClick = { onUIEvent(UIEvent.Next) }) {
            Icon(
                imageVector        = SimpIcons.SkipNext,
                contentDescription = "Next",
                tint               = FpFg2,
                modifier           = Modifier.size(16.dp),
            )
        }

        val isRepeatActive = controllerState.repeatState !is RepeatState.None
        val isRepeatOne    = controllerState.repeatState is RepeatState.One

        SecondaryButton(
            active  = isRepeatActive,
            onClick = { onUIEvent(UIEvent.Repeat) },
        ) {
            Icon(
                imageVector        = if (isRepeatOne) SimpIcons.RepeatOne else SimpIcons.Repeat,
                contentDescription = "Repeat",
                tint               = if (isRepeatActive) FpFg else FpFg2,
                modifier           = Modifier.size(16.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Play/Pause  34x34dp circular, #F2F2F7 bg, #0E0E10 icon
// hover: 3dp ring; press: scale 0.88
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PlayButton(
    isPlaying: Boolean,
    onClick:   () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered   by interaction.collectIsHoveredAsState()
    val isPressed   by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.88f else 1f,
        animationSpec = tween(100),
        label         = "FpPlayScale",
    )

    Box(contentAlignment = Alignment.Center) {
        // Hover ring  rgba(242,242,247,0.16)
        if (isHovered && !isPressed) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(0x28F2F2F7)),
            )
        }

        Box(
            modifier = Modifier
                .size(34.dp)
                .graphicsLayer { scaleX = scale; scaleY = scale }
                .clip(CircleShape)
                .background(FpPlayBg)
                .hoverable(interaction)
                .pointerInput(Unit) { detectTapGestures { onClick() } },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = if (isPlaying) SimpIcons.Pause else SimpIcons.PlayArrow,
                contentDescription = if (isPlaying) "Pause" else "Play",
                tint               = FpPlayFg,
                modifier           = Modifier.size(18.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Secondary transport button  30x30dp, 8dp radius, hover fill rgba(255,255,255,0.07)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun SecondaryButton(
    active:  Boolean = false,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered   by interaction.collectIsHoveredAsState()
    val isPressed   by interaction.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue   = if (isPressed) 0.84f else 1f,
        animationSpec = tween(80),
        label         = "FpSecScale",
    )

    Box(
        modifier = Modifier
            .size(30.dp)
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .clip(RoundedCornerShape(8.dp))
            .background(if (isHovered) Color(0x12FFFFFF) else Color.Transparent)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Right zone: Waveform Like Queue Volume | Close
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun RightZone(
    isPlaying:    Boolean,
    isLiked:      Boolean,
    volume:       Float = 1f,
    onUIEvent:    (UIEvent) -> Unit,
    onQueueClick: () -> Unit,
    onClose:      () -> Unit,
) {
    var showVolumePopup by remember { mutableStateOf(false) }
    var lastNonZeroVolume by remember { mutableFloatStateOf(1f) }

    Row(
        verticalAlignment     = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        RightZoneIconSlot(onClick = {}) {
            FpWaveformBars(isPlaying = isPlaying, modifier = Modifier.size(16.dp))
        }

        RightZoneIconSlot(onClick = { onUIEvent(UIEvent.ToggleLike) }) {
            Icon(
                imageVector        = if (isLiked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                contentDescription = "Like",
                tint               = FpFg3,
                modifier           = Modifier.size(16.dp),
            )
        }

        RightZoneIconSlot(onClick = onQueueClick) {
            Icon(
                imageVector        = SimpIcons.QueueMusic,
                contentDescription = "Queue",
                tint               = FpFg3,
                modifier           = Modifier.size(16.dp),
            )
        }

        Box {
            RightZoneIconSlot(onClick = { showVolumePopup = !showVolumePopup }) {
                val icon = when {
                    volume <= 0f -> SimpIcons.VolumeOff
                    volume < 0.5f -> SimpIcons.VolumeDown
                    else -> SimpIcons.VolumeUp
                }
                Icon(
                    imageVector        = icon,
                    contentDescription = "Volume",
                    tint               = if (showVolumePopup || volume > 0f) FpFg else FpFg3,
                    modifier           = Modifier.size(16.dp),
                )
            }

            if (showVolumePopup) {
                Popup(
                    alignment = Alignment.TopCenter,
                    offset = androidx.compose.ui.unit.IntOffset(0, -68),
                    onDismissRequest = { showVolumePopup = false },
                    properties = PopupProperties(focusable = true)
                ) {
                    Box(
                        modifier = Modifier
                            .shadow(16.dp, RoundedCornerShape(12.dp), spotColor = Color(0x60000000))
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1E1E20))
                            .border(1.dp, Color(0xFF333336), RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.width(200.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0x12FFFFFF))
                                    .clickable {
                                        if (volume > 0f) {
                                            lastNonZeroVolume = volume
                                            onUIEvent(UIEvent.UpdateVolume(0f))
                                        } else {
                                            onUIEvent(UIEvent.UpdateVolume(lastNonZeroVolume.coerceIn(0.1f, 1f)))
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (volume <= 0f) SimpIcons.VolumeOff else SimpIcons.VolumeUp,
                                    contentDescription = "Mute Toggle",
                                    tint = if (volume > 0f) FpFg else FpFg2,
                                    modifier = Modifier.size(13.dp)
                                )
                            }

                            // Custom sleek slider bar with drag gesture
                            val volCoerced = volume.coerceIn(0f, 1f)
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(20.dp)
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val newVol = (offset.x / size.width).coerceIn(0f, 1f)
                                            if (newVol > 0f) lastNonZeroVolume = newVol
                                            onUIEvent(UIEvent.UpdateVolume(newVol))
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures { change, _ ->
                                            val newVol = (change.position.x / size.width).coerceIn(0f, 1f)
                                            if (newVol > 0f) lastNonZeroVolume = newVol
                                            onUIEvent(UIEvent.UpdateVolume(newVol))
                                            change.consume()
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Canvas(modifier = Modifier.fillMaxWidth().height(4.dp)) {
                                    val trackRadius = 2.dp.toPx()
                                    // Inactive track
                                    drawRoundRect(
                                        color = Color(0xFF38383A),
                                        cornerRadius = CornerRadius(trackRadius)
                                    )
                                    // Active track
                                    val activeWidth = size.width * volCoerced
                                    if (activeWidth > 0f) {
                                        drawRoundRect(
                                            color = Color.White,
                                            size = Size(activeWidth, size.height),
                                            cornerRadius = CornerRadius(trackRadius)
                                        )
                                    }
                                    // Scrubber circle
                                    drawCircle(
                                        color = Color.White,
                                        radius = 5.dp.toPx(),
                                        center = Offset(activeWidth, size.height / 2f)
                                    )
                                }
                            }

                            // Percentage text (e.g. 5%, 80%, 100%)
                            val percentText = (volCoerced * 100f).toInt().toString() + "%"
                            Text(
                                text = percentText,
                                style = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = FpFg2
                                ),
                                modifier = Modifier.width(36.dp)
                            )
                        }
                    }
                }
            }
        }        // 1dp vertical separator #3A3A3C
        Box(
            modifier = Modifier
                .width(1.dp)
                .height(16.dp)
                .background(FpBorder),
        )

        // Close — circular 24x24dp, bg rgba(255,255,255,0.07)
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(Color(0x12FFFFFF))
                .pointerInput(Unit) { detectTapGestures { onClose() } },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = SimpIcons.Close,
                contentDescription = "Close",
                tint               = FpFg3,
                modifier           = Modifier.size(13.dp),
            )
        }
    }
}

// 28x28dp slot with hover background, 7dp radius
@Composable
private fun RightZoneIconSlot(
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isHovered   by interaction.collectIsHoveredAsState()

    Box(
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(7.dp))
            .background(if (isHovered) Color(0x0DFFFFFF) else Color.Transparent)
            .hoverable(interaction)
            .pointerInput(Unit) { detectTapGestures { onClick() } },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Animated 4-bar waveform
// 2.5dp wide, 2dp corner radius, color #48484A
// heights 3dp <-> 13dp, paused when not playing
// ─────────────────────────────────────────────────────────────────────────────

private val FpWaveformDurations = intArrayOf(1050, 780, 1180, 920)
private val FpWaveformOffsets   = intArrayOf(0, 260, 130, 390)

@Composable
private fun FpWaveformBars(
    isPlaying: Boolean,
    modifier:  Modifier = Modifier,
) {
    val transition = rememberInfiniteTransition(label = "fpWaveform")

    val h0 by transition.animateFloat(
        initialValue  = 3f,
        targetValue   = 13f,
        animationSpec = infiniteRepeatable(
            animation  = tween(FpWaveformDurations[0], easing = FastOutSlowInEasing, delayMillis = FpWaveformOffsets[0]),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fpWave0",
    )
    val h1 by transition.animateFloat(
        initialValue  = 3f,
        targetValue   = 13f,
        animationSpec = infiniteRepeatable(
            animation  = tween(FpWaveformDurations[1], easing = LinearEasing, delayMillis = FpWaveformOffsets[1]),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fpWave1",
    )
    val h2 by transition.animateFloat(
        initialValue  = 3f,
        targetValue   = 13f,
        animationSpec = infiniteRepeatable(
            animation  = tween(FpWaveformDurations[2], easing = FastOutSlowInEasing, delayMillis = FpWaveformOffsets[2]),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fpWave2",
    )
    val h3 by transition.animateFloat(
        initialValue  = 3f,
        targetValue   = 13f,
        animationSpec = infiniteRepeatable(
            animation  = tween(FpWaveformDurations[3], easing = LinearEasing, delayMillis = FpWaveformOffsets[3]),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "fpWave3",
    )

    Canvas(modifier = modifier) {
        val barW    = 2.5.dp.toPx()
        val radius  = 2.dp.toPx()
        val spacing = (size.width - 4f * barW) / 3f
        val centerY = size.height / 2f
        val raw     = floatArrayOf(h0, h1, h2, h3)

        raw.forEachIndexed { i, rawH ->
            val h = if (isPlaying) rawH.dp.toPx() else 3.dp.toPx()
            val x = i * (barW + spacing)
            drawRoundRect(
                color        = FpFg3,
                topLeft      = Offset(x, centerY - h / 2f),
                size         = Size(barW, h),
                cornerRadius = CornerRadius(radius, radius),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Progress strip  24dp tall, below main row
// Background #252527, bottom-14dp corners
// 2dp track at bottom edge, fill #8E8E93, scrubber head on hover
// Timestamps: monospace 9.5sp, #48484A, 12dp padding
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ProgressStrip(
    sliderValue:  Float,           // 0..1
    timelineState: TimeLine,
    isSliding:    Boolean,
    onSlideStart: () -> Unit,
    onSlideEnd:   (Float) -> Unit,
) {
    val interaction  = remember { MutableInteractionSource() }
    val isHovered    by interaction.collectIsHoveredAsState()
    val showScrubber = isHovered || isSliding

    val elapsedText = timelineState.elapsedLabel(sliderValue)
    val totalText   = timelineState.lengthLabel()

    val scrubberRadiusDp = 4.5.dp
    val trackPaddingBottomDp = 6.dp

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(26.dp)
            .background(
                color = FpBar2,
                shape = RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp),
            )
            .clip(RoundedCornerShape(bottomStart = 14.dp, bottomEnd = 14.dp))
            .hoverable(interaction)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val insetPx = scrubberRadiusDp.toPx()
                    val usableWidth = size.width - (insetPx * 2f)
                    val pct = if (usableWidth > 0f) {
                        ((offset.x - insetPx) / usableWidth).coerceIn(0f, 1f)
                    } else 0f
                    onSlideEnd(pct)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart   = { onSlideStart() },
                    onHorizontalDrag = { change, _ ->
                        val insetPx = scrubberRadiusDp.toPx()
                        val usableWidth = size.width - (insetPx * 2f)
                        val pct = if (usableWidth > 0f) {
                            ((change.position.x - insetPx) / usableWidth).coerceIn(0f, 1f)
                        } else 0f
                        onSlideEnd(pct)
                        change.consume()
                    },
                    onDragEnd     = {},
                    onDragCancel  = { onSlideEnd(sliderValue) },
                )
            },
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val scrubberR = scrubberRadiusDp.toPx()
            val trackH = 2.dp.toPx()
            val trackY = size.height - trackPaddingBottomDp.toPx() - trackH / 2f
            val startX = scrubberR
            val usableW = (size.width - scrubberR * 2f).coerceAtLeast(0f)
            val fillW = usableW * sliderValue
            val thumbCenterX = (startX + fillW).coerceIn(scrubberR, size.width - scrubberR)

            // Top hairline
            drawRect(
                color   = Color(0x0DFFFFFF),
                topLeft = Offset(0f, 0f),
                size    = Size(size.width, 1.dp.toPx()),
            )

            // Track background - full usable width with rounded ends
            drawRoundRect(
                color        = FpFg3,
                topLeft      = Offset(startX, trackY - trackH / 2f),
                size         = Size(usableW, trackH),
                cornerRadius = CornerRadius(trackH / 2f),
            )

            // Track fill - played portion
            if (fillW > 0f) {
                drawRoundRect(
                    color        = FpFg2,
                    topLeft      = Offset(startX, trackY - trackH / 2f),
                    size         = Size(fillW, trackH),
                    cornerRadius = CornerRadius(trackH / 2f),
                )
            }

            // Scrubber thumb + soft glow on hover or sliding (never clipped by borders or ends)
            if (showScrubber) {
                drawCircle(
                    color  = Color(0x30F2F2F7),
                    radius = scrubberR * 1.8f,
                    center = Offset(thumbCenterX, trackY),
                )
                drawCircle(
                    color  = FpFg,
                    radius = scrubberR,
                    center = Offset(thumbCenterX, trackY),
                )
            }
        }

        // Timestamps with enhanced contrast and clear typography
        Row(
            modifier              = Modifier
                .matchParentSize()
                .padding(start = 12.dp, end = 12.dp, top = 2.dp, bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment     = Alignment.Top,
        ) {
            Text(
                text  = elapsedText,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color      = FpFg2,
                ),
            )
            Text(
                text  = totalText,
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize   = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color      = FpFg2,
                ),
            )
        }
    }
}
