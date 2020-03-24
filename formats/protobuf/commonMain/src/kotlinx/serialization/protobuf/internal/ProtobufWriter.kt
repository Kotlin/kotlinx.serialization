/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.io.*
import kotlinx.serialization.protobuf.*
import kotlin.jvm.*

internal class ProtobufWriter(@JvmField val out: ByteArrayOutput) {
    fun writeBytes(bytes: ByteArray, tag: Int) {
        out.encode32((tag shl 3) or ProtoBuf.SIZE_DELIMITED)
        out.encode32(bytes.size)
        out.write(bytes)
    }

    fun writeBytes(bytes: ByteArray) {
        out.encode32(bytes.size)
        out.write(bytes)
    }

    fun writeInt(value: Int, tag: Int, format: ProtoNumberType) {
        val wireType = if (format == ProtoNumberType.FIXED) ProtoBuf.i32 else ProtoBuf.VARINT
        out.encode32((tag shl 3) or wireType)
        out.encode32(value, format)
    }

    fun writeInt(value: Int) {
        out.encode32(value)
    }

    fun writeLong(value: Long, tag: Int, format: ProtoNumberType) {
        val wireType = if (format == ProtoNumberType.FIXED) ProtoBuf.i64 else ProtoBuf.VARINT
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
        out.encode32((tag shl 3) or ProtoBuf.i64)
        out.writeLong(value.reverseBytes())
    }

    fun writeDouble(value: Double) {
        out.writeLong(value.reverseBytes())
    }

    fun writeFloat(value: Float, tag: Int) {
        out.encode32((tag shl 3) or ProtoBuf.i32)
        out.writeInt(value.reverseBytes())
    }

    fun writeFloat(value: Float) {
        out.writeInt(value.reverseBytes())
    }

    private fun Output.encode32(
        number: Int,
        format: ProtoNumberType = ProtoNumberType.DEFAULT
    ) {
        when (format) {
            ProtoNumberType.FIXED -> out.writeInt(number.reverseBytes())
            ProtoNumberType.DEFAULT -> encodeVarint64(number.toLong())
            ProtoNumberType.SIGNED -> encodeVarint32(((number shl 1) xor (number shr 31)))
        }
    }

    private fun Output.encode64(number: Long, format: ProtoNumberType = ProtoNumberType.DEFAULT) {
        when (format) {
            ProtoNumberType.FIXED -> out.writeLong(number.reverseBytes())
            ProtoNumberType.DEFAULT -> encodeVarint64(number)
            ProtoNumberType.SIGNED -> encodeVarint64((number shl 1) xor (number shr 63))
        }
    }

    private fun Float.reverseBytes(): Int = toRawBits().reverseBytes()

    private fun Double.reverseBytes(): Long = toRawBits().reverseBytes()
}
