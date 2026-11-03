package com.mika.dspot.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun photoPickerResult(onResultUri: (String?) -> Unit): PhotoPickerLauncher {
    return remember(onResultUri) {
        object : PhotoPickerLauncher {
            override fun launch() {
                onResultUri(null)
            }
        }
    }
}

