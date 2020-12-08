/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*

internal abstract class ProtobufTaggedDecoder : ProtobufTaggedBase(), Decoder, CompositeDecoder {
    protected abstract fun SerialDescriptor.getTag(index: Int): ProtoDesc

    protected abstract fun decodeTaggedBoolean(tag: ProtoDesc): Boolean
    protected abstract fun decodeTaggedByte(tag: ProtoDesc): Byte
    protected abstract fun decodeTaggedShort(tag: ProtoDesc): Short
    protected abstract fun decodeTaggedInt(tag: ProtoDesc): Int
    protected abstract fun decodeTaggedLong(tag: ProtoDesc): Long
    protected abstract fun decodeTaggedFloat(tag: ProtoDesc): Float
    protected abstract fun decodeTaggedDouble(tag: ProtoDesc): Double
    protected abstract fun decodeTaggedChar(tag: ProtoDesc): Char
    protected abstract fun decodeTaggedString(tag: ProtoDesc): String
    protected abstract fun decodeTaggedEnum(tag: ProtoDesc, enumDescription: SerialDescriptor): Int
    protected abstract fun <T : Any?> decodeSerializableValue(deserializer: DeserializationStrategy<T>, previousValue: T?): T

    protected open fun decodeTaggedInline(tag: ProtoDesc, inlineDescriptor: SerialDescriptor): Decoder = this.apply { pushTag(tag) }

    final override fun decodeNotNullMark(): Boolean = true
    final override fun decodeNull(): Nothing? = null
    final override fun decodeBoolean(): Boolean = decodeTaggedBoolean(popTagOrDefault())
    final override fun decodeByte(): Byte = decodeTaggedByte(popTagOrDefault())
    final override fun decodeShort(): Short = decodeTaggedShort(popTagOrDefault())
    final override fun decodeInt(): Int = decodeTaggedInt(popTagOrDefault())
    final override fun decodeLong(): Long = decodeTaggedLong(popTagOrDefault())
    final override fun decodeFloat(): Float = decodeTaggedFloat(popTagOrDefault())
    final override fun decodeDouble(): Double = decodeTaggedDouble(popTagOrDefault())
    final override fun decodeChar(): Char = decodeTaggedChar(popTagOrDefault())
    final override fun decodeString(): String = decodeTaggedString(popTagOrDefault())
    final override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = decodeTaggedEnum(popTagOrDefault(), enumDescriptor)

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
        // Nothing
    }

    final override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean =
        decodeTaggedBoolean(descriptor.getTag(index))

    final override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte =
        decodeTaggedByte(descriptor.getTag(index))

    final override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short =
        decodeTaggedShort(descriptor.getTag(index))

    final override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int =
        decodeTaggedInt(descriptor.getTag(index))

    final override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long =
        decodeTaggedLong(descriptor.getTag(index))

    final override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float =
        decodeTaggedFloat(descriptor.getTag(index))

    final override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double =
        decodeTaggedDouble(descriptor.getTag(index))

    final override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char =
        decodeTaggedChar(descriptor.getTag(index))

    final override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String =
        decodeTaggedString(descriptor.getTag(index))

    final override fun <T : Any?> decodeSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        previousValue: T?
    ): T = tagBlock(descriptor.getTag(index)) { decodeSerializableValue(deserializer, previousValue) }

    final override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        previousValue: T?
    ): T? = tagBlock(descriptor.getTag(index)) {
        if (decodeNotNullMark()) {
            decodeSerializableValue(deserializer, previousValue)
        } else {
            decodeNull()
        }
    }

    override fun decodeInline(inlineDescriptor: SerialDescriptor): Decoder {
        return decodeTaggedInline(popTag(), inlineDescriptor)
    }

    override fun decodeInlineElement(
        descriptor: SerialDescriptor,
        index: Int,
        inlineDescriptor: SerialDescriptor
    ): Decoder {
        return decodeTaggedInline(descriptor.getTag(index), inlineDescriptor)
    }
}
