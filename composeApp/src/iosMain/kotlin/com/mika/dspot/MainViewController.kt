package com.mika.dspot

import androidx.compose.ui.window.ComposeUIViewController
import com.mika.common.AppIdentity
import com.mika.data.di.loader.loadAllModules
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.loadKoinModules
import com.mika.dspot.di.viewModelModule

fun MainViewController() = ComposeUIViewController {
    // Initialize Koin once — guard against hot-reload / multiple calls
    if (GlobalContext.getOrNull() == null) {
        startKoin {
            loadAllModules(
                AppIdentity(
                    applicationId = "com.mika.dspot",
                    versionName = BuildKonfig.versionName,
                    platform = "iOS",
                    isDevBuild = false,
                ),
            )
            loadKoinModules(viewModelModule)
        }
    }
    App()
}