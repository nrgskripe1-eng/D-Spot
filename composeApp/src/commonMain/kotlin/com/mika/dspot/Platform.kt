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
        "android" -> "dspot-v$cleanVersion.apk"
        "windows" -> "dspot-setup-v$cleanVersion.exe"
        "macos" -> "dspot-v$cleanVersion.dmg"
        "linux" -> "dspot-v$cleanVersion.AppImage"
        else -> "dspot-v$cleanVersion.bin"
    }
}

