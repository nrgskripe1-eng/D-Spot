package com.mika.dspot.ui.screen.library

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.kmpalette.loader.rememberNetworkLoader
import com.kmpalette.rememberDominantColorState
import com.mika.dspot.Platform
import com.mika.dspot.extension.angledGradientBackground
import com.mika.dspot.extension.artworkScrimBrush
import com.mika.dspot.extension.barBlurStyle
import com.mika.dspot.extension.copy
import com.mika.dspot.extension.rgbFactor
import com.mika.dspot.getPlatform
import com.mika.dspot.ui.component.AmbientGlowHeight
import com.mika.dspot.ui.component.GridLibraryPlaylist
import com.mika.dspot.ui.icon.Sensors
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.navigation.destination.login.LoginDestination
import com.mika.dspot.ui.theme.desktopPanelDark
import com.mika.dspot.ui.theme.typo
import com.mika.dspot.viewModel.LibraryViewModel
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.blur.hazeBlur
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.mix_for_you
import dspot.composeapp.generated.resources.no_mixes_found
import dspot.composeapp.generated.resources.sign_in
import dspot.composeapp.generated.resources.sign_in_to_youtube_mix_prompt
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.http.Url
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

/**
 * The YouTube "Mix for you" playlists, promoted out of the Library chip row into a permanent tab.
 *
 * When signed out, it renders a friendly sign-in prompt instead of an empty/broken list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MixForYouScreen(
    innerPadding: PaddingValues,
    viewModel: LibraryViewModel = koinViewModel(),
    navController: NavController,
    onScrolling: (onTop: Boolean) -> Unit = {},
) {
    val density = LocalDensity.current
    val isYouTubeLoggedIn by viewModel.youtubeLoggedIn.collectAsStateWithLifecycle(initialValue = false)
    val mixForYou by viewModel.youTubeMixForYou.collectAsStateWithLifecycle()
    val hazeState = rememberHazeState()
    val gridState = rememberLazyGridState()

    val isAtTop by remember {
        derivedStateOf { gridState.firstVisibleItemIndex == 0 && gridState.firstVisibleItemScrollOffset == 0 }
    }

    var topAppBarHeight by remember { mutableStateOf(0.dp) }

    val backgroundColor = MaterialTheme.colorScheme.background
    val isLightTheme = backgroundColor.luminance() > 0.5f
    val pageBackground =
        if (getPlatform() == Platform.Desktop) {
            if (isLightTheme) MaterialTheme.colorScheme.surfaceContainer else desktopPanelDark
        } else {
            backgroundColor
        }
    var topHeaderColor by remember { mutableStateOf(backgroundColor) }
    val animatedColor by animateColorAsState(topHeaderColor, tween(500))
    val networkLoader = rememberNetworkLoader(HttpClient(CIO))
    val dominantColorState =
        rememberDominantColorState(
            defaultColor = backgroundColor,
            defaultOnColor = backgroundColor,
            loader = networkLoader,
        )
    val firstMixThumbnail =
        mixForYou.data
            ?.firstOrNull()
            ?.thumbnails
            ?.lastOrNull()
            ?.url
    LaunchedEffect(firstMixThumbnail) {
        firstMixThumbnail?.let { dominantColorState.updateFrom(Url(it)) }
    }
    LaunchedEffect(dominantColorState, isLightTheme) {
        snapshotFlow { dominantColorState.color }.collect {
            topHeaderColor = if (isLightTheme) lerp(it, Color.White, 0.85f) else it.rgbFactor(0.3f)
        }
    }

    LaunchedEffect(isYouTubeLoggedIn) {
        if (isYouTubeLoggedIn && mixForYou.data.isNullOrEmpty()) {
            viewModel.getYouTubeMixedForYou()
        }
    }

    if (!isYouTubeLoggedIn) {
        MixSignedOutView(
            innerPadding = innerPadding,
            onSignInClick = { navController.navigate(LoginDestination) },
        )
    } else {
        Box(Modifier.hazeSource(hazeState)) {
            Box(
                Modifier
                    .graphicsLayer {
                        translationY =
                            if (gridState.firstVisibleItemIndex == 0) {
                                -gridState.firstVisibleItemScrollOffset.toFloat()
                            } else {
                                -size.height
                            }
                    }.fillMaxWidth()
                    .height(AmbientGlowHeight)
                    .angledGradientBackground(listOf(animatedColor, pageBackground), 25f),
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .align(Alignment.BottomCenter)
                        .background(artworkScrimBrush(pageBackground)),
                )
            }
            GridLibraryPlaylist(
                navController,
                innerPadding.copy(top = topAppBarHeight),
                mixForYou,
                emptyText = Res.string.no_mixes_found,
                state = gridState,
                onScrolling = onScrolling,
            ) {
                viewModel.getYouTubeMixedForYou()
            }
        }
        AnimatedContent(
            targetState = isAtTop,
            transitionSpec = {
                fadeIn(tween(300)).togetherWith(fadeOut(tween(300)))
            },
        ) { atTop ->
            Column(
                Modifier
                    .then(
                        if (atTop) {
                            Modifier.background(Color.Transparent)
                        } else {
                            Modifier.hazeBlur(HazeInput.Sources(hazeState), barBlurStyle(pageBackground, 0.3f))
                        },
                    ).onGloballyPositioned { coordinates ->
                        topAppBarHeight = with(density) { coordinates.size.height.toDp() }
                    },
            ) {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.mix_for_you),
                            style = typo().titleMedium,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    colors =
                        TopAppBarDefaults.topAppBarColors(
                            containerColor = Color.Transparent,
                        ),
                )
            }
        }
    }
}

@Composable
fun MixSignedOutView(
    innerPadding: PaddingValues,
    onSignInClick: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Surface(
            modifier = Modifier.size(80.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = SimpIcons.Sensors,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(40.dp),
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = stringResource(Res.string.mix_for_you),
            style = typo().titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = stringResource(Res.string.sign_in_to_youtube_mix_prompt),
            style = typo().bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = onSignInClick,
            shape = RoundedCornerShape(24.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
            modifier = Modifier.height(48.dp),
        ) {
            Text(
                text = stringResource(Res.string.sign_in),
                style = typo().labelLarge,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Preview
@Composable
fun MixSignedOutPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            MixSignedOutView(
                innerPadding = PaddingValues(0.dp),
                onSignInClick = {},
            )
        }
    }
}