package com.my.kizzy

import com.mika.domain.data.entities.SongEntity
import com.my.kizzy.rpc.KizzyRPC
import com.my.kizzy.rpc.RpcImage
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DiscordRPC(
    token: String,
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

        setActivity(
            name = APP_NAME,
            details = song.title,
            state = song.artistName?.joinToString(", "),
            largeImage = song.thumbnails?.let { RpcImage.ExternalImage(it) },
            smallImage = RpcImage.ExternalImage(APP_ICON),
            largeText = song.albumName,
            smallText = song.artistName?.firstOrNull(),
            buttons =
                listOf(
                    "Listen on D-Spot" to "https://dspot.app/app/watch?v=${song.videoId}",
                    "Visit D-Spot" to "https://github.com/Mika/D-Spot",
                ),
            type = Type.LISTENING,
            since = currentTime,
            startTime = calculatedStartTime,
            endTime = currentTime + adjustedRemainingDuration,
            applicationId = APPLICATION_ID,
        )
    }

    companion object {
        private const val APPLICATION_ID = "1557223074083110933"
        private const val APP_NAME: String = "D-Spot"
        private const val APP_ICON: String =
            "https://syd.cloud.appwrite.io/v1/storage/buckets/6ac646100001f9f95c29/files/6ad30ebc0030079878c9/view?project=6ac645f9001be50b8303&impersonateuserid"
    }
}