package com.mika.data.mediaservice

import com.mika.common.ASC
import com.mika.common.CUSTOM_ORDER
import com.mika.common.Config.ALBUM_CLICK
import com.mika.common.Config.PLAYLIST_CLICK
import com.mika.common.Config.RADIO_CLICK
import com.mika.common.Config.RECOVER_TRACK_QUEUE
import com.mika.common.Config.SHARE
import com.mika.common.Config.SONG_CLICK
import com.mika.common.Config.VIDEO_CLICK
import com.mika.common.DESC
import com.mika.common.LOCAL_PLAYLIST_ID
import com.mika.common.LOCAL_PLAYLIST_ID_SAVED_QUEUE
import com.mika.common.MERGING_DATA_TYPE
import com.mika.common.SPONSOR_BLOCK_MIN_SEGMENT_SECONDS
import com.mika.common.SPONSOR_BLOCK_SKIP_MARGIN_MS
import com.mika.common.SponsorBlockType
import com.mika.common.TITLE
import com.mika.data.db.Converters
import com.mika.domain.data.entities.NewFormatEntity
import com.mika.domain.data.entities.SongEntity
import com.mika.domain.data.model.browse.album.Track
import com.mika.domain.data.model.mediaService.SponsorSkipSegments
import com.mika.domain.data.model.searchResult.songs.Artist
import com.mika.domain.data.model.streams.YouTubeWatchEndpoint
import com.mika.domain.data.player.GenericCastState
import com.mika.domain.data.player.GenericCommandButton
import com.mika.domain.data.player.GenericMediaItem
import com.mika.domain.data.player.GenericMediaMetadata
import com.mika.domain.data.player.GenericPlaybackParameters
import com.mika.domain.data.player.PlayerConstants
import com.mika.domain.data.player.PlayerError
import com.mika.domain.extension.isVideo
import com.mika.domain.extension.now
import com.mika.domain.extension.toGenericMediaItem
import com.mika.domain.extension.toSongEntity
import com.mika.domain.manager.DataStoreManager
import com.mika.domain.manager.DataStoreManager.Values.FALSE
import com.mika.domain.manager.DataStoreManager.Values.TRUE
import com.mika.domain.mediaservice.handler.ControlState
import com.mika.domain.mediaservice.handler.MediaPlayerHandler
import com.mika.domain.mediaservice.handler.NowPlayingTrackState
import com.mika.domain.mediaservice.handler.PlayerEvent
import com.mika.domain.mediaservice.handler.PlaylistType
import com.mika.domain.mediaservice.handler.QueueData
import com.mika.domain.mediaservice.handler.RadioQueueTrim
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
import com.mika.domain.utils.FilterState
import com.mika.domain.utils.MusicVideoType
import com.mika.domain.utils.Resource
import com.mika.domain.utils.connectArtists
import com.mika.domain.utils.toArrayListTrack
import com.mika.domain.utils.toListName
import com.mika.domain.utils.toSongEntity
import com.mika.domain.utils.toTrack
import com.mika.logger.Logger
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.lastOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.single
import kotlinx.coroutines.flow.singleOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import platform.AVFAudio.*
import platform.AVFoundation.*
import platform.CoreMedia.*
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.*
import platform.MediaPlayer.*
import platform.UIKit.UIImage
import platform.darwin.dispatch_async
import platform.darwin.dispatch_get_main_queue
import kotlin.math.PI
import kotlin.math.cos

private const val TAG = "IosMediaPlayerHandler"

