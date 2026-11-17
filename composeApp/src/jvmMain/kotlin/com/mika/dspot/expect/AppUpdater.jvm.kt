package com.mika.dspot.expect

import com.mika.logger.Logger
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.onDownload
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsChannel
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.readRemaining
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.io.readByteArray
import java.io.File
import kotlin.system.exitProcess

actual fun startInAppUpdate(
    downloadUrl: String,
    fileName: String,
    onProgress: (UpdateDownloadState) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        var client: HttpClient? = null
        try {
            client = HttpClient(CIO)
            val tempDir = File(System.getProperty("java.io.tmpdir"), "dspot-update")
            if (!tempDir.exists()) {
                tempDir.mkdirs()
            }
            val targetFile = File(tempDir, fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            var totalBytesReported = -1L
            var currentBytes = 0L

            val response = client.get(downloadUrl) {
                onDownload { bytesSentTotal, contentLength ->
                    if (contentLength != null && contentLength > 0) {
                        totalBytesReported = contentLength
                        currentBytes = bytesSentTotal
                        val progressFraction = (bytesSentTotal.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        onProgress(UpdateDownloadState.Downloading(progressFraction, bytesSentTotal, contentLength))
                    }
                }
            }

            val channel: ByteReadChannel = response.bodyAsChannel()
            targetFile.outputStream().buffered().use { output ->
                while (!channel.isClosedForRead) {
                    val packet = channel.readRemaining(8192L)
                    while (!packet.exhausted()) {
                        val bytes = packet.readByteArray()
                        output.write(bytes)
                        if (totalBytesReported <= 0) {
                            currentBytes += bytes.size
                            onProgress(UpdateDownloadState.Downloading(-1f, currentBytes, -1L))
                        }
                    }
                }
            }

            Logger.d("AppUpdater", "Desktop update downloaded successfully to: ${targetFile.absolutePath}")
            onProgress(UpdateDownloadState.ReadyToInstall(targetFile.absolutePath))
        } catch (e: Exception) {
            Logger.e("AppUpdater", "Failed to download update on Desktop: ${e.message}")
            onProgress(UpdateDownloadState.Error(e.message ?: "Failed to download update"))
        } finally {
            client?.close()
        }
    }
}

actual fun installDownloadedUpdate(filePath: String) {
    try {
        val file = File(filePath)
        if (!file.exists()) {
            Logger.e("AppUpdater", "Installer file not found: $filePath")
            return
        }

        val os = System.getProperty("os.name")?.lowercase().orEmpty()
        when {
            os.contains("windows") -> {
                // Windows executable / installer
                ProcessBuilder(file.absolutePath).start()
                exitProcess(0)
            }
            os.contains("mac") -> {
                // macOS DMG or PKG
                ProcessBuilder("open", file.absolutePath).start()
                exitProcess(0)
            }
            else -> {
                // Linux AppImage / deb / etc.
                file.setExecutable(true)
                ProcessBuilder(file.absolutePath).start()
                exitProcess(0)
            }
        }
    } catch (e: Exception) {
        Logger.e("AppUpdater", "Failed to launch installer: ${e.message}")
    }
}
