package com.mika.dspot.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun openEqResult(audioSessionId: Int): OpenEqLauncher {
    return remember {
        object : OpenEqLauncher {
            override fun launch() {}
        }
    }
}

