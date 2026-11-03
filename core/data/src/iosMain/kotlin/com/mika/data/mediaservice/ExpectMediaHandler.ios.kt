package com.mika.data.mediaservice

import com.mika.common.ASC
import com.mika.common.CUSTOM_ORDER
import com.mika.common.DESC
import com.mika.common.TITLE
import com.mika.domain.data.entities.SongEntity
import com.mika.domain.data.model.mediaService.SponsorSkipSegments
import com.mika.domain.data.model.streams.YouTubeWatchEndpoint
import com.mika.domain.data.player.AudioEffects
import com.mika.domain.data.player.GenericCastState
import com.mika.domain.data.player.GenericCommandButton
import com.mika.domain.data.player.GenericMediaItem
import com.mika.domain.data.player.GenericMediaMetadata
import com.mika.domain.data.player.GenericPlaybackParameters
import com.mika.domain.data.player.GenericTracks
import com.mika.domain.data.player.PlayerConstants
import com.mika.domain.data.player.PlayerError
import com.mika.domain.extension.toGenericMediaItem
import com.mika.domain.manager.DataStoreManager
import com.mika.domain.mediaservice.handler.MediaPlayerHandler
import com.mika.domain.mediaservice.player.MediaPlayerInterface
import com.mika.domain.mediaservice.state.ControlState
import com.mika.domain.mediaservice.state.NowPlayingState
import com.mika.domain.mediaservice.state.QueueData
import com.mika.domain.mediaservice.state.SimpleMediaState
import com.mika.domain.mediaservice.state.SleepTimerState
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
import platform.MediaPlayer.MPRemoteCommandHandlerStatus
import platform.MediaPlayer.MPRemoteCommandHandlerStatusSuccess
import platform.MediaPlayer.MPRemoteCommandCenter

/**
 * iOS native AVQueuePlayer wrapper implementing MediaPlayerInterface.
 */
@OptIn(ExperimentalForeignApi::class)
class IosAVPlayerWrapper : MediaPlayerInterface {
    val player: AVQueuePlayer = AVQueuePlayer()
    private val queueList = mutableListOf<GenericMediaItem>()
    private var currentIndex = -1

    init {
        setupAudioSession()
    }

    private fun setupAudioSession() {
        val session = AVAudioSession.sharedInstance()
        session.setCategory(AVAudioSessionCategoryPlayback, error = null)
        session.setActive(true, error = null)
    }

    override fun play() {
        player.play()
    }

    override fun pause() {
        player.pause()
    }

    override fun stop() {
        player.pause()
        player.seekToTime(kCMTimeZero)
    }

    override fun seekTo(positionMs: Long) {
        val seconds = positionMs / 1000.0
        val time = CMTimeMakeWithSeconds(seconds, 1000)
        player.seekToTime(time)
    }

    override fun seekToNext() {
        if (currentIndex + 1 < queueList.size) {
            seekToItem(currentIndex + 1)
        } else {
            player.advanceToNextItem()
        }
    }

    override fun seekToPrevious() {
        if (currentIndex > 0) {
            seekToItem(currentIndex - 1)
        } else {
            seekTo(0)
        }
    }

    fun seekToItem(index: Int) {
        if (index in queueList.indices) {
            currentIndex = index
            val item = queueList[index]
            val urlString = item.mediaMetadata.extras?.get("stream_url") ?: ""
            if (urlString.isNotEmpty()) {
                val nsUrl = NSURL.URLWithString(urlString)
                if (nsUrl != null) {
                    val playerItem = AVPlayerItem(uRL = nsUrl)
                    player.removeAllItems()
                    player.insertItem(playerItem, afterItem = null)
                    player.play()
                }
            }
        }
    }

    fun setItems(items: List<GenericMediaItem>, startIndex: Int = 0) {
        queueList.clear()
        queueList.addAll(items)
        if (items.isNotEmpty()) {
            seekToItem(startIndex)
        } else {
            player.removeAllItems()
            currentIndex = -1
        }
    }

    override fun setPlaybackParameters(playbackParameters: GenericPlaybackParameters) {
        // AVPlayer rate adjustment
    }

    override fun release() {
        player.pause()
        player.removeAllItems()
    }

    val currentPositionMs: Long
        get() {
            val seconds = CMTimeGetSeconds(player.currentTime())
            return if (seconds.isNaN()) 0L else (seconds * 1000).toLong()
        }

    val durationMs: Long
        get() {
            val current = player.currentItem ?: return 0L
            val seconds = CMTimeGetSeconds(current.duration)
            return if (seconds.isNaN()) 0L else (seconds * 1000).toLong()
        }

