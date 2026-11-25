package com.mika.dspot.expect

import com.mika.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.exitProcess

actual fun startInAppUpdate(
    downloadUrl: String,
    fileName: String,
    onProgress: (UpdateDownloadState) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            onProgress(UpdateDownloadState.Downloading(0f, 0L, 0L))

            // Save directly to user's standard Downloads folder
            val userHome = System.getProperty("user.home")
            val downloadsDir = if (userHome != null) {
                File(userHome, "Downloads")
            } else {
                File(System.getProperty("java.io.tmpdir"), "dspot-update")
            }
            if (!downloadsDir.exists()) {
                downloadsDir.mkdirs()
            }
            val targetFile = File(downloadsDir, fileName)
            if (targetFile.exists()) {
                targetFile.delete()
            }

            var currentUrl = downloadUrl
            var connection: HttpURLConnection? = null
            var redirectCount = 0

            // Manually resolve HTTP -> HTTPS redirects (like Vercel 302 -> GitHub Release)
            while (redirectCount < 10) {
                val urlObj = URL(currentUrl)
                connection = (urlObj.openConnection() as HttpURLConnection).apply {
                    instanceFollowRedirects = true
                    connectTimeout = 60000
                    readTimeout = 0 // 0 means no timeout while downloading large files
                    setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                }
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM ||
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP ||
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 ||
                    responseCode == 308
                ) {
                    val location = connection.getHeaderField("Location")
                    if (!location.isNullOrEmpty()) {
                        currentUrl = location
                        connection.disconnect()
                        redirectCount++
                        continue
                    }
                }
                break
            }

            val conn = connection ?: throw IllegalStateException("Could not establish connection to $downloadUrl")
            val contentLength = conn.contentLengthLong

            conn.inputStream.use { input ->
                FileOutputStream(targetFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalDownloaded = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalDownloaded += bytesRead

                        val progressFraction = if (contentLength > 0L) {
                            (totalDownloaded.toFloat() / contentLength.toFloat()).coerceIn(0f, 1f)
                        } else {
                            -1f
                        }
                        onProgress(UpdateDownloadState.Downloading(progressFraction, totalDownloaded, contentLength))
                    }
                    output.flush()
                }
            }
            conn.disconnect()

            Logger.d("AppUpdater", "Desktop update downloaded successfully to: ${targetFile.absolutePath}")
            onProgress(UpdateDownloadState.ReadyToInstall(targetFile.absolutePath))
        } catch (e: Exception) {
            Logger.e("AppUpdater", "Failed to download update on Desktop: ${e.message}")
            onProgress(UpdateDownloadState.Error(e.message ?: "Failed to download update"))
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
                // Silent MSI install on Windows (no setup wizard)
                if (file.name.endsWith(".msi", ignoreCase = true)) {
                    ProcessBuilder("msiexec", "/i", file.absolutePath, "/qn", "/norestart").start()
                } else {
                    ProcessBuilder(file.absolutePath).start()
                }
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
