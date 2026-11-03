package com.mika.dspot.ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.hoverable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsHoveredAsState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.currentBackStackEntryAsState
import com.mika.dspot.ui.theme.typo
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.account
import dspot.composeapp.generated.resources.circle_app_icon
import dspot.composeapp.generated.resources.listen_together
import dspot.composeapp.generated.resources.recently_added
import dspot.composeapp.generated.resources.settings
import dspot.composeapp.generated.resources.what_do_you_want_to_listen_to
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.window.PopupProperties
import androidx.compose.material3.DropdownMenu
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.mika.dspot.ui.icon.ArrowOutward
import com.mika.dspot.ui.icon.Close
import com.mika.dspot.ui.icon.Groups
import com.mika.dspot.ui.icon.History
import com.mika.dspot.ui.icon.Person
import com.mika.dspot.ui.icon.Search
import com.mika.dspot.ui.icon.Settings
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.navigation.destination.account.AccountDestination
import com.mika.dspot.ui.navigation.destination.home.ListenTogetherDestination
import com.mika.dspot.ui.navigation.destination.home.RecentlySongsDestination
import com.mika.dspot.ui.navigation.destination.home.SettingsDestination
import com.mika.dspot.ui.navigation.destination.search.SearchDestination
import com.mika.dspot.viewModel.SearchViewModel
import org.koin.compose.koinInject
import kotlin.reflect.KClass

/**
 * Desktop-only top navigation bar, Apple Music-style.
 *
 * A slim bar (52 dp) drawn at the top of the content panel on the desktop shell. It carries:
 *  - App logo + name on the left for brand anchoring.
 *  - Horizontal tab pills (icon + animated label for the selected one, icon-only for others).
 *  - An accent colour derived from the currently-playing artwork via [rememberNowPlayingGlowTint]:
 *    the selected tab and its pill background animate to that colour whenever the track changes,
 *    giving the same ambient dynamic-colour feel Apple Music has without touching the rest of the
 *    theme.
 *
 * All navigation logic mirrors [AppNavigationRail] (same popUpTo/saveState/restoreState policy).
 */
