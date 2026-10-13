package com.mika.dspot.expect

actual fun getDownloadFolderPath(): String = System.getProperty("user.home") + "/Downloads"