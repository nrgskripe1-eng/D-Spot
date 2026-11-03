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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.Text
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
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
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mika.domain.data.entities.DownloadState
import com.mika.domain.data.entities.SongEntity
import com.mika.domain.mediaservice.handler.AddToQueueBlock
import com.mika.domain.utils.connectArtists
import com.mika.domain.utils.toListName
import com.mika.dspot.ui.icon.Album
import com.mika.dspot.ui.icon.Favorite
import com.mika.dspot.ui.icon.FavoriteBorder
import com.mika.dspot.ui.icon.DownloadForOffline
import com.mika.dspot.ui.icon.DownloadForOfflineOutlined
import com.mika.dspot.ui.icon.Downloading
import com.mika.dspot.ui.icon.PeopleAlt
import com.mika.dspot.ui.icon.PlayCircle
import com.mika.dspot.ui.icon.PlaylistAdd
import com.mika.dspot.ui.icon.QueueMusic
import com.mika.dspot.ui.icon.Sensors
import com.mika.dspot.ui.icon.Share
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.navigation.destination.list.AlbumDestination
import com.mika.dspot.ui.navigation.destination.list.ArtistDestination
import com.mika.domain.data.model.searchResult.songs.Artist
import com.mika.dspot.ui.icon.ArrowForwardIos
import com.mika.dspot.viewModel.NowPlayingBottomSheetUIEvent
import com.mika.dspot.viewModel.NowPlayingBottomSheetUIState
import com.mika.dspot.viewModel.NowPlayingBottomSheetViewModel
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.add_to_a_playlist
import dspot.composeapp.generated.resources.add_to_queue
import dspot.composeapp.generated.resources.album
import dspot.composeapp.generated.resources.artists
import dspot.composeapp.generated.resources.download
import dspot.composeapp.generated.resources.downloaded
import dspot.composeapp.generated.resources.downloading
import dspot.composeapp.generated.resources.no_album
import dspot.composeapp.generated.resources.play_next
import dspot.composeapp.generated.resources.radio
import dspot.composeapp.generated.resources.share
import dspot.composeapp.generated.resources.start_radio
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.viewmodel.koinViewModel
import kotlin.math.roundToInt

// ──────────────────────────────────────────────────────────────────────────────
// Colour tokens
// ──────────────────────────────────────────────────────────────────────────────

private val MenuSurface      = Color(0xFF222326)
private val MenuBorder       = Color(0xFF36383D)
private val MenuHover        = Color(0xFF303237)
private val MenuHoverLight   = Color(0x55303237)
private val ContentPrimary   = Color(0xFFE8E8EA)
private val ContentSecondary = Color(0xFF9A9CA3)
private val ContentDisabled  = Color(0xFF82848B)
private val LikeRed          = Color(0xFFF0645A)
private val DownloadBlue     = Color(0xFF00A0CB)

// ──────────────────────────────────────────────────────────────────────────────
// Popup position provider
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Positions the context-menu popup.
 *
 * Priority:
 * 1. [windowOffset] != null  → cursor-placed (right-click).  Open just below-right of cursor.
 * 2. [anchorBoundsOverride] != null → real button bounds passed by the caller.  Open below,
 *    right-edge-aligned with the button.  Flip up if it would overflow the bottom edge.
 * 3. Both null → fall back to Popup's own [anchorBounds] (rarely reached).
 *
 * In all cases the result is clamped inside the window with an 8-dp-equivalent margin.
 */
