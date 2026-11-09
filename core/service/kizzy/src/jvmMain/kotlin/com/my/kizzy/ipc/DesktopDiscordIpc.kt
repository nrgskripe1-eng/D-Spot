package com.my.kizzy.ipc

import com.mika.domain.data.entities.SongEntity
import com.my.kizzy.DiscordRPC
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
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
    fun closeRPC() {
        if (channels.isNotEmpty()) {
            val clearFrame = buildJsonObject {
                put("cmd", "SET_ACTIVITY")
                putJsonObject("args") {
                    put("pid", processId)
                }
                put("nonce", Clock.System.now().toEpochMilliseconds().toString())
            }.toString()

            sendFrame(clearFrame)
        }

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
        for (i in 0..9) {
            try {
                if (os.contains("win")) {
                    val pipeFile = File("\\\\.\\pipe\\discord-ipc-$i")
                    if (pipeFile.exists()) {
                        val raf = RandomAccessFile(pipeFile, "rw")
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
                    val socketPath = "$tempDir/discord-ipc-$i"
                    val socketFile = File(socketPath)
                    if (socketFile.exists()) {
                        val address = UnixDomainSocketAddress.of(socketPath)
                        val socketChannel = SocketChannel.open(StandardProtocolFamily.UNIX)
                        socketChannel.connect(address)
                        if (performHandshake(socketChannel)) {
                            channels.add(socketChannel)
                        } else {
                            try { socketChannel.close() } catch (_: Exception) {}
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
    ): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            if (!connect()) return@runCatching

            val currentTime = Clock.System.now().toEpochMilliseconds()
            val adjustedPlaybackTime = (currentPlaybackTimeMillis / playbackSpeed).toLong()
            val calculatedStartTime = currentTime - adjustedPlaybackTime

            val remainingDuration = durationMillis - currentPlaybackTimeMillis
            val adjustedRemainingDuration = (remainingDuration / playbackSpeed).toLong()

            val songTitle = song.title ?: "Unknown Track"
            val artistDisplay = song.artistName?.filter { it.isNotBlank() }?.joinToString(", ") ?: "Unknown Artist"
            val albumDisplay = song.albumName ?: "D-Spot Audio"
            val thumbUrl = song.thumbnails ?: DiscordRPC.APP_ICON

            val frameJson = buildJsonObject {
                put("cmd", "SET_ACTIVITY")
                putJsonObject("args") {
                    put("pid", processId)
                    putJsonObject("activity") {
                        put("type", 2)
                        put("name", songTitle)
                        put("details", songTitle)
                        put("state", artistDisplay)
                        putJsonObject("timestamps") {
                            put("start", calculatedStartTime)
                            if (durationMillis > 0) {
                                put("end", currentTime + adjustedRemainingDuration)
                            }
                        }
                        putJsonObject("assets") {
                            put("large_image", thumbUrl)
                            put("large_text", albumDisplay)
                            put("small_image", DiscordRPC.APP_ICON)
                            put("small_text", "D-Spot")
                        }
                        put("buttons", buildJsonArray {
                            addJsonObject {
                                put("label", "Play Track")
                                put("url", "https://dspot-web.vercel.app")
                            }
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

