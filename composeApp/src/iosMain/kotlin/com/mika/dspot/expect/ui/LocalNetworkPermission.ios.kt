package com.mika.dspot.expect.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
actual fun rememberLocalNetworkPermission(): LocalNetworkPermissionRequester {
    return remember {
        object : LocalNetworkPermissionRequester {
            override fun requestIfNeeded(url: String) {}
        }
    }
}

