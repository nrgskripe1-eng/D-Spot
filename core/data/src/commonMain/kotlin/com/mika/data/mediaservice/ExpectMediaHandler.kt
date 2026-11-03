package com.mika.data.mediaservice

import com.mika.domain.manager.DataStoreManager
import com.mika.domain.mediaservice.handler.MediaPlayerHandler
import com.mika.domain.repository.AnalyticsRepository
import com.mika.domain.repository.LocalPlaylistRepository
import com.mika.domain.repository.SongRepository
import com.mika.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope

expect fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler