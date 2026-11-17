package com.mika.dspot.expect

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
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
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

actual fun startInAppUpdate(
    downloadUrl: String,
    fileName: String,
    onProgress: (UpdateDownloadState) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        var client: HttpClient? = null
        try {
            val context: Context = getKoin().get()
            client = HttpClient(CIO)
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }
            val targetFile = File(updateDir, fileName)
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

            Logger.d("AppUpdater", "Android APK downloaded successfully to: ${targetFile.absolutePath}")
            onProgress(UpdateDownloadState.ReadyToInstall(targetFile.absolutePath))
        } catch (e: Exception) {
            Logger.e("AppUpdater", "Failed to download update on Android: ${e.message}")
            onProgress(UpdateDownloadState.Error(e.message ?: "Failed to download update"))
        } finally {
            client?.close()
        }
    }
}

actual fun installDownloadedUpdate(filePath: String) {
    try {
        val context: AppCompatActivity = getKoin().get()
        val file = File(filePath)
        if (!file.exists()) {
            Logger.e("AppUpdater", "APK file not found: $filePath")
            return
        }

        val authority = "${context.packageName}.FileProvider"
        val apkUri = FileProvider.getUriForFile(context, authority, file)

        val installIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION
        }
        context.startActivity(installIntent)
    } catch (e: Exception) {
        Logger.e("AppUpdater", "Failed to trigger Android install intent: ${e.message}")
    }
}
