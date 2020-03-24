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

    init {
        readTag()
    }

    private fun readTag() {
        val header = input.readVarint64(true).toInt()
        if (header == -1) {
            currentId = -1
            currentType = -1
        } else {
            currentId = header ushr 3
            currentType = header and 0b111
        }
    }

    fun skipElement() {
        when (currentType) {
            ProtoBuf.VARINT -> nextInt(ProtoNumberType.DEFAULT)
            ProtoBuf.i64 -> nextLong(ProtoNumberType.FIXED)
            ProtoBuf.SIZE_DELIMITED -> nextObject()
            ProtoBuf.i32 -> nextInt(ProtoNumberType.FIXED)
            else -> throw ProtobufDecodingException("Unsupported start group or end group wire type")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun assertWireType(expected: Int) {
        if (currentType != expected) throw ProtobufDecodingException("Expected wire type $expected, but found $currentType")
    }

    fun nextObject(): ByteArray {
        assertWireType(ProtoBuf.SIZE_DELIMITED)
        val length = decode32()
        check(length >= 0)
        val result = input.readExactNBytes(length)
        readTag()
        return result
    }

    fun nextObjectNoTag(): ByteArray {
        assertWireType(ProtoBuf.SIZE_DELIMITED)
        val length = decode32()
        check(length >= 0)
        return input.readExactNBytes(length)
    }

    private fun Input.readExactNBytes(bytes: Int): ByteArray {
        val array = ByteArray(bytes)
        var read = 0
        while (read < bytes) {
            val i = this.read(array, read, bytes - read)
            if (i == -1) error("Unexpected EOF")
            read += i
        }
        return array
    }

    fun nextBoolean(): Boolean = when (val i = nextInt(ProtoNumberType.DEFAULT)) {
        0 -> false
        1 -> true
        else -> throw ProtobufDecodingException("Expected boolean value (0 or 1), found $i")
    }

    fun nextInt(format: ProtoNumberType): Int {
        val wireType = if (format == ProtoNumberType.FIXED) ProtoBuf.i32 else ProtoBuf.VARINT
        assertWireType(wireType)
        val result = decode32(format)
        readTag()
        return result
    }

    fun nextInt32NoTag(): Int {
        assertWireType(ProtoBuf.i32)
        return decode32()
    }

    fun nextLong(format: ProtoNumberType): Long {
        val wireType = if (format == ProtoNumberType.FIXED) ProtoBuf.i64 else ProtoBuf.VARINT
        assertWireType(wireType)
        val result = decode64(format)
        readTag()
        return result
    }

    fun nextLongNoTag(): Long {
        assertWireType(ProtoBuf.i64)
        return decode64(ProtoNumberType.DEFAULT)
    }

    fun nextFloat(): Float {
        assertWireType(ProtoBuf.i32)
        val result = Float.fromBits(readIntLittleEndian())
        readTag()
        return result
    }

    fun nextFloatNoTag(): Float {
        assertWireType(ProtoBuf.i32)
        return Float.fromBits(readIntLittleEndian())
    }

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

    fun nextDouble(): Double {
        assertWireType(ProtoBuf.i64)
        val result = Double.fromBits(readLongLittleEndian())
        readTag()
        return result
    }

    fun nextDoubleNoTag(): Double {
        assertWireType(ProtoBuf.i64)
        return Double.fromBits(readLongLittleEndian())
    }

    fun nextString(): String {
        assertWireType(ProtoBuf.SIZE_DELIMITED)
        val length = decode32()
        check(length >= 0)
        val result = input.readString(length)
        readTag()
        return result
    }

    fun nextStringNoTag(): String {
        assertWireType(ProtoBuf.SIZE_DELIMITED)
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