internal class SongContextMenuPositionProvider(
    private val windowOffset: IntOffset?,
    private val anchorBoundsOverride: IntRect?,
    private val margin: Int = 0,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val effective = anchorBoundsOverride ?: anchorBounds

        val baseX: Int
        val baseY: Int
        val flipAboveY: Int

        if (windowOffset != null) {
            // Right-click: open just below/right of cursor.
            baseX    = windowOffset.x
            baseY    = windowOffset.y
            flipAboveY = windowOffset.y - popupContentSize.height
        } else {
            // Button click: right-edge-aligned, gap below the button.
            baseX    = effective.right - popupContentSize.width
            baseY    = effective.bottom + 6          // 6 px gap
            flipAboveY = effective.top - popupContentSize.height - 6
        }

        // Flip up if menu overflows the bottom edge.
        val finalY = if (baseY + popupContentSize.height + margin <= windowSize.height) baseY
                     else flipAboveY

        // Clamp to keep the menu inside the window.
        val maxX = maxOf(0, windowSize.width  - popupContentSize.width  - margin)
        val maxY = maxOf(0, windowSize.height - popupContentSize.height - margin)
        return IntOffset(baseX.coerceIn(margin, maxX), finalY.coerceIn(margin, maxY))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Public entry point
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Desktop-only floating context menu that replaces [NowPlayingBottomSheet] for song rows.
 *
 * **Placement:**
 * - [windowOffset] == null → anchors to the Popup's parent composable (the MoreVert button),
 *   right-edge-aligned, just below it, flipping up/left when it would overflow the window.
 * - [windowOffset] != null → appears at the cursor position (right-click trigger).
 *
 * **Keyboard:** Up/Down navigates (skipping disabled items), Enter activates, Esc dismisses.
 *
 * Reuses [NowPlayingBottomSheetViewModel] and all existing action events unchanged.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class,
    ExperimentalComposeUiApi::class)
