@file:OptIn(
    kotlinx.cinterop.ExperimentalForeignApi::class,
    kotlin.io.encoding.ExperimentalEncodingApi::class,
)

package com.mika.ktorext.crypto

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CCHmac
import platform.CommonCrypto.kCCHmacAlgSHA1
import platform.CommonCrypto.kCCHmacAlgSHA256
import platform.Foundation.NSDate
import platform.Foundation.timeIntervalSince1970
import kotlin.io.encoding.Base64

actual class Hmac actual constructor(algorithm: String, secretKey: String) {
    private val tokenTtl: Long = 300000L // 5 minutes in milliseconds
    private val isSha256 = algorithm.contains("256", ignoreCase = true)
    private val algo: UInt = if (isSha256) kCCHmacAlgSHA256 else kCCHmacAlgSHA1
    private val keyBytes: ByteArray = secretKey.encodeToByteArray()
    private val digestLength: Int = if (isSha256) 32 else 20

    actual fun getMacTimestampPair(uri: String): Pair<String, String> {
        val timestamp = (NSDate().timeIntervalSince1970 * 1000).toLong().toString()
        val data = "$timestamp$uri"
        val hmac = this.generateHmac(data)
        return hmac to timestamp
    }

    actual fun generateHmac(data: String): String {
        val dataBytes = data.encodeToByteArray()
        val result = ByteArray(digestLength)

        if (keyBytes.isEmpty()) {
            return Base64.Default.encode(result)
        }

        keyBytes.usePinned { keyPinned ->
            val keyPtr = keyPinned.addressOf(0)
            val keyLen = keyBytes.size.toULong()

            if (dataBytes.isEmpty()) {
                result.usePinned { resultPinned ->
                    CCHmac(
                        algo,
                        keyPtr,
                        keyLen,
                        null,
                        0uL,
                        resultPinned.addressOf(0),
                    )
                }
            } else {
                dataBytes.usePinned { dataPinned ->
                    result.usePinned { resultPinned ->
                        CCHmac(
                            algo,
                            keyPtr,
                            keyLen,
                            dataPinned.addressOf(0),
                            dataBytes.size.toULong(),
                            resultPinned.addressOf(0),
                        )
                    }
                }
            }
        }

        return Base64.Default.encode(result)
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
