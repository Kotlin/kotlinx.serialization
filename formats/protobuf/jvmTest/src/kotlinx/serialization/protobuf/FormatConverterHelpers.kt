/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import com.google.protobuf.GeneratedMessageV3
import kotlinx.io.*
import java.io.ByteArrayOutputStream
import kotlinx.serialization.dump
import kotlinx.serialization.loads

interface IMessage {
    fun toProtobufMessage(): GeneratedMessageV3
}

fun GeneratedMessageV3.toHex(): String {
    val b = ByteArrayOutputStream()
    this.writeTo(b)
    return (HexConverter.printHexBinary(b.toByteArray(), lowerCase = true))
}

inline fun <reified T : IMessage> dumpCompare(it: T): Boolean {
    val msg = it.toProtobufMessage()
    var parsed: GeneratedMessageV3?
    val c = try {
        val bytes = ProtoBuf.dump(it)
        parsed = msg.parserForType.parseFrom(bytes)
        msg == parsed
    } catch (e: Exception) {
        e.printStackTrace()
        parsed = null
        false
    }
    return c
}

inline fun <reified T : IMessage> readCompare(it: T, alwaysPrint: Boolean = false): Boolean {
    var obj: T?
    val c = try {
        val msg = it.toProtobufMessage()
        val hex = msg.toHex()
        obj = ProtoBuf.loads(hex)
        obj == it
    } catch (e: Exception) {
        obj = null
        e.printStackTrace()
        false
    }
    if (!c || alwaysPrint) println("Expected: $it\nfound: $obj")
    return c
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

    fun toHexString(n: Int) = printHexBinary(ByteBuffer.allocate(4).putInt(n).flip().array(), true)
        .trimStart('0').takeIf { it.isNotEmpty() } ?: "0"
}
