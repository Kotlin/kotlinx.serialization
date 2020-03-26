/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.protobuf.internal

import kotlinx.serialization.*
import kotlinx.serialization.protobuf.*

internal abstract class ProtobufTaggedDecoder : TaggedBase(), Decoder, CompositeDecoder {
    override val updateMode: UpdateMode =
        UpdateMode.UPDATE


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

    // ---- Implementation of low-level API ----

    final override fun decodeNotNullMark(): Boolean = true
    final override fun decodeNull(): Nothing? = null
    final override fun decodeBoolean(): Boolean = decodeTaggedBoolean(popTagOrMissing())
    final override fun decodeByte(): Byte = decodeTaggedByte(popTagOrMissing())
    final override fun decodeShort(): Short = decodeTaggedShort(popTagOrMissing())
    final override fun decodeInt(): Int = decodeTaggedInt(popTagOrMissing())
    final override fun decodeLong(): Long = decodeTaggedLong(popTagOrMissing())
    final override fun decodeFloat(): Float = decodeTaggedFloat(popTagOrMissing())
    final override fun decodeDouble(): Double = decodeTaggedDouble(popTagOrMissing())
    final override fun decodeChar(): Char = decodeTaggedChar(popTagOrMissing())
    final override fun decodeString(): String = decodeTaggedString(popTagOrMissing())
    final override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = decodeTaggedEnum(popTagOrMissing(), enumDescriptor)

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
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
        deserializer: DeserializationStrategy<T>
    ): T = // TODO inline
        tagBlock(descriptor.getTag(index)) { decodeSerializableValue(deserializer) }

    final override fun <T : Any> decodeNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>
    ): T? =
        tagBlock(descriptor.getTag(index)) { decodeNullableSerializableValue(deserializer) }

    override fun <T> updateSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T>,
        old: T
    ): T = tagBlock(descriptor.getTag(index)) { updateSerializableValue(deserializer, old) }

    override fun <T : Any> updateNullableSerializableElement(
        descriptor: SerialDescriptor,
        index: Int,
        deserializer: DeserializationStrategy<T?>,
        old: T?
    ): T? =
        tagBlock(descriptor.getTag(index)) { updateNullableSerializableValue(deserializer, old) }

    override fun decodeUnit() {
        error("Should not be called")
    }

    override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) {
        error("Should not be called")
    }
}
