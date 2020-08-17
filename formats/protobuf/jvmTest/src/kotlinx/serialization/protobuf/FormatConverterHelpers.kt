/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf

import com.google.protobuf.*
import kotlinx.serialization.*
import java.io.*

interface IMessage {
    fun toProtobufMessage(): GeneratedMessageV3
}

fun GeneratedMessageV3.toHex(): String {
    val b = ByteArrayOutputStream()
    this.writeTo(b)
    return (HexConverter.printHexBinary(b.toByteArray(), lowerCase = true))
}

/**
 * Check serialization of [ProtoBuf].
 *
 * 1. Serializes the given [IMessage] into bytes using [ProtoBuf].
 * 2. Parses those bytes via the `Java ProtoBuf library`.
 * 3. Compares parsed `Java ProtoBuf object` to expected object ([IMessage.toProtobufMessage]).
 *
 * @param it The [IMessage] to check.
 * @param protoBuf Provide custom [ProtoBuf] instance (default: [ProtoBuf.plain]).
 *
 * @return `true` if the de-serialization returns the expected object.
 */
inline fun <reified T : IMessage> dumpCompare(it: T, alwaysPrint: Boolean = false, protoBuf: BinaryFormat = ProtoBuf): Boolean {
    val msg = it.toProtobufMessage()
    var parsed: GeneratedMessageV3?
    val c = try {
        val bytes = protoBuf.encodeToByteArray(it)
        if (alwaysPrint) println("Serialized bytes: ${HexConverter.printHexBinary(bytes)}")
        parsed = msg.parserForType.parseFrom(bytes)
        msg == parsed
    } catch (e: Exception) {
        e.printStackTrace()
        parsed = null
        false
    }
    if (!c || alwaysPrint) println("Expected: $msg\nfound: $parsed")
    return c
}

/**
 * Check de-serialization of [ProtoBuf].
 *
 * 1. Converts expected `Java ProtoBuf object` ([IMessage.toProtobufMessage]) to bytes.
 * 2. Parses those bytes via [ProtoBuf].
 * 3. Compares parsed ProtoBuf object to given object.
 *
 * @param it The [IMessage] to check.
 * @param alwaysPrint Set to `true` if expected/found objects should always get printed to console (default: `false`).
 * @param protoBuf Provide custom [ProtoBuf] instance (default: [ProtoBuf.plain]).
 *
 * @return `true` if the de-serialization returns the original object.
 */
inline fun <reified T : IMessage> readCompare(it: T, alwaysPrint: Boolean = false, protoBuf: BinaryFormat = ProtoBuf): Boolean {
    var obj: T?
    val c = try {
        val msg = it.toProtobufMessage()
        val hex = msg.toHex()
        obj = protoBuf.decodeFromHexString<T>(hex)
        obj == it
    } catch (e: Exception) {
        obj = null
        e.printStackTrace()
        false
    }
    if (!c || alwaysPrint) println("Expected: $it\nfound: $obj")
    return c
}

