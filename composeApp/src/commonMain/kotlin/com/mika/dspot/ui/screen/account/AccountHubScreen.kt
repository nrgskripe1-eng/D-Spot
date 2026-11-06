package com.mika.dspot.ui.screen.account

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.mika.domain.manager.DataStoreManager
import com.mika.dspot.Platform
import com.mika.dspot.getPlatform
import com.mika.dspot.ui.component.SettingGroup
import com.mika.dspot.ui.component.SettingItem
import com.mika.dspot.ui.component.YouTubeAccountDialog
import com.mika.dspot.ui.icon.AutoGraph
import com.mika.dspot.ui.icon.Person
import com.mika.dspot.ui.icon.SimpIcons
import com.mika.dspot.ui.navigation.destination.home.AnalyticsDestination
import com.mika.dspot.ui.navigation.destination.home.HomeDestination
import com.mika.dspot.ui.navigation.destination.library.LibraryDestination
import com.mika.dspot.ui.navigation.destination.login.LastfmLoginDestination
import com.mika.dspot.ui.navigation.destination.login.LoginDestination
import com.mika.dspot.ui.navigation.destination.login.SpotifyLoginDestination
import com.mika.dspot.ui.screen.home.DesktopNavPill
import com.mika.dspot.ui.theme.typo
import com.mika.dspot.viewModel.HomeViewModel
import com.mika.dspot.viewModel.SettingsViewModel
import dspot.composeapp.generated.resources.Res
import dspot.composeapp.generated.resources.account
import dspot.composeapp.generated.resources.analytics
import dspot.composeapp.generated.resources.discord_integration
import dspot.composeapp.generated.resources.enable_rich_presence
import dspot.composeapp.generated.resources.enable_scrobbling
import dspot.composeapp.generated.resources.lastfm_integration
import dspot.composeapp.generated.resources.log_in_to_discord
import dspot.composeapp.generated.resources.log_in_to_lastfm
import dspot.composeapp.generated.resources.log_in_to_spotify
import dspot.composeapp.generated.resources.log_out_from_discord
import dspot.composeapp.generated.resources.log_out_from_lastfm
import dspot.composeapp.generated.resources.log_out_from_spotify
import dspot.composeapp.generated.resources.logged_in
import dspot.composeapp.generated.resources.logged_in_as
import dspot.composeapp.generated.resources.manage_your_youtube_accounts
import dspot.composeapp.generated.resources.rich_presence_info
import dspot.composeapp.generated.resources.scrobbling_info
import dspot.composeapp.generated.resources.send_back_listening_data_to_google
import dspot.composeapp.generated.resources.sign_in
import dspot.composeapp.generated.resources.spotify
import dspot.composeapp.generated.resources.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in
import dspot.composeapp.generated.resources.youtube_account
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountHubScreen(
    innerPadding: PaddingValues,
    navController: NavController,
    settingsViewModel: SettingsViewModel = koinViewModel(),
    homeViewModel: HomeViewModel = koinViewModel(),
) {
    val accountInfo by homeViewModel.accountInfo.collectAsStateWithLifecycle()
    val isYouTubeLoggedIn by settingsViewModel.loggedIn.collectAsStateWithLifecycle()
    val spotifyLoggedIn by settingsViewModel.spotifyLogIn.collectAsStateWithLifecycle()
    val discordLoggedIn by settingsViewModel.discordLoggedIn.collectAsStateWithLifecycle()
    val richPresenceEnabled by settingsViewModel.richPresenceEnabled.collectAsStateWithLifecycle()
    val lastfmLoggedIn by settingsViewModel.lastfmLoggedIn.collectAsStateWithLifecycle()
    val lastfmUsername by settingsViewModel.lastfmUsername.collectAsStateWithLifecycle()
    val lastfmScrobbleEnabled by settingsViewModel.lastfmScrobbleEnabled.collectAsStateWithLifecycle()
    val localTrackingEnabled by settingsViewModel.localTrackingEnabled.collectAsStateWithLifecycle(initialValue = false)
    val sendData by remember { settingsViewModel.sendBackToGoogle.map { it == DataStoreManager.TRUE } }.collectAsStateWithLifecycle(initialValue = false)

    var showYouTubeAccountDialog by rememberSaveable { mutableStateOf(false) }

    if (showYouTubeAccountDialog) {
        YouTubeAccountDialog(
            viewModel = settingsViewModel,
            navController = navController,
            onDismissRequest = { showYouTubeAccountDialog = false },
        )
    }

    val basisAlertData by settingsViewModel.basicAlertData.collectAsStateWithLifecycle()
    if (basisAlertData != null) {
        val alertBasicState = basisAlertData ?: return
        AlertDialog(
            onDismissRequest = { settingsViewModel.setBasicAlertData(null) },
            title = {
                Text(
                    text = alertBasicState.title,
                    style = typo().titleSmall,
                )
            },
            text = {
                if (alertBasicState.message != null) {
                    Text(text = alertBasicState.message)
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        alertBasicState.confirm.second.invoke()
                        settingsViewModel.setBasicAlertData(null)
                    },
                ) {
                    Text(text = alertBasicState.confirm.first)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        settingsViewModel.setBasicAlertData(null)
                    },
                ) {
                    Text(text = alertBasicState.dismiss)
                }
            },
        )
    }

    Scaffold(
        modifier = Modifier.padding(innerPadding),
        topBar = {
            Column {
                if (getPlatform() == Platform.Desktop) {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                                .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
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
                                selected = false,
                                onClick = {
                                    navController.navigate(HomeDestination) {
                                        popUpTo(HomeDestination) { inclusive = true }
                                        launchSingleTop = true
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
                                selected = true,
                                leadingContent = {
                                    if (!accountAvatar.isNullOrEmpty()) {
                                        AsyncImage(
                                            model = ImageRequest.Builder(LocalPlatformContext.current)
                                                .data(accountAvatar)
                                                .crossfade(true)
                                                .build(),
                                            contentDescription = null,
                                            contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                            modifier = Modifier
                                                .size(18.dp)
                                                .clip(CircleShape),
                                        )
                                    }
                                },
                                onClick = { /* already here */ },
                            )
                        }
                    }
                }
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(Res.string.account),
                            style = typo().titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                    ),
                )
            }
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { contentPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Profile Header Card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        val avatarUrl = accountInfo?.second
                        if (!avatarUrl.isNullOrEmpty()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalPlatformContext.current)
                                    .data(avatarUrl)
                                    .crossfade(300)
                                    .build(),
                                placeholder = rememberVectorPainter(SimpIcons.Person),
                                error = rememberVectorPainter(SimpIcons.Person),
                                contentDescription = null,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape),
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(56.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.surfaceContainerHighest,
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = SimpIcons.Person,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(32.dp),
                                    )
                                }
                            }
                        }

                        Spacer(Modifier.width(16.dp))

                        Column(Modifier.weight(1f)) {
                            val name = accountInfo?.first
                            Text(
                                text = if (!name.isNullOrEmpty()) name else stringResource(Res.string.account),
                                style = typo().titleMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                text = if (isYouTubeLoggedIn == DataStoreManager.TRUE) {
                                    stringResource(Res.string.logged_in)
                                } else {
                                    stringResource(Res.string.sign_in)
                                },
                                style = typo().bodySmall,
                                color = if (isYouTubeLoggedIn == DataStoreManager.TRUE) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.onSurfaceVariant
                                },
                            )
                        }
                    }
                }
            }

            // Analytics Hub Entry Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            navController.navigate(AnalyticsDestination)
                        },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(44.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = SimpIcons.AutoGraph,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = stringResource(Res.string.analytics),
                                style = typo().titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (localTrackingEnabled) {
                                    "Your listening trends and insights"
                                } else {
                                    "Local tracking is disabled (turn on in Settings)"
                                },
                                style = typo().bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // YouTube / Google Services
            item {
                SettingGroup(title = stringResource(Res.string.youtube_account)) {
                    SettingItem(
                        title = stringResource(Res.string.youtube_account),
                        subtitle = stringResource(Res.string.manage_your_youtube_accounts),
                        onClick = {
                            settingsViewModel.getAllGoogleAccount()
                            showYouTubeAccountDialog = true
                        },
                    )
                    SettingItem(
                        title = stringResource(Res.string.send_back_listening_data_to_google),
                        subtitle = stringResource(
                            Res.string.upload_your_listening_history_to_youtube_music_server_it_will_make_yt_music_recommendation_system_better_working_only_if_logged_in,
                        ),
                        smallSubtitle = true,
                        switch = (sendData to { settingsViewModel.setSendBackToGoogle(it) }),
                    )
                }
            }

            // Spotify
            item {
                SettingGroup(title = stringResource(Res.string.spotify)) {
                    SettingItem(
                        title = if (spotifyLoggedIn) {
                            stringResource(Res.string.log_out_from_spotify)
                        } else {
                            stringResource(Res.string.log_in_to_spotify)
                        },
                        subtitle = if (spotifyLoggedIn) {
                            stringResource(Res.string.logged_in)
                        } else {
                            "Sync lyrics and Canvas"
                        },
                        onClick = {
                            if (spotifyLoggedIn) {
                                settingsViewModel.confirmLogOut(
                                    confirmLabel = runBlocking { getString(Res.string.log_out_from_spotify) },
                                ) { settingsViewModel.setSpotifyLogIn(false) }
                            } else {
                                navController.navigate(SpotifyLoginDestination)
                            }
                        },
                    )
                }
            }

            // Discord (Zero Login Required)
            item {
                SettingGroup(title = stringResource(Res.string.discord_integration)) {
                    SettingItem(
                        title = stringResource(Res.string.enable_rich_presence),
                        subtitle = "Automatically shares current song on Discord without login",
                        switch = (richPresenceEnabled to { settingsViewModel.setDiscordRichPresenceEnabled(it) }),
                    )
                }
            }

            // Last.fm (gated on lastfmAvailable for FOSS vs Full builds)
            if (settingsViewModel.lastfmAvailable) {
                item {
                    SettingGroup(title = stringResource(Res.string.lastfm_integration)) {
                        SettingItem(
                            title = if (lastfmLoggedIn) {
                                stringResource(Res.string.log_out_from_lastfm)
                            } else {
                                stringResource(Res.string.log_in_to_lastfm)
                            },
                            subtitle = if (lastfmLoggedIn) {
                                stringResource(Res.string.logged_in_as, lastfmUsername)
                            } else {
                                "Scrobble your listening history"
                            },
                            onClick = {
                                if (lastfmLoggedIn) {
                                    settingsViewModel.confirmLogOut(
                                        confirmLabel = runBlocking { getString(Res.string.log_out_from_lastfm) },
                                    ) { settingsViewModel.logOutLastfm() }
                                } else {
                                    navController.navigate(LastfmLoginDestination)
                                }
                            },
                        )
                        SettingItem(
                            title = stringResource(Res.string.enable_scrobbling),
                            subtitle = stringResource(Res.string.scrobbling_info),
                            switch = (lastfmScrobbleEnabled to { settingsViewModel.setLastfmScrobbleEnabled(it) }),
                            isEnable = lastfmLoggedIn,
                        )
                    }
                }
            }

            item {
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Preview
@Composable
fun AccountHubPreview() {
    MaterialTheme {
        Surface(color = MaterialTheme.colorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Account",
                    style = typo().titleLarge,
                    fontWeight = FontWeight.Bold,
                )
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainer,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            modifier = Modifier.size(56.dp),
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHighest,
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = SimpIcons.Person,
                                    contentDescription = null,
                                    modifier = Modifier.size(32.dp),
                                )
                            }
                        }
                        Spacer(Modifier.width(16.dp))
                        Column {
                            Text("D-Spot User", style = typo().titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Signed in", style = typo().bodySmall, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
    }
}
