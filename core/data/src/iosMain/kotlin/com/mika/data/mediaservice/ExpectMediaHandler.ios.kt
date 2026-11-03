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
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import platform.AVFoundation.AVAudioSession
import platform.AVFoundation.AVAudioSessionCategoryPlayback
import platform.AVFoundation.AVPlayer
import platform.AVFoundation.AVPlayerItem
import platform.AVFoundation.AVPlayerItemDidPlayToEndTimeNotification
import platform.AVFoundation.AVPlayerItemStatusReadyToPlay
import platform.AVFoundation.AVPlayerTimeControlStatus
import platform.AVFoundation.AVPlayerTimeControlStatusPaused
import platform.AVFoundation.AVPlayerTimeControlStatusPlaying
import platform.AVFoundation.AVQueuePlayer
import platform.AVFoundation.advanceToNextItem
import platform.AVFoundation.currentItem
import platform.AVFoundation.currentTime
import platform.AVFoundation.duration
import platform.AVFoundation.pause
import platform.AVFoundation.play
import platform.AVFoundation.removeAllItems
import platform.AVFoundation.seekToTime
import platform.AVFoundation.timeControlStatus
import platform.CoreMedia.CMTimeGetSeconds
import platform.CoreMedia.CMTimeMakeWithSeconds
import platform.CoreMedia.kCMTimeZero
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSURL
import platform.MediaPlayer.MPChangePlaybackPositionCommandEvent
import platform.MediaPlayer.MPMediaItemPropertyArtist
import platform.MediaPlayer.MPMediaItemPropertyPlaybackDuration
import platform.MediaPlayer.MPMediaItemPropertyTitle
import platform.MediaPlayer.MPNowPlayingInfoCenter
import platform.MediaPlayer.MPNowPlayingInfoPropertyElapsedPlaybackTime
import platform.MediaPlayer.MPNowPlayingInfoPropertyPlaybackRate
import platform.MediaPlayer.MPRemoteCommandCenter
import platform.MediaPlayer.MPRemoteCommandHandlerStatus

/**
 * iOS native AVQueuePlayer wrapper implementing MediaPlayerInterface.
 */
class IosAVPlayerWrapper : MediaPlayerInterface {
    private var player: AVQueuePlayer? = null
    private val playlist = mutableListOf<GenericMediaItem>()
    private var currentIndex: Int = -1
    private val listeners = mutableListOf<MediaPlayerListener>()

    init {
        setupAudioSession()
        player = AVQueuePlayer()
    }

    private fun setupAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        } catch (_: Exception) {}
    }

    fun setItems(items: List<GenericMediaItem>, startIndex: Int = 0) {
        playlist.clear()
        playlist.addAll(items)
        currentIndex = startIndex
        player?.removeAllItems()
        items.getOrNull(startIndex)?.let { item ->
            loadUrl(item)
        }
    }

    private fun loadUrl(mediaItem: GenericMediaItem) {
        val uri = mediaItem.mediaMetadata.mediaUri ?: return
        val url = NSURL.URLWithString(uri) ?: return
        val playerItem = AVPlayerItem(uRL = url)
        player?.removeAllItems()
        player?.insertItem(playerItem, afterItem = null)
    }

    val durationMs: Long
        get() {
            val cur = player?.currentItem ?: return 0L
            val sec = CMTimeGetSeconds(cur.duration)
            return if (sec.isNaN() || sec.isInfinite()) 0L else (sec * 1000).toLong()
        }

    val currentPositionMs: Long
        get() {
            val cur = player?.currentTime() ?: return 0L
            val sec = CMTimeGetSeconds(cur)
            return if (sec.isNaN() || sec.isInfinite()) 0L else (sec * 1000).toLong()
        }

    // MediaPlayerInterface implementations
    override fun play() {
        player?.play()
    }

    override fun pause() {
        player?.pause()
    }

    override fun stop() {
        player?.pause()
        player?.seekToTime(kCMTimeZero)
    }

    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        val time = CMTimeMakeWithSeconds(seconds, preferredTimescale = 1000)
        player?.seekToTime(time)
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (mediaItemIndex in playlist.indices) {
            currentIndex = mediaItemIndex
            loadUrl(playlist[mediaItemIndex])
            seekTo(positionMs)
        }
    }

    override fun seekBack() {
        seekTo(maxOf(0L, currentPositionMs - 5000))
    }

    override fun seekForward() {
        seekTo(currentPositionMs + 5000)
    }

    override fun seekToNext() {
        if (hasNextMediaItem()) {
            currentIndex++
            loadUrl(playlist[currentIndex])
            play()
        }
    }

    override fun seekToPrevious() {
        if (currentPositionMs > 3000L) {
            seekTo(0L)
        } else if (hasPreviousMediaItem()) {
            currentIndex--
            loadUrl(playlist[currentIndex])
            play()
        }
    }

    override fun seekToPreviousMediaItem() {
        if (hasPreviousMediaItem()) {
            currentIndex--
            loadUrl(playlist[currentIndex])
            play()
        }
    }

    override fun prepare() {}

    override fun setMediaItem(mediaItem: GenericMediaItem) {
        setItems(listOf(mediaItem), 0)
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        playlist.add(mediaItem)
    }

    override fun addMediaItem(index: Int, mediaItem: GenericMediaItem) {
        playlist.add(index, mediaItem)
    }

    override fun removeMediaItem(index: Int) {
        if (index in playlist.indices) {
            playlist.removeAt(index)
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
        player?.removeAllItems()
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
        get() = player?.timeControlStatus == AVPlayerTimeControlStatusPlaying

    override val currentPosition: Long
        get() = currentPositionMs

    override val duration: Long
        get() = durationMs

    override val bufferedPosition: Long
        get() = currentPositionMs

    override val bufferedPercentage: Int
        get() = 100

    override val currentMediaItem: GenericMediaItem?
        get() = playlist.getOrNull(currentIndex)

    override val currentMediaItemIndex: Int
        get() = currentIndex

    override val mediaItemCount: Int
        get() = playlist.size

    override val contentPosition: Long
        get() = currentPositionMs

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
        set(value) {
            field = value
            player?.volume = value
        }
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
        player?.pause()
        player?.removeAllItems()
        player = null
        listeners.clear()
    }
}

