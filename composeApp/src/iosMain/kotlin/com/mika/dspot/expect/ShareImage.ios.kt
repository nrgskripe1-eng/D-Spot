package com.mika.dspot.expect

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSData
import platform.Photos.PHPhotoLibrary
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
actual suspend fun saveImageToDevice(bytes: ByteArray, fileName: String): Boolean {
    return true
}

@OptIn(ExperimentalForeignApi::class)
actual suspend fun shareImage(
    bytes: ByteArray,
    fileName: String,
    chooserTitle: String,
    mimeType: String,
): Boolean {
    return true
}

