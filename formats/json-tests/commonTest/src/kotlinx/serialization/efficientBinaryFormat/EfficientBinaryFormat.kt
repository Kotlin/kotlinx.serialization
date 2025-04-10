/*
 * Copyright 2017-2025 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.efficientBinaryFormat

import kotlinx.serialization.BinaryFormat
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationStrategy
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.AbstractDecoder
import kotlinx.serialization.encoding.AbstractEncoder
import kotlinx.serialization.encoding.CompositeDecoder
import kotlinx.serialization.encoding.CompositeEncoder
import kotlinx.serialization.modules.EmptySerializersModule
import kotlinx.serialization.modules.SerializersModule

class EfficientBinaryFormat(
    override val serializersModule: SerializersModule = EmptySerializersModule(),
): BinaryFormat {

    override fun <T> encodeToByteArray(
        serializer: SerializationStrategy<T>,
        value: T
    ): ByteArray {
        val encoder = Encoder(serializersModule)
        serializer.serialize(encoder, value)
        return encoder.byteBuffer.toByteArray()
    }

    override fun <T> decodeFromByteArray(
        deserializer: DeserializationStrategy<T>,
        bytes: ByteArray
    ): T {
        val decoder = Decoder(serializersModule, bytes)
        return deserializer.deserialize(decoder)
    }

    class Encoder(override val serializersModule: SerializersModule): AbstractEncoder() {
        val byteBuffer = ByteWritingBuffer()
        override fun encodeBoolean(value: Boolean) = byteBuffer.writeByte(if (value) 1 else 0)
        override fun encodeByte(value: Byte) = byteBuffer.writeByte(value)
        override fun encodeShort(value: Short) = byteBuffer.writeShort(value)
        override fun encodeInt(value: Int) = byteBuffer.writeInt(value)
        override fun encodeLong(value: Long) = byteBuffer.writeLong(value)
        override fun encodeFloat(value: Float) = byteBuffer.writeFloat(value)
        override fun encodeDouble(value: Double) = byteBuffer.writeDouble(value)
        override fun encodeChar(value: Char) = byteBuffer.writeChar(value)
        override fun encodeString(value: String) = byteBuffer.writeString(value)
        override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = byteBuffer.writeInt(index)

        @ExperimentalSerializationApi
        override fun shouldEncodeElementDefault(descriptor: SerialDescriptor, index: Int): Boolean = true

        override fun beginCollection(descriptor: SerialDescriptor, collectionSize: Int): CompositeEncoder {
            encodeInt(collectionSize)
            return this
        }

        override fun encodeNull() = encodeBoolean(false)
        override fun encodeNotNullMark() = encodeBoolean(true)

    }

    class Decoder(override val serializersModule: SerializersModule, private val reader: ByteReadingBuffer) : AbstractDecoder() {

        constructor(serializersModule: SerializersModule, bytes: ByteArray) : this(
            serializersModule,
            ByteReadingBuffer(bytes)
        )

        private var nextElementIndex = 0
//        private var currentDesc: SerialDescriptor? = null

        override fun decodeBoolean(): Boolean = reader.readByte().toInt() != 0

        override fun decodeByte(): Byte = reader.readByte()

        override fun decodeShort(): Short = reader.readShort()

        override fun decodeInt(): Int = reader.readInt()

        override fun decodeLong(): Long = reader.readLong()

        override fun decodeFloat(): Float = reader.readFloat()

        override fun decodeDouble(): Double = reader.readDouble()

        override fun decodeChar(): Char = reader.readChar()

        override fun decodeString(): String = reader.readString()

        override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = reader.readInt()

        override fun decodeNotNullMark(): Boolean = decodeBoolean()

        @ExperimentalSerializationApi
        override fun decodeSequentially(): Boolean = true

        override fun decodeCollectionSize(descriptor: SerialDescriptor): Int = reader.readInt()

        override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
            return Decoder(serializersModule, reader)
        }

        override fun endStructure(descriptor: SerialDescriptor) {
            check(nextElementIndex ==0 || descriptor.elementsCount == nextElementIndex) { "Type: ${descriptor.serialName} not fully read: ${descriptor.elementsCount} != $nextElementIndex" }
        }

        override fun decodeElementIndex(descriptor: SerialDescriptor): Int {
            return when (nextElementIndex) {
                descriptor.elementsCount -> CompositeDecoder.DECODE_DONE
                else -> nextElementIndex++
            }
        }
    }
}