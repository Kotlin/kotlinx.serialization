/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.io.InputStream
import kotlinx.serialization.*

@InternalSerializationApi
public fun InputStream.readExactNBytes(bytes: Int): ByteArray {
    val array = ByteArray(bytes)
    var read = 0
    while (read < bytes) {
        val i = this.read(array, read, bytes - read)
        if (i == -1) error("Unexpected EOF")
        read += i
    }
    return array
}

object HexConverter {
    fun parseHexBinary(s: String): ByteArray {
        val len = s.length
        require(len % 2 == 0) { "HexBinary string must be even length" }
        val bytes = ByteArray(len / 2)
        var i = 0

        while (i < len) {
            val h = hexToInt(s[i])
            val l = hexToInt(s[i + 1])
            require(!(h == -1 || l == -1)) { "Invalid hex chars: ${s[i]}${s[i+1]}" }

            bytes[i / 2] = ((h shl 4) + l).toByte()
            i += 2
        }

        return bytes
    }

    private fun hexToInt(ch: Char): Int = when (ch) {
        in '0'..'9' -> ch - '0'
        in 'A'..'F' -> ch - 'A' + 10
        in 'a'..'f' -> ch - 'a' + 10
        else -> -1
    }

    private const val hexCode = "0123456789ABCDEF"

    fun printHexBinary(data: ByteArray, lowerCase: Boolean = false): String {
        val r = StringBuilder(data.size * 2)
        for (b in data) {
            r.append(hexCode[b.toInt() shr 4 and 0xF])
            r.append(hexCode[b.toInt() and 0xF])
        }
        return if (lowerCase) r.toString().toLowerCase() else r.toString()
    }

    fun toHexString(n: Int): String {
        val arr = ByteArray(4)
        for (i in 0 until 4) {
            arr[i] = (n shr (24 - i * 8)).toByte()
        }
        return printHexBinary(arr, true).trimStart('0').takeIf { it.isNotEmpty() } ?: "0"
    }
}
