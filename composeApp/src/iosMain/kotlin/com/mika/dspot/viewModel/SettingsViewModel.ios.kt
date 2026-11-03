package com.mika.dspot.viewModel

import com.eygraber.uri.Uri
import com.mika.domain.repository.CacheRepository
import com.mika.domain.repository.CommonRepository

actual suspend fun calculateDataFraction(cacheRepository: CacheRepository): SettingsStorageSectionFraction? = null

actual suspend fun readPickedFile(uri: Uri): ByteArray = ByteArray(0)

actual suspend fun restoreNative(
    commonRepository: CommonRepository,
    uri: Uri,
    getData: () -> Unit,
) {}

actual suspend fun backupNative(
    commonRepository: CommonRepository,
    uri: Uri,
    backupDownloaded: Boolean,
) {}

actual fun getPackageName(): String = ""

actual fun getFileDir(): String = ""

actual fun changeLanguageNative(code: String) {}
