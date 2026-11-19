package com.my.kizzy

import com.mika.domain.data.entities.SongEntity
import com.mika.domain.utils.MusicVideoType
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DiscordRPC(
    token: String = "",
) : KizzyRPC(token) {
    @OptIn(ExperimentalTime::class)
    suspend fun updateSong(
        currentPlaybackTimeMillis: Long,
        durationMillis: Long,
        playbackSpeed: Float = 1.0f,
        song: SongEntity,
        roomCode: String? = null,
        roomMemberCount: Int = 1,
        isRepeatOne: Boolean = false,
        isShuffle: Boolean = false,
        isPaused: Boolean = false,
    ) = runCatching {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val remainingDuration = durationMillis - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        val songTitle = song.title ?: "Unknown Track"
        val rawArtist = song.artistName?.filter { it.isNotBlank() }?.joinToString(", ") ?: "Unknown Artist"

        // State: artist line — with room or paused indicator
        val stateText = when {
            !roomCode.isNullOrBlank() -> "$rawArtist • 🎧 Room $roomCode ($roomMemberCount)"
            isPaused -> "⏸ Paused • $rawArtist"
            else -> rawArtist
        }

        val albumDisplay = song.albumName ?: "D-Spot Audio"

        // small_text tooltip: surface the most useful playback flag
        val tooltipText = when {
            isRepeatOne -> "D-Spot • 🔂 Repeat"
            isShuffle -> "D-Spot • 🔀 Shuffle"
            !roomCode.isNullOrBlank() -> "D-Spot • Listen Together"
            else -> "D-Spot"
        }

        val trackUrl = if (!song.videoId.isNullOrBlank()) {
            "https://dspot.app/app/watch?v=${song.videoId}"
        } else {
            "https://dspot-web.vercel.app"
        }

        // Activity type: Watching for music videos, Listening for audio/podcasts
        val activityType = if (MusicVideoType.isVideoSong(song.videoType)) Type.WATCHING else Type.LISTENING

        setActivity(
            name = songTitle,
            details = songTitle,
            state = stateText,
            largeImage = song.thumbnails?.let { RpcImage.ExternalImage(it) },
            smallImage = RpcImage.ExternalImage(APP_ICON),
            largeText = albumDisplay,
            smallText = tooltipText,
            buttons = buildList {
                // Button 1: direct track link
                add("▶ Play on D-Spot" to trackUrl)
                // Button 2: listen together room link, or download link as fallback
                if (!roomCode.isNullOrBlank()) {
                    add("Listen Together 🎧" to "https://dspot-web.vercel.app/room/$roomCode")
                } else {
                    add("Get D-Spot" to "https://dspot-web.vercel.app/download")
                }
            },
            type = activityType,
            since = currentTime,
            // Omit timestamps when paused — no ticking bar on a static card
            startTime = if (isPaused) null else calculatedStartTime,
            endTime = if (isPaused) null else currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID,
        )
    }

    companion object {
        const val APPLICATION_ID = "1565241843439108106"
        const val APP_NAME: String = "D-Spot"
        const val APP_ICON: String =
            "https://raw.githubusercontent.com/nrgskripe1-eng/D-Spot/account-tab/composeApp/icon/circle_app_icon.png"
    }
}
