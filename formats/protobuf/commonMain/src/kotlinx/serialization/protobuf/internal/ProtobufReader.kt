/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.io.*
import kotlinx.serialization.protobuf.*
import kotlin.jvm.*

internal class ProtobufReader(private val input: ByteArrayInput) {
    @JvmField
    public var currentId = -1
    @JvmField
    public var currentType = -1
    private var pushBack = false

    public fun readTag(): Int {
        if (pushBack) {
            pushBack = false
            return currentId
        }

        val header = input.readVarint64(true).toInt()
        return if (header == -1) {
            currentId = -1
            currentType = -1
            -1
        } else {
            currentId = header ushr 3
            currentType = header and 0b111
            currentId
        }
    }

    public fun pushBackTag() {
        pushBack = true
    }

    fun skipElement() {
        when (currentType) {
            ProtoBuf.VARINT -> readInt(ProtoNumberType.DEFAULT)
            ProtoBuf.i64 -> readLong(ProtoNumberType.FIXED)
            ProtoBuf.SIZE_DELIMITED -> readObject()
            ProtoBuf.i32 -> readInt(ProtoNumberType.FIXED)
            else -> throw ProtobufDecodingException("Unsupported start group or end group wire type")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun assertWireType(expected: Int) {
        if (currentType != expected) throw ProtobufDecodingException("Expected wire type $expected, but found $currentType")
    }

    fun readObject(): ByteArray {
        assertWireType(ProtoBuf.SIZE_DELIMITED)
        val length = decode32()
        check(length >= 0)
        return input.readExactNBytes(length)
    }

    fun readObjectNoTag(): ByteArray {
        val length = decode32()
        check(length >= 0)
        return input.readExactNBytes(length)
    }

    private fun Input.readExactNBytes(bytesCount: Int): ByteArray {
        if (bytesCount > availableBytes) {
            error("Unexpected EOF, available $availableBytes bytes, requested: $bytesCount")
        }
        val array = ByteArray(bytesCount)
        read(array, 0, bytesCount)
        return array
    }

    fun readInt(format: ProtoNumberType): Int {
        val wireType = if (format == ProtoNumberType.FIXED) ProtoBuf.i32 else ProtoBuf.VARINT
        assertWireType(wireType)
        return decode32(format)
    }

    fun readInt32NoTag(): Int = decode32()

    fun readLong(format: ProtoNumberType): Long {
        val wireType = if (format == ProtoNumberType.FIXED) ProtoBuf.i64 else ProtoBuf.VARINT
        assertWireType(wireType)
        return decode64(format)
    }

    fun readLongNoTag(): Long = decode64(ProtoNumberType.DEFAULT)

    fun readFloat(): Float {
        assertWireType(ProtoBuf.i32)
        return Float.fromBits(readIntLittleEndian())
    }

    fun readFloatNoTag(): Float = Float.fromBits(readIntLittleEndian())

    private fun readIntLittleEndian(): Int {
        // TODO this could be optimized by extracting method to the IS
        var result = 0
        for (i in 0..3) {
            val byte = input.read() and 0x000000FF
            result = result or (byte shl (i * 8))
        }
        return result
    }

    private fun readLongLittleEndian(): Long {
        // TODO this could be optimized by extracting method to the IS
        var result = 0L
        for (i in 0..7) {
            val byte = (input.read() and 0x000000FF).toLong()
            result = result or (byte shl (i * 8))
        }
        return result
    }

    fun readDouble(): Double {
        assertWireType(ProtoBuf.i64)
        return Double.fromBits(readLongLittleEndian())
    }

    fun readDoubleNoTag(): Double {
        return Double.fromBits(readLongLittleEndian())
    }

    fun readString(): String {
        assertWireType(ProtoBuf.SIZE_DELIMITED)
        val length = decode32()
        check(length >= 0)
        return input.readString(length)
    }

    fun readStringNoTag(): String {
        val length = decode32()
        check(length >= 0)
        return input.readString(length)
    }

    private fun decode32(format: ProtoNumberType = ProtoNumberType.DEFAULT): Int = when (format) {
        ProtoNumberType.DEFAULT -> input.readVarint64(false).toInt()
        ProtoNumberType.SIGNED -> Varint.decodeSignedVarintInt(
            input
        )
        ProtoNumberType.FIXED -> readIntLittleEndian()
    }

    private fun decode64(format: ProtoNumberType = ProtoNumberType.DEFAULT): Long = when (format) {
        ProtoNumberType.DEFAULT -> input.readVarint64(false)
        ProtoNumberType.SIGNED -> Varint.decodeSignedVarintLong(
            input
        )
        ProtoNumberType.FIXED -> readLongLittleEndian()
    }
}
