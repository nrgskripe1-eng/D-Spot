package com.mika.ktorext

import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.HttpClientEngineFactory
import io.ktor.client.engine.darwin.Darwin

@Suppress("UNCHECKED_CAST")
actual fun getEngine(): HttpClientEngineFactory<HttpClientEngineConfig> =
    Darwin as HttpClientEngineFactory<HttpClientEngineConfig>
