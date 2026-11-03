package com.mika.data.di.loader

import com.mika.dspot.media_jvm.di.loadDesktopPlayerModule
import com.mika.data.loginsync.LoginSyncHostRepositoryImpl
import com.mika.data.loginsync.LoginSyncStore
import com.mika.domain.repository.LoginSyncHostRepository
import org.koin.core.context.loadKoinModules
import org.koin.dsl.module

actual fun loadMediaService() {
    loadDesktopPlayerModule()
}

actual fun loadLoginSyncModule() {
    loadKoinModules(
        module {
            single { LoginSyncStore(get(), get(), get()) }
            single<LoginSyncHostRepository> { LoginSyncHostRepositoryImpl(get()) }
        },
    )
}