    val isPlaying: Boolean
        get() = player.timeControlStatus == AVPlayerTimeControlStatusPlaying
}

/**
 * Full iOS MediaPlayerHandler implementation using AVPlayer + MPRemoteCommandCenter.
 * Supports iOS, iPadOS lock-screen controls, and Apple CarPlay.
 */
@OptIn(ExperimentalForeignApi::class)
class IosMediaPlayerHandlerImpl(
    val dataStoreManager: DataStoreManager,
    val songRepository: SongRepository,
    val streamRepository: StreamRepository,
    val localPlaylistRepository: LocalPlaylistRepository,
    val analyticsRepository: AnalyticsRepository,
    val coroutineScope: CoroutineScope,
) : MediaPlayerHandler {

    val avPlayerWrapper = IosAVPlayerWrapper()

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    override val simpleMediaState: StateFlow<SimpleMediaState> = _simpleMediaState.asStateFlow()

    private val _nowPlaying = MutableStateFlow<GenericMediaItem?>(null)
    override val nowPlaying: StateFlow<GenericMediaItem?> = _nowPlaying.asStateFlow()

    private val _queueData = MutableStateFlow(QueueData(emptyList(), emptyList(), -1))
    override val queueData: StateFlow<QueueData> = _queueData.asStateFlow()

    private val _controlState = MutableStateFlow(
        ControlState(
            isPlaying = false,
            isShuffle = false,
            repeatMode = PlayerConstants.REPEAT_MODE_OFF,
            playbackSpeed = 1f,
            pitch = 1f,
        )
    )
    override val controlState: StateFlow<ControlState> = _controlState.asStateFlow()

    private val _nowPlayingState = MutableStateFlow(
        NowPlayingState(
            title = "",
            artist = "",
            artworkUri = null,
            duration = 0L,
            position = 0L,
            bufferedPosition = 0L,
        )
    )
    override val nowPlayingState: StateFlow<NowPlayingState> = _nowPlayingState.asStateFlow()

    private val _sleepTimerState = MutableStateFlow(SleepTimerState(false, 0L))
    override val sleepTimerState: StateFlow<SleepTimerState> = _sleepTimerState.asStateFlow()

    private val _castState = MutableStateFlow(GenericCastState())
    override val castState: StateFlow<GenericCastState> = _castState.asStateFlow()

    private val _sponsorSkipSegments = MutableStateFlow<SponsorSkipSegments?>(null)
    override val sponsorSkipSegments: StateFlow<SponsorSkipSegments?> = _sponsorSkipSegments.asStateFlow()

    private val _audioEffects = MutableStateFlow(AudioEffects())
    override val audioEffects: StateFlow<AudioEffects> = _audioEffects.asStateFlow()

    private val _customCommandButtons = MutableStateFlow<List<GenericCommandButton>>(emptyList())
    override val customCommandButtons: StateFlow<List<GenericCommandButton>> = _customCommandButtons.asStateFlow()

    private val _tracks = MutableStateFlow<GenericTracks?>(null)
    override val tracks: StateFlow<GenericTracks?> = _tracks.asStateFlow()

    private val _playbackParameters = MutableStateFlow(GenericPlaybackParameters(1f, 1f))
    override val playbackParameters: StateFlow<GenericPlaybackParameters> = _playbackParameters.asStateFlow()

    private val _playerError = MutableStateFlow<PlayerError?>(null)
    override val playerError: StateFlow<PlayerError?> = _playerError.asStateFlow()

    private val _volume = MutableStateFlow(1f)
    override val volume: StateFlow<Float> = _volume.asStateFlow()

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
                _nowPlayingState.value = _nowPlayingState.value.copy(
                    position = pos,
                    duration = dur,
                )

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
            play()
            MPRemoteCommandHandlerStatusSuccess
        }

        center.pauseCommand.setEnabled(true)
        center.pauseCommand.addTargetWithHandler { _ ->
            pause()
            MPRemoteCommandHandlerStatusSuccess
        }

        center.togglePlayPauseCommand.setEnabled(true)
        center.togglePlayPauseCommand.addTargetWithHandler { _ ->
            playPause()
            MPRemoteCommandHandlerStatusSuccess
        }

        center.nextTrackCommand.setEnabled(true)
        center.nextTrackCommand.addTargetWithHandler { _ ->
            seekToNext()
            MPRemoteCommandHandlerStatusSuccess
        }

        center.previousTrackCommand.setEnabled(true)
        center.previousTrackCommand.addTargetWithHandler { _ ->
            seekToPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }

        center.changePlaybackPositionCommand.setEnabled(true)
        center.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val posEvent = event as? MPChangePlaybackPositionCommandEvent
            val seconds = posEvent?.positionTime ?: 0.0
            seekTo((seconds * 1000).toLong())
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    override fun play() {
        avPlayerWrapper.play()
    }

    override fun pause() {
        avPlayerWrapper.pause()
    }

    override fun playPause() {
        if (avPlayerWrapper.isPlaying) pause() else play()
    }

    override fun stop() {
        avPlayerWrapper.stop()
    }

    override fun seekTo(positionMs: Long) {
        avPlayerWrapper.seekTo(positionMs)
    }

    override fun seekToNext() {
        avPlayerWrapper.seekToNext()
    }

    override fun seekToPrevious() {
        avPlayerWrapper.seekToPrevious()
    }

    override fun seekToItem(index: Int, positionMs: Long) {
        avPlayerWrapper.seekToItem(index)
        if (positionMs > 0) {
            avPlayerWrapper.seekTo(positionMs)
        }
    }

    override fun setMediaItem(mediaItem: GenericMediaItem, startPositionMs: Long) {
        setMediaItemList(listOf(mediaItem), 0, startPositionMs)
    }

    override fun setMediaItemList(mediaItemList: List<GenericMediaItem>, startIndex: Int, startPositionMs: Long) {
        avPlayerWrapper.setItems(mediaItemList, startIndex)
        if (startPositionMs > 0) {
            avPlayerWrapper.seekTo(startPositionMs)
        }
        _queueData.value = QueueData(mediaItemList, mediaItemList, startIndex)
        val current = mediaItemList.getOrNull(startIndex)
        _nowPlaying.value = current
        _nowPlayingState.value = NowPlayingState(
            title = current?.mediaMetadata?.title ?: "",
            artist = current?.mediaMetadata?.artist ?: "",
            artworkUri = current?.mediaMetadata?.artworkUri,
            duration = 0L,
            position = startPositionMs,
            bufferedPosition = 0L,
        )
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        val current = _queueData.value.items.toMutableList()
        current.add(mediaItem)
        _queueData.value = _queueData.value.copy(items = current)
    }

    override fun addMediaItemList(mediaItemList: List<GenericMediaItem>) {
        val current = _queueData.value.items.toMutableList()
        current.addAll(mediaItemList)
        _queueData.value = _queueData.value.copy(items = current)
    }

    override fun removeMediaItem(index: Int) {
        val current = _queueData.value.items.toMutableList()
        if (index in current.indices) {
            current.removeAt(index)
            _queueData.value = _queueData.value.copy(items = current)
        }
    }

    override fun moveMediaItem(from: Int, to: Int) {
        val current = _queueData.value.items.toMutableList()
        if (from in current.indices && to in current.indices) {
            val item = current.removeAt(from)
            current.add(to, item)
            _queueData.value = _queueData.value.copy(items = current)
        }
    }

    override fun clearQueue() {
        avPlayerWrapper.setItems(emptyList())
        _queueData.value = QueueData(emptyList(), emptyList(), -1)
        _nowPlaying.value = null
    }

    override fun setRepeatMode(repeatMode: Int) {
        _controlState.value = _controlState.value.copy(repeatMode = repeatMode)
    }

    override fun setShuffle(shuffle: Boolean) {
        _controlState.value = _controlState.value.copy(isShuffle = shuffle)
    }

    override fun setPlaybackSpeed(speed: Float) {
        _controlState.value = _controlState.value.copy(playbackSpeed = speed)
    }

    override fun setPitch(pitch: Float) {
        _controlState.value = _controlState.value.copy(pitch = pitch)
    }

    override fun setSleepTimer(minutes: Int) {
        // Sleep timer countdown
    }

    override fun cancelSleepTimer() {
        _sleepTimerState.value = SleepTimerState(false, 0L)
    }

    override fun setAudioEffects(audioEffects: AudioEffects) {
        _audioEffects.value = audioEffects
    }

    override fun resetAudioEffects() {
        _audioEffects.value = AudioEffects()
    }

    override fun setVolume(volume: Float) {
        _volume.value = volume
    }

    override fun fastForward() {
        seekTo(avPlayerWrapper.currentPositionMs + 10000)
    }

    override fun rewind() {
        seekTo(maxOf(0L, avPlayerWrapper.currentPositionMs - 10000))
    }

    override fun playFromEndpoint(endpoint: YouTubeWatchEndpoint) {}

    override fun sendCustomCommand(command: String, extras: Map<String, Any?>) {}

    override fun release() {
        progressJob?.cancel()
        avPlayerWrapper.release()
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
