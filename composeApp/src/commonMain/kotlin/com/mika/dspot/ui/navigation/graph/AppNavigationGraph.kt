package com.mika.dspot.ui.navigation.graph

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.mika.dspot.ui.navigation.destination.home.AnalyticsDestination
import com.mika.dspot.ui.navigation.destination.home.HomeDestination
import com.mika.dspot.ui.navigation.destination.home.WrappedDestination
import com.mika.dspot.ui.theme.ForceDarkContent
import com.mika.dspot.ui.navigation.destination.library.LibraryDestination
import com.mika.dspot.ui.navigation.destination.library.MixForYouDestination
import com.mika.dspot.ui.navigation.destination.player.FullscreenDestination
import com.mika.dspot.ui.navigation.destination.search.SearchDestination
import com.mika.dspot.ui.navigation.destination.account.AccountDestination
import com.mika.dspot.ui.screen.account.AccountHubScreen
import com.mika.dspot.ui.screen.home.HomeScreen
import com.mika.dspot.ui.screen.home.analytics.AnalyticsScreen
import com.mika.dspot.ui.screen.home.wrapped.WrappedScreen
import com.mika.dspot.ui.screen.library.LibraryScreen
import com.mika.dspot.ui.screen.library.MixForYouScreen
import com.mika.dspot.ui.screen.other.SearchScreen
import com.mika.dspot.ui.screen.player.FullscreenPlayer

@Composable
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun AppNavigationGraph(
    innerPadding: PaddingValues,
    navController: NavHostController,
    startDestination: Any = HomeDestination,
    hideNavBar: () -> Unit = { },
    showNavBar: (shouldShowNowPlayingSheet: Boolean) -> Unit = { },
    showNowPlayingSheet: () -> Unit = {},
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    NavHost(
        navController,
        startDestination = startDestination,
        enterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
        },
        exitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing))
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing))
        },
    ) {
        // Bottom bar destinations
        composable<HomeDestination> {
            HomeScreen(
                onScrolling = onScrolling,
                navController = navController,
            )
        }
        composable<SearchDestination> {
            SearchScreen(
                navController = navController,
            )
        }
        composable<LibraryDestination> {
            LibraryScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        // Only reachable as a tab while signed in to YouTube
        composable<MixForYouDestination> {
            MixForYouScreen(
                innerPadding = innerPadding,
                navController = navController,
                onScrolling = onScrolling,
            )
        }
        composable<AccountDestination> {
            AccountHubScreen(
                innerPadding = innerPadding,
                navController = navController,
            )
        }
        // Reachable via Account hub or deep link
        composable<AnalyticsDestination> {
            ForceDarkContent {
                AnalyticsScreen(
                    navController = navController,
                    innerPadding = innerPadding,
                )
            }
        }
        // Reached only from the Analytics screen's entry banner, so it inherits that screen's
        // gate on local tracking. ForceDarkContent for a different reason than Analytics: the reel
        // is drawn on its own near-black ground whatever the user's theme is, because it is an
        // event and because every card is also a share image that has to survive leaving the app.
        composable<WrappedDestination> {
            ForceDarkContent {
                WrappedScreen(
                    navController = navController,
                    hideNavBar = hideNavBar,
                    // Deliberately not the fullscreen player's `showNavBar(true)` + open sheet:
                    // leaving the reel goes back to Analytics, and raising the Now Playing sheet
                    // over it would be a screen the user never asked for.
                    showNavBar = { showNavBar(false) },
                )
            }
        }
        composable<FullscreenDestination> {
            ForceDarkContent {
                FullscreenPlayer(
                    navController,
                    hideNavBar = hideNavBar,
                    showNavBar = {
                        showNavBar.invoke(true)
                        showNowPlayingSheet.invoke()
                    },
                )
            }
        }
        // Home screen graph
        homeScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // Library screen graph
        libraryScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // List screen graph
        listScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
        )
        // Login screen graph
        loginScreenGraph(
            innerPadding = innerPadding,
            navController = navController,
            hideBottomBar = hideNavBar,
            showBottomBar = {
                showNavBar(false)
            },
        )
    }
}