package com.mika.spotify.auth

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.CommonCrypto.CCHmac
import platform.CommonCrypto.kCCHmacAlgSHA1
import kotlin.math.pow

/**
 * Standard RFC 6238 TOTP implementation for iOS using CommonCrypto CCHmac.
 */
@OptIn(ExperimentalForeignApi::class)
actual fun generateTotp(secret: String, timestamp: Long): String {
    val timeStepSeconds = 30L
    val digits = 6
    val timeIndex = timestamp / 1000L / timeStepSeconds

    val timeBytes = ByteArray(8)
    for (i in 7 downTo 0) {
        timeBytes[i] = ((timeIndex ushr ((7 - i) * 8)) and 0xFF).toByte()
    }

    val keyBytes = secret.encodeToByteArray()
    val hash = ByteArray(20) // SHA1 is 20 bytes

    keyBytes.usePinned { keyPinned ->
        timeBytes.usePinned { timePinned ->
            hash.usePinned { hashPinned ->
                CCHmac(
                    kCCHmacAlgSHA1,
                    keyPinned.addressOf(0),
                    keyBytes.size.toULong(),
                    timePinned.addressOf(0),
                    timeBytes.size.toULong(),
                    hashPinned.addressOf(0),
                )
            }
        }
    }

    val offset = (hash.last().toInt() and 0x0F)
    val binary = ((hash[offset].toInt() and 0x7F) shl 24) or
        ((hash[offset + 1].toInt() and 0xFF) shl 16) or
        ((hash[offset + 2].toInt() and 0xFF) shl 8) or
        (hash[offset + 3].toInt() and 0xFF)

    val otp = binary % 10.0.pow(digits.toDouble()).toInt()
    return otp.toString().padStart(digits, '0')
}