@Composable
fun DesktopSongContextMenu(
    song: SongEntity?,
    onDismiss: () -> Unit,
    onNavigateToOtherScreen: () -> Unit = {},
    onDelete: (() -> Unit)? = null,
    onLibraryDelete: (() -> Unit)? = null,
    queuePosition: QueuePosition? = null,
    /**
     * Absolute window-coordinate bounds of the trigger button (e.g. MoreVert).
     * When provided the menu is right-edge-aligned just below the button.
     * Pass null when [windowOffset] is used instead (right-click at cursor).
     */
    anchorBoundsInWindow: IntRect? = null,
    /** Cursor position for right-click triggered menus; takes priority over [anchorBoundsInWindow]. */
    windowOffset: IntOffset? = null,
    viewModel: NowPlayingBottomSheetViewModel = koinViewModel(),
) {
    val navController   = LocalNavController.current
    val uiState         by viewModel.uiState.collectAsStateWithLifecycle()
    val addToQueueBlock = rememberAddToQueueBlock()

    LaunchedEffect(song) { viewModel.setSongEntity(song) }

    // Sub-sheets rendered outside the Popup so they can open their own windows.
    var showAddToPlaylist    by remember { mutableStateOf(false) }
    var showArtistSubmenu   by remember { mutableStateOf(false) }
    // Bounds (in window coords) of the "Artists" row, used to anchor the submenu.
    var artistsRowBounds    by remember { mutableStateOf<IntRect?>(null) }

    if (showAddToPlaylist) {
        AddToPlaylistModalBottomSheet(
            isBottomSheetVisible = true,
            listLocalPlaylist    = uiState.listLocalPlaylist,
            listYouTubePlaylist  = uiState.listYouTubePlaylist,
            onDismiss            = { showAddToPlaylist = false },
            onClick              = { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToPlaylist(it.id)) },
            onYTPlaylistClick    = { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToYouTubePlaylist(it.browseId)) },
            videoId              = uiState.songUIState.videoId,
        )
    }
    if (showArtistSubmenu) {
        ArtistSubmenu(
            artists   = uiState.songUIState.listArtists,
            anchorBounds = artistsRowBounds,
            onArtistClick = { artist ->
                val id = artist.id
                if (!id.isNullOrBlank()) {
                    showArtistSubmenu = false
                    onNavigateToOtherScreen()
                    onDismiss()
                    navController?.navigate(ArtistDestination(id))
                }
            },
            onDismiss = { showArtistSubmenu = false },
        )
    }

    // ── Keyboard-navigation item list ─────────────────────────────────────────
    val albumEnabled = uiState.songUIState.album != null
    val queueEnabled = addToQueueBlock == null

    // Flat ordered list of (key, enabled) matching the visual top-to-bottom order.
    // Toolbar occupies slots 0-3; list items follow.
    val entries by remember(albumEnabled, queueEnabled, onDelete, onLibraryDelete, queuePosition) {
        derivedStateOf {
            buildList {
                add("like"        to true)
                add("download"    to true)
                if (queuePosition == null) add("playNext" to true)
                add("addPlaylist" to true)
                if (onDelete != null)        add("delete"    to true)
                if (onLibraryDelete != null) add("libDelete" to true)
                add("artist"  to true)
                add("album"   to albumEnabled)
                add("radio"   to true)
                add("queue"   to queueEnabled)
                add("share"   to true)
            }
        }
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
            guard++
        }
        if (next in entries.indices && entries[next].second) focusedIndex = next
    }

    fun activateFocused() {
        val key = entries.getOrNull(focusedIndex)?.first ?: return
        when (key) {
            "like"        -> viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike)
            "download"    -> viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download)
            "playNext"    -> viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.PlayNext)
            "addPlaylist" -> { viewModel.resetPlaylists(); showAddToPlaylist = true }
            "delete"      -> { onDismiss(); onDelete?.invoke() }
            "libDelete"   -> { onDismiss(); onLibraryDelete?.invoke() }
            "artist"      -> {
                val artists = uiState.songUIState.listArtists
                if (artists.size == 1) {
                    val id = artists.first().id
                    if (!id.isNullOrBlank()) {
                        onNavigateToOtherScreen()
                        onDismiss()
                        navController?.navigate(ArtistDestination(id))
                    }
                } else if (artists.size > 1) {
                    showArtistSubmenu = true
                }
            }
            "album"       -> uiState.songUIState.album?.id?.let { id ->
                onNavigateToOtherScreen(); onDismiss()
                navController?.navigate(AlbumDestination(browseId = id))
            }
            "radio"       -> {
                viewModel.onUIEvent(
                    NowPlayingBottomSheetUIEvent.StartRadio(
                        videoId = uiState.songUIState.videoId,
                        name    = "\"${uiState.songUIState.title}\" ${runBlocking { getString(Res.string.radio) }}",
                    )
                )
                onDismiss()
            }
            "queue"       -> viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToQueue)
            "share"       -> { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Share); onDismiss() }
        }
    }

    Popup(
        popupPositionProvider = remember(windowOffset, anchorBoundsInWindow) {
            SongContextMenuPositionProvider(
                windowOffset          = windowOffset,
                anchorBoundsOverride  = anchorBoundsInWindow,
                margin                = 8,
            )
        },
        onDismissRequest      = onDismiss,
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
            MenuContent(
                uiState         = uiState,
                addToQueueBlock = addToQueueBlock,
                queuePosition   = queuePosition,
                onDelete        = onDelete,
                onLibraryDelete = onLibraryDelete,
                keyAt           = ::keyAt,
                focusedIndex    = focusedIndex,
                onDismiss       = onDismiss,
                onToggleLike    = { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.ToggleLike) },
                onDownload      = { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Download) },
                onPlayNext      = { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.PlayNext) },
                onAddToPlaylist = { viewModel.resetPlaylists(); showAddToPlaylist = true },
                onArtist        = { artists ->
                    val navArtists = uiState.songUIState.listArtists
                    if (navArtists.size == 1) {
                        val id = navArtists.first().id
                        if (!id.isNullOrBlank()) {
                            onNavigateToOtherScreen(); onDismiss()
                            navController?.navigate(ArtistDestination(id))
                        }
                    } else if (navArtists.size > 1) {
                        showArtistSubmenu = true
                    }
                },
                onArtistRowPositioned = { bounds -> artistsRowBounds = bounds },
                onAlbum         = {
                    uiState.songUIState.album?.id?.let { id ->
                        onNavigateToOtherScreen(); onDismiss()
                        navController?.navigate(AlbumDestination(browseId = id))
                    }
                },
                onRadio         = {
                    viewModel.onUIEvent(
                        NowPlayingBottomSheetUIEvent.StartRadio(
                            videoId = uiState.songUIState.videoId,
                            name    = "\"${uiState.songUIState.title}\" ${runBlocking { getString(Res.string.radio) }}",
                        )
                    )
                    onDismiss()
                },
                onAddToQueue    = { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.AddToQueue) },
                onShare         = { viewModel.onUIEvent(NowPlayingBottomSheetUIEvent.Share); onDismiss() },
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Menu card content
// ──────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MenuContent(
    uiState: NowPlayingBottomSheetUIState,
    addToQueueBlock: AddToQueueBlock?,
    queuePosition: QueuePosition?,
    onDelete: (() -> Unit)?,
    onLibraryDelete: (() -> Unit)?,
    keyAt: (String) -> Int,
    focusedIndex: Int,
    onDismiss: () -> Unit,
    onToggleLike: () -> Unit,
    onDownload: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onArtist: (List<Artist>) -> Unit,
    onArtistRowPositioned: (IntRect) -> Unit,
    onAlbum: () -> Unit,
    onRadio: () -> Unit,
    onAddToQueue: () -> Unit,
    onShare: () -> Unit,
) {
    Column {
        // ── Header ─────────────────────────────────────────────────────────────
        Row(
            modifier          = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            val thumb = uiState.songUIState.thumbnails
            AsyncImage(
                model = ImageRequest.Builder(LocalPlatformContext.current)
                    .data(thumb)
                    .diskCachePolicy(CachePolicy.ENABLED)
                    .diskCacheKey(thumb)
                    .crossfade(true)
                    .build(),
                placeholder        = rememberHolderPainter(),
                error              = rememberHolderPainter(),
                contentDescription = null,
                contentScale       = ContentScale.Crop,
                modifier           = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(4.dp)),
            )
            Spacer(Modifier.width(10.dp))
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier            = Modifier.weight(1f),
            ) {
                Text(
                    text     = uiState.songUIState.title,
                    style    = TextStyle(
                        fontSize   = 13.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color      = ContentPrimary,
                    ),
                    maxLines = 1,
                    modifier = Modifier
                        .fillMaxWidth()
                        .basicMarquee(animationMode = MarqueeAnimationMode.Immediately)
                        .focusable(),
                )
                val artistStr = uiState.songUIState.listArtists.toListName().connectArtists()
                val albumName = uiState.songUIState.album?.name?.takeIf { it.isNotBlank() }
                val subtitle  = listOfNotNull(
                    artistStr.takeIf { it.isNotBlank() },
                    albumName,
                ).joinToString(" · ")
                if (subtitle.isNotBlank()) {
                    Text(
                        text     = subtitle,
                        style    = TextStyle(fontSize = 12.sp, color = ContentSecondary),
                        maxLines = 1,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        MenuDivider()

        // ── Icon toolbar (Like / Download / Play next / Add to playlist) ───────
        Row(
            modifier              = Modifier
                .fillMaxWidth()
                .height(44.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment     = Alignment.CenterVertically,
        ) {
            ToolbarButton(
                icon     = if (uiState.songUIState.liked) SimpIcons.Favorite else SimpIcons.FavoriteBorder,
                tint     = if (uiState.songUIState.liked) LikeRed else ContentPrimary,
                tooltip  = if (uiState.songUIState.liked) "Unlike" else "Like",
                selected = focusedIndex == keyAt("like"),
                onClick  = onToggleLike,
            )
            val dlState = uiState.songUIState.downloadState
            ToolbarButton(
                icon    = when (dlState) {
                    DownloadState.STATE_DOWNLOADING, DownloadState.STATE_PREPARING -> SimpIcons.Downloading
                    DownloadState.STATE_DOWNLOADED -> SimpIcons.DownloadForOffline
                    else                           -> SimpIcons.DownloadForOfflineOutlined
                },
                tint    = if (dlState == DownloadState.STATE_DOWNLOADED) DownloadBlue else ContentPrimary,
                tooltip = when (dlState) {
                    DownloadState.STATE_DOWNLOADING, DownloadState.STATE_PREPARING -> stringResource(Res.string.downloading)
                    DownloadState.STATE_DOWNLOADED -> stringResource(Res.string.downloaded)
                    else                           -> stringResource(Res.string.download)
                },
                selected = focusedIndex == keyAt("download"),
                onClick  = onDownload,
            )
            if (queuePosition == null) {
                ToolbarButton(
                    icon     = SimpIcons.PlayCircle,
                    tooltip  = stringResource(Res.string.play_next),
                    selected = focusedIndex == keyAt("playNext"),
                    onClick  = onPlayNext,
                )
            }
            ToolbarButton(
                icon     = SimpIcons.PlaylistAdd,
                tooltip  = stringResource(Res.string.add_to_a_playlist),
                selected = focusedIndex == keyAt("addPlaylist"),
                onClick  = onAddToPlaylist,
            )
        }

        MenuDivider()

        // ── Optional delete rows ───────────────────────────────────────────────
        if (onDelete != null) {
            MenuItem(
                icon     = SimpIcons.QueueMusic,
                text     = "Remove from playlist",
                selected = focusedIndex == keyAt("delete"),
                onClick  = { onDismiss(); onDelete() },
            )
        }
        if (onLibraryDelete != null) {
            MenuItem(
                icon     = SimpIcons.QueueMusic,
                text     = "Delete",
                selected = focusedIndex == keyAt("libDelete"),
                onClick  = { onDismiss(); onLibraryDelete() },
            )
        }

        // ── GO TO section ──────────────────────────────────────────────────────
        Spacer(Modifier.height(4.dp))
        Text(
            text     = "GO TO",
            style    = TextStyle(
                fontSize      = 10.5.sp,
                fontWeight    = FontWeight.Medium,
                color         = ContentSecondary,
                letterSpacing = 1.sp,
            ),
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp),
        )

        val artists = uiState.songUIState.listArtists
        ArtistMenuItem(
            artists  = artists,
            selected = focusedIndex == keyAt("artist"),
            onClick  = { onArtist(artists) },
            onPositioned = onArtistRowPositioned,
        )

        val albumLabel = when {
            uiState.songUIState.album == null
                -> stringResource(Res.string.no_album)
            uiState.songUIState.album?.name.isNullOrBlank()
                -> stringResource(Res.string.album)
            else
                -> uiState.songUIState.album?.name.orEmpty()
        }
        MenuItem(
            icon     = SimpIcons.Album,
            text     = albumLabel,
            enabled  = uiState.songUIState.album != null,
            selected = focusedIndex == keyAt("album"),
            onClick  = onAlbum,
        )

        MenuItem(
            icon     = SimpIcons.Sensors,
            text     = stringResource(Res.string.start_radio),
            selected = focusedIndex == keyAt("radio"),
            onClick  = onRadio,
        )

        MenuDivider()
        Spacer(Modifier.height(2.dp))

        // ── Add to queue + Share ───────────────────────────────────────────────
        MenuItemWithNote(
            icon     = SimpIcons.QueueMusic,
            text     = stringResource(Res.string.add_to_queue),
            note     = when (addToQueueBlock) {
                com.mika.domain.mediaservice.handler.AddToQueueBlock.ENDLESS_QUEUE  -> "Endless queue"
                null                                                                 -> null
            },
            enabled  = addToQueueBlock == null,
            selected = focusedIndex == keyAt("queue"),
            onClick  = onAddToQueue,
        )

        MenuItem(
            icon     = SimpIcons.Share,
            text     = stringResource(Res.string.share),
            selected = focusedIndex == keyAt("share"),
            onClick  = onShare,
        )

        Spacer(Modifier.height(4.dp))
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Sub-composables
// ──────────────────────────────────────────────────────────────────────────────

@Composable
private fun MenuDivider() {
    HorizontalDivider(
        modifier  = Modifier.fillMaxWidth(),
        thickness = 1.dp,
        color     = MenuBorder,
    )
}

/** Single list-style row in the menu. */
@Composable
private fun MenuItem(
    icon: ImageVector,
    text: String,
    enabled: Boolean = true,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    MenuItemWithNote(
        icon     = icon,
        text     = text,
        note     = null,
        enabled  = enabled,
        selected = selected,
        onClick  = onClick,
    )
}

/**
 * List-style row with an optional right-aligned [note] (shown when the item is disabled).
 * Keeps label and note both on one line with ellipsis if needed.
 */
@Composable
private fun MenuItemWithNote(
    icon: ImageVector,
    text: String,
    note: String?,
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
        label         = "menuItemBg",
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
                            indication        = null,
                            onClick           = onClick,
                        )
                else Modifier
            )
            .height(30.dp)
            .padding(horizontal = 8.dp),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = null,
                modifier           = Modifier.size(16.dp),
                tint               = if (enabled) ContentPrimary else ContentDisabled,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text     = text,
                style    = TextStyle(
                    fontSize = 13.5.sp,
                    color    = if (enabled) ContentPrimary else ContentDisabled,
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (note != null) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text      = note,
                    style     = TextStyle(
                        fontSize  = 11.sp,
                        color     = ContentDisabled,
                        textAlign = TextAlign.End,
                    ),
                    maxLines  = 1,
                    overflow  = TextOverflow.Ellipsis,
                    modifier  = Modifier.weight(1f),
                )
            }
        }
    }
}

