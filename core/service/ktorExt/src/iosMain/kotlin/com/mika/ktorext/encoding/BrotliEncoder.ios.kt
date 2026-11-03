package com.mika.ktorext.encoding

import io.ktor.util.ContentEncoder
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.ByteWriteChannel
import kotlin.coroutines.CoroutineContext

/**
 * Brotli native decoding not available on iOS without a native C library.
 * Returning identity/passthrough encoder — server should negotiate a supported encoding (gzip/deflate).
 */
object BrotliEncoder : ContentEncoder {
    override val name: String
        get() = "br"

    override fun encode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel = source

    override fun encode(
        source: ByteWriteChannel,
        coroutineContext: CoroutineContext,
    ): ByteWriteChannel = source

    override fun decode(
        source: ByteReadChannel,
        coroutineContext: CoroutineContext,
    ): ByteReadChannel = source
}

actual fun createBrotliEncoder(): ContentEncoder = BrotliEncoder
