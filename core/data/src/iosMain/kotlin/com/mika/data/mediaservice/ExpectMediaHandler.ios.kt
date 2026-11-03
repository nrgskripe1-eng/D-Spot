package com.mika.data.mediaservice

import com.mika.domain.data.entities.NewFormatEntity
import com.mika.domain.data.entities.SongEntity
import com.mika.domain.data.model.browse.album.Track
import com.mika.domain.data.model.mediaService.SponsorSkipSegments
import com.mika.domain.data.player.AudioEffects
import com.mika.domain.data.player.GenericCastState
import com.mika.domain.data.player.GenericCommandButton
import com.mika.domain.data.player.GenericMediaItem
import com.mika.domain.data.player.GenericPlaybackParameters
import com.mika.domain.data.player.PlayerConstants
import com.mika.domain.data.player.PlayerError
import com.mika.domain.manager.DataStoreManager
import com.mika.domain.mediaservice.handler.ControlState
import com.mika.domain.mediaservice.handler.MediaPlayerHandler
import com.mika.domain.mediaservice.handler.NowPlayingTrackState
import com.mika.domain.mediaservice.handler.PlayerEvent
import com.mika.domain.mediaservice.handler.QueueData
import com.mika.domain.mediaservice.handler.RepeatState
import com.mika.domain.mediaservice.handler.SimpleMediaState
import com.mika.domain.mediaservice.handler.SleepTimerState
import com.mika.domain.mediaservice.handler.ToastType
import com.mika.domain.mediaservice.player.MediaPlayerInterface
import com.mika.domain.mediaservice.player.MediaPlayerListener
import com.mika.domain.repository.AnalyticsRepository
import com.mika.domain.repository.LocalPlaylistRepository
import com.mika.domain.repository.SongRepository
import com.mika.domain.repository.StreamRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * iOS implementation of MediaPlayerInterface.
 * Pure Kotlin stub without platform framework symbols to ensure clean multiplatform build.
 */
class IosAVPlayerWrapper : MediaPlayerInterface {
    private val playlist = mutableListOf<GenericMediaItem>()
    private var currentIndex: Int = -1
    private val listeners = mutableListOf<MediaPlayerListener>()
    private var _isPlaying: Boolean = false
    private var _currentPositionMs: Long = 0L

    override fun play() {
        _isPlaying = true
    }

    override fun pause() {
        _isPlaying = false
    }

    override fun stop() {
        _isPlaying = false
        _currentPositionMs = 0L
    }

    override fun seekTo(positionMs: Long) {
        _currentPositionMs = positionMs
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (mediaItemIndex in playlist.indices) {
            currentIndex = mediaItemIndex
            _currentPositionMs = positionMs
        }
    }

    override fun seekBack() {
        _currentPositionMs = maxOf(0L, _currentPositionMs - 5000)
    }

    override fun seekForward() {
        _currentPositionMs += 5000
    }

    override fun seekToNext() {
        if (hasNextMediaItem()) {
            currentIndex++
            _currentPositionMs = 0L
        }
    }

    override fun seekToPrevious() {
        if (_currentPositionMs > 3000L) {
            _currentPositionMs = 0L
        } else if (hasPreviousMediaItem()) {
            currentIndex--
            _currentPositionMs = 0L
        }
    }

    override fun seekToPreviousMediaItem() {
        if (hasPreviousMediaItem()) {
            currentIndex--
            _currentPositionMs = 0L
        }
    }

    override fun prepare() {}

    override fun setMediaItem(mediaItem: GenericMediaItem) {
        playlist.clear()
        playlist.add(mediaItem)
        currentIndex = 0
        _currentPositionMs = 0L
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        playlist.add(mediaItem)
        if (currentIndex == -1) currentIndex = 0
    }

    override fun addMediaItem(index: Int, mediaItem: GenericMediaItem) {
        playlist.add(index, mediaItem)
        if (currentIndex == -1) currentIndex = 0
    }

    override fun removeMediaItem(index: Int) {
        if (index in playlist.indices) {
            playlist.removeAt(index)
            if (currentIndex >= playlist.size) {
                currentIndex = playlist.size - 1
            }
        }
    }