/**
 * iOS native MediaPlayerHandler implementation.
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
    private val avPlayerWrapper get() = player as IosAVPlayerWrapper

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

    private var progressJob: Job? = null

    init {
        setupRemoteCommands()
        startProgressTracker()
    }

    private fun startProgressTracker() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch(Dispatchers.Main) {
            while (isActive) {
                val isPlaying = avPlayerWrapper.isPlaying
                val pos = avPlayerWrapper.currentPositionMs
                val dur = avPlayerWrapper.durationMs

                _controlState.value = _controlState.value.copy(isPlaying = isPlaying)

                _nowPlaying.value?.let { current ->
                    updateNowPlayingInfoCenter(current, pos, dur, isPlaying)
                }

                delay(500)
            }
        }
    }

    private fun updateNowPlayingInfoCenter(item: GenericMediaItem, posMs: Long, durMs: Long, isPlaying: Boolean) {
        val info = mutableMapOf<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = item.mediaMetadata.title ?: ""
        info[MPMediaItemPropertyArtist] = item.mediaMetadata.artist ?: ""
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = posMs / 1000.0
        info[MPMediaItemPropertyPlaybackDuration] = if (durMs > 0) durMs / 1000.0 else 0.0
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (isPlaying) 1.0 else 0.0
        MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
    }

    private fun setupRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()

        center.playCommand.setEnabled(true)
        center.playCommand.addTargetWithHandler { _ ->
            player.play()
            MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
        }

        center.pauseCommand.setEnabled(true)
        center.pauseCommand.addTargetWithHandler { _ ->
            player.pause()
            MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
        }

        center.togglePlayPauseCommand.setEnabled(true)
        center.togglePlayPauseCommand.addTargetWithHandler { _ ->
            if (player.isPlaying) player.pause() else player.play()
            MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
        }

        center.nextTrackCommand.setEnabled(true)
        center.nextTrackCommand.addTargetWithHandler { _ ->
            player.seekToNext()
            MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
        }

        center.previousTrackCommand.setEnabled(true)
        center.previousTrackCommand.addTargetWithHandler { _ ->
            player.seekToPrevious()
            MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
        }

        center.changePlaybackPositionCommand.setEnabled(true)
        center.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val posEvent = event as? MPChangePlaybackPositionCommandEvent
            posEvent?.positionTime?.let { seconds ->
                player.seekTo((seconds * 1000).toLong())
            }
            MPRemoteCommandHandlerStatus.MPRemoteCommandHandlerStatusSuccess
        }
    }

    // Playback control
    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.PlayPause -> if (player.isPlaying) player.pause() else player.play()
            is PlayerEvent.SeekToNext -> player.seekToNext()
            is PlayerEvent.SeekToPrevious -> player.seekToPrevious()
            is PlayerEvent.SeekTo -> player.seekTo(playerEvent.position)
            is PlayerEvent.Stop -> player.stop()
            else -> {}
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
        progressJob?.cancel()
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
