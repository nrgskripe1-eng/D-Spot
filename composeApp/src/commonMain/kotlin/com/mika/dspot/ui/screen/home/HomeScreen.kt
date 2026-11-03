package com.mika.dspot.ui.screen.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.SnapPosition
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.currentStateAsState
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.CachePolicy
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.eygraber.uri.Uri
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.mika.common.CHART_SUPPORTED_COUNTRY
import com.mika.common.Config
import com.mika.domain.data.model.browse.album.Track
import com.mika.domain.data.model.home.HomeItem
import com.mika.domain.data.model.home.chart.Chart
import com.mika.domain.data.model.intent.GenericIntent
import com.mika.domain.data.model.mood.Mood
import com.mika.domain.extension.now
import com.mika.domain.manager.DataStoreManager.Values.TRUE
import com.mika.domain.mediaservice.handler.PlaylistType
import com.mika.domain.mediaservice.handler.QueueData
import com.mika.domain.utils.toSongEntity
import com.mika.domain.utils.toTrack
import com.mika.logger.Logger
import com.mika.dspot.Platform
import com.mika.dspot.expect.openUrl
import com.mika.dspot.expect.ui.HorizontalScrollBar
import com.mika.dspot.extension.angledGradientBackground
import com.mika.dspot.extension.artworkScrimBrush
import com.mika.dspot.extension.getScreenSizeInfo
import com.mika.dspot.extension.isScrollingUp
import com.mika.dspot.extension.rgbFactor
import com.mika.dspot.extension.ultraThinBarStyle
import com.mika.dspot.getPlatform
import com.mika.dspot.ui.component.BlogPromoDialog
import com.mika.dspot.ui.component.CenterLoadingBox
import com.mika.dspot.ui.component.Chip
import com.mika.dspot.ui.component.DropdownButton
import com.mika.dspot.ui.component.EndOfPage
import com.mika.dspot.ui.component.FootgunsStarDialog
import com.mika.dspot.ui.component.HomeDiscoverSection
import com.mika.dspot.ui.component.HomeItem
import com.mika.dspot.ui.component.HomeItemContentPlaylist
import com.mika.dspot.ui.component.HomeShimmer
import com.mika.dspot.ui.component.ItemArtistChart
import com.mika.dspot.ui.component.ListenTogetherIconButton
import com.mika.dspot.ui.component.LocalAppDialogOpen
import com.mika.dspot.ui.component.LocalNowPlayingOpen
import com.mika.dspot.ui.component.LocalUserStarted
import com.mika.dspot.ui.component.MoodMomentAndGenreHomeItem
import com.mika.dspot.ui.component.NowPlayingBottomSheet
import com.mika.dspot.ui.component.OfflineErrorState
import com.mika.dspot.ui.component.PromoBanner
import com.mika.dspot.ui.component.PromoBannerData
import com.mika.dspot.ui.component.QuickPicksItem
import com.mika.dspot.ui.component.ReviewDialog
import com.mika.dspot.ui.component.RippleIconButton
import com.mika.dspot.ui.component.ShareSavedLyricsDialog
import com.mika.dspot.ui.component.homeCardAspectRatio
import com.mika.dspot.ui.component.rememberHolderPainter
import com.mika.dspot.ui.icon.Groups
import com.mika.dspot.ui.icon.History
import com.mika.dspot.ui.icon.MoreVert
import com.mika.dspot.ui.icon.Notifications
import com.mika.dspot.ui.icon.PlayArrow
import com.mika.dspot.ui.icon.Settings
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.navigation.destination.home.HomeDestination
import com.mika.dspot.ui.navigation.destination.home.ListenTogetherDestination
import com.mika.dspot.ui.navigation.destination.home.MoodDestination
import com.mika.dspot.ui.navigation.destination.home.NotificationDestination
import com.mika.dspot.ui.navigation.destination.home.RecentlySongsDestination
import com.mika.dspot.ui.navigation.destination.home.SettingsDestination
import com.mika.dspot.ui.navigation.destination.account.AccountDestination
import com.mika.dspot.ui.navigation.destination.library.LibraryDestination
import com.mika.dspot.ui.navigation.destination.library.LibraryDynamicPlaylistDestination
import com.mika.dspot.ui.navigation.destination.list.ArtistDestination
import com.mika.dspot.ui.navigation.destination.list.PodcastDestination
import com.mika.dspot.ui.navigation.destination.list.PlaylistDestination
import com.mika.dspot.ui.navigation.destination.login.LoginDestination
import com.mika.dspot.ui.screen.library.LibraryDynamicPlaylistType
import com.mika.dspot.ui.theme.desktopPanelDark
import com.mika.dspot.ui.theme.typo
import com.mika.dspot.utils.VersionManager
import com.mika.dspot.utils.eligiblePromos
import com.mika.dspot.viewModel.FOOTGUNS_STAR_KEY
import com.mika.dspot.viewModel.HomeViewModel
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_COMMUTE
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_ENERGIZE
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_FEEL_GOOD
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_FOCUS
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_PARTY
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_RELAX
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_ROMANCE
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_SAD
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_SLEEP
import com.mika.dspot.viewModel.HomeViewModel.Companion.HOME_PARAMS_WORKOUT
import com.mika.dspot.viewModel.ListState
import com.mika.dspot.viewModel.SharedViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.all
import dspot.composeapp.generated.resources.app_name
import dspot.composeapp.generated.resources.cancel
import dspot.composeapp.generated.resources.chart
import dspot.composeapp.generated.resources.commute
import dspot.composeapp.generated.resources.do_not_show_again
import dspot.composeapp.generated.resources.energize
import dspot.composeapp.generated.resources.feel_good
import dspot.composeapp.generated.resources.focus
import dspot.composeapp.generated.resources.go_to_log_in_page
import dspot.composeapp.generated.resources.good_afternoon
import dspot.composeapp.generated.resources.good_evening
import dspot.composeapp.generated.resources.good_morning
import dspot.composeapp.generated.resources.good_night
import dspot.composeapp.generated.resources.let_s_pick_a_playlist_for_you
import dspot.composeapp.generated.resources.let_s_start_with_a_radio
import dspot.composeapp.generated.resources.log_in_warning
import dspot.composeapp.generated.resources.party
import dspot.composeapp.generated.resources.play_all
import dspot.composeapp.generated.resources.quick_picks
import dspot.composeapp.generated.resources.relax
import dspot.composeapp.generated.resources.romance
import dspot.composeapp.generated.resources.sad
import dspot.composeapp.generated.resources.sleep
import dspot.composeapp.generated.resources.listen_together
import dspot.composeapp.generated.resources.recently_added
import dspot.composeapp.generated.resources.settings
import dspot.composeapp.generated.resources.top_artists
import dspot.composeapp.generated.resources.warning
import dspot.composeapp.generated.resources.welcome_back
import dspot.composeapp.generated.resources.what_is_best_choice_today
import dspot.composeapp.generated.resources.workout

