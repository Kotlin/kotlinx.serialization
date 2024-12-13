/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*
import kotlin.jvm.*

internal class ProtobufReader(private val input: ByteArrayInput) {
    @JvmField
    public var currentId = -1
    @JvmField
    public var currentType = ProtoWireType.INVALID
    private var pushBack = false
    private var pushBackHeader = 0

    public val eof
        get() = !pushBack && input.availableBytes == 0

    public fun readTag(): Int {
        if (pushBack) {
            pushBack = false
            val previousHeader = (currentId shl 3) or currentType.typeId
            return updateIdAndType(pushBackHeader).also {
                pushBackHeader = previousHeader
            }
        }
        // Header to use when pushed back is the old id/type
        pushBackHeader = (currentId shl 3) or currentType.typeId

        val header = input.readVarint64(true).toInt()
        return updateIdAndType(header)
    }

    private fun updateIdAndType(header: Int): Int {
        return if (header == -1) {
            currentId = -1
            currentType = ProtoWireType.INVALID
            -1
        } else {
            currentId = header ushr 3
            currentType = ProtoWireType.fromLeastSignificantBits(header)
            currentId
        }
    }

    public fun pushBackTag() {
        pushBack = true

        val nextHeader = (currentId shl 3) or currentType.typeId
        updateIdAndType(pushBackHeader)
        pushBackHeader = nextHeader
    }

    fun skipElement() {
        when (currentType) {
            ProtoWireType.VARINT -> readInt(ProtoIntegerType.DEFAULT)
            ProtoWireType.i64 -> readLong(ProtoIntegerType.FIXED)
            ProtoWireType.SIZE_DELIMITED -> skipSizeDelimited()
            ProtoWireType.i32 -> readInt(ProtoIntegerType.FIXED)
            else -> throw ProtobufDecodingException("Unsupported start group or end group wire type: $currentType")
        }
    }

    @Suppress("NOTHING_TO_INLINE")
    private inline fun assertWireType(expected: ProtoWireType) {
        if (currentType != expected) throw ProtobufDecodingException("Expected wire type $expected, but found $currentType")
    }

    fun readByteArray(): ByteArray {
        assertWireType(ProtoWireType.SIZE_DELIMITED)
        return readByteArrayNoTag()
    }

    fun skipSizeDelimited() {
        assertWireType(ProtoWireType.SIZE_DELIMITED)
        val length = decode32()
        checkLength(length)
        input.skipExactNBytes(length)
    }

    fun readByteArrayNoTag(): ByteArray {
        val length = decode32()
        checkLength(length)
        return input.readExactNBytes(length)
    }

    fun objectInput(): ByteArrayInput {
        assertWireType(ProtoWireType.SIZE_DELIMITED)
        return objectTaglessInput()
    }

    fun objectTaglessInput(): ByteArrayInput {
        val length = decode32()
        checkLength(length)
        return input.slice(length)
    }

    fun readInt(format: ProtoIntegerType): Int {
        val wireType = if (format == ProtoIntegerType.FIXED) ProtoWireType.i32 else ProtoWireType.VARINT
        assertWireType(wireType)
        return decode32(format)
    }

    fun readInt32NoTag(): Int = decode32()

    fun readLong(format: ProtoIntegerType): Long {
        val wireType = if (format == ProtoIntegerType.FIXED) ProtoWireType.i64 else ProtoWireType.VARINT
        assertWireType(wireType)
        return decode64(format)
    }

    fun readLongNoTag(): Long = decode64(ProtoIntegerType.DEFAULT)

    fun readFloat(): Float {
        assertWireType(ProtoWireType.i32)
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
        assertWireType(ProtoWireType.i64)
        return Double.fromBits(readLongLittleEndian())
    }

    fun readDoubleNoTag(): Double {
        return Double.fromBits(readLongLittleEndian())
    }

    fun readString(): String {
        assertWireType(ProtoWireType.SIZE_DELIMITED)
        val length = decode32()
        checkLength(length)
        return input.readString(length)
    }

    fun readStringNoTag(): String {
        val length = decode32()
        checkLength(length)
        return input.readString(length)
    }

    private fun checkLength(length: Int) {
        if (length < 0) {
            throw ProtobufDecodingException("Unexpected negative length: $length")
        }
    }

    private fun decode32(format: ProtoIntegerType = ProtoIntegerType.DEFAULT): Int = when (format) {
        ProtoIntegerType.DEFAULT -> input.readVarint64(false).toInt()
        ProtoIntegerType.SIGNED -> decodeSignedVarintInt(
            input
        )
        ProtoIntegerType.FIXED -> readIntLittleEndian()
    }

    private fun decode64(format: ProtoIntegerType = ProtoIntegerType.DEFAULT): Long = when (format) {
        ProtoIntegerType.DEFAULT -> input.readVarint64(false)
        ProtoIntegerType.SIGNED -> decodeSignedVarintLong(
            input
        )
        ProtoIntegerType.FIXED -> readLongLittleEndian()
    }

    /**
     *  Source for all varint operations:
     *  https://github.com/addthis/stream-lib/blob/master/src/main/java/com/clearspring/analytics/util/Varint.java
     */
    private fun decodeSignedVarintInt(input: ByteArrayInput): Int {
        val raw = input.readVarint32()
        val temp = raw shl 31 shr 31 xor raw shr 1
        // This extra step lets us deal with the largest signed values by treating
        // negative results from read unsigned methods as like unsigned values.
        // Must re-flip the top bit if the original read value had it set.
        return temp xor (raw and (1 shl 31))
    }

    private fun decodeSignedVarintLong(input: ByteArrayInput): Long {
        val raw = input.readVarint64(false)
        val temp = raw shl 63 shr 63 xor raw shr 1
        // This extra step lets us deal with the largest signed values by treating
        // negative results from read unsigned methods as like unsigned values
        // Must re-flip the top bit if the original read value had it set.
        return temp xor (raw and (1L shl 63))

    }
}
