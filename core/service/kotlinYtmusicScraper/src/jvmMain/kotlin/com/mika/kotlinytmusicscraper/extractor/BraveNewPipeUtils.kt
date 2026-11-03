package com.mika.kotlinytmusicscraper.extractor

import com.mika.common.ITAG
import com.mika.kotlinytmusicscraper.models.YouTubeClient
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException
import java.io.IOException
import java.net.Proxy

internal fun List<Pair<Int, String>>.hasRequiredItags(): Boolean {
    val itags = this.mapTo(HashSet()) { it.first }
    val hasAudio = ITAG.AUDIO.any { it in itags }
    val hasVideo = ITAG.VIDEO.any { it in itags }
    return hasAudio && hasVideo
}

private val streamHealthCheckClient: OkHttpClient by lazy {
    OkHttpClient
        .Builder()
        .build()
}

/**
 * Pick one random URL whose itag belongs to [ITAG.AUDIO] or [ITAG.VIDEO] and HEAD it. Returns true
 * only when the response code is in the 200..299 range. We only health-check those URLs because
 * they are the ones the player will actually use; lower-quality extras can stay unverified.
 *
 * Returns true on any network exception (timeout, DNS failure, etc.) — a failed HEAD probe does
 * not prove the URLs are invalid, it only means the probe itself could not complete. Falling back
 * to BravePipe on a transient network hiccup added unnecessary latency and occasionally triggered
 * full extraction retries when the original URLs were perfectly usable.
 */
internal fun List<Pair<Int, String>>.headCheckRandomStream(): Boolean {
    val required = ITAG.AUDIO + ITAG.VIDEO
    val candidate = this.filter { it.first in required }.randomOrNull() ?: return false
    return runCatching {
        val request =
            okhttp3.Request
                .Builder()
                .head()
                .url(candidate.second)
                .build()
        streamHealthCheckClient.newCall(request).execute().use { response ->
            response.code in 200..299
        }
    }.getOrDefault(true) // assume valid on exception — see KDoc above
}

class BraveNewPipeDownloaderImpl(
    proxy: Proxy?,
) : Downloader() {
    private val client =
        OkHttpClient
            .Builder()
            .proxy(proxy)
            .build()

    @Throws(IOException::class, ReCaptchaException::class)
    override fun execute(request: Request): Response {
        val response = client.newCall(buildOkHttpRequest(request)).execute()

        if (response.code == 429) {
            response.close()
            throw ReCaptchaException("reCaptcha Challenge requested", request.url())
        }

        val body = response.body?.string()
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            body,
            response.request.url.toString(),
        )
    }

    private fun buildOkHttpRequest(request: Request): okhttp3.Request {
        val builder =
            okhttp3.Request
                .Builder()
                .method(request.httpMethod(), request.dataToSend()?.toRequestBody())
                .url(request.url())
                .addHeader("User-Agent", YouTubeClient.USER_AGENT_WEB)

        request.headers().forEach { (headerName, headerValueList) ->
            if (headerValueList.size > 1) {
                builder.removeHeader(headerName)
                headerValueList.forEach { headerValue ->
                    builder.addHeader(headerName, headerValue)
                }
            } else if (headerValueList.size == 1) {
                builder.header(headerName, headerValueList[0])
            }
        }
        return builder.build()
    }
}
