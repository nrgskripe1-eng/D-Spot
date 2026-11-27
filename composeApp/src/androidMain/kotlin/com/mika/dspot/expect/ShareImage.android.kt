package com.mika.dspot.expect

import android.content.ContentValues
import android.content.Intent
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.mika.logger.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.koin.mp.KoinPlatform.getKoin
import java.io.File

private const val TAG = "ShareImage"
private const val MIME_PNG = "image/png"
private const val ALBUM = "D-Spot"

actual suspend fun saveImageToDevice(
    bytes: ByteArray,
    fileName: String,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: AppCompatActivity = getKoin().get()
            val resolver = context.contentResolver
            val isScopedStorage = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q

            val values =
                ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, MIME_PNG)
                    if (isScopedStorage) {
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$ALBUM")
                        // Marks the row incomplete so no gallery app indexes a half-written file.
                        // Cleared below once the bytes are on disk.
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                }

            val uri =
                resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: return@runCatching false

            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: return@runCatching false

            if (isScopedStorage) {
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            true
        }.getOrElse { error ->
            // Below API 29 this insert needs WRITE_EXTERNAL_STORAGE to have been granted at
            // runtime, and nothing in the app asks for it — so a save on Android 8 or 9 lands
            // here rather than succeeding. Reported to the caller, never swallowed.
            Logger.e(TAG, "Could not save $fileName to the gallery: ${error.message}")
            false
        }
    }

actual suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
    mimeType: String,
    contentUrl: String?,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: AppCompatActivity = getKoin().get()

            // cacheDir, which provider_paths.xml already exposes as `cache`. The gallery copy is a
            // separate, explicit action — sharing must not silently write to the user's photos.
            val dir = File(context.cacheDir, "shared_images").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(bytes)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
            val pm = context.packageManager

            // Grant read URI permission to key story platforms
            listOf("com.facebook.katana", "com.facebook.orca", "com.instagram.android").forEach { pkg ->
                try {
                    context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Throwable) {
                }
            }

            // Target 1: Facebook Stories Intent
            val fbStoryIntent = Intent("com.facebook.stories.ADD_TO_STORY").apply {
                setDataAndType(uri, mimeType)
                putExtra("interactive_asset_uri", uri)
                if (!contentUrl.isNullOrBlank()) {
                    putExtra("content_url", contentUrl)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            }

            // Target 2: Instagram Stories Intent
            val igStoryIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, mimeType)
                putExtra("interactive_asset_uri", uri)
                if (!contentUrl.isNullOrBlank()) {
                    putExtra("content_url", contentUrl)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            }

            // Base Send Intent for general chooser (WhatsApp, Telegram, etc.)
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_STREAM, uri)
                if (!contentUrl.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, contentUrl)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)

                val initialIntents = mutableListOf<Intent>()
                if (fbStoryIntent.resolveActivity(pm) != null) {
                    initialIntents.add(fbStoryIntent)
                }
                if (igStoryIntent.resolveActivity(pm) != null) {
                    initialIntents.add(igStoryIntent)
                }
                if (initialIntents.isNotEmpty()) {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents.toTypedArray())
                }
            }

            context.startActivity(chooser)
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share $fileName: ${error.message}")
            false
        }
    }

actual suspend fun shareVideo(
    videoBytes: ByteArray,
    fileName: String,
    chooserTitle: String,
    contentUrl: String?,
): Boolean =
    withContext(Dispatchers.IO) {
        runCatching {
            val context: AppCompatActivity = getKoin().get()
            val dir = File(context.cacheDir, "shared_videos").apply { mkdirs() }
            val file = File(dir, fileName)
            file.writeBytes(videoBytes)

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.FileProvider", file)
            val pm = context.packageManager

            listOf("com.facebook.katana", "com.facebook.orca", "com.instagram.android").forEach { pkg ->
                try {
                    context.grantUriPermission(pkg, uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                } catch (_: Throwable) {
                }
            }

            // Facebook Stories for video
            val fbStoryIntent = Intent("com.facebook.stories.ADD_TO_STORY").apply {
                setDataAndType(uri, "video/mp4")
                putExtra("interactive_asset_uri", uri)
                if (!contentUrl.isNullOrBlank()) {
                    putExtra("content_url", contentUrl)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            }

            // Instagram Stories for video
            val igStoryIntent = Intent("com.instagram.share.ADD_TO_STORY").apply {
                setDataAndType(uri, "video/mp4")
                putExtra("interactive_asset_uri", uri)
                if (!contentUrl.isNullOrBlank()) {
                    putExtra("content_url", contentUrl)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(FLAG_ACTIVITY_NEW_TASK)
            }

            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                if (!contentUrl.isNullOrBlank()) {
                    putExtra(Intent.EXTRA_TEXT, contentUrl)
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(sendIntent, chooserTitle).apply {
                addFlags(FLAG_ACTIVITY_NEW_TASK)
                val initialIntents = mutableListOf<Intent>()
                if (fbStoryIntent.resolveActivity(pm) != null) {
                    initialIntents.add(fbStoryIntent)
                }
                if (igStoryIntent.resolveActivity(pm) != null) {
                    initialIntents.add(igStoryIntent)
                }
                if (initialIntents.isNotEmpty()) {
                    putExtra(Intent.EXTRA_INITIAL_INTENTS, initialIntents.toTypedArray())
                }
            }

            context.startActivity(chooser)
            true
        }.getOrElse { error ->
            Logger.e(TAG, "Could not share video $fileName: ${error.message}")
            false
        }
    }
