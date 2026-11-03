package com.mika.data.di.loader

import com.mika.media3.di.loadMediaService
import com.mika.data.loginsync.LoginSyncSenderRepositoryImpl
import com.mika.data.loginsync.LoginSyncStore
import com.mika.domain.repository.LoginSyncSenderRepository
import org.koin.core.context.loadKoinModules
import com.mika.dspot.loginsync.LoginSyncClient
import org.koin.dsl.module

actual fun loadMediaService() {
    loadMediaService()
}

actual fun loadLoginSyncModule() {
    loadKoinModules(
        module {
            single { LoginSyncStore(get(), get(), get()) }
            single<LoginSyncSenderRepository> { LoginSyncSenderRepositoryImpl(get(), LoginSyncClient()) }
        },
    )
}