@Composable
fun DesktopTopNavigationBar(
    navController: NavController,
    thumbnailUrl: String? = null,
    showAnalyticsTab: Boolean = false,
    showMixForYouTab: Boolean = false,
    searchViewModel: SearchViewModel = koinInject(),
    reloadDestinationIfNeeded: (KClass<*>) -> Unit = {},
) {
    val tabs =
        listOfNotNull(
            BottomNavScreen.Home,
            BottomNavScreen.Library,
            BottomNavScreen.MixForYou.takeIf { showMixForYouTab },
            BottomNavScreen.Analytics.takeIf { showAnalyticsTab },
            BottomNavScreen.Account,
        )

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    var selectedIndex by rememberSaveable { mutableIntStateOf(BottomNavScreen.Home.ordinal) }
    val isSearchActive = currentBackStackEntry?.destination?.hierarchy?.any {
        it.hasRoute(SearchDestination::class)
    } == true

    // Keep selectedIndex in sync with back-stack navigation (back button, deep links, etc.).
    LaunchedEffect(currentBackStackEntry) {
        val dest = currentBackStackEntry?.destination
        val match =
            tabs.firstOrNull { screen ->
                dest?.hierarchy?.any { it.hasRoute(screen.destination::class) } == true
            }
        if (match != null) selectedIndex = match.ordinal
    }

    // Fall back to Home if a conditional tab disappears while it is selected.
    LaunchedEffect(showAnalyticsTab, showMixForYouTab) {
        if ((!showAnalyticsTab && selectedIndex == BottomNavScreen.Analytics.ordinal) ||
            (!showMixForYouTab && selectedIndex == BottomNavScreen.MixForYou.ordinal)
        ) {
            selectedIndex = BottomNavScreen.Home.ordinal
        }
    }

    // Dynamic accent from the currently-playing artwork -- animates on track change.
    val glowTint = rememberNowPlayingGlowTint(thumbnailUrl)
    val isLightTheme = MaterialTheme.colorScheme.background.luminance() > 0.5f
    val accentColor by animateColorAsState(
        targetValue =
            when {
                glowTint != null && isLightTheme ->
                    lerp(glowTint, MaterialTheme.colorScheme.primary, 0.35f)
                glowTint != null -> glowTint
                else -> MaterialTheme.colorScheme.primary
            },
        animationSpec = tween(500),
        label = "desktopNavAccent",
    )

    val selectTab: (BottomNavScreen) -> Unit = { screen ->
        if (selectedIndex == screen.ordinal) {
            if (currentBackStackEntry?.destination?.hierarchy?.any {
                    it.hasRoute(screen.destination::class)
                } == true
            ) {
                reloadDestinationIfNeeded(screen.destination::class)
            } else {
                navController.navigate(screen.destination)
            }
        } else {
            selectedIndex = screen.ordinal
            navController.navigate(screen.destination) {
                popUpTo(navController.graph.startDestinationId) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
    }

    // Search query state for the persistent top search bar
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
    val searchScope = rememberCoroutineScope()
    var debounceJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val activeQuery by searchViewModel.activeSearchQuery.collectAsStateWithLifecycle()
    LaunchedEffect(activeQuery) {
        if (activeQuery != searchQuery) {
            searchQuery = activeQuery
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF0C0C0F)),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // App logo
            Box(
                modifier =
                    Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(Res.drawable.circle_app_icon),
                    contentDescription = "D-Spot",
                    modifier =
                        Modifier
                            .size(22.dp)
                            .clip(CircleShape),
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = "D-Spot",
                style =
                    MaterialTheme.typography.titleSmall.copy(
                        fontWeight = FontWeight.SemiBold,
                    ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(Modifier.weight(1f))

            // Centered Search Bar with Spotify-style Recent Searches popup
            DesktopCenterSearchBar(
                query = searchQuery,
                onQueryChange = { newQuery ->
                    searchQuery = newQuery
                    searchViewModel.setActiveSearchQuery(newQuery)
                    if (newQuery.isNotBlank()) {
                        searchViewModel.suggestQuery(newQuery)
                        if (!isSearchActive) {
                            navController.navigate(SearchDestination)
                        }
                        debounceJob?.cancel()
                        debounceJob = searchScope.launch {
                            kotlinx.coroutines.delay(350)
                            searchViewModel.searchAll(newQuery)
                        }
                    }
                },
                onSearch = { query ->
                    if (query.isNotBlank()) {
                        focusManager.clearFocus()
                        searchQuery = query
                        searchViewModel.setActiveSearchQuery(query)
                        searchViewModel.insertSearchHistory(query)
                        searchViewModel.searchAll(query)
                        if (!isSearchActive) {
                            navController.navigate(SearchDestination)
                        }
                    }
                },
                onClick = {
                    if (!isSearchActive) {
                        navController.navigate(SearchDestination)
                    }
                },
                searchViewModel = searchViewModel,
                accentColor = accentColor,
                isLightTheme = isLightTheme,
            )

            Spacer(Modifier.weight(1f))

            // Right-side actions: History, Account, Settings
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Recently Added / History
                DesktopNavActionButton(
                    icon = SimpIcons.History,
                    contentDescription = stringResource(Res.string.recently_added),
                    onClick = { navController.navigate(RecentlySongsDestination) },
                )

                // Account
                DesktopNavActionButton(
                    icon = SimpIcons.Person,
                    contentDescription = stringResource(Res.string.account),
                    onClick = { navController.navigate(AccountDestination) },
                )

                // Settings
                DesktopNavActionButton(
                    icon = SimpIcons.Settings,
                    contentDescription = stringResource(Res.string.settings),
                    onClick = { navController.navigate(SettingsDestination) },
                )
            }
        }

        // Subtle separator between nav bar and content area.
        HorizontalDivider(
            thickness = 0.5.dp,
            color = Color(0xFF22232D),
        )
    }
}

@Composable
private fun DesktopCenterSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onClick: () -> Unit,
    searchViewModel: SearchViewModel,
    accentColor: Color,
    isLightTheme: Boolean,
    modifier: Modifier = Modifier,
) {
    var isFocused by remember { mutableStateOf(false) }
    var isPopupOpen by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val searchHistory by searchViewModel.searchHistory.collectAsStateWithLifecycle()
    val searchScreenState by searchViewModel.searchScreenState.collectAsStateWithLifecycle()

    val bg by animateColorAsState(
        targetValue =
            when {
                isFocused || isPopupOpen -> MaterialTheme.colorScheme.surfaceContainerHighest
                isHovered -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.85f)
                else -> MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.55f)
            },
        animationSpec = tween(150),
        label = "searchBarBg",
    )

    val borderColor by animateColorAsState(
        targetValue =
            if (isFocused || isPopupOpen) accentColor.copy(alpha = 0.7f)
            else if (isHovered) MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
            else Color.Transparent,
        animationSpec = tween(150),
        label = "searchBarBorder",
    )

    val focusRequester = remember { FocusRequester() }

    Box(modifier = modifier) {
        Row(
            modifier =
                Modifier
                    .width(380.dp)
                    .height(38.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(bg)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .hoverable(interactionSource)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) {
                        onClick()
                        focusRequester.requestFocus()
                        isPopupOpen = true
                    }
                    .padding(horizontal = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = SimpIcons.Search,
                contentDescription = "Search",
                tint = if (isFocused || isPopupOpen) accentColor else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp),
            )

            Spacer(Modifier.width(10.dp))

            Box(
                modifier = Modifier.weight(1f),
                contentAlignment = Alignment.CenterStart,
            ) {
                if (query.isEmpty()) {
                    Text(
                        text = stringResource(Res.string.what_do_you_want_to_listen_to),
                        style = typo().bodyMedium.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1,
                    )
                }

                BasicTextField(
                    value = query,
                    onValueChange = {
                        onQueryChange(it)
                        isPopupOpen = true
                    },
                    singleLine = true,
                    textStyle =
                        typo().bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 13.sp,
                        ),
                    cursorBrush = SolidColor(accentColor),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = {
                        isPopupOpen = false
                        onSearch(query)
                    }),
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                            .onFocusChanged {
                                isFocused = it.isFocused
                                if (it.isFocused) {
                                    onClick()
                                    isPopupOpen = true
                                }
                            },
                )
            }

            if (query.isNotEmpty()) {
                IconButton(
                    onClick = {
                        onQueryChange("")
                    },
                    modifier = Modifier.size(22.dp),
                ) {
                    Icon(
                        imageVector = SimpIcons.Close,
                        contentDescription = "Clear",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
        }

        // Spotify-style Recent Searches dropdown
        DropdownMenu(
            expanded = isPopupOpen,
            onDismissRequest = { isPopupOpen = false },
            properties = PopupProperties(focusable = false),
            modifier =
                Modifier
                    .width(380.dp)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceContainer,
                        shape = RoundedCornerShape(12.dp),
                    )
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(vertical = 6.dp),
        ) {
            if (query.isNotBlank()) {
                // When user is typing, show search for query & suggestions
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clickable {
                                isPopupOpen = false
                                onSearch(query)
                            }
                            .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = SimpIcons.Search,
                        contentDescription = null,
                        tint = accentColor,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = "Search for \"$query\"",
                        style = typo().bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }

                if (searchScreenState.suggestQueries.isNotEmpty()) {
                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 4.dp),
                        thickness = 0.5.dp,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                    )
                    searchScreenState.suggestQueries.take(6).forEach { suggestion ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onQueryChange(suggestion)
                                        onSearch(suggestion)
                                        isPopupOpen = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = SimpIcons.Search,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = suggestion,
                                style = typo().bodyMedium.copy(fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f),
                            )
                            Icon(
                                imageVector = SimpIcons.ArrowOutward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            } else {
                // Header: Recent searches
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent searches",
                        style = typo().titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    if (searchHistory.isNotEmpty()) {
                        Text(
                            text = "Clear",
                            style = typo().labelSmall.copy(fontSize = 11.sp),
                            color = accentColor,
                            modifier =
                                Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .clickable {
                                        searchViewModel.deleteSearchHistory()
                                    }
                                    .padding(horizontal = 4.dp, vertical = 2.dp),
                        )
                    }
                }

                if (searchHistory.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "No recent searches",
                            style = typo().bodySmall.copy(fontSize = 12.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        )
                    }
                } else {
                    searchHistory.take(8).forEach { historyItem ->
                        Row(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onQueryChange(historyItem)
                                        onSearch(historyItem)
                                        isPopupOpen = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(MaterialTheme.colorScheme.surfaceContainerHighest),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = SimpIcons.History,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = historyItem,
                                    style = typo().bodyMedium.copy(fontWeight = FontWeight.SemiBold, fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = "Search history",
                                    style = typo().bodySmall.copy(fontSize = 11.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Icon(
                                imageVector = SimpIcons.ArrowOutward,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DesktopNavActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isHovered by interactionSource.collectIsHoveredAsState()
    val bgColor by animateColorAsState(
        targetValue =
            if (isHovered) {
                MaterialTheme.colorScheme.surfaceContainerHighest
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            },
        animationSpec = tween(150),
        label = "btnBg",
    )
    val iconTint by animateColorAsState(
        targetValue =
            if (isHovered) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        animationSpec = tween(150),
        label = "btnTint",
    )

    Box(
        modifier =
            Modifier
                .size(34.dp)
                .clip(CircleShape)
                .background(bgColor)
                .hoverable(interactionSource)
                .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        androidx.compose.material3.Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier.size(18.dp),
        )
    }
}
