package com.mika.dspot.di

import com.mika.dspot.viewModel.AlbumViewModel
import com.mika.dspot.utils.VersionManager
import com.mika.dspot.viewModel.AnalyticsViewModel
import com.mika.dspot.viewModel.ListenTogetherSettingsViewModel
import com.mika.dspot.viewModel.ListenTogetherViewModel
import com.mika.dspot.viewModel.ArtistViewModel
import com.mika.dspot.viewModel.BrowseViewModel
import com.mika.dspot.viewModel.HomeViewModel
import com.mika.dspot.viewModel.ImportViewModel
import com.mika.dspot.viewModel.LibraryDynamicPlaylistViewModel
import com.mika.dspot.viewModel.LibraryViewModel
import com.mika.dspot.viewModel.LocalPlaylistViewModel
import com.mika.dspot.viewModel.LogInViewModel
import com.mika.dspot.viewModel.LoginSyncHostViewModel
import com.mika.dspot.viewModel.LoginSyncSenderViewModel
import com.mika.dspot.viewModel.MoodViewModel
import com.mika.dspot.viewModel.MoreAlbumsViewModel
import com.mika.dspot.viewModel.NotificationViewModel
import com.mika.dspot.viewModel.NowPlayingBottomSheetViewModel
import com.mika.dspot.viewModel.PlaylistViewModel
import com.mika.dspot.viewModel.PodcastViewModel
import com.mika.dspot.viewModel.RecentlySongsViewModel
import com.mika.dspot.viewModel.SearchViewModel
import com.mika.dspot.viewModel.AutoEqViewModel
import com.mika.dspot.viewModel.SettingsViewModel
import com.mika.dspot.viewModel.SharedViewModel
import com.mika.dspot.viewModel.SongSelectionViewModel
import com.mika.dspot.viewModel.TasteViewModel
import com.mika.dspot.viewModel.WrappedViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val viewModelModule =
    module {
        single {
            SharedViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        single {
            SearchViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            SongSelectionViewModel(
                get(),
                get(),
            )
        }
        // Only one of these resolves on a given platform: Desktop binds the host repository, Android the
        // sender. Koin resolves dependencies when a ViewModel is created, not when it is declared.
        viewModel { LoginSyncHostViewModel(get()) }
        viewModel { LoginSyncSenderViewModel(get()) }
        viewModel {
            NowPlayingBottomSheetViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LibraryDynamicPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ImportViewModel(
                get(),
            )
        }
        viewModel {
            AlbumViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            HomeViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            AutoEqViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            SettingsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ArtistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            PlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            LogInViewModel(
                get(),
            )
        }
        viewModel {
            PodcastViewModel(
                get(),
            )
        }
        viewModel {
            MoreAlbumsViewModel(
                get(),
            )
        }
        viewModel {
            BrowseViewModel(
                get(),
            )
        }
        viewModel {
            RecentlySongsViewModel(
                get(),
            )
        }
        viewModel {
            LocalPlaylistViewModel(
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            NotificationViewModel(
                get(),
            )
        }
        viewModel {
            MoodViewModel(
                get(),
                get(),
            )
        }
        viewModel {
            AnalyticsViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            WrappedViewModel(
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            TasteViewModel(
                get(),
                get(),
                get(),
                get(),
                get(),
                get(),
            )
        }
        viewModel {
            ListenTogetherSettingsViewModel(get())
        }
        viewModel {
            ListenTogetherViewModel(
                repository = get(),
                dataStore = get(),
                bridge = get<com.mika.data.listentogether.ListenTogetherPlaybackBridge>(),
            )
        }

    }