/** Square icon button in the 4-slot toolbar row, with a plain tooltip on hover. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ToolbarButton(
    icon: ImageVector,
    tooltip: String,
    tint: Color = ContentPrimary,
    selected: Boolean = false,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            pressed || selected -> MenuHover
            hovered             -> MenuHoverLight
            else                -> Color.Transparent
        },
        animationSpec = tween(80),
        label         = "toolbarBtnBg",
    )

    TooltipBox(
        positionProvider = TooltipDefaults.rememberPlainTooltipPositionProvider(),
        tooltip = {
            PlainTooltip {
                Text(
                    text  = tooltip,
                    style = TextStyle(fontSize = 11.sp),
                )
            }
        },
        state = rememberTooltipState(),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(bgColor)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = interactionSource,
                    indication        = null,
                    onClick           = onClick,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector        = icon,
                contentDescription = tooltip,
                modifier           = Modifier.size(18.dp),
                tint               = tint,
            )
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Artist menu-item + submenu
// ──────────────────────────────────────────────────────────────────────────────

/**
 * "Artists" row in the GO-TO section.
 * - Single artist → plain row (no count badge, no chevron).
 * - Multiple artists → shows "(n)" count and a small chevron on the right.
 *
 * Captures its window-space bounds via [onPositioned] so the submenu knows
 * where to anchor itself.
 */
