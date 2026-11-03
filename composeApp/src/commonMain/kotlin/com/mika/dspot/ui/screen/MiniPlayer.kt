package com.mika.dspot.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.CircularWavyProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.navigation.NavController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mika.domain.data.entities.SongEntity
import com.mika.domain.manager.DataStoreManager
import com.mika.domain.utils.connectArtists
import com.mika.logger.Logger
import com.mika.dspot.Platform
import com.mika.dspot.expect.toggleMiniPlayer
import com.mika.dspot.expect.ui.PlatformBackdrop
import com.mika.dspot.extension.elapsedLabel
import com.mika.dspot.extension.formatDuration
import com.mika.dspot.getPlatform
import com.mika.dspot.ui.component.DesktopFloatingPlayerBar
import com.mika.dspot.ui.component.ExplicitBadge
import com.mika.dspot.ui.component.HeartCheckBox
import com.mika.dspot.ui.component.PlayPauseButton
import com.mika.dspot.ui.component.PlayerControlLayout
import com.mika.dspot.ui.component.QueueBottomSheet
import com.mika.dspot.ui.component.liquidGlass
import com.mika.dspot.ui.component.rememberGlassLuminance
import com.mika.dspot.ui.component.rememberHolderPainter
import com.mika.dspot.ui.icon.Close
import com.mika.dspot.ui.icon.Headphones
import com.mika.dspot.ui.icon.OpenInFull
import com.mika.dspot.ui.icon.PictureInPictureAlt
import com.mika.dspot.ui.icon.QueueMusic
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.icon.VolumeOff
import com.mika.dspot.ui.icon.VolumeUp
import com.mika.dspot.ui.screen.player.content.toAudioQualityLabel
import com.mika.dspot.ui.theme.LocalIsDarkTheme
import com.mika.dspot.ui.theme.typo
import com.mika.dspot.viewModel.SharedViewModel
import com.mika.dspot.viewModel.UIEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.crossfading
import dspot.composeapp.generated.resources.live_badge
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private const val TAG = "MiniPlayer"

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MiniPlayer(
    modifier: Modifier,
    backdrop: PlatformBackdrop,
    sharedViewModel: SharedViewModel = koinInject(),
    onClose: () -> Unit,
    onClick: () -> Unit,
    onOpenFullscreenLyrics: () -> Unit = {},
    // ConstraintLayout keeps Gone content composed, so its visibility must gate sampling separately.
    isVisible: Boolean = true,
    /** Lets the queue's song sheets open artist and album pages; the Desktop capsule's queue needs it. */
    navController: NavController? = null,
) {
    val isLiquidGlassEnabled by sharedViewModel.getEnableLiquidGlass().collectAsStateWithLifecycle(DataStoreManager.FALSE)
    val controllerState by sharedViewModel.controllerState.collectAsStateWithLifecycle()
    val timelineState by sharedViewModel.timeline.collectAsStateWithLifecycle()
    // The Desktop capsule's quality line between its timestamps — see toAudioQualityLabel.
    val formatState by sharedViewModel.format.collectAsStateWithLifecycle(initialValue = null)

    val layer = rememberGraphicsLayer()

    // The Desktop capsule is always liquid glass, so it needs the glass code paths whatever the
    // setting says — both the luminance sampling loop that drives the glass and the theme-following
    // text colour. Leaving them gated left the capsule with luminance stuck at 0: a 2dp blur and a
    // 0.12 darken, which is why it looked like a smear rather than glass. The setting still governs
    // the Android card below.
    val useGlassSurface = isLiquidGlassEnabled == DataStoreManager.TRUE || getPlatform() == Platform.Desktop
    val luminance = rememberGlassLuminance(layer, enabled = useGlassSurface && isVisible)

    val isDarkTheme = LocalIsDarkTheme.current
    val textColor by animateColorAsState(
        // With liquid glass the surface follows the theme (light = frosted white → black text);
        // without it, the card is a theme surface, so its foreground token.
        targetValue =
            if (useGlassSurface) {
                if (isDarkTheme) Color.White else Color.Black
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        label = "MiniPlayerTextColor",
        animationSpec = tween(500),
    )

    val (songEntity, setSongEntity) =
        remember {
            mutableStateOf<SongEntity?>(null)
        }
    val (liked, setLiked) =
        remember {
            mutableStateOf(false)
        }
    val (isPlaying, setIsPlaying) =
        remember {
            mutableStateOf(false)
        }
    val (progress, setProgress) =
        remember {
            mutableFloatStateOf(0f)
        }
    val (isCrossfading, setIsCrossfading) =
        remember {
            mutableStateOf(false)
        }

    val coroutineScope = rememberCoroutineScope()

    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
        label = "",
    )

    val offsetX = remember { Animatable(initialValue = 0f) }
    val offsetY = remember { Animatable(0f) }

    var loading by rememberSaveable {
        mutableStateOf(true)
    }

    LaunchedEffect(key1 = true) {
        val job1 =
            launch {
                sharedViewModel.nowPlayingState.collect { item ->
                    if (item != null) {
                        setSongEntity(item.songEntity)
                    }
                }
            }
        val job2 =
            launch {
                sharedViewModel.controllerState.collectLatest { state ->
                    setLiked(state.isLiked)
                    setIsPlaying(state.isPlaying)
                    setIsCrossfading(state.isCrossfading)
                }
            }
        val job4 =
            launch {
                sharedViewModel.timeline.collect { timeline ->
                    loading = timeline.loading
                    val prog =
                        if (timeline.total > 0L && timeline.current >= 0L) {
                            timeline.current.toFloat() / timeline.total
                        } else {
                            0f
                        }
                    setProgress(prog)
                }
            }
        job1.join()
        job2.join()
        job4.join()
    }

    if (getPlatform() == Platform.Android) {
        // One shape for both the Card and the clip below. They must not diverge: the clip wraps
        // the Card's own background draw, so the larger radius wins and silently becomes the
        // visible one.
        val miniPlayerShape = CircleShape
        // Without glass the card follows the theme, not the playing artwork.
        val cardColor =
            if (isLiquidGlassEnabled == DataStoreManager.TRUE) {
                Color.Transparent
            } else {
                // Same opaque surface as the bottom bar capsule, so the two floating surfaces read as one set.
                MaterialTheme.colorScheme.surfaceContainer
            }
        // The flat (default) card: round artwork and controls sitting in filled circles. Glass keeps its own look.
        val isFlat = isLiquidGlassEnabled != DataStoreManager.TRUE
        Card(
            shape = miniPlayerShape,
            colors =
                CardDefaults.cardColors(
                    containerColor = cardColor,
                    disabledContainerColor = cardColor,
                ),
            modifier =
                modifier
                    .then(
                        if (isLiquidGlassEnabled == DataStoreManager.TRUE) {
                            Modifier.liquidGlass(backdrop, layer, luminance, RoundedCornerShape(16.dp))
                        } else {
                            Modifier
                        },
                    ).then(
                        Modifier
                            .clip(miniPlayerShape)
                            .offset { IntOffset(0, offsetY.value.roundToInt()) }
                            .clickable(
                                onClick = onClick,
                            ).pointerInput(Unit) {
                                detectVerticalDragGestures(
                                    onDragStart = {
                                    },
                                    onVerticalDrag = { change: PointerInputChange, dragAmount: Float ->
                                        if (offsetY.value + dragAmount > 0) {
                                            coroutineScope.launch {
                                                change.consume()
                                                offsetY.animateTo(offsetY.value + 2 * dragAmount)
                                                Logger.w("MiniPlayer", "Dragged ${offsetY.value}")
                                            }
                                        }
                                    },
                                    onDragCancel = {
                                        coroutineScope.launch {
                                            offsetY.animateTo(0f)
                                        }
                                    },
                                    onDragEnd = {
                                        Logger.w("MiniPlayer", "Drag Ended")
                                        coroutineScope.launch {
                                            if (offsetY.value > 70) {
                                                onClose()
                                            }
                                            offsetY.animateTo(0f)
                                        }
                                    },
                                )
                            },
                    ),
        ) {
            Box(modifier = Modifier.fillMaxHeight()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier =
                        Modifier
                            .fillMaxSize(),
                ) {
                    Spacer(modifier = Modifier.size(8.dp))
                    Box(modifier = Modifier.weight(1F)) {
                        Row(
                            modifier =
                                Modifier
                                    .offset { IntOffset(offsetX.value.roundToInt(), 0) }
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = {
                                            },
                                            onHorizontalDrag = {
                                                change: PointerInputChange,
                                                dragAmount: Float,
                                                ->
                                                coroutineScope.launch {
                                                    change.consume()
                                                    offsetX.animateTo(offsetX.value + dragAmount * 2)
                                                    Logger.w("MiniPlayer", "Dragged ${offsetX.value}")
                                                }
                                            },
                                            onDragCancel = {
                                                Logger.w("MiniPlayer", "Drag Cancelled")
                                                coroutineScope.launch {
                                                    if (offsetX.value > 200) {
                                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                                    } else if (offsetX.value < -120) {
                                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                                    }
                                                    offsetX.animateTo(0f)
                                                }
                                            },
                                            onDragEnd = {
                                                Logger.w("MiniPlayer", "Drag Ended")
                                                coroutineScope.launch {
                                                    if (offsetX.value > 200) {
                                                        sharedViewModel.onUIEvent(UIEvent.Previous)
                                                    } else if (offsetX.value < -120) {
                                                        sharedViewModel.onUIEvent(UIEvent.Next)
                                                    }
                                                    offsetX.animateTo(0f)
                                                }
                                            },
                                        )
                                    },
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(40.dp)
                                        .align(Alignment.CenterVertically),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isFlat) {
                                    // Progress rides a ring around the artwork: wavy while playing, flat when paused.
                                    val ringStroke = Stroke(width = with(LocalDensity.current) { 3.dp.toPx() }, cap = StrokeCap.Round)
                                    CircularWavyProgressIndicator(
                                        progress = { animatedProgress },
                                        modifier = Modifier.fillMaxSize(),
                                        color = MaterialTheme.colorScheme.primary,
                                        trackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                                        stroke = ringStroke,
                                        trackStroke = ringStroke,
                                        // A raw 0/1, never a tweened value: the node animates amplitude
                                        // itself and drops new targets mid-animation (see WavySeekBar).
                                        amplitude = { p -> if (p > 0f && isPlaying) 1f else 0f },
                                    )
                                }
                                AsyncImage(
                                    model =
                                        ImageRequest
                                            .Builder(LocalPlatformContext.current)
                                            .data(songEntity?.thumbnails)
                                            .crossfade(550)
                                            .build(),
                                    placeholder = rememberHolderPainter(),
                                    error = rememberHolderPainter(),
                                    contentDescription = null,
                                    // Crop in the circle: FillWidth would leave bands around a 16:9 video thumbnail.
                                    contentScale = if (isFlat) ContentScale.Crop else ContentScale.FillWidth,
                                    modifier =
                                        Modifier
                                            .size(if (isFlat) 32.dp else 44.dp)
                                            .clip(if (isFlat) CircleShape else RoundedCornerShape(4.dp)),
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            AnimatedContent(
                                targetState = songEntity,
                                modifier = Modifier.weight(1F).fillMaxHeight(),
                                contentAlignment = Alignment.CenterStart,
                                transitionSpec = {
                                    // Compare the incoming number with the previous number.
                                    if (targetState != initialState) {
                                        // If the target number is larger, it slides up and fades in
                                        // while the initial (smaller) number slides up and fades out.
                                        (
                                            slideInHorizontally { width ->
                                                width
                                            } + fadeIn()
                                        ).togetherWith(
                                            slideOutHorizontally { width -> +width } + fadeOut(),
                                        )
                                    } else {
                                        // If the target number is smaller, it slides down and fades in
                                        // while the initial number slides down and fades out.
                                        (
                                            slideInHorizontally { width ->
                                                +width
                                            } + fadeIn()
                                        ).togetherWith(
                                            slideOutHorizontally { width -> width } + fadeOut(),
                                        )
                                    }.using(
                                        // Disable clipping since the faded slide-in/out should
                                        // be displayed out of bounds.
                                        SizeTransform(clip = false),
                                    )
                                },
                            ) { target ->
                                if (target != null) {
                                    Column(
                                        Modifier
                                            .wrapContentHeight()
                                            .align(Alignment.CenterVertically),
                                    ) {
                                        Text(
                                            text = (songEntity?.title ?: "").toString(),
                                            style = if (isFlat) typo().titleSmall else typo().labelSmall,
                                            color = textColor,
                                            maxLines = 1,
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .wrapContentHeight(
                                                        align = Alignment.CenterVertically,
                                                    ).basicMarquee(
                                                        iterations = Int.MAX_VALUE,
                                                        animationMode = MarqueeAnimationMode.Immediately,
                                                    ).focusable(),
                                        )
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            androidx.compose.animation.AnimatedVisibility(visible = songEntity?.isExplicit == true) {
                                                ExplicitBadge(
                                                    modifier =
                                                        Modifier
                                                            .size(20.dp)
                                                            .padding(end = 4.dp)
                                                            .weight(1f),
                                                )
                                            }
                                            Text(
                                                text = (songEntity?.artistName?.connectArtists() ?: ""),
                                                style = if (isFlat) typo().bodySmall.copy(fontSize = 10.sp) else typo().bodySmall,
                                                maxLines = 1,
                                                color = if (isFlat) MaterialTheme.colorScheme.onSurfaceVariant else textColor,
                                                modifier =
                                                    Modifier
                                                        .weight(1f)
                                                        .wrapContentHeight(
                                                            align = Alignment.CenterVertically,
                                                        ).basicMarquee(
                                                            iterations = Int.MAX_VALUE,
                                                            animationMode = MarqueeAnimationMode.Immediately,
                                                        ).focusable(),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                    // Flat: 40dp in the 56dp pill leaves an even 8dp ring.
                    val controlSize = if (isFlat) 40.dp else 48.dp
                    val playColor = if (isFlat) MaterialTheme.colorScheme.onPrimary else textColor
                    Spacer(modifier = Modifier.width(if (isFlat) 8.dp else 15.dp))
                    // background(shape), not clip: the heart's like-burst draws outside its bounds.
                    Box(
                        modifier =
                            if (isFlat) {
                                Modifier
                                    .size(controlSize)
                                    .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape)
                            } else {
                                Modifier
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        HeartCheckBox(checked = liked, size = 30, tint = textColor) {
                            sharedViewModel.onUIEvent(UIEvent.ToggleLike)
                        }
                    }
                    Spacer(modifier = Modifier.width(if (isFlat) 8.dp else 15.dp))
                    Box(
                        modifier =
                            Modifier
                                .size(controlSize)
                                .then(if (isFlat) Modifier.background(MaterialTheme.colorScheme.primary, CircleShape) else Modifier),
                        contentAlignment = Alignment.Center,
                    ) {
                        Crossfade(targetState = loading, label = "") {
                            if (it) {
                                Box(modifier = Modifier.size(controlSize), contentAlignment = Alignment.Center) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = playColor,
                                        strokeWidth = 3.dp,
                                    )
                                }
                            } else {
                                PlayPauseButton(isPlaying = isPlaying, modifier = Modifier.size(controlSize), tint = playColor) {
                                    sharedViewModel.onUIEvent(UIEvent.PlayPause)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(if (isFlat) 8.dp else 15.dp))
                }
                // The flat card shows progress as the ring around its artwork instead.
                if (!isFlat) {
                    Box(
                        modifier =
                            Modifier
                                .wrapContentSize(Alignment.Center)
                                .padding(
                                    horizontal = 10.dp,
                                ).align(Alignment.BottomCenter),
                    ) {
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(
                                        color = Color.Transparent,
                                        shape = RoundedCornerShape(4.dp),
                                    ),
                            color = textColor,
                            trackColor = Color.Transparent,
                            strokeCap = StrokeCap.Round,
                            drawStopIndicator = {},
                        )
                    }
                }
            }
        }
    } else {
        var showQueueBottomSheet by rememberSaveable {
            mutableStateOf(false)
        }

        if (showQueueBottomSheet && navController != null) {
            QueueBottomSheet(
                onDismiss = {
                    showQueueBottomSheet = false
                },
                navController = navController,
                onNavigateToOtherScreen = { showQueueBottomSheet = false },
            )
        }

        DesktopFloatingPlayerBar(
            modifier        = modifier,
            songEntity      = songEntity,
            controllerState = controllerState,
            timelineState   = timelineState,
            onUIEvent       = { sharedViewModel.onUIEvent(it) },
            onClick         = onClick,
            onClose         = onClose,
            onQueueClick    = { showQueueBottomSheet = true },
        )
    }
}
