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
    private var channel: ByteChannel? = null
    private var isConnected = false
    private val processId = ProcessHandle.current().pid()

    @Synchronized
    fun isRpcRunning(): Boolean = isConnected && channel?.isOpen == true

    @Synchronized
    fun closeRPC() {
        try {
            if (isConnected) {
                // Send clear activity
                sendFrame(
                    buildJsonObject {
                        put("cmd", "SET_ACTIVITY")
                        putJsonObject("args") {
                            put("pid", processId)
                        }
                        put("nonce", Clock.System.now().toEpochMilliseconds().toString())
                    }.toString()
                )
            }
        } catch (_: Exception) {}
        try {
            channel?.close()
        } catch (_: Exception) {}
        channel = null
        isConnected = false
    }

    private fun connect(): Boolean {
        if (isRpcRunning()) return true

        val os = System.getProperty("os.name")?.lowercase().orEmpty()
        for (i in 0..9) {
            try {
                if (os.contains("win")) {
                    val pipeFile = File("\\\\.\\pipe\\discord-ipc-$i")
                    if (pipeFile.exists()) {
                        val raf = RandomAccessFile(pipeFile, "rw")
                        channel = raf.channel
                        if (performHandshake()) {
                            isConnected = true
                            return true
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
                        channel = socketChannel
                        if (performHandshake()) {
                            isConnected = true
                            return true
                        }
                    }
                }
            } catch (_: Exception) {
                try { channel?.close() } catch (_: Exception) {}
                channel = null
            }
        }
        return false
    }

    private fun performHandshake(): Boolean {
        val payload = buildJsonObject {
            put("v", 1)
            put("client_id", applicationId)
        }.toString()

        sendPacket(OP_HANDSHAKE, payload)
        val response = readPacket() ?: return false
        return response.contains("DISPATCH") || response.contains("READY")
    }

    private fun sendFrame(json: String) {
        sendPacket(OP_FRAME, json)
    }

    private fun sendPacket(op: Int, json: String) {
        val bytes = json.toByteArray(Charsets.UTF_8)
        val buffer = ByteBuffer.allocate(8 + bytes.size).order(ByteOrder.LITTLE_ENDIAN)
        buffer.putInt(op)
        buffer.putInt(bytes.size)
        buffer.put(bytes)
        buffer.flip()
        val ch = channel ?: return
        while (buffer.hasRemaining()) {
            ch.write(buffer)
        }
    }

    private fun readPacket(): String? {
        val ch = channel ?: return null
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
                        put("type", 2) // LISTENING (Displays as 🎵 Title)
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