@Composable
private fun ArtistMenuItem(
    artists: List<Artist>,
    selected: Boolean,
    onClick: () -> Unit,
    onPositioned: (IntRect) -> Unit,
) {
    val multipleArtists = artists.size > 1
    val interactionSource = remember { MutableInteractionSource() }
    val hovered by interactionSource.collectIsHoveredAsState()
    val pressed by interactionSource.collectIsPressedAsState()

    val bgColor by animateColorAsState(
        targetValue = when {
            pressed || selected -> MenuHover
            hovered             -> MenuHoverLight
            else                -> Color.Transparent
        },
        animationSpec = tween(80),
        label         = "artistMenuItemBg",
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 1.dp)
            .clip(RoundedCornerShape(5.dp))
            .background(bgColor)
            .hoverable(interactionSource)
            .clickable(
                interactionSource = interactionSource,
                indication        = null,
                onClick           = onClick,
            )
            .height(30.dp)
            .padding(horizontal = 8.dp)
            .onGloballyPositioned { coords ->
                val bounds = coords.boundsInWindow()
                onPositioned(
                    IntRect(
                        left   = bounds.left.roundToInt(),
                        top    = bounds.top.roundToInt(),
                        right  = bounds.right.roundToInt(),
                        bottom = bounds.bottom.roundToInt(),
                    )
                )
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier          = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector        = SimpIcons.PeopleAlt,
                contentDescription = null,
                modifier           = Modifier.size(16.dp),
                tint               = ContentPrimary,
            )
            Spacer(Modifier.width(10.dp))
            Text(
                text     = stringResource(Res.string.artists),
                style    = TextStyle(fontSize = 13.5.sp, color = ContentPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (multipleArtists) {
                Spacer(Modifier.width(6.dp))
                Text(
                    text  = "(${artists.size})",
                    style = TextStyle(fontSize = 11.sp, color = ContentSecondary),
                )
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector        = SimpIcons.ArrowForwardIos,
                    contentDescription = null,
                    modifier           = Modifier.size(10.dp),
                    tint               = ContentSecondary,
                )
            }
        }
    }
}

