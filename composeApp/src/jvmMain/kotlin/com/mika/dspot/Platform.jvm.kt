package com.mika.dspot


actual fun getPlatform(): Platform = Platform.Desktop

actual fun getDownloadPlatform(): String {
    val os = System.getProperty("os.name")?.lowercase().orEmpty()
    return when {
        os.contains("mac") -> "macos"
        os.contains("linux") -> "linux"
        else -> "windows"
    }
}
