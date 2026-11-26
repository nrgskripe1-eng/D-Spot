package com.mika.dspot.expect

sealed interface UpdateDownloadState {
    object Idle : UpdateDownloadState
    data class Downloading(val progress: Float, val bytesDownloaded: Long, val totalBytes: Long) : UpdateDownloadState
    data class ReadyToInstall(val filePath: String) : UpdateDownloadState
    data class Error(val message: String) : UpdateDownloadState
}

expect fun startInAppUpdate(
    downloadUrl: String,
    fileName: String,
    onProgress: (UpdateDownloadState) -> Unit,
)

expect fun installDownloadedUpdate(filePath: String)

expect fun getDownloadedUpdatePath(fileName: String): String?

expect fun cleanupDownloadedUpdates(currentVersion: String)

