@file:OptIn(kotlin.io.encoding.ExperimentalEncodingApi::class)

package com.mika.ktorext.crypto

import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import kotlin.io.encoding.Base64

actual class Hmac actual constructor(algorithm: String, secretKey: String) {
    private val tokenTtl: Long = 300000L // 5 minutes in milliseconds
    private val isSha256 = algorithm.contains("256", ignoreCase = true)
    private val keyBytes = secretKey.encodeToByteArray()

    actual fun getMacTimestampPair(uri: String): Pair<String, String> {
        val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong().toString()
        val data = "$timestamp$uri"
        val hmac = this.generateHmac(data)
        return hmac to timestamp
    }

    actual fun generateHmac(data: String): String {
        val dataBytes = data.encodeToByteArray()
        val mac = if (isSha256) {
            hmacSha256(keyBytes, dataBytes)
        } else {
            hmacSha1(keyBytes, dataBytes)
        }
        return Base64.Default.encode(mac)
    }

    actual fun validateHmac(
        data: String,
        hmac: String,
    ): Boolean {
        val calculatedHmac = generateHmac(data)
        return calculatedHmac == hmac
    }

    actual fun isValidTimestamp(timestamp: String): Boolean {
        val requestTime = timestamp.toLongOrNull() ?: return false
        val currentTime = (NSDate().timeIntervalSince1970 * 1000).toLong()
        return (currentTime - requestTime) < tokenTtl
    }

    // ---------------------------------------------------------------------------
    // Pure Kotlin HMAC (RFC 2104) Implementation - Zero native/C dependencies
    // ---------------------------------------------------------------------------

    private fun hmacSha256(key: ByteArray, message: ByteArray): ByteArray {
        val blockSize = 64
        val paddedKey = when {
            key.size > blockSize -> sha256(key)
            key.size < blockSize -> key.copyOf(blockSize)
            else -> key
        }

        val oKeyPad = ByteArray(blockSize) { i -> (paddedKey[i].toInt() xor 0x5c).toByte() }
        val iKeyPad = ByteArray(blockSize) { i -> (paddedKey[i].toInt() xor 0x36).toByte() }

        val innerHash = sha256(iKeyPad + message)
        return sha256(oKeyPad + innerHash)
    }

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

    // SHA-256
    private fun sha256(data: ByteArray): ByteArray {
        val k = intArrayOf(
            0x428a2f98.toInt(), 0x71374491.toInt(), 0xb5c0fbcf.toInt(), 0xe9b5dba5.toInt(),
            0x3956c25b, 0x59f111f1, 0x923f82a4.toInt(), 0xab1c5ed5.toInt(),
            0xd807aa98.toInt(), 0x12835b01, 0x243185be, 0x550c7dc3,
            0x72be5d74, 0x80deb1fe.toInt(), 0x9bdc06a7.toInt(), 0xc19bf174.toInt(),
            0xe49b69c1.toInt(), 0xefbe4786.toInt(), 0x0fc19dc6, 0x240ca1cc,
            0x2de92c6f, 0x4a7484aa, 0x5cb0a9dc, 0x76f988da,
            0x983e5152.toInt(), 0xa831c66d.toInt(), 0xb00327c8.toInt(), 0xbf597fc7.toInt(),
            0xc6e00bf3.toInt(), 0xd5a79147.toInt(), 0x06ca6351, 0x14292967,
            0x27b70a85, 0x2e1b2138, 0x4d2c6dfc, 0x53380d13,
            0x650a7354, 0x766a0abb, 0x81c2c92e.toInt(), 0x92722c85.toInt(),
            0xa2bfe8a1.toInt(), 0xa81a664b.toInt(), 0xc24b8b70.toInt(), 0xc76c51a3.toInt(),
            0xd192e819.toInt(), 0xd6990624.toInt(), 0xf40e3585.toInt(), 0x106aa070,
            0x19a4c116, 0x1e376c08, 0x2748774c, 0x34b0bcb5,
            0x391c0cb3, 0x4ed8aa4a, 0x5b9cca4f, 0x682e6ff3,
            0x748f82ee, 0x78a5636f, 0x84c87814.toInt(), 0x8cc70208.toInt(),
            0x90befffa.toInt(), 0xa4506ceb.toInt(), 0xbef9a3f7.toInt(), 0xc67178f2.toInt()
        )

        var h0 = 0x6a09e667
        var h1 = 0xbb67ae85.toInt()
        var h2 = 0x3c6ef372
        var h3 = 0xa54ff53a.toInt()
        var h4 = 0x510e527f
        var h5 = 0x9b05688c.toInt()
        var h6 = 0x1f83d9ab
        var h7 = 0x5be0cd19

        val bitLength = data.size.toLong() * 8
        val paddingLength = ((56 - (data.size + 1) % 64 + 64) % 64)
        val totalLength = data.size + 1 + paddingLength + 8
        val padded = ByteArray(totalLength)
        data.copyInto(padded)
        padded[data.size] = 0x80.toByte()
        for (i in 0 until 8) {
            padded[totalLength - 1 - i] = ((bitLength ushr (i * 8)) and 0xFF).toByte()
        }

        val w = IntArray(64)
        for (chunk in 0 until totalLength / 64) {
            val offset = chunk * 64
            for (i in 0 until 16) {
                w[i] = ((padded[offset + i * 4].toInt() and 0xFF) shl 24) or
                        ((padded[offset + i * 4 + 1].toInt() and 0xFF) shl 16) or
                        ((padded[offset + i * 4 + 2].toInt() and 0xFF) shl 8) or
                        (padded[offset + i * 4 + 3].toInt() and 0xFF)
            }
            for (i in 16 until 64) {
                val s0 = w[i - 15].rotateRight(7) xor w[i - 15].rotateRight(18) xor (w[i - 15] ushr 3)
                val s1 = w[i - 2].rotateRight(17) xor w[i - 2].rotateRight(19) xor (w[i - 2] ushr 10)
                w[i] = w[i - 16] + s0 + w[i - 7] + s1
            }

            var a = h0
            var b = h1
            var c = h2
            var d = h3
            var e = h4
            var f = h5
            var g = h6
            var h = h7

            for (i in 0 until 64) {
                val s1 = e.rotateRight(6) xor e.rotateRight(11) xor e.rotateRight(25)
                val ch = (e and f) xor (e.inv() and g)
                val temp1 = h + s1 + ch + k[i] + w[i]
                val s0 = a.rotateRight(2) xor a.rotateRight(13) xor a.rotateRight(22)
                val maj = (a and b) xor (a and c) xor (b and c)
                val temp2 = s0 + maj

                h = g
                g = f
                f = e
                e = d + temp1
                d = c
                c = b
                b = a
                a = temp1 + temp2
            }

            h0 += a
            h1 += b
            h2 += c
            h3 += d
            h4 += e
            h5 += f
            h6 += g
            h7 += h
        }

        val result = ByteArray(32)
        val hash = intArrayOf(h0, h1, h2, h3, h4, h5, h6, h7)
        for (i in 0 until 8) {
            result[i * 4] = ((hash[i] ushr 24) and 0xFF).toByte()
            result[i * 4 + 1] = ((hash[i] ushr 16) and 0xFF).toByte()
            result[i * 4 + 2] = ((hash[i] ushr 8) and 0xFF).toByte()
            result[i * 4 + 3] = (hash[i] and 0xFF).toByte()
        }
        return result
    }

    // SHA-1
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
}
