/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import com.google.protobuf.GeneratedMessageV3
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
    val parsed: GeneratedMessageV3?
    return try {
        val bytes = ProtoBuf.dump(it)
        parsed = msg.parserForType.parseFrom(bytes)
        msg == parsed
    } catch (e: Exception) {
        e.printStackTrace()
        false
    }
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


    private const val hexCode = "0123456789ABCDEF"

    fun printHexBinary(data: ByteArray, lowerCase: Boolean = false): String {
        val r = StringBuilder(data.size * 2)
        for (b in data) {
            r.append(hexCode[b.toInt() shr 4 and 0xF])
            r.append(hexCode[b.toInt() and 0xF])
        }
        return if (lowerCase) r.toString().toLowerCase() else r.toString()
    }
}
