package com.mika.dspot.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import platform.Photos.PHAuthorizationStatusAuthorized
import platform.Photos.PHPhotoLibrary

@Composable
actual fun rememberSaveImagePermission(onResult: (granted: Boolean) -> Unit): SaveImagePermissionRequester {
    return remember(onResult) {
        object : SaveImagePermissionRequester {
            override fun requestIfNeeded() {
                PHPhotoLibrary.requestAuthorization { status ->
                    onResult(status == PHAuthorizationStatusAuthorized)
                }
            }
        }
    }
}

