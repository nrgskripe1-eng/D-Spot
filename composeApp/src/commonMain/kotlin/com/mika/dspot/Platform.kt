package com.mika.dspot

sealed class Platform {
    object Android : Platform()
    object iOS : Platform()
    object Desktop : Platform()

    fun osName(): String = when (this) {
        Android -> "android"
        iOS -> "iOS"
        Desktop -> "desktop"
    }
}

expect fun getPlatform(): Platform

expect fun getDownloadPlatform(): String

fun getUpdateFileName(version: String): String {
    val cleanVersion = version.removePrefix("v")
    return when (getDownloadPlatform()) {
        "android" -> "D-Spot-$cleanVersion.apk"
        "windows" -> "D-Spot-$cleanVersion.msi"
        "macos" -> "D-Spot-$cleanVersion.dmg"
        "linux" -> "D-Spot-$cleanVersion.AppImage"
        else -> "D-Spot-$cleanVersion.bin"
    }
}

