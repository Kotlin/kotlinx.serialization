/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.formats

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.dataformat.cbor.CBORFactory
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.registerKotlinModule
import com.google.protobuf.GeneratedMessageV3
import kotlinx.serialization.*
import kotlinx.serialization.cbor.Cbor
import kotlinx.serialization.internal.HexConverter
import kotlinx.serialization.protobuf.ProtoBuf
import java.io.ByteArrayOutputStream

interface IMessage {
    fun toProtobufMessage(): GeneratedMessageV3
}

fun GeneratedMessageV3.toHex(): String {
    val b = ByteArrayOutputStream()
    this.writeTo(b)
    return (HexConverter.printHexBinary(b.toByteArray(), lowerCase = true))
}

inline fun <reified T : IMessage> dumpCompare(it: T, alwaysPrint: Boolean = false): Boolean {
    val msg = it.toProtobufMessage()
    var parsed: GeneratedMessageV3?
    val c = try {
        val bytes = ProtoBuf.dump(it)
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

inline fun <reified T : IMessage> readCompare(it: T, alwaysPrint: Boolean = false): Boolean {
    var obj: T?
    val c = try {
        val msg = it.toProtobufMessage()
        val hex = msg.toHex()
        obj = ProtoBuf.loads<T>(hex)
        obj == it
    } catch (e: Exception) {
        obj = null
        e.printStackTrace()
        false
    }
    if (!c || alwaysPrint) println("Expected: $it\nfound: $obj")
    return c
}

internal val cborJackson = ObjectMapper(CBORFactory()).apply { registerKotlinModule() }

internal inline fun <reified T : IMessage> dumpCborCompare(it: T, alwaysPrint: Boolean = false): Boolean {
    var parsed: T?
    val c = try {
        val bytes = Cbor.dump(it)
        parsed = cborJackson.readValue<T>(bytes)
        it == parsed
    } catch (e: Exception) {
        e.printStackTrace()
        parsed = null
        false
    }
    if (!c || alwaysPrint) println("Expected: $it\nfound: $parsed")
    return c
}

internal inline fun <reified T : IMessage> readCborCompare(it: T, alwaysPrint: Boolean = false): Boolean {
    var obj: T?
    val c = try {
        val hex = cborJackson.writeValueAsBytes(it)
        obj = Cbor.load<T>(hex)
        obj == it
    } catch (e: Exception) {
        obj = null
        e.printStackTrace()
        false
    }
    if (!c || alwaysPrint) println("Expected: $it\nfound: $obj")
    return c
}
