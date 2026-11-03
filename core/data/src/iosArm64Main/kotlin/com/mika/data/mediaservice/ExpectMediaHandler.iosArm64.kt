package com.mika.data.mediaservice

actual fun createMediaServiceHandler(
    dataStoreManager: com.mika.domain.manager.DataStoreManager,
    songRepository: com.mika.domain.repository.SongRepository,
    streamRepository: com.mika.domain.repository.StreamRepository,
    localPlaylistRepository: com.mika.domain.repository.LocalPlaylistRepository,
    analyticsRepository: com.mika.domain.repository.AnalyticsRepository,
    coroutineScope: kotlinx.coroutines.CoroutineScope,
): com.mika.domain.mediaservice.handler.MediaPlayerHandler {
    TODO("Not yet implemented")
}