package com.mika.kotlinytmusicscraper.utils

import com.mika.kotlinytmusicscraper.models.response.AudioData
import com.mika.logger.Logger
import kotlin.time.Clock
import kotlinx.serialization.json.Json
import kotlin.io.encoding.Base64

fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte ->
    (eachByte.toInt() and 0xFF).toString(16).padStart(2, '0')
}

fun sha1(str: String): String = pureSha1(str.encodeToByteArray()).toHex()

private fun pureSha1(data: ByteArray): ByteArray {
    var h0 = 0x67452301
    var h1 = 0xEFCDAB89.toInt()
    var h2 = 0x98BADCFE.toInt()
    var h3 = 0x10325476
    var h4 = 0xC3D2E1F0.toInt()

    val bitLength = data.size.toLong() * 8
    val paddingLength = ((56 - (data.size + 1) % 64 + 64) % 64)
    val totalLength = data.size + 1 + paddingLength + 8
    val padded = ByteArray(totalLength)
    data.copyInto(padded)
    padded[data.size] = 0x80.toByte()

    for (i in 0..7) {
        padded[totalLength - 1 - i] = ((bitLength ushr (i * 8)) and 0xFF).toByte()
    }

    val w = IntArray(80)
    for (chunk in padded.indices step 64) {
        for (i in 0..15) {
            val j = chunk + i * 4
            w[i] = ((padded[j].toInt() and 0xFF) shl 24) or
                    ((padded[j + 1].toInt() and 0xFF) shl 16) or
                    ((padded[j + 2].toInt() and 0xFF) shl 8) or
                    (padded[j + 3].toInt() and 0xFF)
        }
        for (i in 16..79) {
            val t = w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]
            w[i] = (t shl 1) or (t ushr 31)
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4

        for (i in 0..79) {
            val (f, k) = when (i) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) to 0xCA62C1D6.toInt()
            }
            val temp = ((a shl 5) or (a ushr 27)) + f + e + k + w[i]
            e = d
            d = c
            c = (b shl 30) or (b ushr 2)
            b = a
            a = temp
        }

        h0 += a
        h1 += b
        h2 += c
        h3 += d
        h4 += e
    }

    val result = ByteArray(20)
    for (i in 0..3) {
        result[i] = ((h0 ushr (24 - i * 8)) and 0xFF).toByte()
        result[4 + i] = ((h1 ushr (24 - i * 8)) and 0xFF).toByte()
        result[8 + i] = ((h2 ushr (24 - i * 8)) and 0xFF).toByte()
        result[12 + i] = ((h3 ushr (24 - i * 8)) and 0xFF).toByte()
        result[16 + i] = ((h4 ushr (24 - i * 8)) and 0xFF).toByte()
    }
    return result
}

fun parseCookieString(cookie: String): Map<String, String> =
    cookie
        .split("; ")
        .filter { it.isNotEmpty() }
        .associate {
            val (key, value) = it.split("=")
            key to value
        }

fun String.parseTime(): Int? {
    try {
        val parts =
            if (this.contains(":")) split(":").map { it.toInt() } else split(".").map { it.toInt() }
        if (parts.size == 2) {
            return parts[0] * 60 + parts[1]
        }
        if (parts.size == 3) {
            return parts[0] * 3600 + parts[1] * 60 + parts[2]
        }
    } catch (e: Exception) {
        return null
    }
    return null
}

fun generateNetscapeCookies(
    cookies: Map<String, String>,
    domain: String = ".example.com",
    path: String = "/",
    secure: Boolean = false,
    httpOnly: Boolean = false,
    expirationTimeSeconds: Long = (Clock.System.now().toEpochMilliseconds() / 1000) + 86400 * 365,
): String {
    val header =
        "# Netscape HTTP Cookie File\n" +
            "# This is a generated file! Do not edit.\n\n"

    val cookieLines =
        cookies
            .map { (name, value) ->
                // Netscape format: domain, domainFlag, path, secure, expiration, name, value
                buildString {
                    append(domain)
                    append("\t")
                    append("TRUE") // domain flag - TRUE means domain includes subdomains
                    append("\t")
                    append(path)
                    append("\t")
                    append(if (secure) "TRUE" else "FALSE")
                    append("\t")
                    append(expirationTimeSeconds)
                    append("\t")
                    append(name)
                    append("\t")
                    append(value)
                }
            }.joinToString("\n")

    return header + cookieLines
}

fun String.decodeTidalManifest(): AudioData? {
    val decodedBytes = Base64.decode(this)
    val jsonString = decodedBytes.decodeToString()
    val json =
        Json {
            explicitNulls = false
            ignoreUnknownKeys = true
        }
    return try {
        json.decodeFromString<AudioData?>(jsonString)
    } catch (e: Exception) {
        Logger.e("Utils", "Failed to decode Tidal manifest: ${e.message}")
        e.printStackTrace()
        null
    }
}

fun String.decodeBase64(): String {
    val decodedBytes = Base64.decode(this)
    return decodedBytes.decodeToString()
}