    override fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex in playlist.indices && toIndex in playlist.indices) {
            val item = playlist.removeAt(fromIndex)
            playlist.add(toIndex, item)
        }
    }

    override fun clearMediaItems() {
        playlist.clear()
        currentIndex = -1
        _currentPositionMs = 0L
        _isPlaying = false
    }

    override fun replaceMediaItem(index: Int, mediaItem: GenericMediaItem) {
        if (index in playlist.indices) {
            playlist[index] = mediaItem
        }
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? = playlist.getOrNull(index)

    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> = playlist.toList()

    override fun getUnshuffledIndex(shuffledIndex: Int): Int = shuffledIndex

    override val isPlaying: Boolean
        get() = _isPlaying

    override val currentPosition: Long
        get() = _currentPositionMs

    override val duration: Long
        get() = 0L

    override val bufferedPosition: Long
        get() = _currentPositionMs

    override val bufferedPercentage: Int
        get() = 100

    override val currentMediaItem: GenericMediaItem?
        get() = playlist.getOrNull(currentIndex)

    override val currentMediaItemIndex: Int
        get() = currentIndex

    override val mediaItemCount: Int
        get() = playlist.size

    override val contentPosition: Long
        get() = _currentPositionMs

    override val playbackState: Int
        get() = PlayerConstants.STATE_READY

    override fun hasNextMediaItem(): Boolean = currentIndex < playlist.size - 1

    override fun hasPreviousMediaItem(): Boolean = currentIndex > 0

    override var shuffleModeEnabled: Boolean = false
    override var repeatMode: Int = PlayerConstants.REPEAT_MODE_OFF
    override var playWhenReady: Boolean = true
    override var playbackParameters: GenericPlaybackParameters = GenericPlaybackParameters(1f, 1f)
    override val audioSessionId: Int = 0
    override var volume: Float = 1f
    override var sleepFadeFactor: Float = 1f
    override var crossfadeSuppressed: Boolean = false
    override var albumTrackIds: Set<String> = emptySet()
    override var skipSilenceEnabled: Boolean = false

    override fun addListener(listener: MediaPlayerListener) {
        listeners.add(listener)
    }

    override fun removeListener(listener: MediaPlayerListener) {
        listeners.remove(listener)
    }

    override fun release() {
        clearMediaItems()
        listeners.clear()
    }
}

/**
 * iOS MediaPlayerHandler implementation for KMP.
 */
