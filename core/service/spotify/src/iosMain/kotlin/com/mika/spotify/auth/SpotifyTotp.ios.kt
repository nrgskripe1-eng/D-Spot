package com.mika.spotify.auth

import kotlin.math.pow

/**
 * Standard RFC 6238 TOTP implementation for iOS using pure Kotlin HMAC-SHA1.
 * Zero platform/C dependencies.
 */
actual fun generateTotp(secret: String, timestamp: Long): String {
    val timeStepSeconds = 30L
    val digits = 6
    val timeIndex = timestamp / 1000L / timeStepSeconds

    val timeBytes = ByteArray(8)
    for (i in 7 downTo 0) {
        timeBytes[i] = ((timeIndex ushr ((7 - i) * 8)) and 0xFF).toByte()
    }

    val keyBytes = secret.encodeToByteArray()
    val hash = hmacSha1(keyBytes, timeBytes)

    val offset = (hash.last().toInt() and 0x0F)
    val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
        ((hash[offset + 1].toInt() and 0xFF) shl 16) or
        ((hash[offset + 2].toInt() and 0xFF) shl 8) or
        (hash[offset + 3].toInt() and 0xFF)

    val otp = binary % 10.0.pow(digits.toDouble()).toInt()
    return otp.toString().padStart(digits, '0')
}

// ---------------------------------------------------------------------------
// Pure Kotlin HMAC-SHA1 (RFC 2104)
// ---------------------------------------------------------------------------

private fun hmacSha1(key: ByteArray, message: ByteArray): ByteArray {
    val blockSize = 64
    val paddedKey = when {
        key.size > blockSize -> sha1(key)
        key.size < blockSize -> key.copyOf(blockSize)
        else -> key
    }

    val oKeyPad = ByteArray(blockSize) { i -> (paddedKey[i].toInt() xor 0x5c).toByte() }
    val iKeyPad = ByteArray(blockSize) { i -> (paddedKey[i].toInt() xor 0x36).toByte() }

    val innerHash = sha1(iKeyPad + message)
    return sha1(oKeyPad + innerHash)
}

private fun sha1(data: ByteArray): ByteArray {
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
    for (i in 0 until 8) {
        padded[totalLength - 1 - i] = ((bitLength ushr (i * 8)) and 0xFF).toByte()
    }

    val w = IntArray(80)
    for (chunk in 0 until totalLength / 64) {
        val offset = chunk * 64
        for (i in 0 until 16) {
            w[i] = ((padded[offset + i * 4].toInt() and 0xFF) shl 24) or
                    ((padded[offset + i * 4 + 1].toInt() and 0xFF) shl 16) or
                    ((padded[offset + i * 4 + 2].toInt() and 0xFF) shl 8) or
                    (padded[offset + i * 4 + 3].toInt() and 0xFF)
        }
        for (i in 16 until 80) {
            w[i] = (w[i - 3] xor w[i - 8] xor w[i - 14] xor w[i - 16]).rotateLeft(1)
        }

        var a = h0
        var b = h1
        var c = h2
        var d = h3
        var e = h4

        for (i in 0 until 80) {
            val (f, k) = when (i) {
                in 0..19 -> ((b and c) or (b.inv() and d)) to 0x5A827999
                in 20..39 -> (b xor c xor d) to 0x6ED9EBA1
                in 40..59 -> ((b and c) or (b and d) or (c and d)) to 0x8F1BBCDC.toInt()
                else -> (b xor c xor d) to 0xCA62C1D6.toInt()
            }
            val temp = a.rotateLeft(5) + f + e + k + w[i]
            e = d
            d = c
            c = b.rotateLeft(30)
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
    val hash = intArrayOf(h0, h1, h2, h3, h4)
    for (i in 0 until 5) {
        result[i * 4] = ((hash[i] ushr 24) and 0xFF).toByte()
        result[i * 4 + 1] = ((hash[i] ushr 16) and 0xFF).toByte()
        result[i * 4 + 2] = ((hash[i] ushr 8) and 0xFF).toByte()
        result[i * 4 + 3] = (hash[i] and 0xFF).toByte()
    }
    return result
}
