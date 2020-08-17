/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor.internal

import kotlinx.serialization.*

internal class CborDecodingException(expected: String, foundByte: Int) :
    SerializationException("Expected $expected, but found ${printByte(foundByte)}")

internal fun printByte(b: Int): String {
    val hexCode = "0123456789ABCDEF"
    return buildString {
        append(hexCode[b shr 4 and 0xF])
        append(hexCode[b and 0xF])
    }
}
