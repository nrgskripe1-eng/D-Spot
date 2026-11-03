package com.mika.dspot.expect.ui

import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import com.mika.logger.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.withContext
import org.jetbrains.skia.Image
import platform.Foundation.NSData
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSSearchPathForDirectoriesInDomains
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask
import platform.Foundation.dataWithBytes
import platform.Foundation.writeToURL

private const val TAG = "ImageIo"

actual fun decodeImageBitmap(bytes: ByteArray): ImageBitmap? =
    runCatching {
        Image.makeFromEncoded(bytes).toComposeImageBitmap()
    }.onFailure { Logger.w(TAG, "Could not decode image: ${it.message}") }.getOrNull()

@OptIn(ExperimentalForeignApi::class)
actual suspend fun persistPickedImage(
    bytes: ByteArray,
    fileName: String,
): String? = withContext(Dispatchers.IO) {
    runCatching {
        val paths = NSSearchPathForDirectoriesInDomains(NSDocumentDirectory, NSUserDomainMask, true)
        val documentsPath = paths.firstOrNull() as? String ?: return@runCatching null
        val targetDir = "$documentsPath/playlist_covers"
        NSFileManager.defaultManager.createDirectoryAtPath(targetDir, withIntermediateDirectories = true, attributes = null, error = null)
        val fileUrl = NSURL.fileURLWithPath("$targetDir/$fileName")
        bytes.usePinned { pinned ->
            val nsData = NSData.dataWithBytes(pinned.addressOf(0), bytes.size.toULong())
            nsData.writeToURL(fileUrl, true)
        }
        fileUrl.absoluteString
    }.onFailure { Logger.w(TAG, "Could not persist image: ${it.message}") }.getOrNull()
}

