package com.mika.dspot.extension

import com.eygraber.uri.Uri

private const val APP_SCHEME = "dspot"

/**
 * Turn a pasted YouTube link into the `dspot://` deep link that already drives the app.
 *
 * Deliberately a *translation* rather than a second link handler: `App.kt` already knows what to do
 * with `watch` / `playlist` / `channel` — including that a `OLAK5uy_` list is an album and that a
 * playlist id needs the `VL` prefix. Rewriting the host and handing it to the same intent flow
 * reuses all of that instead of duplicating it, so the two paths can never drift apart.
 *
 * Returns null for anything that is not a supported link, which the caller treats as an ordinary
 * search query — a pasted Shorts URL or an `@handle` channel link should search, not fail.
 */
fun String.toAppDeepLinkOrNull(): Uri? {
    val trimmed = trim()
    if (trimmed.startsWith("spotify:", ignoreCase = true)) {
        val parts = trimmed.split(":")
        val type = parts.getOrNull(1)?.lowercase()
        val id = parts.getOrNull(2)?.takeIf { it.isNotBlank() }
        return if (type in setOf("track", "album", "playlist", "artist", "user") && id != null) {
            Uri.parse("$APP_SCHEME://spotify?type=$type&id=$id")
        } else {
            null
        }
    }
    if (!trimmed.startsWith("http://", ignoreCase = true) &&
        !trimmed.startsWith("https://", ignoreCase = true)
    ) {
        return null
    }

    val uri = runCatching { Uri.parse(trimmed) }.getOrNull() ?: return null
    val host = uri.host?.lowercase()?.removePrefix("www.") ?: return null
    val segments = uri.pathSegments

    return when (host) {
        // Short form carries the id in the path: youtu.be/VIDEO_ID
        "youtu.be" ->
            segments
                .firstOrNull()
                ?.takeIf { it.isNotBlank() }
                ?.let { Uri.parse("$APP_SCHEME://watch?v=$it") }

        "youtube.com", "m.youtube.com", "music.youtube.com" ->
            when (segments.firstOrNull()) {
                // A watch link may also carry &list=…; the video wins, because that is the thing
                // the person meant to share.
                "watch" -> uri.getQueryParameter("v")?.let { Uri.parse("$APP_SCHEME://watch?v=$it") }
                "playlist" -> uri.getQueryParameter("list")?.let { Uri.parse("$APP_SCHEME://playlist?list=$it") }
                "channel" -> segments.getOrNull(1)?.let { Uri.parse("$APP_SCHEME://channel/$it") }
                else -> null
            }

        "open.spotify.com" -> {
            // Handle optional locale prefix like /intl-en/track/...
            val normalizedSegments =
                if (segments.firstOrNull()?.startsWith("intl-") == true) segments.drop(1) else segments
            val type = normalizedSegments.getOrNull(0)
            val id = normalizedSegments.getOrNull(1)?.takeIf { it.isNotBlank() }
            if (type in setOf("track", "album", "playlist", "artist", "user") && id != null) {
                Uri.parse("$APP_SCHEME://spotify?type=$type&id=$id")
            } else {
                null
            }
        }

        "soundcloud.com", "m.soundcloud.com", "on.soundcloud.com" ->
            if (segments.isNotEmpty()) {
                Uri.parse("$APP_SCHEME://soundcloud?url=$trimmed")
            } else {
                null
            }

        else -> null
    }
}
