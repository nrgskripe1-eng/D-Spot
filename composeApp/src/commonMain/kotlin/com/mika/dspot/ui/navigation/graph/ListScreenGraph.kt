package com.mika.dspot.ui.navigation.graph

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import com.mika.dspot.ui.navigation.destination.list.AlbumDestination
import com.mika.dspot.ui.navigation.destination.list.ArtistDestination
import com.mika.dspot.ui.navigation.destination.list.BrowseDestination
import com.mika.dspot.ui.navigation.destination.list.LocalPlaylistDestination
import com.mika.dspot.ui.navigation.destination.list.MoreAlbumsDestination
import com.mika.dspot.ui.navigation.destination.list.PlaylistDestination
import com.mika.dspot.ui.navigation.destination.list.PodcastDestination
import com.mika.dspot.ui.screen.library.LocalPlaylistScreen
import com.mika.dspot.ui.screen.other.AlbumScreen
import com.mika.dspot.ui.screen.other.ArtistScreen
import com.mika.dspot.ui.screen.other.BrowseScreen
import com.mika.dspot.ui.screen.other.MoreAlbumsScreen
import com.mika.dspot.ui.screen.other.PlaylistScreen
import com.mika.dspot.ui.screen.other.PodcastScreen
import com.mika.dspot.ui.theme.ForceDarkContent

@ExperimentalMaterial3Api
@ExperimentalFoundationApi
fun NavGraphBuilder.listScreenGraph(
    innerPadding: PaddingValues,
    navController: NavController,
) {
    composable<AlbumDestination> { entry ->
        val data = entry.toRoute<AlbumDestination>()
        ForceDarkContent {
            AlbumScreen(
                browseId = data.browseId,
                navController = navController,
            )
        }
    }
    composable<ArtistDestination> { entry ->
        val data = entry.toRoute<ArtistDestination>()
        ForceDarkContent {
            ArtistScreen(
                channelId = data.channelId,
                navController = navController,
            )
        }
    }
    composable<LocalPlaylistDestination> { entry ->
        val data = entry.toRoute<LocalPlaylistDestination>()
        ForceDarkContent {
            LocalPlaylistScreen(
                id = data.id,
                navController = navController,
            )
        }
    }
    composable<MoreAlbumsDestination> { entry ->
        val data = entry.toRoute<MoreAlbumsDestination>()
        MoreAlbumsScreen(
            innerPadding = innerPadding,
            navController = navController,
            type = data.type,
            id = data.id,
        )
    }
    composable<BrowseDestination> { entry ->
        val data = entry.toRoute<BrowseDestination>()
        BrowseScreen(
            innerPadding = innerPadding,
            navController = navController,
            browseId = data.browseId,
            params = data.params,
            title = data.title,
        )
    }
    composable<PlaylistDestination> { entry ->
        val data = entry.toRoute<PlaylistDestination>()
        ForceDarkContent {
            PlaylistScreen(
                playlistId = data.playlistId,
                isYourYouTubePlaylist = data.isYourYouTubePlaylist,
                navController = navController,
            )
        }
    }
    composable<PodcastDestination> { entry ->
        val data = entry.toRoute<PodcastDestination>()
        ForceDarkContent {
            PodcastScreen(
                podcastId = data.podcastId,
                navController = navController,
            )
        }
    }
}