package com.mika.dspot.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun filePickerResult(onResultUri: (String?) -> Unit): FilePickerLauncher {
    return remember(onResultUri) {
        object : FilePickerLauncher {
            override fun launch() {
                onResultUri(null)
            }
        }
    }
}

@Composable
actual fun fileSaverResult(
    fileName: String,
    mimeType: String,
    onResultUri: (String?) -> Unit,
): FilePickerLauncher {
    return remember(fileName, mimeType, onResultUri) {
        object : FilePickerLauncher {
            override fun launch() {
                onResultUri(null)
            }
        }
    }
}

