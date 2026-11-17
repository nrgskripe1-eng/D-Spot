package com.mika.dspot.expect

import com.mika.logger.Logger

actual fun startInAppUpdate(
    downloadUrl: String,
    fileName: String,
    onProgress: (UpdateDownloadState) -> Unit,
) {
    // iOS cannot sideload or execute binaries directly. Fallback to browser or App Store link.
    Logger.d("AppUpdater", "iOS does not support in-app sideload installation. Opening URL: $downloadUrl")
    openUrl(downloadUrl)
    onProgress(UpdateDownloadState.Idle)
}

actual fun installDownloadedUpdate(filePath: String) {
    // No-op on iOS
}