/**
 * Positions the artist submenu to the **right** of the main-menu's artists row
 * ([anchorBounds]).  If there's not enough room on the right it opens to the left.
 * Result is always clamped [margin] px inside the window.
 */
private class ArtistSubmenuPositionProvider(
    private val anchorBounds: IntRect?,
    private val margin: Int = 8,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val effective = this.anchorBounds ?: anchorBounds
        val gap = 4  // px gap between main menu edge and submenu

        // Prefer opening to the right of the main-menu row.
        val rightX = effective.right + gap
        val leftX  = effective.left - popupContentSize.width - gap

        val x = if (rightX + popupContentSize.width + margin <= windowSize.width) rightX else leftX

        // Align top of submenu with top of the artists row, clamped to window.
        val maxY = windowSize.height - popupContentSize.height - margin
        val y = effective.top.coerceIn(margin, maxOf(margin, maxY))

        val maxX = windowSize.width - popupContentSize.width - margin
        return IntOffset(x.coerceIn(margin, maxOf(margin, maxX)), y)
    }
}

/**
 * Small popup listing each artist.  Matches the main-menu visual style.
 * Width is 232dp.  Closes on Esc or click-outside.
 */
@Composable
private fun ArtistSubmenu(
    artists: List<Artist>,
    anchorBounds: IntRect?,
    onArtistClick: (Artist) -> Unit,
    onDismiss: () -> Unit,
) {
    Popup(
        popupPositionProvider = remember(anchorBounds) {
            ArtistSubmenuPositionProvider(anchorBounds = anchorBounds, margin = 8)
        },
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            focusable             = true,
            dismissOnBackPress    = true,
            dismissOnClickOutside = true,
        ),
    ) {
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
                .width(232.dp)
                .wrapContentHeight()
                .onKeyEvent { event ->
                    if (event.type == KeyEventType.KeyDown && event.key == Key.Escape) {
                        onDismiss(); true
                    } else false
                },
        ) {
            Column(modifier = Modifier.padding(vertical = 4.dp)) {
                artists.forEach { artist ->
                    val interactionSource = remember { MutableInteractionSource() }
                    val hovered by interactionSource.collectIsHoveredAsState()
                    val pressed by interactionSource.collectIsPressedAsState()
                    val bgColor by animateColorAsState(
                        targetValue = when {
                            pressed -> MenuHover
                            hovered -> MenuHoverLight
                            else    -> Color.Transparent
                        },
                        animationSpec = tween(80),
                        label         = "artistSubBg",
                    )
                    val enabled = !artist.id.isNullOrBlank()

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
                                            indication        = null,
                                            onClick           = { onArtistClick(artist) },
                                        )
                                else Modifier
                            )
                            .height(30.dp)
                            .padding(horizontal = 8.dp),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector        = SimpIcons.PeopleAlt,
                                contentDescription = null,
                                modifier           = Modifier.size(16.dp),
                                tint               = if (enabled) ContentPrimary else ContentDisabled,
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text     = artist.name,
                                style    = TextStyle(
                                    fontSize = 13.5.sp,
                                    color    = if (enabled) ContentPrimary else ContentDisabled,
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                }
            }
        }
    }
}

// ──────────────────────────────────────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────────────────────────────────────

/**
 * Converts a [LayoutCoordinates] position-in-window to an [IntOffset], rounding to the nearest pixel.
 * Used by [SongFullWidthItems] to convert a right-click cursor position to window coordinates.
 */
internal fun LayoutCoordinates.positionInWindowOffset(): IntOffset {
    val pos = positionInWindow()
    return IntOffset(pos.x.roundToInt(), pos.y.roundToInt())
}