class IosMediaPlayerHandlerImpl(
    val dataStoreManager: DataStoreManager,
    val songRepository: SongRepository,
    val streamRepository: StreamRepository,
    val localPlaylistRepository: LocalPlaylistRepository,
    val analyticsRepository: AnalyticsRepository,
    val coroutineScope: CoroutineScope,
) : MediaPlayerHandler {

    override val player: MediaPlayerInterface = IosAVPlayerWrapper()

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    override val simpleMediaState: StateFlow<SimpleMediaState> = _simpleMediaState.asStateFlow()

    private val _nowPlaying = MutableStateFlow<GenericMediaItem?>(null)
    override val nowPlaying: StateFlow<GenericMediaItem?> = _nowPlaying.asStateFlow()

    private val _queueData = MutableStateFlow<QueueData?>(null)
    override val queueData: StateFlow<QueueData?> = _queueData.asStateFlow()

    private val _controlState = MutableStateFlow(
        ControlState(
            isPlaying = false,
            isShuffle = false,
            repeatState = RepeatState.None,
            isLiked = false,
            isNextAvailable = false,
            isPreviousAvailable = false,
            isCrossfading = false,
            volume = 1f,
        )
    )
    override val controlState: StateFlow<ControlState> = _controlState.asStateFlow()

    private val _nowPlayingState = MutableStateFlow(NowPlayingTrackState.initial())
    override val nowPlayingState: StateFlow<NowPlayingTrackState> = _nowPlayingState.asStateFlow()

    private val _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0))
    override val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private val _skipSegments = MutableStateFlow<List<SponsorSkipSegments>?>(null)
    override val skipSegments: StateFlow<List<SponsorSkipSegments>?> = _skipSegments.asStateFlow()

    private val _format = MutableStateFlow<NewFormatEntity?>(null)
    override val format: StateFlow<NewFormatEntity?> = _format.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(0)
    override val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    private val _castState = MutableStateFlow(GenericCastState())
    override val castState: StateFlow<GenericCastState> = _castState.asStateFlow()

    override var onUpdateNotification: (List<GenericCommandButton>) -> Unit = {}
    override var pushPlayerError: (PlayerError) -> Unit = {}
    override var showToast: (ToastType) -> Unit = {}

    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            PlayerEvent.PlayPause -> if (player.isPlaying) player.pause() else player.play()
            PlayerEvent.Next -> player.seekToNext()
            PlayerEvent.Previous -> player.seekToPrevious()
            PlayerEvent.SkipToPrevious -> player.seekToPreviousMediaItem()
            PlayerEvent.Backward -> player.seekBack()
            PlayerEvent.Forward -> player.seekForward()
            PlayerEvent.Stop -> player.stop()
            is PlayerEvent.UpdateProgress -> {
                val dur = player.duration
                if (dur > 0) {
                    player.seekTo((dur * playerEvent.newProgress).toLong())
                }
            }
            is PlayerEvent.UpdateVolume -> player.volume = playerEvent.newVolume
            PlayerEvent.ToggleLike -> toggleLike()
            PlayerEvent.Shuffle -> shufflePlaylist()
            PlayerEvent.Repeat -> {}
        }
    }

    override fun toggleRadio() {}
    override fun toggleLike() {}
    override fun like(liked: Boolean) {}
    override fun resetSongAndQueue() {
        player.clearMediaItems()
        _queueData.value = null
        _nowPlaying.value = null
    }

    override fun sleepStart(minutes: Int) {
        _sleepTimerState.value = SleepTimerState(isDone = false, timeRemaining = minutes * 60)
    }

    override fun sleepStop() {
        _sleepTimerState.value = SleepTimerState(isDone = true, timeRemaining = 0)
    }

    override fun removeMediaItem(position: Int) {
        player.removeMediaItem(position)
    }

    override fun addMediaItem(mediaItem: GenericMediaItem, playWhenReady: Boolean) {
        player.addMediaItem(mediaItem)
        if (playWhenReady) player.play()
    }

    override fun clearMediaItems() {
        player.clearMediaItems()
    }

    override fun addMediaItemList(mediaItemList: List<GenericMediaItem>) {
        mediaItemList.forEach { player.addMediaItem(it) }
    }

    override fun playMediaItemInMediaSource(index: Int) {
        player.seekTo(index, 0L)
        player.play()
    }

    override fun currentSongIndex(): Int = player.currentMediaItemIndex
    override fun currentOrderIndex(): Int = player.currentMediaItemIndex

    override suspend fun swap(from: Int, to: Int) {
        player.moveMediaItem(from, to)
    }

    override fun resetCrossfade() {}
    override fun shufflePlaylist(randomTrackIndex: Int) {}
    override fun loadMore() {}
    override fun getRelated(videoId: String) {}
    override fun setQueueData(queueData: QueueData.Data) {
        _queueData.value = QueueData(data = queueData)
    }

    override fun getCurrentMediaItem(): GenericMediaItem? = player.currentMediaItem

    override suspend fun moveItemUp(position: Int) {
        if (position > 0) player.moveMediaItem(position, position - 1)
    }

    override suspend fun moveItemDown(position: Int) {
        player.moveMediaItem(position, position + 1)
    }

    override suspend fun moveItemToPlayNext(position: Int) {
        val target = player.currentMediaItemIndex + 1
        player.moveMediaItem(position, target)
    }

    override fun addFirstMediaItemToIndex(mediaItem: GenericMediaItem?, index: Int) {
        mediaItem?.let { player.addMediaItem(index, it) }
    }

    override fun reset() {
        resetSongAndQueue()
    }

    override suspend fun load(downloaded: Int, index: Int?) {}
    override suspend fun loadMoreCatalog(listTrack: ArrayList<Track>, isAddToQueue: Boolean) {}
    override suspend fun updateCatalog(downloaded: Int, index: Int?): Boolean = false
    override fun addQueueToPlayer() {}
    override fun loadPlaylistOrAlbum(index: Int?) {}
    override fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    override suspend fun playNext(track: Track) {}
    override suspend fun <T> loadMediaItem(anyTrack: T, type: String, index: Int?) {}

    override fun getPlayerDuration(): Long = player.duration
    override fun getProgress(): Long = player.currentPosition
    override fun startProgressUpdate() {}
    override fun stopProgressUpdate() {}
    override fun startBufferedUpdate() {}
    override fun stopBufferedUpdate() {}

    override fun mayBeNormalizeVolume() {}
    override fun mayBeSaveRecentSong(runBlocking: Boolean) {}
    override fun mayBeSavePlaybackState() {}
    override fun mayBeRestoreQueue() {}
    override suspend fun restoreQueueAndPlay(): Boolean = false
    override fun shouldReleaseOnTaskRemoved(): Boolean = false

    override fun release() {
        player.release()
    }
}

actual fun createMediaServiceHandler(
    dataStoreManager: DataStoreManager,
    songRepository: SongRepository,
    streamRepository: StreamRepository,
    localPlaylistRepository: LocalPlaylistRepository,
    analyticsRepository: AnalyticsRepository,
    coroutineScope: CoroutineScope,
): MediaPlayerHandler = IosMediaPlayerHandlerImpl(
    dataStoreManager = dataStoreManager,
    songRepository = songRepository,
    streamRepository = streamRepository,
    localPlaylistRepository = localPlaylistRepository,
    analyticsRepository = analyticsRepository,
    coroutineScope = coroutineScope,
)
