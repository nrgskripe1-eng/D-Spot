package com.mika.dspot.ui.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MarqueeAnimationMode
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mika.domain.data.model.streams.YouTubeWatchEndpoint
import com.mika.domain.repository.LocalPlaylistRepository
import com.mika.dspot.expect.shareUrl
import com.mika.dspot.ui.icon.PlayArrow
import com.mika.dspot.ui.icon.QueueMusic
import com.mika.dspot.ui.icon.Share
import com.mika.dspot.ui.icon.Shuffle
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.icon.SkipNext
import com.mika.dspot.ui.icon.Sync
import com.mika.dspot.ui.icon.SyncDisabled
import com.mika.dspot.viewModel.SharedViewModel
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.add_to_queue
import dspot.composeapp.generated.resources.play_all
import dspot.composeapp.generated.resources.play_next
import dspot.composeapp.generated.resources.save_to_local_playlist
import dspot.composeapp.generated.resources.saved_to_local_playlist
import dspot.composeapp.generated.resources.share
import dspot.composeapp.generated.resources.shuffle
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject

// ──────────────────────────────────────────────────────────────────────────────
// Colour tokens matching DesktopSongContextMenu
// ──────────────────────────────────────────────────────────────────────────────

private val MenuSurface      = Color(0xFF222326)
private val MenuBorder       = Color(0xFF36383D)
private val MenuHover        = Color(0xFF303237)
private val MenuHoverLight   = Color(0x55303237)
private val ContentPrimary   = Color(0xFFE8E8EA)
private val ContentSecondary = Color(0xFF9A9CA3)
private val ContentDisabled  = Color(0xFF82848B)

data class DesktopPlaylistContextData(
    val id: String,
    val title: String,
    val author: String,
    val thumbnail: String?,
    val playEndpoint: YouTubeWatchEndpoint? = null,
    val isRadio: Boolean = false,
)

