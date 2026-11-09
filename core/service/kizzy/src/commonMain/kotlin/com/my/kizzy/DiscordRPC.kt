package com.my.kizzy

import com.mika.domain.data.entities.SongEntity
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
    ) = runCatching {
        val currentTime = Clock.System.now().toEpochMilliseconds()

        val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
        val calculatedStartTime = currentTime - adjustedPlaybackTime

        val remainingDuration = durationMillis - currentPlaybackTimeMillis
        val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

        val songTitle = song.title ?: "Unknown Track"
        val artistDisplay = song.artistName?.filter { it.isNotBlank() }?.joinToString(", ") ?: "Unknown Artist"
        val albumDisplay = song.albumName ?: "D-Spot Audio"

        setActivity(
            name = songTitle,
            details = songTitle,
            state = artistDisplay,
            largeImage = song.thumbnails?.let { RpcImage.ExternalImage(it) },
            smallImage = RpcImage.ExternalImage(APP_ICON),
            largeText = albumDisplay,
            smallText = "D-Spot",
            buttons =
                listOf(
                    "Play Track" to "https://dspot-web.vercel.app",
                    "Get D-Spot" to "https://dspot-web.vercel.app/download",
                ),
            type = Type.LISTENING,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
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
