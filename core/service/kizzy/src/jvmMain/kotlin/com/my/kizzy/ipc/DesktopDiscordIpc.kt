package com.my.kizzy.ipc

import com.mika.domain.data.entities.SongEntity
import com.mika.domain.utils.MusicVideoType
import com.my.kizzy.DiscordRPC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonObject
import java.io.File
import java.io.RandomAccessFile
import java.net.StandardProtocolFamily
import java.net.UnixDomainSocketAddress
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.ByteChannel
import java.nio.channels.SocketChannel
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Pure Kotlin Discord Local IPC Client for Desktop (Windows, macOS, Linux).
 * Connects directly to the running Discord client via local named pipe or unix domain socket.
 * Requires ZERO user login or token!
 */
class DesktopDiscordIpc(
    private val applicationId: String = DiscordRPC.APPLICATION_ID,
) {
    // Keep a list of all active Discord channels (Standard, PTB, Canary, Development)
    private val channels = mutableListOf<ByteChannel>()
    private val processId = ProcessHandle.current().pid()

    @Synchronized
    fun isRpcRunning(): Boolean = channels.any { it.isOpen }

    @Synchronized
    fun clearActivity() {
        if (channels.isNotEmpty()) {
            val clearFrame = buildJsonObject {
                put("cmd", "SET_ACTIVITY")
                putJsonObject("args") {
                    put("pid", processId)
                    put("activity", kotlinx.serialization.json.JsonNull)
                }
                put("nonce", Clock.System.now().toEpochMilliseconds().toString())
            }.toString()

            sendFrame(clearFrame)
        }
    }

    @Synchronized
    fun closeRPC() {
        clearActivity()

        channels.forEach { ch ->
            try { ch.close() } catch (_: Exception) {}
        }
        channels.clear()
    }

    private fun connect(): Boolean {
        // Remove closed channels
        channels.removeAll { !it.isOpen }
        if (channels.isNotEmpty()) return true

        val os = System.getProperty("os.name")?.lowercase().orEmpty()
        // Discord pipe name variants: Stable, PTB, Canary, Development
        val windowsPipeVariants = listOf("discord", "discord-ptb", "discord-canary", "discord-development")
        for (i in 0..9) {
            try {
                if (os.contains("win")) {
                    for (variant in windowsPipeVariants) {
                        val pipeFile = File("\\\\.\\pipe\\$variant-ipc-$i")
                        val raf = try {
                            RandomAccessFile(pipeFile, "rw")
                        } catch (_: Exception) {
                            null
                        } ?: continue
                        val ch = raf.channel
                        if (performHandshake(ch)) {
                            channels.add(ch)
                        } else {
                            try { ch.close() } catch (_: Exception) {}
                        }
                    }
                } else {
                    val tempDir = System.getenv("XDG_RUNTIME_DIR")
                        ?: System.getenv("TMPDIR")
                        ?: System.getenv("TMP")
                        ?: System.getenv("TEMP")
                        ?: "/tmp"
                    // Try all Discord socket variants on Unix: Stable, PTB, Canary, Development
                    val unixSocketVariants = listOf("discord", "discord-ptb", "discord-canary", "discord-development")
                    for (variant in unixSocketVariants) {
                        val socketPath = "$tempDir/$variant-ipc-$i"
                        val socketFile = File(socketPath)
                        if (socketFile.exists()) {
                            val address = UnixDomainSocketAddress.of(socketPath)
                            val socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX)
                            try {
                                socketChannel.connect(address)
                                if (performHandshake(socketChannel)) {
                                    channels.add(socketChannel)
                                } else {
                                    try { socketChannel.close() } catch (_: Exception) {}
                                }
                            } catch (_: Exception) {
                                try { socketChannel.close() } catch (_: Exception) {}
                            }
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        return channels.isNotEmpty()
    }

    private fun performHandshake(ch: ByteChannel): Boolean {
        val payload = buildJsonObject {
            put("v", 1)
            put("client_id", applicationId)
        }.toString()

        sendPacket(ch, OP_HANDSHAKE, payload)
        val response = readPacket(ch) ?: return false
        return response.contains("DISPATCH") || response.contains("READY")
    }

    private fun sendFrame(json: String) {
        val iterator = channels.iterator()
        while (iterator.hasNext()) {
            val ch = iterator.next()
            try {
                if (ch.isOpen) {
                    sendPacket(ch, OP_FRAME, json)
                } else {
                    iterator.remove()
                }
            } catch (_: Exception) {
                try { ch.close() } catch (_: Exception) {}
                iterator.remove()
            }
        }
    }

    private fun sendPacket(ch: ByteChannel, op: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(8 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(op)
        buffer.putInt(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        while (buffer.hasRemaining()) {
            ch.write(buffer)
        }
    }

    private fun readPacket(ch: ByteChannel): String? {
        val header = ByteBuffer.allocate(8).order(ByteOrder.LITTLE_ENDIAN)
        while (header.hasRemaining()) {
            if (ch.read(header) < 0) return null
        }
        header.flip()
        val op = header.getInt()
        val length = header.getInt()
        if (length <= 0 || length > 65536) return null

        val body = ByteBuffer.allocate(length)
        while (body.hasRemaining()) {
            if (ch.read(body) < 0) return null
        }
        return String(body.array(), Charsets.UTF_8)
    }

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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!connect()) return@runCatching

            val currentTime = Clock.System.now().toEpochMilliseconds()
            val effectiveDuration = if (durationMillis > 0L) durationMillis else (song.durationSeconds * 1000L)
            val adjustedPlaybackTime = if (currentPlaybackTimeMillis <= 3000L) 0L else (currentPlaybackTimeMillis / playbackSpeed).toLong()
            val calculatedStartTime = currentTime - adjustedPlaybackTime

            val remainingDuration = (effectiveDuration - currentPlaybackTimeMillis).coerceAtLeast(0L)
            val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

            val songTitle = song.title ?: "Unknown Track"
            val rawArtist = song.artistName?.filter { it.isNotBlank() }?.joinToString(", ") ?: "Unknown Artist"

            // State: artist line — with room or paused indicator
            val stateText = when {
                !roomCode.isNullOrBlank() -> "$rawArtist • 🎧 Room $roomCode ($roomMemberCount)"
                isPaused -> "⏸ Paused • $rawArtist"
                else -> rawArtist
            }

            // small_text tooltip: surface the most useful playback flag
            val tooltipText = when {
                isRepeatOne -> "D-Spot • 🔂 Repeat"
                isShuffle -> "D-Spot • 🔀 Shuffle"
                !roomCode.isNullOrBlank() -> "D-Spot • Listen Together"
                else -> "D-Spot"
            }

            val albumDisplay = song.albumName?.takeIf { it.length >= 2 } ?: "D-Spot Audio"
            val safeTitle = if (songTitle.length < 2) "$songTitle " else songTitle
            val safeState = if (stateText.length < 2) "$stateText " else stateText
            val thumbUrl = if (!song.thumbnails.isNullOrBlank()) song.thumbnails else DiscordRPC.APP_ICON
            val trackUrl = if (!song.videoId.isNullOrBlank()) {
                "https://dspot.app/app/watch?v=${song.videoId}"
            } else {
                "https://dspot-web.vercel.app"
            }

            // Activity type: 2 = Listening (audio / podcast), 3 = Watching (music video)
            val activityType = if (MusicVideoType.isVideoSong(song.videoType)) 3 else 2

            val frameJson = buildJsonObject {
                put("cmd", "SET_ACTIVITY")
                putJsonObject("args") {
                    put("pid", processId)
                    putJsonObject("activity") {
                        put("type", activityType)
                        put("name", safeTitle)
                        put("details", safeTitle)
                        put("state", safeState)
                        // Omit timestamps when paused — no ticking progress bar on a static card
                        if (!isPaused) {
                            putJsonObject("timestamps") {
                                put("start", calculatedStartTime)
                                if (effectiveDuration > 0) {
                                    put("end", currentTime + adjustedRemainingDuration)
                                }
                            }
                        }
                        putJsonObject("assets") {
                            put("large_image", thumbUrl)
                            put("large_text", albumDisplay)
                            put("small_image", DiscordRPC.APP_ICON)
                            put("small_text", tooltipText)
                        }
                        if (!roomCode.isNullOrBlank()) {
                            putJsonObject("party") {
                                put("id", "dspot-room-$roomCode")
                                put("size", buildJsonArray {
                                    add(roomMemberCount.coerceAtLeast(1))
                                    add(10)
                                })
                            }
                        }
                        put("buttons", buildJsonArray {
                            // Button 1: direct track link
                            addJsonObject {
                                put("label", "▶ Play on D-Spot")
                                put("url", trackUrl)
                            }
                            // Button 2: listen together room link, or download link as fallback
                            if (!roomCode.isNullOrBlank()) {
                                addJsonObject {
                                    put("label", "Listen Together 🎧")
                                    put("url", "https://dspot-web.vercel.app/room/$roomCode")
                                }
                            } else {
                                addJsonObject {
                                    put("label", "Get D-Spot")
                                    put("url", "https://dspot-web.vercel.app/download")
                                }
                            }
                        })
                    }
                }
                put("nonce", currentTime.toString())
            }.toString()

            sendFrame(frameJson)
        }
    }

    @OptIn(ExperimentalTime::class)
    suspend fun setBrowsing(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!connect()) return@runCatching

            val currentTime = Clock.System.now().toEpochMilliseconds()
            val frameJson = buildJsonObject {
                put("cmd", "SET_ACTIVITY")
                putJsonObject("args") {
                    put("pid", processId)
                    putJsonObject("activity") {
                        put("type", 0) // Playing / Browsing
                        put("name", "D-Spot")
                        put("details", "Exploring Music")
                        put("state", "Browsing D-Spot")
                        putJsonObject("assets") {
                            put("large_image", DiscordRPC.APP_ICON)
                            put("large_text", "D-Spot Music")
                        }
                        put("buttons", buildJsonArray {
                            addJsonObject {
                                put("label", "Get D-Spot")
                                put("url", "https://dspot-web.vercel.app/download")
                            }
                        })
                    }
                }
                put("nonce", currentTime.toString())
            }.toString()

            sendFrame(frameJson)
        }
    }

    companion object {
        private const val OP_HANDSHAKE = 0
        private const val OP_FRAME = 1
        private const val OP_CLOSE = 2
    }
}