// DataStore key for blog-promo one-shot dialog. Bump the suffix (v2, v3, …) to re-promote.
private const val BLOG_PROMO_KEY = "blog_promo_v1_seen"

// Launch banner timing: how long Home's own launch dialogs get to settle, and how long the screen must
// stay free of other dialogs before the banner draws.
private const val PROMO_SETTLE_MS = 1_000L
private const val PROMO_CLEAR_MS = 400L

private val listOfHomeChip =
    listOf(
        Res.string.all,
        Res.string.relax,
        Res.string.sleep,
        Res.string.energize,
        Res.string.sad,
        Res.string.romance,
        Res.string.feel_good,
        Res.string.workout,
        Res.string.party,
        Res.string.commute,
        Res.string.focus,
    )

@OptIn(ExperimentalMaterial3Api::class)
@ExperimentalFoundationApi
@Composable
fun HomeScreen(
    onScrolling: (onTop: Boolean) -> Unit = {},
    viewModel: HomeViewModel =
        koinViewModel(),
    sharedViewModel: SharedViewModel =
        koinInject(),
    navController: NavController,
) {
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    val isScrollingUp by scrollState.isScrollingUp()
    val accountInfo by viewModel.accountInfo.collectAsStateWithLifecycle()
    val homeData by viewModel.homeItemList.collectAsStateWithLifecycle()
    val newRelease by viewModel.newRelease.collectAsStateWithLifecycle()
    val chart by viewModel.chart.collectAsStateWithLifecycle()
    val moodMomentAndGenre by viewModel.exploreMoodItem.collectAsStateWithLifecycle()
    val moodArtwork by viewModel.moodArtwork.collectAsStateWithLifecycle()
    val chartLoading by viewModel.loadingChart.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    var accountShow by rememberSaveable {
        mutableStateOf(false)
    }
    val regionChart by viewModel.regionCodeChart.collectAsStateWithLifecycle()
    val reloadDestination by sharedViewModel.reloadDestination.collectAsStateWithLifecycle()
    val pullToRefreshState = rememberPullToRefreshState()
    var isRefreshing by remember { mutableStateOf(false) }
    val chipRowState = rememberScrollState()
    val params by viewModel.params.collectAsStateWithLifecycle()
    val homeListState by viewModel.homeListState.collectAsStateWithLifecycle()
    val continuation by viewModel.continuation.collectAsStateWithLifecycle()

    val shouldShowLogInAlert by viewModel.showLogInAlert.collectAsStateWithLifecycle()

    val openAppTime by sharedViewModel.openAppTime.collectAsStateWithLifecycle()
    val shareLyricsPermissions by sharedViewModel.shareSavedLyrics.collectAsStateWithLifecycle()

    // Launch banner inputs. App.kt's own dialogs, its Now Playing sheet and whether the user has
    // started using the app come down from App.kt; Home's dialogs are read where the banner is drawn.
    val appDialogOpen = LocalAppDialogOpen.current
    val nowPlayingOpen = LocalNowPlayingOpen.current
    val userStarted by rememberUpdatedState(LocalUserStarted.current)
    // Home's own lifecycle: RESUMED only while it is the screen in front (not mid-transition to another
    // tab, not behind a system prompt). On Desktop STARTED means its window is showing.
    val homeLifecycle by LocalLifecycleOwner.current.lifecycle.currentStateAsState()
    // The language part of the BCP 47 tag: Android's Locale.language still reports the legacy "in"
    // for Indonesian and "iw" for Hebrew, while the config is keyed "id" and "he".
    val language = Locale.current.toLanguageTag().substringBefore('-')

    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    // What is ACTUALLY painted behind this screen. The desktop shell wraps content in a rounded
    // panel (App.kt: surfaceContainer on light, desktopPanelDark on dark) — deliberately not
    // colorScheme.background — so a gradient tail aimed at colorScheme.background ends on the
    // wrong colour and draws a seam where the first item stops. Same light check as App.kt's
    // isLightScheme.
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    var topHeaderColor by remember {
        mutableStateOf(backgroundColor)
    }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val mainHomeThumbnail by viewModel.mainHomeThumbnail.collectAsStateWithLifecycle()
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )

    LaunchedEffect(mainHomeThumbnail) {
        mainHomeThumbnail?.let {
            dominantColorState.updateFrom(Url(it))
        }
    }

    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            // Light theme: pull the artwork color toward white for a soft pastel header;
            // dark theme keeps the original darkened tone.
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }

    var showReviewDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showRequestShareLyricsPermissions by rememberSaveable {
        mutableStateOf(false)
    }
    var showBlogPromoDialog by rememberSaveable {
        mutableStateOf(false)
    }
    var showFootgunsDialog by rememberSaveable {
        mutableStateOf(false)
    }
    // Decided once per launch (saveable, so a rotation does not count as a new launch).
    var promoLaunchDecided by rememberSaveable {
        mutableStateOf(false)
    }
    var promoBanner by remember {
        mutableStateOf<PromoBannerData?>(null)
    }
    // The banner that has gone a moment with nothing keeping it off screen; see where it is drawn.
    var promoClearFor by remember {
        mutableStateOf<PromoBannerData?>(null)
    }

    var topAppBarHeightPx by rememberSaveable {
        mutableIntStateOf(0)
    }

    val hazeState =
        rememberHazeState()

    LaunchedEffect(scrollState) {
        snapshotFlow { scrollState.firstVisibleItemIndex }
            .collect {
                if (it <= 1) {
                    onScrolling.invoke(true)
                } else {
                    onScrolling.invoke(isScrollingUp)
                }
            }
    }

    val onRefresh: () -> Unit = {
        isRefreshing = true
        viewModel.getHomeItemList(params)
        Logger.w("HomeScreen", "onRefresh")
    }
    LaunchedEffect(key1 = reloadDestination) {
        if (reloadDestination == HomeDestination::class) {
            if (scrollState.firstVisibleItemIndex > 1) {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                scrollState.animateScrollToItem(0)
                sharedViewModel.reloadDestinationDone()
            } else {
                Logger.w("HomeScreen", "scrollState.firstVisibleItemIndex: ${scrollState.firstVisibleItemIndex}")
                onRefresh.invoke()
            }
        }
    }
    LaunchedEffect(key1 = loading) {
        if (!loading) {
            isRefreshing = false
            sharedViewModel.reloadDestinationDone()
            coroutineScope.launch {
                pullToRefreshState.animateToHidden()
            }
        }
    }
    LaunchedEffect(key1 = homeData) {
        accountShow = homeData.find { it.subtitle == accountInfo?.first } == null
    }
    LaunchedEffect(openAppTime, shareLyricsPermissions) {
        Logger.w("HomeScreen", "openAppTime: $openAppTime, shareLyricsPermissions: $shareLyricsPermissions")
        if (openAppTime >= 10 && openAppTime % 10 == 0 && openAppTime <= 50) {
            showReviewDialog = true
        } else if ((openAppTime == 1 || openAppTime % 15 == 0) && openAppTime <= 60 && !shareLyricsPermissions) {
            showRequestShareLyricsPermissions = true
        } else if (openAppTime == 5) {
            // Blog promo: one-shot after 5 app opens, bump key suffix to re-promote later
            if (sharedViewModel.getString(BLOG_PROMO_KEY) != "true") {
                showBlogPromoDialog = true
            }
        } else if (openAppTime % 10 == 6 &&
            openAppTime <= 46 &&
            sharedViewModel.getString(FOOTGUNS_STAR_KEY) != "true"
        ) {
            // kotlin-footguns star prompt: 6, 16, 26, 36, 46 - one open after each review milestone,
            // and clear of the share-lyrics (15, 45) and blog-promo (5) milestones
            showFootgunsDialog = true
        } else {
            showReviewDialog = false
            showFootgunsDialog = false
            showRequestShareLyricsPermissions = false
        }
    }

    val shouldStartPaginate =
        remember {
            derivedStateOf {
                homeListState != ListState.PAGINATION_EXHAUST &&
                    !scrollState.isScrollInProgress &&
                    (
                        scrollState.layoutInfo.visibleItemsInfo
                            .lastOrNull()
                            ?.index ?: -9
                    ) >= (scrollState.layoutInfo.totalItemsCount - 3)
            }
        }

    LaunchedEffect(Unit) {
        snapshotFlow { Triple(shouldStartPaginate.value, homeListState, continuation) }
            .collect { (shouldPaginate, state, cont) ->
                if (shouldPaginate && state == ListState.IDLE && !cont.isNullOrEmpty()) {
                    viewModel.getContinueHomeItem(cont)
                }
            }
    }

