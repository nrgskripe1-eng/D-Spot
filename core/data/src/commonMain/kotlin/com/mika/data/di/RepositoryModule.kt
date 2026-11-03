package com.mika.data.di

import com.mika.common.AppIdentity
import com.mika.common.Config.SERVICE_SCOPE
import com.mika.data.io.fileDir
import com.mika.data.repository.AccountRepositoryImpl
import com.mika.data.repository.AlbumRepositoryImpl
import com.mika.data.repository.AnalyticsRepositoryImpl
import com.mika.data.repository.ArtistRepositoryImpl
import com.mika.data.repository.AutoEqRepositoryImpl
import com.mika.data.lyrics.LyricsRomanizerRepositoryImpl
import com.mika.data.repository.CommonRepositoryImpl
import com.mika.data.repository.HomeRepositoryImpl
import com.mika.data.repository.ImportRepositoryImpl
import com.mika.data.repository.LocalPlaylistRepositoryImpl
import com.mika.data.repository.LyricsCanvasRepositoryImpl
import com.mika.data.repository.PlaylistRepositoryImpl
import com.mika.data.repository.PodcastRepositoryImpl
import com.mika.data.repository.SearchRepositoryImpl
import com.mika.data.repository.SongRepositoryImpl
import com.mika.data.repository.StreamRepositoryImpl
import com.mika.data.repository.TasteRepositoryImpl
import com.mika.data.repository.UpdateRepositoryImpl
import com.mika.domain.repository.AccountRepository
import com.mika.domain.repository.AlbumRepository
import com.mika.domain.repository.AnalyticsRepository
import com.mika.domain.repository.ArtistRepository
import com.mika.domain.repository.AutoEqRepository
import com.mika.domain.repository.LyricsRomanizerRepository
import com.mika.domain.repository.CommonRepository
import com.mika.domain.repository.HomeRepository
import com.mika.domain.repository.ImportRepository
import com.mika.domain.repository.LocalPlaylistRepository
import com.mika.domain.repository.LyricsCanvasRepository
import com.mika.domain.repository.PlaylistRepository
import com.mika.domain.repository.PodcastRepository
import com.mika.domain.repository.SearchRepository
import com.mika.domain.repository.SongRepository
import com.mika.domain.repository.StreamRepository
import com.mika.domain.repository.TasteRepository
import com.mika.domain.repository.UpdateRepository
import org.koin.core.qualifier.named
import org.koin.dsl.module

val repositoryModule =
    module {
        single<AccountRepository>(createdAtStart = true) {
            AccountRepositoryImpl(get(), get())
        }

        single<AlbumRepository>(createdAtStart = true) {
            AlbumRepositoryImpl(get(), get())
        }

        single<ArtistRepository>(createdAtStart = true) {
            ArtistRepositoryImpl(get(), get(), get())
        }

        single<CommonRepository>(createdAtStart = true) {
            CommonRepositoryImpl(get(named(SERVICE_SCOPE)), get(), get(), get(), get(), get()).apply {
                this.init("${fileDir()}/ytdlp-cookie.txt", get(), get<AppIdentity>().isDevBuild)
            }
        }

        // Lazy for the same reason its client is: the picker is the only thing that wants it.
        single<AutoEqRepository> {
            AutoEqRepositoryImpl(get(), get())
        }

        // Lazy: constructing it costs a few File.length() calls (plus, while the pack is missing,
        // one background read of the stored selection), but the kuromoji dictionary behind it is
        // loaded on first Japanese line, or right after a download installs it, and never at
        // startup — so this must NOT be createdAtStart, or every launch pays for a feature most
        // listeners leave off. The path is where Android keeps the downloaded ipadic pack (the
        // APK no longer bundles it); Desktop and iOS ignore it.
        single<LyricsRomanizerRepository> {
            LyricsRomanizerRepositoryImpl("${fileDir()}/kuromoji-ipadic", get())
        }

        single<HomeRepository>(createdAtStart = true) {
            HomeRepositoryImpl(get(), get())
        }

        single<ImportRepository>(createdAtStart = true) {
            ImportRepositoryImpl(get())
        }

        single<LocalPlaylistRepository>(createdAtStart = true) {
            LocalPlaylistRepositoryImpl(get(), get())
        }

        single<LyricsCanvasRepository>(createdAtStart = true) {
            LyricsCanvasRepositoryImpl(get(), get(), get(), get(), get())
        }

        single<PlaylistRepository>(createdAtStart = true) {
            PlaylistRepositoryImpl(get(), get(), get())
        }

        single<PodcastRepository>(createdAtStart = true) {
            PodcastRepositoryImpl(get(), get())
        }

        single<SearchRepository>(createdAtStart = true) {
            SearchRepositoryImpl(get(), get())
        }

        single<SongRepository>(createdAtStart = true) {
            SongRepositoryImpl(get(), get(), get())
        }

        single<StreamRepository>(createdAtStart = true) {
            StreamRepositoryImpl(get(), get())
        }

        single<UpdateRepository>(createdAtStart = true) {
            UpdateRepositoryImpl(get())
        }

        single<AnalyticsRepository>(createdAtStart = true) {
            AnalyticsRepositoryImpl(get())
        }

        // Lazy: only the taste card in Library asks for it.
        single<TasteRepository> {
            TasteRepositoryImpl(get(), get())
        }
    }