/**
 * iOS AVFoundation implementation of MediaPlayerInterface.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosAVPlayerWrapper(
    private val dataStoreManager: DataStoreManager,
    private val streamRepository: StreamRepository,
    private val coroutineScope: CoroutineScope,
) : MediaPlayerInterface {

    private val avPlayer = AVQueuePlayer()
    private val playlist = mutableListOf<GenericMediaItem>()
    private var currentIndex = -1
    private val listeners = mutableListOf<MediaPlayerListener>()

    // Track stream loading job to prevent race conditions when skipping quickly
    private var streamLoadJob: Job? = null
    private var currentEndObserver: Any? = null

    init {
        setupAudioSession()
    }

    private fun setupAudioSession() {
        runCatching {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(AVAudioSessionCategoryPlayback, error = null)
            session.setActive(true, error = null)
        }.onFailure {
            Logger.e(TAG, "Failed to setup AVAudioSession: ${it.message}")
        }
    }

    override fun play() {
        dispatch_async(dispatch_get_main_queue()) {
            avPlayer.play()
            listeners.forEach { it.onIsPlayingChanged(true) }
            listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_READY) }
        }
    }

    override fun pause() {
        dispatch_async(dispatch_get_main_queue()) {
            avPlayer.pause()
            listeners.forEach { it.onIsPlayingChanged(false) }
        }
    }

    override fun stop() {
        dispatch_async(dispatch_get_main_queue()) {
            avPlayer.pause()
            val zero = CMTimeMakeWithSeconds(0.0, 1000)
            avPlayer.seekToTime(zero)
            listeners.forEach { it.onIsPlayingChanged(false) }
            listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_IDLE) }
        }
    }

    override fun seekTo(positionMs: Long) {
        dispatch_async(dispatch_get_main_queue()) {
            val time = CMTimeMakeWithSeconds(positionMs / 1000.0, 1000)
            avPlayer.seekToTime(time)
            listeners.forEach { it.onSeeked(positionMs) }
        }
    }

    override fun seekTo(mediaItemIndex: Int, positionMs: Long) {
        if (mediaItemIndex in playlist.indices) {
            if (mediaItemIndex != currentIndex) {
                loadItemAtIndex(mediaItemIndex, startPositionMs = positionMs)
            } else {
                seekTo(positionMs)
            }
        }
    }

    override fun seekBack() = seekTo(maxOf(0L, currentPosition - 10_000L))
    override fun seekForward() = seekTo(currentPosition + 10_000L)

    override fun seekToNext() {
        if (hasNextMediaItem()) {
            currentIndex++
            loadItemAtIndex(currentIndex)
        }
    }

    override fun seekToPrevious() {
        if (currentPosition > 3_000L) {
            seekTo(0L)
        } else if (hasPreviousMediaItem()) {
            currentIndex--
            loadItemAtIndex(currentIndex)
        }
    }

    override fun seekToPreviousMediaItem() {
        if (hasPreviousMediaItem()) {
            currentIndex--
            loadItemAtIndex(currentIndex)
        }
    }

    override fun prepare() {
        // AVPlayer prepares automatically when item is inserted
    }

    internal fun loadItemAtIndex(index: Int, startPositionMs: Long = 0L) {
        val item = playlist.getOrNull(index) ?: return
        currentIndex = index

        streamLoadJob?.cancel()
        streamLoadJob = coroutineScope.launch {
            val videoId = if (item.isVideo()) {
                item.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
            } else {
                item.mediaId
            }

            listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_BUFFERING) }

            var streamUrl: String? = null

            // 1. Check cached new format
            val cached = streamRepository.getNewFormat(videoId).firstOrNull()
            if (cached != null && cached.expiredTime > now() && !cached.audioUrl.isNullOrEmpty()) {
                streamUrl = cached.audioUrl
            }

            // 2. Fetch fresh stream URL if not cached or expired
            if (streamUrl.isNullOrEmpty()) {
                streamUrl = streamRepository.getStream(
                    dataStoreManager = dataStoreManager,
                    videoId = videoId,
                    isDownloading = false,
                    isVideo = false,
                ).firstOrNull()
            }

            if (streamUrl.isNullOrEmpty()) {
                Logger.e(TAG, "Failed to resolve stream URL for $videoId")
                listeners.forEach {
                    it.onPlayerError(
                        PlayerError(
                            errorCode = PlayerConstants.ERROR_CODE_TIMEOUT,
                            errorCodeName = "STREAM_NOT_FOUND",
                            message = "Could not resolve stream URL for $videoId",
                        )
                    )
                }
                return@launch
            }

            withContext(Dispatchers.Main) {
                val url = NSURL.URLWithString(streamUrl) ?: return@withContext
                val avItem = AVPlayerItem(url)

                currentEndObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }

                avPlayer.removeAllItems()
                avPlayer.insertItem(avItem, afterItem = null)

                if (startPositionMs > 0L) {
                    val time = CMTimeMakeWithSeconds(startPositionMs / 1000.0, 1000)
                    avPlayer.seekToTime(time)
                }

                currentEndObserver = NSNotificationCenter.defaultCenter.addObserverForName(
                    name = AVPlayerItemDidPlayToEndTimeNotification,
                    `object` = avItem,
                    queue = NSOperationQueue.mainQueue,
                ) {
                    if (repeatMode == PlayerConstants.REPEAT_MODE_ONE) {
                        seekTo(0L)
                        play()
                    } else if (hasNextMediaItem()) {
                        seekToNext()
                    } else {
                        listeners.forEach { it.onPlaybackStateChanged(PlayerConstants.STATE_ENDED) }
                    }
                }

                listeners.forEach {
                    it.onMediaItemTransition(item, PlayerConstants.MEDIA_ITEM_TRANSITION_REASON_AUTO)
                }
                listeners.forEach { it.onTimelineChanged(playlist.toList(), "playlist") }

                if (playWhenReady) {
                    play()
                }
            }
        }
    }

    override fun setMediaItem(mediaItem: GenericMediaItem) {
        playlist.clear()
        playlist.add(mediaItem)
        currentIndex = 0
        loadItemAtIndex(0)
    }

    override fun addMediaItem(mediaItem: GenericMediaItem) {
        playlist.add(mediaItem)
        if (currentIndex == -1) {
            currentIndex = 0
            loadItemAtIndex(0)
        }
        listeners.forEach { it.onTimelineChanged(playlist.toList(), "add") }
    }

    override fun addMediaItem(index: Int, mediaItem: GenericMediaItem) {
        val targetIndex = index.coerceIn(0, playlist.size)
        playlist.add(targetIndex, mediaItem)
        if (currentIndex >= targetIndex) currentIndex++
        listeners.forEach { it.onTimelineChanged(playlist.toList(), "add") }
    }

    override fun removeMediaItem(index: Int) {
        if (index !in playlist.indices) return
        playlist.removeAt(index)
        if (currentIndex >= playlist.size) {
            currentIndex = playlist.size - 1
        }
        listeners.forEach { it.onTimelineChanged(playlist.toList(), "remove") }
    }

    override fun moveMediaItem(fromIndex: Int, toIndex: Int) {
        if (fromIndex !in playlist.indices || toIndex !in playlist.indices) return
        val item = playlist.removeAt(fromIndex)
        playlist.add(toIndex, item)
        listeners.forEach { it.onTimelineChanged(playlist.toList(), "move") }
    }

    override fun clearMediaItems() {
        dispatch_async(dispatch_get_main_queue()) {
            streamLoadJob?.cancel()
            currentEndObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
            currentEndObserver = null
            avPlayer.removeAllItems()
            playlist.clear()
            currentIndex = -1
            listeners.forEach { it.onTimelineChanged(emptyList(), "clear") }
        }
    }

    override fun replaceMediaItem(index: Int, mediaItem: GenericMediaItem) {
        if (index !in playlist.indices) return
        playlist[index] = mediaItem
        if (index == currentIndex) {
            loadItemAtIndex(index)
        }
    }

    override fun getMediaItemAt(index: Int): GenericMediaItem? = playlist.getOrNull(index)
    override fun getCurrentMediaTimeLine(): List<GenericMediaItem> = playlist.toList()
    override fun getUnshuffledIndex(shuffledIndex: Int): Int = shuffledIndex

    override val isPlaying: Boolean
        get() = avPlayer.timeControlStatus == AVPlayerTimeControlStatusPlaying

    override val currentPosition: Long
        get() {
            val secs = CMTimeGetSeconds(avPlayer.currentTime())
            return if (secs.isNaN() || secs < 0.0) 0L else (secs * 1000.0).toLong()
        }

    override val duration: Long
        get() {
            val dur = avPlayer.currentItem?.duration ?: return 0L
            val secs = CMTimeGetSeconds(dur)
            return if (secs.isNaN() || secs <= 0.0) 0L else (secs * 1000.0).toLong()
        }

    override val bufferedPosition: Long
        get() {
            val ranges = avPlayer.currentItem?.loadedTimeRanges ?: return currentPosition
            val last = ranges.lastOrNull() ?: return currentPosition
            val range = last as? NSValue ?: return currentPosition
            return try {
                val timeRange = range.CMTimeRangeValue
                val end = CMTimeGetSeconds(CMTimeRangeGetEnd(timeRange))
                if (end.isNaN() || end < 0.0) currentPosition else (end * 1000.0).toLong()
            } catch (_: Exception) {
                currentPosition
            }
        }

    override val bufferedPercentage: Int
        get() {
            val dur = duration
            return if (dur <= 0L) 0 else ((bufferedPosition * 100) / dur).toInt().coerceIn(0, 100)
        }

    override val currentMediaItem: GenericMediaItem?
        get() = playlist.getOrNull(currentIndex)

    override val currentMediaItemIndex: Int get() = currentIndex
    override val mediaItemCount: Int get() = playlist.size
    override val contentPosition: Long get() = currentPosition

    override val playbackState: Int
        get() = when (avPlayer.status) {
            AVPlayerStatusReadyToPlay -> PlayerConstants.STATE_READY
            AVPlayerStatusFailed -> PlayerConstants.STATE_IDLE
            else -> PlayerConstants.STATE_BUFFERING
        }

    override fun hasNextMediaItem(): Boolean = currentIndex < playlist.size - 1
    override fun hasPreviousMediaItem(): Boolean = currentIndex > 0

    override var shuffleModeEnabled: Boolean = false
    override var repeatMode: Int = PlayerConstants.REPEAT_MODE_OFF
    override var playWhenReady: Boolean = true
        set(value) {
            field = value
            if (value) play() else pause()
        }
    override var playbackParameters: GenericPlaybackParameters = GenericPlaybackParameters(1f, 1f)
        set(value) {
            field = value
            avPlayer.rate = value.speed
        }

    override val audioSessionId: Int = 0
    override var volume: Float
        get() = avPlayer.volume
        set(value) { avPlayer.volume = value }
    override var sleepFadeFactor: Float = 1f
        set(value) {
            field = value
            avPlayer.volume = volume * value
        }
    override var crossfadeSuppressed: Boolean = false
    override var albumTrackIds: Set<String> = emptySet()
    override var skipSilenceEnabled: Boolean = false

    override fun addListener(listener: MediaPlayerListener) { listeners.add(listener) }
    override fun removeListener(listener: MediaPlayerListener) { listeners.remove(listener) }

    override fun release() {
        streamLoadJob?.cancel()
        currentEndObserver?.let { NSNotificationCenter.defaultCenter.removeObserver(it) }
        currentEndObserver = null
        avPlayer.pause()
        avPlayer.removeAllItems()
        playlist.clear()
        listeners.clear()
        runCatching {
            AVAudioSession.sharedInstance().setActive(false, error = null)
        }
    }
}

/**
 * iOS MediaPlayerHandler implementation with full AVFoundation and CarPlay / LockScreen integration.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
class IosMediaPlayerHandlerImpl(
    val dataStoreManager: DataStoreManager,
    val songRepository: SongRepository,
    val streamRepository: StreamRepository,
    val localPlaylistRepository: LocalPlaylistRepository,
    val analyticsRepository: AnalyticsRepository,
    val coroutineScope: CoroutineScope,
) : MediaPlayerHandler, MediaPlayerListener {

    private val backgroundScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override val player: MediaPlayerInterface = IosAVPlayerWrapper(
        dataStoreManager = dataStoreManager,
        streamRepository = streamRepository,
        coroutineScope = coroutineScope,
    ).also {
        it.addListener(this)
    }

    private val _simpleMediaState = MutableStateFlow<SimpleMediaState>(SimpleMediaState.Initial)
    override val simpleMediaState: StateFlow<SimpleMediaState> = _simpleMediaState.asStateFlow()

    private val _nowPlaying = MutableStateFlow<GenericMediaItem?>(null)
    override val nowPlaying: StateFlow<GenericMediaItem?> = _nowPlaying.asStateFlow()

    private val _queueData = MutableStateFlow<QueueData?>(
        QueueData(
            queueState = QueueData.StateSource.STATE_CREATED,
            data = QueueData.Data(),
        )
    )
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

    private val sponsorBlockCategories = MutableStateFlow<Set<String>?>(null)

    private val _format = MutableStateFlow<NewFormatEntity?>(null)
    override val format: StateFlow<NewFormatEntity?> = _format.asStateFlow()

    private val _currentSongIndex = MutableStateFlow(0)
    override val currentSongIndex: StateFlow<Int> = _currentSongIndex.asStateFlow()

    override val castState: StateFlow<GenericCastState> = MutableStateFlow(GenericCastState.NOT_CASTING).asStateFlow()

    override var onUpdateNotification: (List<GenericCommandButton>) -> Unit = {}
    override var pushPlayerError: (PlayerError) -> Unit = {}
    override var showToast: (ToastType) -> Unit = {}

    private var progressJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var toggleLikeJob: Job? = null
    private var loadJob: Job? = null
    private var songEntityJob: Job? = null
    private var getSkipSegmentsJob: Job? = null
    private var getFormatJob: Job? = null
    private val restoreQueueMutex = Mutex()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
        explicitNulls = false
    }

    private fun fromListIntToString(list: List<Int>?): String? = list?.let { json.encodeToString(list) }
    private fun fromStringToListInt(value: String?): List<Int>? = try {
        value?.let { json.decodeFromString<List<Int>>(it) }
    } catch (_: Exception) { null }

    init {
        setupRemoteCommands()

        if (runBlocking { dataStoreManager.saveStateOfPlayback.first() } == TRUE) {
            val shuffleKey = runBlocking { dataStoreManager.shuffleKey.first() }
            val repeatKey = runBlocking { dataStoreManager.repeatKey.first() }
            val restoredShuffle = shuffleKey == TRUE
            val restoredRepeatMode = when (repeatKey) {
                DataStoreManager.REPEAT_ONE -> PlayerConstants.REPEAT_MODE_ONE
                DataStoreManager.REPEAT_ALL -> PlayerConstants.REPEAT_MODE_ALL
                else -> PlayerConstants.REPEAT_MODE_OFF
            }
            player.shuffleModeEnabled = restoredShuffle
            player.repeatMode = restoredRepeatMode
            _controlState.value = _controlState.value.copy(
                isShuffle = restoredShuffle,
                repeatState = when (restoredRepeatMode) {
                    PlayerConstants.REPEAT_MODE_ONE -> RepeatState.One
                    PlayerConstants.REPEAT_MODE_ALL -> RepeatState.All
                    else -> RepeatState.None
                },
            )
        }

        player.volume = runBlocking { dataStoreManager.playerVolume.first() }
        mayBeRestoreQueue()

        coroutineScope.launch {
            launch {
                controlState.collectLatest {
                    updateNotification()
                }
            }
            launch {
                combine(
                    SponsorBlockType.toList().map { type ->
                        dataStoreManager.getString(type.value).map { if (it == TRUE) type.value else null }
                    },
                ) { ticked -> ticked.filterNotNull().toSet() }
                    .combine(dataStoreManager.sponsorBlockEnabled) { ticked, enabled -> ticked.takeIf { enabled == TRUE } }
                    .collect { sponsorBlockCategories.value = it }
            }
            launch {
                simpleMediaState
                    .filter { it is SimpleMediaState.Progress }
                    .map {
                        val current = (it as SimpleMediaState.Progress).progress
                        val duration = player.duration
                        if (duration > 0L) (current.toFloat() / duration) * 100 else -1f
                    }
                    .filter { it >= 0f }
                    .distinctUntilChanged()
                    .collect { current ->
                        val categories = sponsorBlockCategories.value
                        val skipSegments = skipSegments.value
                        if (categories != null && player.duration > 0L && skipSegments != null) {
                            for (skip in skipSegments) {
                                if (categories.contains(skip.category)) {
                                    if (skip.segment[1] - skip.segment[0] < SPONSOR_BLOCK_MIN_SEGMENT_SECONDS) continue
                                    val firstPart = ((skip.segment[0] / skip.videoDuration) * 100).toFloat()
                                    val secondPart = ((skip.segment[1] / skip.videoDuration) * 100).toFloat()
                                    if (current in firstPart..secondPart) {
                                        val targetMs = (secondPart * player.duration).toLong() / 100 + SPONSOR_BLOCK_SKIP_MARGIN_MS
                                        player.seekTo(targetMs)
                                        showToast(ToastType.SponsorBlockSkip(skip.category))
                                    }
                                }
                            }
                        }
                    }
            }
        }
    }

    private fun setupRemoteCommands() {
        val center = MPRemoteCommandCenter.sharedCommandCenter()

        center.playCommand.addTargetWithHandler { _ ->
            player.play()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.pauseCommand.addTargetWithHandler { _ ->
            player.pause()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.togglePlayPauseCommand.addTargetWithHandler { _ ->
            if (player.isPlaying) player.pause() else player.play()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.nextTrackCommand.addTargetWithHandler { _ ->
            player.seekToNext()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.previousTrackCommand.addTargetWithHandler { _ ->
            player.seekToPrevious()
            MPRemoteCommandHandlerStatusSuccess
        }
        center.changePlaybackPositionCommand.addTargetWithHandler { event ->
            val e = event as? MPChangePlaybackPositionCommandEvent
            val ms = ((e?.positionTime ?: 0.0) * 1000.0).toLong()
            player.seekTo(ms)
            MPRemoteCommandHandlerStatusSuccess
        }
    }

    private fun updateNowPlayingInfo() {
        val item = player.currentMediaItem ?: return
        val info = mutableMapOf<Any?, Any?>()
        info[MPMediaItemPropertyTitle] = item.metadata.title ?: ""
        info[MPMediaItemPropertyArtist] = item.metadata.artist ?: ""
        info[MPNowPlayingInfoPropertyElapsedPlaybackTime] = player.currentPosition / 1000.0
        info[MPMediaItemPropertyPlaybackDuration] = player.duration / 1000.0
        info[MPNowPlayingInfoPropertyPlaybackRate] = if (player.isPlaying) 1.0 else 0.0

        val artworkUrl = item.metadata.artworkUri
        if (!artworkUrl.isNullOrEmpty()) {
            loadArtwork(artworkUrl) { image ->
                if (image != null) {
                    info[MPMediaItemPropertyArtwork] = MPMediaItemArtwork(
                        boundsSize = CGSizeMake(600.0, 600.0)
                    ) { _ -> image }
                }
                MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
            }
        } else {
            MPNowPlayingInfoCenter.defaultCenter().nowPlayingInfo = info
        }
    }

    private fun loadArtwork(urlString: String, completion: (UIImage?) -> Unit) {
        val url = NSURL.URLWithString(urlString) ?: run { completion(null); return }
        NSURLSession.sharedSession.dataTaskWithURL(url) { data, _, _ ->
            val image = data?.let { UIImage.imageWithData(it) }
            dispatch_async(dispatch_get_main_queue()) { completion(image) }
        }.resume()
    }

    override fun onMediaItemTransition(mediaItem: GenericMediaItem?, reason: Int) {
        _nowPlaying.value = mediaItem
        _currentSongIndex.value = player.currentMediaItemIndex
        _controlState.update {
            it.copy(
                isNextAvailable = player.hasNextMediaItem(),
                isPreviousAvailable = player.hasPreviousMediaItem(),
            )
        }
        updateNowPlayingInfo()

        if (mediaItem != null) {
            getDataOfNowPlayingState(mediaItem)
        } else {
            _nowPlayingState.value = NowPlayingTrackState.initial()
        }

        queueData.value?.data?.listTracks?.let { list ->
            if ((list.size > 3 || runBlocking { dataStoreManager.endlessQueue.first() == TRUE }) &&
                list.size - player.currentMediaItemIndex < 3 &&
                list.size - player.currentMediaItemIndex >= 0 &&
                queueData.value?.queueState == QueueData.StateSource.STATE_INITIALIZED
            ) {
                loadMore()
            }
        }
        updateNotification()
        mayBeSaveRecentSong()
    }

    override fun onIsPlayingChanged(isPlaying: Boolean) {
        _controlState.update { it.copy(isPlaying = isPlaying) }
        if (isPlaying) {
            startProgressUpdate()
        } else {
            stopProgressUpdate()
            mayBeSaveRecentSong()
            mayBeSavePlaybackState()
        }
        updateNowPlayingInfo()
    }

    override fun onTimelineChanged(list: List<GenericMediaItem>, reason: String) {
        val currentTracks = queueData.value?.data?.listTracks ?: emptyList()
        val byId = HashMap<String, Track>(currentTracks.size)
        currentTracks.forEach { track ->
            if (!byId.containsKey(track.videoId)) byId[track.videoId] = track
        }
        val sorted = list.mapNotNull { byId[it.mediaId] }
        if (sorted.size == currentTracks.size && sorted.isNotEmpty()) {
            _queueData.update { current ->
                val data = current?.data ?: return@update current
                current.copy(data = data.copy(listTracks = sorted))
            }
        }
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        _simpleMediaState.value = when (playbackState) {
            PlayerConstants.STATE_READY -> SimpleMediaState.Ready(player.duration)
            PlayerConstants.STATE_BUFFERING -> SimpleMediaState.Buffering(player.currentPosition)
            PlayerConstants.STATE_ENDED -> SimpleMediaState.Ended
            else -> SimpleMediaState.Initial
        }
    }

    override fun onSeeked(positionMs: Long) {
        if (!player.isPlaying) {
            _simpleMediaState.value = SimpleMediaState.Progress(positionMs)
        }
        updateNowPlayingInfo()
    }

    override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean, list: List<GenericMediaItem>) {
        _controlState.update { it.copy(isShuffle = shuffleModeEnabled) }
    }

    override fun onRepeatModeChanged(repeatMode: Int) {
        _controlState.update {
            it.copy(
                repeatState = when (repeatMode) {
                    PlayerConstants.REPEAT_MODE_ONE -> RepeatState.One
                    PlayerConstants.REPEAT_MODE_ALL -> RepeatState.All
                    else -> RepeatState.None
                }
            )
        }
    }

    override fun onPlayerError(error: PlayerError) {
        pushPlayerError(error)
        showToast(ToastType.PlayerError(error.errorCodeName))
        player.pause()
    }

    private fun getDataOfNowPlayingState(mediaItem: GenericMediaItem) {
        val videoId = if (mediaItem.isVideo()) {
            mediaItem.mediaId.removePrefix(MERGING_DATA_TYPE.VIDEO)
        } else {
            mediaItem.mediaId
        }

        val track = queueData.value?.data?.listTracks?.find { it.videoId == videoId }
        _nowPlayingState.update { it.copy(mediaItem = mediaItem, track = track) }
        _format.value = null
        _skipSegments.value = null

        coroutineScope.launch {
            songRepository.getSongById(videoId).cancellable().singleOrNull().let { songEntity ->
                val song: SongEntity = if (songEntity != null) {
                    _controlState.update { it.copy(isLiked = songEntity.liked) }
                    val thumbUrl = Regex("=w\\d+-h\\d+").replace(
                        track?.thumbnails?.lastOrNull()?.url
                            ?: songEntity.thumbnails
                            ?: "http://i.ytimg.com/vi/${songEntity.videoId}/maxresdefault.jpg",
                        "=w544-h544",
                    )
                    if (songEntity.thumbnails != thumbUrl) {
                        songRepository.updateThumbnailsSongEntity(thumbUrl, songEntity.videoId).singleOrNull()
                    }
                    MusicVideoType.normalize(track?.videoType)?.let { freshVideoType ->
                        if (songEntity.videoType != freshVideoType) {
                            songRepository.updateVideoTypeSongEntity(freshVideoType, songEntity.videoId).singleOrNull()
                        }
                    }
                    songRepository.updateSongInLibrary(now(), songEntity.videoId).singleOrNull()
                    songRepository.updateListenCount(songEntity.videoId)
                    songEntity.copy(thumbnails = thumbUrl)
                } else {
                    _controlState.update { it.copy(isLiked = false) }
                    val thumbUrl = Regex("=w\\d+-h\\d+").replace(
                        track?.thumbnails?.lastOrNull()?.url
                            ?: "http://i.ytimg.com/vi/${track?.videoId}/maxresdefault.jpg",
                        "=w544-h544",
                    )
                    val newSong = (track?.toSongEntity() ?: mediaItem.toSongEntity()).copy(thumbnails = thumbUrl)
                    songRepository.insertSong(newSong).singleOrNull()
                    newSong
                }

                _nowPlayingState.update { it.copy(songEntity = song) }
            }

            songEntityJob?.cancel()
            songEntityJob = launch {
                songRepository.getSongAsFlow(videoId).cancellable().filterNotNull().collectLatest { songEntity ->
                    if (dataStoreManager.explicitContentEnabled.first() == FALSE && songEntity.isExplicit) {
                        showToast(ToastType.ExplicitContent)
                        if (player.hasNextMediaItem()) {
                            player.seekToNext()
                        } else if (player.hasPreviousMediaItem()) {
                            player.seekToPrevious()
                        } else {
                            player.stop()
                        }
                        return@collectLatest
                    }
                    _nowPlayingState.update { it.copy(songEntity = songEntity) }
                    _controlState.update { it.copy(isLiked = songEntity.liked) }
                }
            }

            if (dataStoreManager.sponsorBlockEnabled.first() == TRUE) {
                getSkipSegments(videoId)
            }
            if (dataStoreManager.sendBackToGoogle.first() == TRUE) {
                getFormat(videoId)
            }
        }
    }

    private fun getSkipSegments(videoId: String) {
        _skipSegments.value = null
        getSkipSegmentsJob?.cancel()
        getSkipSegmentsJob = coroutineScope.launch {
            streamRepository.getSkipSegments(videoId).collect { response ->
                when (response) {
                    is Resource.Success -> _skipSegments.value = response.data
                    is Resource.Error -> _skipSegments.value = null
                }
            }
        }
    }

    private fun getFormat(mediaId: String?) {
        getFormatJob?.cancel()
        getFormatJob = coroutineScope.launch {
            if (mediaId != null) {
                streamRepository.getFormatFlow(mediaId).cancellable().collectLatest { f ->
                    _format.emit(f)
                }
            }
        }
    }

    private fun updateNotification() {
        coroutineScope.launch {
            var id = player.currentMediaItem?.mediaId ?: ""
            if (id.contains("Video")) {
                id = id.removePrefix("Video")
            }
            val liked = songRepository.getSongById(id).singleOrNull()?.liked ?: false
            _controlState.update { it.copy(isLiked = liked) }
            onUpdateNotification.invoke(
                listOf(
                    GenericCommandButton.Like(liked),
                    GenericCommandButton.Repeat(repeatState = _controlState.value.repeatState),
                    GenericCommandButton.Radio,
                    GenericCommandButton.Shuffle(isShuffled = _controlState.value.isShuffle),
                )
            )
        }
    }

    override fun startProgressUpdate() {
        progressJob?.cancel()
        progressJob = coroutineScope.launch {
            val positionPersistIntervalMs = 5_000L
            val tickIntervalMs = 250L
            var sinceLastPositionSaveMs = 0L

            while (isActive) {
                if (player.isPlaying) {
                    val pos = player.currentPosition
                    _simpleMediaState.value = SimpleMediaState.Progress(pos)
                    updateNowPlayingInfo()

                    sinceLastPositionSaveMs += tickIntervalMs
                    if (sinceLastPositionSaveMs >= positionPersistIntervalMs) {
                        sinceLastPositionSaveMs = 0L
                        mayBeSaveRecentPosition()
                    }
                }
                delay(tickIntervalMs)
            }
        }
    }

    override fun stopProgressUpdate() {
        progressJob?.cancel()
        progressJob = null
    }

    override fun startBufferedUpdate() {}
    override fun stopBufferedUpdate() {}

    override suspend fun onPlayerEvent(playerEvent: PlayerEvent) {
        when (playerEvent) {
            is PlayerEvent.UpdateVolume -> player.volume = playerEvent.newVolume
            PlayerEvent.Backward -> player.seekBack()
            PlayerEvent.Forward -> player.seekForward()
            PlayerEvent.PlayPause -> if (player.isPlaying) player.pause() else player.play()
            PlayerEvent.Next -> player.seekToNext()
            PlayerEvent.Previous -> player.seekToPrevious()
            PlayerEvent.SkipToPrevious -> player.seekToPreviousMediaItem()
            PlayerEvent.Stop -> {
                stopProgressUpdate()
                player.stop()
                _nowPlayingState.value = NowPlayingTrackState.initial()
            }
            is PlayerEvent.UpdateProgress -> {
                val dur = player.duration
                if (dur > 0L) {
                    player.seekTo((dur * playerEvent.newProgress / 100).toLong())
                }
            }
            PlayerEvent.Shuffle -> {
                val nextShuffle = !player.shuffleModeEnabled
                player.shuffleModeEnabled = nextShuffle
                _controlState.update { it.copy(isShuffle = nextShuffle) }
            }
            PlayerEvent.Repeat -> {
                val nextMode = when (player.repeatMode) {
                    PlayerConstants.REPEAT_MODE_OFF -> PlayerConstants.REPEAT_MODE_ALL
                    PlayerConstants.REPEAT_MODE_ALL -> PlayerConstants.REPEAT_MODE_ONE
                    else -> PlayerConstants.REPEAT_MODE_OFF
                }
                player.repeatMode = nextMode
                _controlState.update {
                    it.copy(
                        repeatState = when (nextMode) {
                            PlayerConstants.REPEAT_MODE_ONE -> RepeatState.One
                            PlayerConstants.REPEAT_MODE_ALL -> RepeatState.All
                            else -> RepeatState.None
                        }
                    )
                }
            }
            PlayerEvent.ToggleLike -> toggleLike()
        }
    }

    override fun toggleRadio() {
        coroutineScope.launch {
            val currentSong = nowPlayingState.value.songEntity ?: return@launch
            songRepository.getRadioFromEndpoint(
                YouTubeWatchEndpoint(
                    videoId = currentSong.videoId,
                    playlistId = "RDAMVM${currentSong.videoId}",
                )
            ).collectLatest { res ->
                val data = res.data
                if (res is Resource.Success && data != null && data.first.isNotEmpty()) {
                    setQueueData(
                        QueueData.Data(
                            listTracks = data.first,
                            firstPlayedTrack = data.first.first(),
                            playlistId = "RDAMVM${currentSong.videoId}",
                            playlistName = "\"${currentSong.title}\" Radio",
                            playlistType = PlaylistType.RADIO,
                            continuation = data.second,
                        )
                    )
                    clearMediaItems()
                    addMediaItem(currentSong.toGenericMediaItem(), playWhenReady = true)
                    loadPlaylistOrAlbum(0)
                }
            }
        }
    }

    override fun toggleLike() {
        toggleLikeJob?.cancel()
        toggleLikeJob = coroutineScope.launch {
            var id = player.currentMediaItem?.mediaId ?: ""
            if (id.contains("Video")) {
                id = id.removePrefix("Video")
            }
            songRepository.updateLikeStatus(id, if (!controlState.first().isLiked) 1 else 0)
            delay(200)
        }
    }

    override fun like(liked: Boolean) {
        _controlState.update { it.copy(isLiked = liked) }
    }

    override fun resetSongAndQueue() {
        player.clearMediaItems()
        _queueData.value = QueueData()
        _nowPlaying.value = null
    }

    override fun sleepStart(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalSeconds = minutes * 60
        sleepTimerJob = coroutineScope.launch {
            for (remaining in totalSeconds downTo 0) {
                _sleepTimerState.value = SleepTimerState(isDone = false, timeRemaining = remaining)
                if (remaining <= 30) {
                    player.sleepFadeFactor = remaining / 30f
                }
                delay(1_000L)
            }
            player.pause()
            player.sleepFadeFactor = 1f
            _sleepTimerState.value = SleepTimerState(isDone = true, timeRemaining = 0)
        }
    }

    override fun sleepStop() {
        sleepTimerJob?.cancel()
        player.sleepFadeFactor = 1f
        _sleepTimerState.value = SleepTimerState(isDone = true, timeRemaining = 0)
    }

    override fun removeMediaItem(position: Int) {
        val tracks = queueData.value?.data?.listTracks ?: return
        if (position !in tracks.indices) return
        player.removeMediaItem(position)
        _queueData.update { current ->
            val list = current?.data?.listTracks?.toMutableList() ?: return@update current
            if (position in list.indices) list.removeAt(position)
            current.copy(data = current.data.copy(listTracks = list))
        }
        _currentSongIndex.value = player.currentMediaItemIndex
    }

    override fun addMediaItem(mediaItem: GenericMediaItem, playWhenReady: Boolean) {
        player.clearMediaItems()
        player.setMediaItem(mediaItem)
        player.playWhenReady = playWhenReady
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

    override fun shufflePlaylist(randomTrackIndex: Int) {
        val playlistId = _queueData.value?.data?.playlistId ?: return
        val firstPlayedTrack = _queueData.value?.data?.firstPlayedTrack ?: return
        coroutineScope.launch {
            if (playlistId.startsWith(LOCAL_PLAYLIST_ID)) {
                clearMediaItems()
                addMediaItem(firstPlayedTrack.toGenericMediaItem(), playWhenReady = true)
                val longId = playlistId.replace(LOCAL_PLAYLIST_ID, "").toLongOrNull() ?: return@launch
                val localPlaylist = localPlaylistRepository.getLocalPlaylist(longId).lastOrNull()?.data
                if (localPlaylist != null) {
                    val trackCount = localPlaylist.tracks?.size ?: return@launch
                    val listPosition = (0 until trackCount).toMutableList().apply { remove(randomTrackIndex) }
                    if (listPosition.isEmpty()) return@launch
                    listPosition.shuffle()
                    _queueData.update {
                        it?.copy(
                            data = it.data.copy(
                                continuation = "SHUFFLE0_${fromListIntToString(listPosition)}",
                            )
                        )
                    }
                    loadMore()
                }
            }
        }
    }

    override fun loadMore() {
        if (queueData.value?.queueState == QueueData.StateSource.STATE_INITIALIZING) return
        val playlistId = _queueData.value?.data?.playlistId ?: return
        val continuation = _queueData.value?.data?.continuation

        if (continuation != null) {
            if (playlistId.startsWith(LOCAL_PLAYLIST_ID)) {
                coroutineScope.launch {
                    _queueData.update { it?.copy(queueState = QueueData.StateSource.STATE_INITIALIZING) }
                    val longId = playlistId.replace(LOCAL_PLAYLIST_ID, "").toLongOrNull() ?: return@launch
                    if (continuation.startsWith("SHUFFLE")) {
                        val regex = Regex("(?<=SHUFFLE)\\d+(?=_)")
                        var offset = regex.find(continuation)?.value?.toInt() ?: return@launch
                        val posString = continuation.removePrefix("SHUFFLE${offset}_")
                        val listPosition = fromStringToListInt(posString) ?: return@launch
                        val theLastLoad = 50 * (offset + 1) >= listPosition.size
                        localPlaylistRepository.getPlaylistPairSongByListPosition(
                            longId,
                            listPosition.subList(50 * offset, if (theLastLoad) listPosition.size else 50 * (offset + 1)),
                        ).singleOrNull()?.let { pair ->
                            songRepository.getSongsByListVideoId(pair.map { it.songId }).lastOrNull()?.let { songs ->
                                if (songs.isNotEmpty()) {
                                    delay(300)
                                    loadMoreCatalog(songs.toArrayListTrack())
                                    offset++
                                    _queueData.update {
                                        it?.copy(
                                            data = it.data.copy(
                                                continuation = if (!theLastLoad) "SHUFFLE${offset}_$posString" else null,
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                coroutineScope.launch {
                    _queueData.update { it?.copy(queueState = QueueData.StateSource.STATE_INITIALIZING) }
                    songRepository.getContinueTrack(playlistId, continuation).lastOrNull()?.let { response ->
                        val list = response.first
                        if (list != null) {
                            loadMoreCatalog(list)
                            _queueData.update {
                                it?.copy(data = it.data.copy(continuation = response.second))
                            }
                        } else {
                            _queueData.update { it?.copy(data = it.data.copy(continuation = null)) }
                        }
                    }
                }
            }
        }
    }

    override fun getRelated(videoId: String) {
        if (queueData.value?.queueState == QueueData.StateSource.STATE_INITIALIZING) return
        coroutineScope.launch {
            songRepository.getRelatedData(videoId).collect { response ->
                when (response) {
                    is Resource.Success -> {
                        loadMoreCatalog(response.data?.first?.toCollection(arrayListOf()) ?: arrayListOf())
                        _queueData.update {
                            it?.copy(data = it.data.copy(continuation = response.data?.second))
                        }
                    }
                    is Resource.Error -> {
                        _queueData.update {
                            it?.copy(
                                queueState = QueueData.StateSource.STATE_INITIALIZED,
                                data = it.data.copy(continuation = null),
                            )
                        }
                    }
                }
            }
        }
    }

    override fun setQueueData(queueData: QueueData.Data) {
        _queueData.update { it?.copy(data = queueData) }
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

    override suspend fun load(downloaded: Int, index: Int?) {
        updateCatalog(downloaded, index).let {
            if (index != 0 && index != null) {
                player.moveMediaItem(0, index)
            }
            _queueData.update { it?.copy(queueState = QueueData.StateSource.STATE_INITIALIZED) }
        }
    }

    override suspend fun loadMoreCatalog(listTrack: ArrayList<Track>, isAddToQueue: Boolean) {
        _queueData.update { it?.copy(queueState = QueueData.StateSource.STATE_INITIALIZING) }
        for (track in listTrack) {
            val mediaItem = track.toGenericMediaItem()
            player.addMediaItem(mediaItem)
        }
        _queueData.update {
            it?.copy(queueState = QueueData.StateSource.STATE_INITIALIZED)?.addTrackList(listTrack)
        }
    }

    override suspend fun updateCatalog(downloaded: Int, index: Int?): Boolean {
        _queueData.update { it?.copy(queueState = QueueData.StateSource.STATE_INITIALIZING) }
        val tempQueue = ArrayList<Track>()
        queueData.value?.data?.listTracks?.let { tempQueue.addAll(it) }

        val current = if (index != null) tempQueue.getOrNull(index) else null
        for (track in tempQueue) {
            if (track == current) continue
            player.addMediaItem(track.toGenericMediaItem())
        }
        _queueData.update { it?.copy(queueState = QueueData.StateSource.STATE_INITIALIZED) }
        return true
    }

    override fun addQueueToPlayer() {
        loadJob?.cancel()
        loadJob = coroutineScope.launch { load() }
    }

    override fun loadPlaylistOrAlbum(index: Int?) {
        loadJob?.cancel()
        loadJob = coroutineScope.launch { load(index = index) }
    }

    override fun setCurrentSongIndex(index: Int) {
        _currentSongIndex.value = index
    }

    override suspend fun playNext(track: Track) {
        val nextIndex = player.currentMediaItemIndex + 1
        player.addMediaItem(nextIndex, track.toGenericMediaItem())
        _queueData.update { current ->
            val list = current?.data?.listTracks?.toMutableList() ?: return@update current
            list.add(nextIndex.coerceIn(0, list.size), track)
            current.copy(data = current.data.copy(listTracks = list))
        }
    }

    override suspend fun <T> loadMediaItem(anyTrack: T, type: String, index: Int?) {
        val track = when (anyTrack) {
            is Track -> anyTrack
            is SongEntity -> anyTrack.toTrack()
            else -> return
        }

        if (track.isExplicit && runBlocking { dataStoreManager.explicitContentEnabled.first() } == FALSE) {
            showToast(ToastType.ExplicitContent)
            return
        }

        songRepository.insertSong(track.toSongEntity()).singleOrNull()
        clearMediaItems()

        track.durationSeconds?.let {
            songRepository.updateDurationSeconds(it, track.videoId)
        }

        addMediaItem(track.toGenericMediaItem(), playWhenReady = type != RECOVER_TRACK_QUEUE)

        when (type) {
            SONG_CLICK, VIDEO_CLICK, SHARE -> getRelated(track.videoId)
            PLAYLIST_CLICK, ALBUM_CLICK, RADIO_CLICK -> loadPlaylistOrAlbum(index)
        }
    }

    override fun getPlayerDuration(): Long = player.duration
    override fun getProgress(): Long = player.currentPosition

    private fun mayBeSaveRecentPosition() {
        coroutineScope.launch {
            val videoId = nowPlayingState.value.songEntity?.videoId ?: return@launch
            dataStoreManager.saveRecentSong(videoId, player.contentPosition)
        }
    }

    override fun mayBeNormalizeVolume() {}

    override fun mayBeSaveRecentSong(runBlocking: Boolean) {
        val action = suspend {
            val videoId = nowPlayingState.value.songEntity?.videoId
            if (videoId != null && queueData.value?.queueState == QueueData.StateSource.STATE_INITIALIZED) {
                dataStoreManager.saveRecentSong(videoId, player.contentPosition)
                dataStoreManager.setPlaylistFromSaved(queueData.value?.data?.playlistName ?: "")
                val temp = ArrayList<Track>()
                queueData.value?.data?.listTracks?.let { temp.addAll(it) }
                songRepository.recoverQueue(temp)
            }
        }
        if (runBlocking) {
            runBlocking { action() }
        } else {
            coroutineScope.launch { action() }
        }
    }

    override fun mayBeSavePlaybackState() {
        if (runBlocking { dataStoreManager.saveStateOfPlayback.first() } == TRUE) {
            runBlocking {
                dataStoreManager.recoverShuffleAndRepeatKey(
                    player.shuffleModeEnabled,
                    player.repeatMode,
                )
            }
        }
    }

    override fun mayBeRestoreQueue() {
        coroutineScope.launch { restoreSavedQueue(playWhenReady = false) }
    }

    override suspend fun restoreQueueAndPlay(): Boolean = restoreSavedQueue(playWhenReady = true)

    private suspend fun restoreSavedQueue(playWhenReady: Boolean): Boolean = restoreQueueMutex.withLock {
        if (player.mediaItemCount > 0) {
            if (playWhenReady) player.play()
            return@withLock true
        }

        val currentPlayingTrack = songRepository
            .getSongById(dataStoreManager.recentMediaId.first())
            .lastOrNull()
            ?.toTrack() ?: return@withLock false

        val savedPosition = dataStoreManager.recentPosition.first().toLongOrNull() ?: 0L
        val savedTracks = songRepository
            .getSavedQueue()
            .singleOrNull()
            ?.firstOrNull()
            ?.listTrack
            .orEmpty()

        var index = savedTracks.indexOfFirst { it.videoId == currentPlayingTrack.videoId }
        val listTracks = if (index == -1) {
            index = 0
            (listOf(currentPlayingTrack) + savedTracks).toCollection(arrayListOf())
        } else {
            savedTracks.toCollection(arrayListOf())
        }

        setQueueData(
            QueueData.Data(
                listTracks = listTracks,
                firstPlayedTrack = currentPlayingTrack,
                playlistId = LOCAL_PLAYLIST_ID_SAVED_QUEUE,
                playlistName = dataStoreManager.playlistFromSaved.first(),
                playlistType = PlaylistType.PLAYLIST,
                continuation = null,
            )
        )

        addMediaItem(currentPlayingTrack.toGenericMediaItem(), playWhenReady = playWhenReady)
        loadPlaylistOrAlbum(index = index)
        loadJob?.join()
        player.seekTo(index, savedPosition)
        _simpleMediaState.value = SimpleMediaState.Progress(savedPosition)
        true
    }

    override fun shouldReleaseOnTaskRemoved(): Boolean =
        runBlocking { dataStoreManager.killServiceOnExit.first() == TRUE }

    override fun release() {
        player.removeListener(this)
        progressJob?.cancel()
        sleepTimerJob?.cancel()
        toggleLikeJob?.cancel()
        loadJob?.cancel()
        songEntityJob?.cancel()
        getSkipSegmentsJob?.cancel()
        getFormatJob?.cancel()
        player.release()
        coroutineScope.cancel()
        backgroundScope.cancel()
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