//    if (shouldShowGetDataSyncIdBottomSheet) {
//        GetDataSyncIdBottomSheet(
//            cookie = youTubeCookie,
//            onDismissRequest = {
//                shouldShowGetDataSyncIdBottomSheet = false
//            },
//        )
//    }

    if (showReviewDialog) {
        ReviewDialog(
            onDismissRequest = {
                sharedViewModel.onDoneReview(
                    isDismissOnly = true,
                )
                showReviewDialog = false
            },
            onDoneReview = {
                sharedViewModel.onDoneReview(
                    isDismissOnly = false,
                )
                showReviewDialog = false
            },
        )
    }

    if (showFootgunsDialog) {
        FootgunsStarDialog(
            onDismissRequest = {
                // "Later" advances OPEN_APP_TIME, the same way the review and share-lyrics dialogs do.
                // Home's launch effect runs again every time Home re-enters composition, reading the
                // stored count; leaving it untouched kept the milestone condition true, so the prompt
                // came back on every return to Home until the app was restarted.
                showFootgunsDialog = false
                sharedViewModel.onDoneReview(isDismissOnly = true)
            },
            onDoneStar = {
                sharedViewModel.putString(FOOTGUNS_STAR_KEY, "true")
                showFootgunsDialog = false
            },
        )
    }

    if (showBlogPromoDialog) {
        BlogPromoDialog(
            onDismissRequest = {
                sharedViewModel.putString(BLOG_PROMO_KEY, "true")
                showBlogPromoDialog = false
            },
            onVisitBlog = {
                sharedViewModel.putString(BLOG_PROMO_KEY, "true")
                showBlogPromoDialog = false
            },
        )
    }

    if (showRequestShareLyricsPermissions) {
        ShareSavedLyricsDialog(
            onDismissRequest = {
                showRequestShareLyricsPermissions = false
                sharedViewModel.onDoneReview(
                    isDismissOnly = true,
                )
            },
            onConfirm = { contributor ->
                sharedViewModel.onDoneRequestingShareLyrics(
                    contributor,
                )
            },
        )
    }

    val otherDialogOpen =
        appDialogOpen || showReviewDialog || showRequestShareLyricsPermissions || showBlogPromoDialog ||
            showFootgunsDialog || shouldShowLogInAlert
    // The launch banner, decided once per launch. It waits for Home to settle on its own dialogs (the
    // open count and the lyrics-sharing flag load just after the first frame) and for the update check
    // started at launch, however long that takes, so an update dialog comes first. Then it takes the
    // first banner this version has not shown yet.
    LaunchedEffect(Unit) {
        if (promoLaunchDecided) return@LaunchedEffect
        // Marked at the start, not the end: leaving Home cancels this for good (by then the user is
        // busy elsewhere), and a value written while Home is being left would not be saved.
        promoLaunchDecided = true
        snapshotFlow { openAppTime }.first { it > 0 }
        delay(PROMO_SETTLE_MS)
        sharedViewModel.isCheckingUpdate.first { !it }
        if (userStarted) {
            Logger.i("Promo", "Skipped: the user started before the banner's turn")
            return@LaunchedEffect
        }
        val localTracking = sharedViewModel.getLocalTrackingEnabled().first() == TRUE
        val youTubeLoggedIn = sharedViewModel.getYouTubeLoggedIn().first() == TRUE
        val shown = sharedViewModel.shownPromos()
        val promos = sharedViewModel.cachedPromos().first()
        val eligible = eligiblePromos(promos, VersionManager.getVersionName(), language, localTracking, youTubeLoggedIn)
        val banner =
            eligible
                .firstOrNull { (promo, _) -> promo.id.orEmpty() !in shown }
                ?.let { (promo, url) -> PromoBannerData(id = promo.id.orEmpty(), imageUrl = url, link = promo.link.orEmpty()) }
        when {
            banner == null ->
                Logger.i(
                    "Promo",
                    "Nothing to show: ${eligible.size} of ${promos.size} banners usable here " +
                        "(local tracking ${if (localTracking) "on" else "off"}, " +
                        "YouTube ${if (youTubeLoggedIn) "signed in" else "signed out"}), " +
                        "already shown in ${VersionManager.getVersionName()}: $shown",
                )

            userStarted -> Logger.i("Promo", "Skipped ${banner.id}: the user started before its turn")

            else -> {
                Logger.i("Promo", "Picked ${banner.id}, waiting for its turn")
                promoBanner = banner
            }
        }
    }
    // Everything that keeps the banner off screen: any other dialog (it waits its turn behind them),
    // Now Playing, and Home not being in front. On Desktop that only asks for a showing window: one
    // opened from a terminal or an IDE may start without focus, and the click that focuses it would
    // count as the user starting, so the banner would never get its turn.
    val homeInFront =
        homeLifecycle.isAtLeast(
            if (getPlatform() == Platform.Desktop) Lifecycle.State.STARTED else Lifecycle.State.RESUMED,
        )
    val promoBlocked = otherDialogOpen || nowPlayingOpen || !homeInFront
    // It draws only once the banner has gone a moment unblocked since it was decided. The wait belongs
    // to that banner, so nothing left over from before it arrived can let it through early: it never
    // flashes in the gap between two dialogs, and the update dialog, raised a frame after its check
    // ends, still gets there first.
    LaunchedEffect(promoBlocked, promoBanner) {
        val banner = promoBanner
        promoClearFor = null
        if (!promoBlocked && banner != null) {
            delay(PROMO_CLEAR_MS)
            promoClearFor = banner
        }
    }
    // Whatever the user starts before the banner's turn puts it off to a later launch, uncounted.
    LaunchedEffect(userStarted) {
        val pending = promoBanner
        if (userStarted && pending != null) {
            Logger.i("Promo", "Put off ${pending.id}: the user started before its turn")
            promoBanner = null
        }
    }
    promoBanner?.takeIf { it == promoClearFor && !promoBlocked }?.let { banner ->
        LaunchedEffect(banner.id) { Logger.i("Promo", "On screen: ${banner.id}") }
        PromoBanner(
            banner = banner,
            onOpen = {
                Logger.i("Promo", "Opened ${banner.id}: ${banner.link}")
                promoBanner = null
                sharedViewModel.markPromoShown(banner.id)
                val link = banner.link
                when {
                    // The same router as links from the web and the widgets.
                    link.startsWith("dspot://") -> {
                        runCatching { Uri.parse(link) }.getOrNull()?.let {
                            sharedViewModel.setIntent(GenericIntent(data = it))
                        }
                    }

                    // openUrl rather than LocalUriHandler, which throws where no browser answers.
                    link.startsWith("https://", ignoreCase = true) -> {
                        runCatching { openUrl(link) }
                    }
                    // Anything else is a mistake in the config: ignoring it beats crashing on a tap.
                }
            },
            onClose = {
                Logger.i("Promo", "Closed ${banner.id}")
                promoBanner = null
                sharedViewModel.markPromoShown(banner.id)
            },
            // No picture, no banner: closed without counting, so a later launch tries again.
            onFailed = {
                Logger.w("Promo", "Picture failed to load for ${banner.id}; closed without counting it")
                promoBanner = null
            },
        )
    }

    if (shouldShowLogInAlert) {
        var doNotShowAgain by rememberSaveable {
            mutableStateOf(false)
        }
        AlertDialog(
            title = {
                Text(stringResource(Res.string.warning))
            },
            text = {
                Column {
                    Text(text = stringResource(Res.string.log_in_warning))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    doNotShowAgain = !doNotShowAgain
                                }.fillMaxWidth(),
                    ) {
                        Checkbox(
                            checked = doNotShowAgain,
                            onCheckedChange = {
                                doNotShowAgain = it
                            },
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(stringResource(Res.string.do_not_show_again))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                    navController.navigate(LoginDestination)
                }) {
                    Text(stringResource(Res.string.go_to_log_in_page))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.doneShowLogInAlert(doNotShowAgain)
                }) {
                    Text(stringResource(Res.string.cancel))
                }
            },
            onDismissRequest = {
                viewModel.doneShowLogInAlert()
            },
        )
    }

    Box {
        val isDesktop = getPlatform() == Platform.Desktop
        PullToRefreshBox(
            modifier =
                Modifier
                    .hazeSource(hazeState),
            state = pullToRefreshState,
            onRefresh = onRefresh,
            isRefreshing = isRefreshing,
            indicator = {
                if (!isDesktop) {
                    PullToRefreshDefaults.Indicator(
                        state = pullToRefreshState,
                        isRefreshing = isRefreshing,
                        modifier =
                            Modifier
                                .align(Alignment.TopCenter)
                                .padding(
                                    top =
                                        with(LocalDensity.current) {
                                            topAppBarHeightPx.toDp()
                                        },
                                ),
                        containerColor = PullToRefreshDefaults.indicatorContainerColor,
                        color = PullToRefreshDefaults.indicatorColor,
                        maxDistance = PullToRefreshDefaults.PositionalThreshold,
                    )
                }
            },
        ) {
            Crossfade(targetState = loading, label = "Home Shimmer") { loading ->
                if (!loading) {
                    if (homeData.isEmpty()) {
                        OfflineErrorState(
                            onRetry = onRefresh,
                            onOpenDownloaded = {
                                navController.navigate(
                                    LibraryDynamicPlaylistDestination(
                                        type = LibraryDynamicPlaylistType.Downloaded.toStringParams(),
                                    ),
                                )
                            },
                        )
                        return@Crossfade
                    }
                    val isDesktopLayout = getPlatform() == Platform.Desktop
                    LazyColumn(
                        state = scrollState,
                        verticalArrangement = Arrangement.spacedBy(if (isDesktopLayout) 24.dp else 12.dp),
                    ) {
                        itemsIndexed(homeData, key = { index, item ->
                            "home_item_${item.title}_$index"
                        }) { index, item ->
                            Box {
                                if (index == 0) {
                                    Box(
                                        modifier =
                                            Modifier
                                                // matchParentSize, not height(300.dp): 300 is the
                                                // height of this shelf ON A PHONE. On a desktop
                                                // window the first item is taller, the gradient
                                                // stopped mid-item and everything below it fell
                                                // back to the flat background — a hard colour seam
                                                // straight across Home. Sized by the item, the
                                                // bottom scrim always lands on the item's edge.
                                                .matchParentSize()
                                                .angledGradientBackground(listOf(animatedColor, pageBackground), 25f),
                                    ) {
                                        Box(
                                            modifier =
                                                Modifier
                                                    .fillMaxWidth()
                                                    .height(180.dp)
                                                    .align(Alignment.BottomCenter)
                                                    .background(artworkScrimBrush(pageBackground)),
                                        )
                                    }
                                }
                                Column(
                                    modifier =
                                        Modifier
                                            .padding(horizontal = if (isDesktop) 24.dp else 15.dp),
                                ) {
                                    if (index == 0) {
                                        Spacer(
                                            Modifier.height(
                                                with(LocalDensity.current) { topAppBarHeightPx.toDp() },
                                            ),
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    if (index == 0 && accountInfo != null && accountShow) {
                                        AccountLayout(
                                            accountName = accountInfo?.first ?: "",
                                            url = accountInfo?.second ?: "",
                                        )
                                        Spacer(Modifier.height(8.dp))
                                    }
                                    if (item.title == stringResource(Res.string.quick_picks)) {
                                        AnimatedVisibility(
                                            visible =
                                                homeData.find {
                                                    it.title ==
                                                        stringResource(
                                                            Res.string.quick_picks,
                                                        )
                                                } != null,
                                        ) {
                                            QuickPicks(
                                                homeItem =
                                                    (
                                                        homeData.find {
                                                            it.title ==
                                                                stringResource(
                                                                    Res.string.quick_picks,
                                                                )
                                                        } ?: return@AnimatedVisibility
                                                    ).let { content ->
                                                        content.copy(
                                                            contents =
                                                                content.contents.mapNotNull { ct ->
                                                                    ct?.copy(
                                                                        artists =
                                                                            ct.artists?.let { art ->
                                                                                if (art.size > 1) {
                                                                                    art.dropLast(1)
                                                                                } else {
                                                                                    art
                                                                                }
                                                                            },
                                                                    )
                                                                },
                                                        )
                                                    },
                                                navController = navController,
                                                viewModel = viewModel,
                                            )
                                        }
                                    } else if (item.isSongCardShelf()) {
                                        HomeDiscoverSection(
                                            data = item,
                                            navController = navController,
                                            homeViewModel = viewModel,
                                        )
                                    } else {
                                        HomeItem(
                                            navController = navController,
                                            data = item,
                                        )
                                    }
                                }
                            }
                        }
                        item(key = "paginating_spinner") {
                            val isExhausted = homeListState == ListState.PAGINATION_EXHAUST
                            AnimatedVisibility(
                                visible = !isExhausted,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(200.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    AnimatedVisibility(
                                        homeListState == ListState.PAGINATING,
                                        enter = fadeIn(),
                                        exit = fadeOut(),
                                    ) {
                                        CenterLoadingBox(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(200.dp),
                                        )
                                    }
                                }
                            }
                        }
                        itemsIndexed(newRelease, key = { index, releaseItem -> "new_release_${releaseItem.title}_$index" }) { _, releaseItem ->
                            AnimatedVisibility(
                                visible = homeListState == ListState.PAGINATION_EXHAUST && newRelease.isNotEmpty(),
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 15.dp)) {
                                    HomeItem(navController = navController, data = releaseItem)
                                }
                            }
                        }
                        item(key = "mood_moment_genre") {
                            AnimatedVisibility(
                                visible = homeListState == ListState.PAGINATION_EXHAUST && moodMomentAndGenre != null,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Box(modifier = Modifier.padding(horizontal = 15.dp)) {
                                    moodMomentAndGenre?.let {
                                        MoodMomentAndGenre(
                                            mood = it,
                                            navController = navController,
                                            moodArtwork = moodArtwork,
                                            onRequestArtwork = viewModel::loadMoodArtwork,
                                        )
                                    }
                                }
                            }
                        }
                        item(key = "chart_section") {
                            AnimatedVisibility(
                                visible = homeListState == ListState.PAGINATION_EXHAUST,
                                enter = fadeIn(),
                                exit = fadeOut(),
                            ) {
                                Column(
                                    Modifier
                                        .padding(vertical = 10.dp)
                                        .padding(horizontal = 15.dp),
                                    verticalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    ChartTitle()
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(targetState = regionChart) {
                                        if (it != null) {
                                            DropdownButton(
                                                items = CHART_SUPPORTED_COUNTRY.itemsData.toList(),
                                                defaultSelected =
                                                    CHART_SUPPORTED_COUNTRY.itemsData.getOrNull(
                                                        CHART_SUPPORTED_COUNTRY.items.indexOf(it),
                                                    )
                                                        ?: CHART_SUPPORTED_COUNTRY.itemsData[1],
                                            ) {
                                                viewModel.exploreChart(
                                                    CHART_SUPPORTED_COUNTRY.items[
                                                        CHART_SUPPORTED_COUNTRY.itemsData.indexOf(it),
                                                    ],
                                                )
                                            }
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(5.dp))
                                    Crossfade(
                                        targetState = chartLoading,
                                        label = "Chart",
                                    ) { loading ->
                                        if (!loading) {
                                            chart?.let {
                                                ChartData(
                                                    chart = it,
                                                    navController = navController,
                                                )
                                            }
                                        } else {
                                            CenterLoadingBox(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(400.dp),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        item(key = "end_of_page") {
                            EndOfPage()
                        }
                    }
                } else {
                    Column {
                        Spacer(
                            Modifier.height(
                                with(LocalDensity.current) {
                                    topAppBarHeightPx.toDp()
                                },
                            ),
                        )
                        HomeShimmer()
                    }
                }
            }
        }
        AnimatedContent(
            targetState = scrollState.firstVisibleItemIndex == 0 && scrollState.firstVisibleItemScrollOffset == 0,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
        ) { target ->
            Column(
                modifier =
                    Modifier
                        .align(Alignment.TopCenter)
                        .then(
                            if (target) {
                                Modifier.background(Color.Transparent)
                            } else {
                                Modifier
                                    .hazeBlur(HazeInput.Sources(hazeState), ultraThinBarStyle())
                            },
                        ).onGloballyPositioned { coordinates ->
                            topAppBarHeightPx = coordinates.size.height
                        },
            ) {
                if (getPlatform() != Platform.Desktop) {
                    AnimatedVisibility(
                        visible = isScrollingUp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        HomeTopAppBar(navController)
                    }
                    AnimatedVisibility(
                        visible = !isScrollingUp,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically(),
                    ) {
                        Spacer(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .windowInsetsPadding(
                                        WindowInsets.statusBars,
                                    ),
                        )
                    }
                }
                if (isDesktop) {
                    // Desktop: pill nav (Home / Library / Account) on the left,
                    // mood filter chips on the right — separated by a flexible spacer.
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Pill navigation tabs
                        Row(
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(Color(0xFF13141A))
                                    .padding(3.dp),
                            horizontalArrangement = Arrangement.spacedBy(2.dp),
                        ) {
                            DesktopNavPill(
                                label = "Home",
                                selected = true,
                                onClick = {
                                    coroutineScope.launch {
                                        if (scrollState.firstVisibleItemIndex > 0 || scrollState.firstVisibleItemScrollOffset > 0) {
                                            scrollState.animateScrollToItem(0)
                                        }
                                        onRefresh()
                                    }
                                },
                            )
                            DesktopNavPill(
                                label = "Library",
                                selected = false,
                                onClick = {
                                    navController.navigate(LibraryDestination) {
                                        popUpTo(HomeDestination) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                            val accountAvatar = accountInfo?.second
                            val accountName = accountInfo?.first
                            DesktopNavPill(
                                label = if (!accountName.isNullOrEmpty()) accountName else "Account",
                                selected = false,
                                leadingContent = {
                                    if (!accountAvatar.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                                .data(accountAvatar)
                                                .diskCachePolicy(CachePolicy.ENABLED)
                                                .diskCacheKey(accountAvatar)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape),
                                        )
                                    }
                                },
                                onClick = {
                                    navController.navigate(AccountDestination) {
                                        popUpTo(HomeDestination) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                            )
                        }

                        Spacer(Modifier.weight(1f))

                        // Mood / filter chips (horizontally scrollable)
                        Row(
                            modifier =
                                Modifier
                                    .horizontalScroll(chipRowState)
                                    .background(Color.Transparent),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            listOfHomeChip.forEach { id ->
                                val isSelected =
                                    when (params) {
                                        HOME_PARAMS_RELAX -> id == Res.string.relax
                                        HOME_PARAMS_SLEEP -> id == Res.string.sleep
                                        HOME_PARAMS_ENERGIZE -> id == Res.string.energize
                                        HOME_PARAMS_SAD -> id == Res.string.sad
                                        HOME_PARAMS_ROMANCE -> id == Res.string.romance
                                        HOME_PARAMS_FEEL_GOOD -> id == Res.string.feel_good
                                        HOME_PARAMS_WORKOUT -> id == Res.string.workout
                                        HOME_PARAMS_PARTY -> id == Res.string.party
                                        HOME_PARAMS_COMMUTE -> id == Res.string.commute
                                        HOME_PARAMS_FOCUS -> id == Res.string.focus
                                        else -> id == Res.string.all
                                    }
                                Chip(
                                    isAnimated = loading,
                                    isSelected = isSelected,
                                    text = stringResource(id),
                                ) {
                                    when (id) {
                                        Res.string.all -> viewModel.setParams(null)
                                        Res.string.relax -> viewModel.setParams(HOME_PARAMS_RELAX)
                                        Res.string.sleep -> viewModel.setParams(HOME_PARAMS_SLEEP)
                                        Res.string.energize -> viewModel.setParams(HOME_PARAMS_ENERGIZE)
                                        Res.string.sad -> viewModel.setParams(HOME_PARAMS_SAD)
                                        Res.string.romance -> viewModel.setParams(HOME_PARAMS_ROMANCE)
                                        Res.string.feel_good -> viewModel.setParams(HOME_PARAMS_FEEL_GOOD)
                                        Res.string.workout -> viewModel.setParams(HOME_PARAMS_WORKOUT)
                                        Res.string.party -> viewModel.setParams(HOME_PARAMS_PARTY)
                                        Res.string.commute -> viewModel.setParams(HOME_PARAMS_COMMUTE)
                                        Res.string.focus -> viewModel.setParams(HOME_PARAMS_FOCUS)
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // Non-desktop: original horizontally scrollable chip row
                    Row(
                        modifier =
                            Modifier
                                .horizontalScroll(chipRowState)
                                .padding(vertical = 8.dp)
                                .padding(start = 16.dp, end = 24.dp)
                                .background(Color.Transparent),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Chip(
                            isAnimated = false,
                            isSelected = false,
                            text = stringResource(Res.string.recently_added),
                        ) {
                            navController.navigate(RecentlySongsDestination)
                        }
                        listOfHomeChip.forEach { id ->
                            val isSelected =
                                when (params) {
                                    HOME_PARAMS_RELAX -> id == Res.string.relax
                                    HOME_PARAMS_SLEEP -> id == Res.string.sleep
                                    HOME_PARAMS_ENERGIZE -> id == Res.string.energize
                                    HOME_PARAMS_SAD -> id == Res.string.sad
                                    HOME_PARAMS_ROMANCE -> id == Res.string.romance
                                    HOME_PARAMS_FEEL_GOOD -> id == Res.string.feel_good
                                    HOME_PARAMS_WORKOUT -> id == Res.string.workout
                                    HOME_PARAMS_PARTY -> id == Res.string.party
                                    HOME_PARAMS_COMMUTE -> id == Res.string.commute
                                    HOME_PARAMS_FOCUS -> id == Res.string.focus
                                    else -> id == Res.string.all
                                }
                            Chip(
                                isAnimated = loading,
                                isSelected = isSelected,
                                text = stringResource(id),
                            ) {
                                when (id) {
                                    Res.string.all -> viewModel.setParams(null)
                                    Res.string.relax -> viewModel.setParams(HOME_PARAMS_RELAX)
                                    Res.string.sleep -> viewModel.setParams(HOME_PARAMS_SLEEP)
                                    Res.string.energize -> viewModel.setParams(HOME_PARAMS_ENERGIZE)
                                    Res.string.sad -> viewModel.setParams(HOME_PARAMS_SAD)
                                    Res.string.romance -> viewModel.setParams(HOME_PARAMS_ROMANCE)
                                    Res.string.feel_good -> viewModel.setParams(HOME_PARAMS_FEEL_GOOD)
                                    Res.string.workout -> viewModel.setParams(HOME_PARAMS_WORKOUT)
                                    Res.string.party -> viewModel.setParams(HOME_PARAMS_PARTY)
                                    Res.string.commute -> viewModel.setParams(HOME_PARAMS_COMMUTE)
                                    Res.string.focus -> viewModel.setParams(HOME_PARAMS_FOCUS)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun DesktopNavPill(
    label: String,
    selected: Boolean,
    leadingContent: (@Composable () -> Unit)? = null,
    onClick: () -> Unit,
) {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        targetValue =
            when {
                selected -> Color.White
                isHovered -> MaterialTheme.colorScheme.surfaceContainerHighest
                else -> Color.Transparent
            },
        animationSpec = tween(220, easing = androidx.compose.animation.core.FastOutSlowInEasing),
        label = "pillBg",
    )
    val textColor by animateColorAsState(
        targetValue =
            when {
                selected -> Color(0xFF0C0C0F)
                isHovered -> MaterialTheme.colorScheme.onSurface
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(180),
        label = "pillText",
    )
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(17.dp))
                .background(bgColor)
                .hoverable(interactionSource)
                .clickable(
                    interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                ).padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            leadingContent?.invoke()
            Text(
                text = label,
                style = typo().labelMedium.copy(
                    fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.SemiBold else androidx.compose.ui.text.font.FontWeight.Normal,
                    fontSize = 13.sp,
                ),
                color = textColor,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(navController: NavController) {
    val hour =
        remember {
            val date = now().time
            date.hour
        }
    TopAppBar(
        windowInsets =
            TopAppBarDefaults.windowInsets.exclude(
                TopAppBarDefaults.windowInsets.only(WindowInsetsSides.Start),
            ),
        title = {
            Column {
                Text(
                    text = stringResource(Res.string.app_name),
                    style = typo().titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
                Text(
                    text =
                        when (hour) {
                            in 6..12 -> {
                                stringResource(Res.string.good_morning)
                            }

                            in 13..17 -> {
                                stringResource(Res.string.good_afternoon)
                            }

                            in 18..23 -> {
                                stringResource(Res.string.good_evening)
                            }

                            else -> {
                                stringResource(Res.string.good_night)
                            }
                        },
                    style = typo().bodySmall,
                )
            }
        },
        actions = {
            ListenTogetherIconButton {
                navController.navigate(ListenTogetherDestination)
            }
            RippleIconButton(imageVector = SimpIcons.Notifications, tint = MaterialTheme.colorScheme.onBackground) {
                navController.navigate(NotificationDestination)
            }
            RippleIconButton(imageVector = SimpIcons.Settings, tint = MaterialTheme.colorScheme.onBackground) {
                navController.navigate(SettingsDestination)
            }
        },
        colors =
            TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
            ),
    )
}

@Composable
fun AccountLayout(
    accountName: String,
    url: String,
) {
    Column {
        Text(
            text = stringResource(Res.string.welcome_back),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(bottom = 3.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 5.dp),
        ) {
            AsyncImage(
                model =
                    ImageRequest
                        .Builder(LocalPlatformContext.current)
                        .data(url)
                        .diskCachePolicy(CachePolicy.ENABLED)
                        .diskCacheKey(url)
                        .crossfade(true)
                        .build(),
                placeholder = rememberHolderPainter(),
                error = rememberHolderPainter(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .size(40.dp)
                        .clip(
                            CircleShape,
                        ),
            )
            Text(
                text = accountName,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier =
                    Modifier
                        .padding(start = 8.dp),
            )
        }
    }
}

// Portrait fills the width with one column (the next one peeking in). On a landscape window that one
// column stretched across the whole screen, so every row there is capped instead.
private val LandscapeGridItemMaxWidth = 400.dp

// A card shelf of songs only that leads to no page of its own ("Your daily discover"). List shelves
// (Quick picks, Heard in Shorts) carry itemsPerColumn and keep their own look; a shelf with a More
// page (Listen again can be all songs) keeps its cards; videos draw as wide cards, so never match.
private fun HomeItem.isSongCardShelf(): Boolean =
    itemsPerColumn == null &&
        moreEndpoint == null &&
        contents.isNotEmpty() &&
        contents.all { it != null && !it.videoId.isNullOrEmpty() && it.homeCardAspectRatio() == 1f }

@ExperimentalFoundationApi
@Composable
fun QuickPicks(
    homeItem: HomeItem,
    navController: NavController,
    viewModel: HomeViewModel = koinViewModel(),
) {
    val lazyListState = rememberLazyGridState()
    val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState, snapPosition = SnapPosition.Start))
    val isPortrait = getScreenSizeInfo().let { it.wDP < it.hDP }
    val density = LocalDensity.current
    var widthDp by remember {
        mutableStateOf(0.dp)
    }
    var bottomSheetShow by remember { mutableStateOf(false) }
    var track by remember { mutableStateOf<Track?>(null) }

    if (bottomSheetShow) {
        NowPlayingBottomSheet(
            onDismiss = { bottomSheetShow = false },
            song = track?.toSongEntity(),
            navController = navController,
        )
    }

    Column(
        Modifier
            .padding(vertical = 8.dp)
            .onGloballyPositioned { coordinates ->
                with(density) {
                    widthDp = (coordinates.size.width).toDp()
                }
            },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = stringResource(Res.string.let_s_start_with_a_radio),
                    style = typo().bodySmall,
                )
                Text(
                    text = stringResource(Res.string.quick_picks),
                    style = typo().headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                )
            }
            if (homeItem.playAllEndpoint != null) {
                androidx.compose.material3.FilledTonalButton(
                    onClick = { viewModel.playAll(homeItem) },
                    shape = RoundedCornerShape(20.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    colors =
                        androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                        ),
                ) {
                    androidx.compose.material3.Icon(
                        imageVector = SimpIcons.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp).padding(end = 4.dp),
                    )
                    Text(
                        stringResource(Res.string.play_all),
                        style = typo().labelSmall,
                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                    )
                }
            }
        }
        val isDesktop = getPlatform() == Platform.Desktop
        val columnWidth =
            if (isPortrait) {
                widthDp
            } else if (isDesktop) {
                // On wide desktop screens, comfortably fit 3 or 4 columns (approx 320.dp - 360.dp each)
                // without stretching single lines or wrapping awkwardly.
                val available = (widthDp - 32.dp).coerceAtLeast(300.dp)
                val targetCols = (available / 340.dp).toInt().coerceIn(2, 4)
                (available / targetCols).coerceIn(300.dp, 380.dp)
            } else {
                minOf(widthDp, LandscapeGridItemMaxWidth + 30.dp)
            }

        Spacer(Modifier.height(12.dp))
        LazyHorizontalGrid(
            rows = GridCells.Fixed(4),
            modifier = Modifier.height(290.dp),
            state = lazyListState,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            flingBehavior = snapperFlingBehavior,
        ) {
            items(homeItem.contents, key = { it.hashCode() }) {
                if (it != null) {
                    QuickPicksItem(
                        onClick = {
                            val firstQueue: Track = it.toTrack()
                            viewModel.setQueueData(
                                QueueData.Data(
                                    listTracks = arrayListOf(firstQueue),
                                    firstPlayedTrack = firstQueue,
                                    playlistId = "RDAMVM${it.videoId}",
                                    playlistName = "\"${it.title}\" Radio",
                                    playlistType = PlaylistType.RADIO,
                                    continuation = null,
                                ),
                            )
                            viewModel.loadMediaItem(
                                firstQueue,
                                type = Config.SONG_CLICK,
                            )
                        },
                        onLongClick = {
                            track = it.toTrack()
                            bottomSheetShow = true
                        },
                        data = it,
                        widthDp = columnWidth,
                    )
                }
            }
        }
        HorizontalScrollBar(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            scrollState = lazyListState,
            flingBehavior = snapperFlingBehavior,
        )
    }
}

@Composable
fun MoodMomentAndGenre(
    mood: Mood,
    navController: NavController,
    moodArtwork: Map<String, String> = emptyMap(),
    onRequestArtwork: (String) -> Unit = {},
) {
    BoxWithConstraints(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
    ) {
        // Responsive column count: 4 on wide, 3 mid, 2 narrow.
        // Computed synchronously from BoxWithConstraints maxWidth, ensuring rock-solid layout height.
        val columns = when {
            maxWidth >= 600.dp -> 4
            maxWidth >= 400.dp -> 3
            else -> 2
        }

        Column(Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(Res.string.let_s_pick_a_playlist_for_you),
            style = typo().bodyMedium,
        )
        // One block per section YouTube returned, headed by ITS OWN title. Hard-coding
        // "Moods & moment" / "Genre" here (and reading mood.moodsMoments / mood.genres by
        // index) mislabelled every row as soon as a signed-in account got an extra
        // "For you" section, and hid the real Genres section altogether.
        mood.sections.forEach { section ->
            Text(
                text = section.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
            )
            // Manual grid: chunked rows so we avoid nested lazy lists inside the parent LazyColumn.
            val rows = section.items.chunked(columns)
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                rows.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { item ->
                            // LaunchedEffect fires once per item when it first enters composition
                            // (i.e., when it scrolls into view) — this IS the lazy request dedup.
                            LaunchedEffect(item.params) {
                                onRequestArtwork(item.params)
                            }
                            Box(Modifier.weight(1f)) {
                                MoodMomentAndGenreHomeItem(
                                    title = item.title,
                                    stripeColor = item.stripeColor,
                                    artworkUrl = moodArtwork[item.params],
                                    fillMaxWidth = true,
                                ) {
                                    navController.navigate(
                                        MoodDestination(
                                            item.params,
                                        ),
                                    )
                                }
                            }
                        }
                        // Pad the last row so incomplete rows don't stretch tiles.
                        repeat(columns - row.size) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}
}

@Composable
fun ChartTitle() {
    Column {
        Text(
            text = stringResource(Res.string.what_is_best_choice_today),
            style = typo().bodyMedium,
        )
        Text(
            text = stringResource(Res.string.chart),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
        )
    }
}

@Composable
fun ChartData(
    chart: Chart,
    navController: NavController,
) {
    val lazyListState2 = rememberLazyGridState()
    val snapperFlingBehavior2 = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = lazyListState2))
    val isPortrait = getScreenSizeInfo().let { it.wDP < it.hDP }

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val gridWidthDp = maxWidth

        Column(Modifier.fillMaxWidth()) {
        chart.listChartItem.forEach { item ->
            Text(
                text = item.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val lazyListState = rememberLazyListState()
            val snapperFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyListState = lazyListState))
            LazyRow(flingBehavior = snapperFlingBehavior) {
                items(item.playlists.size, key = { index ->
                    val data = item.playlists[index]
                    data.id + data.title + index
                }) {
                    HomeItemContentPlaylist(
                        onClick = {
                            navController.navigate(
                                PlaylistDestination(
                                    playlistId = item.playlists[it].id,
                                    isYourYouTubePlaylist = false,
                                ),
                            )
                        },
                        data = item.playlists[it],
                    )
                }
            }
        }
        Text(
            text = stringResource(Res.string.top_artists),
            style = typo().headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
        )
        LazyHorizontalGrid(
            rows = GridCells.Fixed(3),
            modifier = Modifier.height(240.dp),
            state = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        ) {
            items(chart.artists.itemArtists.size, key = { index ->
                val item = chart.artists.itemArtists[index]
                item.title + item.browseId + index
            }) {
                val data = chart.artists.itemArtists[it]
                ItemArtistChart(
                    onClick = {
                        navController.navigate(
                            ArtistDestination(
                                channelId = data.browseId,
                            ),
                        )
                    },
                    data = data,
                    widthDp = if (isPortrait) gridWidthDp else minOf(gridWidthDp, LandscapeGridItemMaxWidth),
                )
            }
        }
        HorizontalScrollBar(
            modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
            scrollState = lazyListState2,
            flingBehavior = snapperFlingBehavior2,
        )
        // Ranked podcast shows, laid out exactly like the artist chart above.
        chart.podcasts?.let { podcasts ->
            Text(
                text = podcasts.title,
                style = typo().headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
            )
            val podcastGridState = rememberLazyGridState()
            val podcastFlingBehavior = rememberSnapFlingBehavior(SnapLayoutInfoProvider(lazyGridState = podcastGridState))
            LazyHorizontalGrid(
                rows = GridCells.Fixed(3),
                modifier = Modifier.height(240.dp),
                state = podcastGridState,
                flingBehavior = podcastFlingBehavior,
            ) {
                items(podcasts.shows.size, key = { index -> podcasts.shows[index].browseId + index }) {
                    val data = podcasts.shows[it]
                    ItemArtistChart(
                        onClick = { navController.navigate(PodcastDestination(podcastId = data.browseId)) },
                        data = data,
                        widthDp = if (isPortrait) gridWidthDp else minOf(gridWidthDp, LandscapeGridItemMaxWidth),
                        thumbnailShape = RoundedCornerShape(8.dp),
                        subtitle = data.subscribers,
                    )
                }
            }
            HorizontalScrollBar(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                scrollState = podcastGridState,
                flingBehavior = podcastFlingBehavior,
            )
        }
    }
}
}
