package com.mika.ktorext.crypto

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CCHmac
import platform.CommonCrypto.kCCHmacAlgSHA1
import platform.CommonCrypto.kCCHmacAlgSHA256
import platform.Foundation.NSDate
import platform.Foundation.NSISO8601DateFormatter
import platform.Foundation.timeIntervalSince1970
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

@OptIn(ExperimentalForeignApi::class, ExperimentalEncodingApi::class)
actual class Hmac actual constructor(algorithm: String, secretKey: String) {
    private val tokenTtl: Long = 300000L // 5 minutes in milliseconds
    private val algo = if (algorithm.contains("256", ignoreCase = true)) kCCHmacAlgSHA256 else kCCHmacAlgSHA1
    private val keyBytes = secretKey.encodeToByteArray()
    private val digestLength = if (algo == kCCHmacAlgSHA256) 32 else 20

    actual fun getMacTimestampPair(uri: String): Pair<String, String> {
        val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong().toString()
        val data = "$timestamp$uri"
        val hmac = this.generateHmac(data)
        return hmac to timestamp
    }

    actual fun generateHmac(data: String): String {
        val dataBytes = data.encodeToByteArray()
        val result = ByteArray(digestLength)

        keyBytes.usePinned { keyPinned ->
            dataBytes.usePinned { dataPinned ->
                result.usePinned { resultPinned ->
                    CCHmac(
                        algo,
                        keyPinned.addressOf(0),
                        keyBytes.size.toULong(),
                        dataPinned.addressOf(0),
                        dataBytes.size.toULong(),
                        resultPinned.addressOf(0),
                    )
                }
            }
        }

        return Base64.encode(result)
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
}