/**
 * Desktop-only floating context menu for playlists (matching DesktopSongContextMenu design).
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DesktopPlaylistContextMenu(
    data: DesktopPlaylistContextData,
    windowOffset: IntOffset?,
    onDismiss: () -> Unit,
) {
    val localPlaylistRepository: LocalPlaylistRepository = koinInject()
    val sharedViewModel: SharedViewModel = koinInject()
    val addToQueueBlock = rememberAddToQueueBlock()

    val localPlaylists by localPlaylistRepository.getAllLocalPlaylists().collectAsState(initial = emptyList())
    val isSavedToLocal = remember(localPlaylists, data.id) {
        val targetId = data.id.removePrefix("VL")
        localPlaylists.any { it.youtubePlaylistId?.removePrefix("VL") == targetId }
    }

    val entries = remember(addToQueueBlock, isSavedToLocal) {
        listOf(
            "play" to true,
            "shuffle" to true,
            "playNext" to true,
            "addToQueue" to (addToQueueBlock == null),
            "saveToLocal" to (!isSavedToLocal),
            "share" to true,
        )
    }

    var focusedIndex by remember { mutableIntStateOf(-1) }
    val focusRequester = remember { FocusRequester() }

    fun keyAt(key: String) = entries.indexOfFirst { it.first == key }

    fun moveIndex(delta: Int) {
        val size = entries.size
        if (size == 0) return
        val step = if (delta > 0) 1 else -1
        var next = (focusedIndex + step).coerceIn(0, size - 1)
        var guard = 0
        while (next in entries.indices && !entries[next].second && guard < size) {
            next = (next + step).coerceIn(0, size - 1)
        }
        if (next in entries.indices && entries[next].second) focusedIndex = next
    }

    // All action lambdas dismiss the popup first, then delegate the heavy work to
    // SharedViewModel methods that run on viewModelScope — that scope lives beyond
    // the lifetime of this composable, so the network calls are never cancelled when
    // the Popup unmounts.

    val playAction = {
        val endpoint = data.playEndpoint
            ?: YouTubeWatchEndpoint(playlistId = data.id.removePrefix("VL"))
        sharedViewModel.playFromEndpoint(endpoint, data.title)
        onDismiss()
        Unit
    }

    val shuffleAction = {
        val endpoint = data.playEndpoint
            ?: YouTubeWatchEndpoint(playlistId = data.id.removePrefix("VL"))
        sharedViewModel.shuffleFromEndpoint(endpoint, data.title)
        onDismiss()
        Unit
    }

    val playNextAction = {
        sharedViewModel.playNextFromPlaylist(data.id.removePrefix("VL"))
        onDismiss()
        Unit
    }

    val addToQueueAction = {
        sharedViewModel.addToQueueFromPlaylist(data.id.removePrefix("VL"))
        onDismiss()
        Unit
    }

    val saveToLocalAction = {
        sharedViewModel.syncYouTubePlaylist(data.id.removePrefix("VL"))
        onDismiss()
        Unit
    }

    val shareAction = {
        val url = "https://music.youtube.com/playlist?list=${data.id.removePrefix("VL")}"
        shareUrl(data.title, url)
        onDismiss()
        Unit
    }

    fun activateFocused() {
        val key = entries.getOrNull(focusedIndex)?.first ?: return
        when (key) {
            "play"        -> playAction()
            "shuffle"     -> shuffleAction()
            "playNext"    -> playNextAction()
            "addToQueue"  -> addToQueueAction()
            "saveToLocal" -> saveToLocalAction()
            "share"       -> shareAction()
        }
    }

    Popup(
        popupPositionProvider = remember(windowOffset) {
            SongContextMenuPositionProvider(
                windowOffset          = windowOffset,
                anchorBoundsOverride  = null,
                margin                = 8,
            )
        },
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable             = true,
            dismissOnBackPress    = true,
            dismissOnClickOutside = true,
        ),
    ) {
        LaunchedEffect(Unit) { focusRequester.requestFocus() }

        Box(
            modifier = Modifier
                .shadow(
                    elevation    = 20.dp,
                    shape        = RoundedCornerShape(8.dp),
                    ambientColor = Color.Black.copy(alpha = 0.6f),
                    spotColor    = Color.Black.copy(alpha = 0.8f),
                )
                .border(1.dp, MenuBorder, RoundedCornerShape(8.dp))
                .clip(RoundedCornerShape(8.dp))
                .background(MenuSurface)
                .width(264.dp)
                .wrapContentHeight()
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    when (event.key) {
                        Key.DirectionDown -> { moveIndex(+1); true }
                        Key.DirectionUp   -> { moveIndex(-1); true }
                        Key.Enter         -> { activateFocused(); true }
                        Key.Escape        -> { onDismiss(); true }
                        else              -> false
                    }
                },
        ) {
            Column {
                // Header (Thumbnail, Title, Author)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalPlatformContext.current)
                            .data(data.thumbnail)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .diskCacheKey(data.thumbnail)
                            .crossfade(true)
                            .build(),
                        placeholder = rememberHolderPainter(),
                        error = rememberHolderPainter(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        modifier = Modifier.weight(1f),
                    ) {
                        Text(
                            text = data.title,
                            style = TextStyle(
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = ContentPrimary,
                            ),
                            maxLines = 1,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                                .focusable(),
                        )
                        if (data.author.isNotBlank()) {
                            Text(
                                text = data.author,
                                style = TextStyle(fontSize = 12.sp, color = ContentSecondary),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth(),
                    thickness = 1.dp,
                    color = MenuBorder,
                )

                Spacer(Modifier.height(4.dp))

                // Play
                PlaylistContextMenuItem(
                    icon = SimpIcons.PlayArrow,
                    text = stringResource(Res.string.play_all),
                    selected = focusedIndex == keyAt("play"),
                    onClick = playAction,
                )

                // Shuffle
                PlaylistContextMenuItem(
                    icon = SimpIcons.Shuffle,
                    text = stringResource(Res.string.shuffle),
                    selected = focusedIndex == keyAt("shuffle"),
                    onClick = shuffleAction,
                )

                // Play next
                PlaylistContextMenuItem(
                    icon = SimpIcons.SkipNext,
                    text = stringResource(Res.string.play_next),
                    selected = focusedIndex == keyAt("playNext"),
                    onClick = playNextAction,
                )

                // Add to queue
                PlaylistContextMenuItem(
                    icon = SimpIcons.QueueMusic,
                    text = stringResource(Res.string.add_to_queue),
                    enabled = addToQueueBlock == null,
                    selected = focusedIndex == keyAt("addToQueue"),
                    onClick = addToQueueAction,
                )

                // Save to library / local playlist
                PlaylistContextMenuItem(
                    icon = if (isSavedToLocal) SimpIcons.SyncDisabled else SimpIcons.Sync,
                    text = stringResource(if (isSavedToLocal) Res.string.saved_to_local_playlist else Res.string.save_to_local_playlist),
                    enabled = !isSavedToLocal,
                    selected = focusedIndex == keyAt("saveToLocal"),
                    onClick = saveToLocalAction,
                )

                // Share
                PlaylistContextMenuItem(
                    icon = SimpIcons.Share,
                    text = stringResource(Res.string.share),
                    selected = focusedIndex == keyAt("share"),
                    onClick = shareAction,
                )

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
private fun PlaylistContextMenuItem(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            !enabled            -> Color.Transparent
            pressed || selected -> MenuHover
            hovered             -> MenuHoverLight
            else                -> Color.Transparent
        },
        animationSpec = tween(80),
        label = "playlistMenuItemBg",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .then(
                if (enabled)
                    Modifier
                        .hoverable(interactionSource)
                        .clickable(
                            interactionSource = interactionSource,
                            indication = null,
                            onClick = onClick,
                        )
                else Modifier
            )
            .height(30.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = if (enabled) ContentPrimary else ContentDisabled,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text = text,
                style = TextStyle(
                    fontSize = 13.5.sp,
                    color = if (enabled) ContentPrimary else ContentDisabled,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
