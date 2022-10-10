/*
 * Copyright 2017-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json.internal

import java.io.InputStream

// an example of working with surrogates is taken from okio library with minor changes, see https://github.com/square/okio
internal fun InputStream.processUtf16Chars(
    maxCharsCount: Int,
    yield: (Char) -> Unit
) {
    var charsCount = 0
    var b0 = 0
    var needReadB0 = true
    nextCodePoint@while (charsCount < maxCharsCount) {
        if (needReadB0) {
            b0 = this.readOrNull() ?: return
        } else {
            needReadB0 = true
        }

        when {
            b0 >= 0 -> {
                // 0b0xxxxxxx
                yield(b0.toChar())
                charsCount++

                // Assume there is going to be more ASCII
                // This is almost double the performance of the outer loop
                while (charsCount < maxCharsCount) {
                    b0 = readOrNull() ?: return
                    if (b0 < 0) {
                        needReadB0 = false
                        continue@nextCodePoint
                    }
                    yield(b0.toChar())
                    charsCount++
                }
            }
            b0 shr 5 == -2 -> {
                // 0b110xxxxx
                process2Utf8Bytes(b0, readOrNull()) {
                    yield(it.toChar())
                    charsCount++
                }
            }
            b0 shr 4 == -2 -> {
                // 0b1110xxxx
                process3Utf8Bytes(b0, readOrNull(), readOrNull()) {
                    yield(it.toChar())
                    charsCount++
                }
            }
            b0 shr 3 == -2 -> {
                // 0b11110xxx
                process4Utf8Bytes(b0, readOrNull(), readOrNull(), readOrNull()) { codePoint ->
                    if (codePoint != REPLACEMENT_CODE_POINT) {
                        // Unicode code point:    00010000000000000000 + xxxxxxxxxxyyyyyyyyyy (21 bits)
                        // UTF-16 high surrogate: 110110xxxxxxxxxx (10 bits)
                        // UTF-16 low surrogate:  110111yyyyyyyyyy (10 bits)
                        yield(((codePoint ushr 10) + HIGH_SURROGATE_HEADER).toChar())
                        yield(((codePoint and 0x03ff) + LOW_SURROGATE_HEADER).toChar())
                        charsCount += 2
                    } else {
                        yield(REPLACEMENT_CHARACTER)
                        charsCount++
                    }
                }
            }
            else -> {
                // 0b10xxxxxx - Unexpected continuation
                // 0b111111xxx - Unknown encoding
                yield(REPLACEMENT_CHARACTER)
                charsCount++
            }
        }
    }
}

@Suppress("NOTHING_TO_INLINE")
internal inline fun InputStream.readOrNull(): Int? {
    val intValue = read()
    if (intValue < 0) return null
    return if (intValue < 128) {
        intValue
    } else {
        intValue - 256
    }
}

// Value added to the high UTF-16 surrogate after shifting
private const val HIGH_SURROGATE_HEADER = 0xd800 - (0x010000 ushr 10)

// Value added to the low UTF-16 surrogate after masking
private const val LOW_SURROGATE_HEADER = 0xdc00

private const val REPLACEMENT_CHARACTER: Char = '\ufffd'
private const val REPLACEMENT_CODE_POINT: Int = REPLACEMENT_CHARACTER.code

// Mask used to remove byte headers from a 2 byte encoded UTF-8 character
private const val MASK_2BYTES = 0x0f80
// MASK_2BYTES =
//    (0xc0.toByte() shl 6) xor
//    (0x80.toByte().toInt())

internal inline  fun process2Utf8Bytes(
    b0: Int,
    b1: Int?,
    yield: (Int) -> Unit
) {
    if (b1 == null) {
        // Only 1 byte remaining - underflow
        yield(REPLACEMENT_CODE_POINT)
        return
    }
    if (!isUtf8Continuation(b1)) {
        yield(REPLACEMENT_CODE_POINT)
        return
    }

    val codePoint = MASK_2BYTES xor b1 xor (b0 shl 6)

    when {
        codePoint < 0x80 -> {
            yield(REPLACEMENT_CODE_POINT) // Reject overlong code points.
        }
        else -> {
            yield(codePoint)
        }
    }
}

// Mask used to remove byte headers from a 3 byte encoded UTF-8 character
private const val MASK_3BYTES = -0x01e080
// MASK_3BYTES =
//    (0xe0.toByte() shl 12) xor
//    (0x80.toByte() shl 6) xor
//    (0x80.toByte().toInt())

internal inline fun process3Utf8Bytes(
    b0: Int,
    b1: Int?,
    b2: Int?,
    yield: (Int) -> Unit
) {
    if (b1 == null || b2 == null) {
        // At least 2 bytes remaining
        yield(REPLACEMENT_CODE_POINT)
        return
    }

    if (!isUtf8Continuation(b1) || !isUtf8Continuation(b2)) {
        yield(REPLACEMENT_CODE_POINT)
        return
    }

    val codePoint = MASK_3BYTES xor b2 xor (b1 shl 6) xor (b0 shl 12)

    when {
        codePoint < 0x800 -> {
            yield(REPLACEMENT_CODE_POINT) // Reject overlong code points.
        }
        codePoint in 0xd800..0xdfff -> {
            yield(REPLACEMENT_CODE_POINT) // Reject partial surrogates.
        }
        else -> {
            yield(codePoint)
        }
    }
}


// Mask used to remove byte headers from a 4 byte encoded UTF-8 character
private const val MASK_4BYTES = 0x381f80
// MASK_4BYTES =
//    (0xf0.toByte() shl 18) xor
//    (0x80.toByte() shl 12) xor
//    (0x80.toByte() shl 6) xor
//    (0x80.toByte().toInt())

internal inline fun process4Utf8Bytes(
    b0: Int,
    b1: Int?,
    b2: Int?,
    b3: Int?,
    yield: (Int) -> Unit
) {
    if (b1 == null || b2 == null || b3 == null) {
        // At least 3 bytes remaining
        yield(REPLACEMENT_CODE_POINT)
        return
    }

    if (!isUtf8Continuation(b1) || !isUtf8Continuation(b2) || !isUtf8Continuation(b3)) {
        yield(REPLACEMENT_CODE_POINT)
        return
    }

    val codePoint = MASK_4BYTES xor b3 xor (b2 shl 6) xor (b1 shl 12) xor (b0 shl 18)

    when {
        codePoint > 0x10ffff -> {
            yield(REPLACEMENT_CODE_POINT) // Reject code points larger than the Unicode maximum.
        }
        codePoint in 0xd800..0xdfff -> {
            yield(REPLACEMENT_CODE_POINT) // Reject partial surrogates.
        }
        codePoint < 0x10000 -> {
            yield(REPLACEMENT_CODE_POINT) // Reject overlong code points.
        }
        else -> {
            yield(codePoint)
        }
    }
}

@Suppress("NOTHING_TO_INLINE") // Syntactic sugar.
internal inline fun isUtf8Continuation(byte: Int): Boolean {
    // 0b10xxxxxx
    return byte and 0xc0 == 0x80
}
