package com.mika.data.mediaservice

import com.mika.domain.repository.AnalyticsRepository

actual fun createMediaServiceHandler(
    dataStoreManager: com.mika.domain.manager.DataStoreManager,
    songRepository: com.mika.domain.repository.SongRepository,
    streamRepository: com.mika.domain.repository.StreamRepository,
    localPlaylistRepository: com.mika.domain.repository.LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.mika.domain.mediaservice.handler.MediaPlayerHandler =
    MediaServiceHandlerImpl(
        dataStoreManager,
        songRepository,
        streamRepository,
        localPlaylistRepository,
        analyticsRepository,
        coroutineScope,
    )