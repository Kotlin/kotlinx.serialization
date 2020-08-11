/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

internal class ProtobufWriter(private val out: ByteArrayOutput) {
    fun writeBytes(bytes: ByteArray, tag: Int) {
        out.encode32((tag shl 3) or SIZE_DELIMITED)
        writeBytes(bytes)
    }

    fun writeBytes(bytes: ByteArray) {
        out.encode32(bytes.size)
        out.write(bytes)
    }

    fun writeOutput(output: ByteArrayOutput, tag: Int) {
        out.encode32((tag shl 3) or SIZE_DELIMITED)
        writeOutput(output)
    }

    fun writeOutput(output: ByteArrayOutput) {
        out.encode32(output.size())
        out.write(output)
    }

    fun writeInt(value: Int, tag: Int, format: ProtoIntegerType) {
        val wireType = if (format == ProtoIntegerType.FIXED) i32 else VARINT
        out.encode32((tag shl 3) or wireType)
        out.encode32(value, format)
    }

    fun writeInt(value: Int) {
        out.encode32(value)
    }

    fun writeLong(value: Long, tag: Int, format: ProtoIntegerType) {
        val wireType = if (format == ProtoIntegerType.FIXED) i64 else VARINT
        out.encode32((tag shl 3) or wireType)
        out.encode64(value, format)
    }

    fun writeLong(value: Long) {
        out.encode64(value)
    }

    fun writeString(value: String, tag: Int) {
        val bytes = value.encodeToByteArray()
        writeBytes(bytes, tag)
    }

    fun writeString(value: String) {
        val bytes = value.encodeToByteArray()
        writeBytes(bytes)
    }

    fun writeDouble(value: Double, tag: Int) {
        out.encode32((tag shl 3) or i64)
        out.writeLong(value.reverseBytes())
    }

    fun writeDouble(value: Double) {
        out.writeLong(value.reverseBytes())
    }

    fun writeFloat(value: Float, tag: Int) {
        out.encode32((tag shl 3) or i32)
        out.writeInt(value.reverseBytes())
    }

    fun writeFloat(value: Float) {
        out.writeInt(value.reverseBytes())
    }

    private fun ByteArrayOutput.encode32(
        number: Int,
        format: ProtoIntegerType = ProtoIntegerType.DEFAULT
    ) {
        when (format) {
            ProtoIntegerType.FIXED -> out.writeInt(number.reverseBytes())
            ProtoIntegerType.DEFAULT -> encodeVarint64(number.toLong())
            ProtoIntegerType.SIGNED -> encodeVarint32(((number shl 1) xor (number shr 31)))
        }
    }

    private fun ByteArrayOutput.encode64(number: Long, format: ProtoIntegerType = ProtoIntegerType.DEFAULT) {
        when (format) {
            ProtoIntegerType.FIXED -> out.writeLong(number.reverseBytes())
            ProtoIntegerType.DEFAULT -> encodeVarint64(number)
            ProtoIntegerType.SIGNED -> encodeVarint64((number shl 1) xor (number shr 63))
        }
    }

    private fun Float.reverseBytes(): Int = toRawBits().reverseBytes()

    private fun Double.reverseBytes(): Long = toRawBits().reverseBytes()
}
