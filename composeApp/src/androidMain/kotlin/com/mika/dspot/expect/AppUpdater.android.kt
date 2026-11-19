package com.mika.dspot.expect

import android.content.Context
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.mika.logger.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.mp.KoinPlatform.getKoin
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

actual fun startInAppUpdate(
    downloadUrl: String,
    fileName: String,
    onProgress: (UpdateDownloadState) -> Unit,
) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            onProgress(UpdateDownloadState.Downloading(0f, 0L, 0L))

            val context: Context = getKoin().get()
            val updateDir = File(context.cacheDir, "updates")
            if (!updateDir.exists()) {
                updateDir.mkdirs()
            }
            val targetFile = File(updateDir, fileName)
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
                    connectTimeout = 30000
                    readTimeout = 60000
                    setRequestProperty("User-Agent", "D-Spot-Android-Updater")
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

            Logger.d("AppUpdater", "Android APK downloaded successfully to: ${targetFile.absolutePath}")
            onProgress(UpdateDownloadState.ReadyToInstall(targetFile.absolutePath))
        } catch (e: Exception) {
            Logger.e("AppUpdater", "Failed to download update on Android: ${e.message}")
            onProgress(UpdateDownloadState.Error(e.message ?: "Failed to download update"))
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
