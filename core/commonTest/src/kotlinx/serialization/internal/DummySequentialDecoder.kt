/*
 * Copyright 2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.modules.*

/**
 * The purpose of this decoder is to check whether its methods were called currectly,
 * rather than implement any concrete format.
 */
class DummySequentialDecoder(
    override val serializersModule: SerializersModule = EmptySerializersModule()
) : Decoder, CompositeDecoder {
    private fun notImplemented(): Nothing = throw Error("Implement this method if needed")

    override fun decodeSequentially(): Boolean = true
    override fun decodeElementIndex(descriptor: SerialDescriptor): Int = throw Error("This method shouldn't be called in sequential mode")

    var beginStructureCalled = 0
    var endStructureCalled = 0

    override fun beginStructure(descriptor: SerialDescriptor): CompositeDecoder {
        ++beginStructureCalled
        return this
    }
    override fun endStructure(descriptor: SerialDescriptor): Unit {
        ++endStructureCalled
        return Unit
    }

    override fun decodeInline(descriptor: SerialDescriptor): Decoder = notImplemented()

    override fun decodeBoolean(): Boolean = notImplemented()
    override fun decodeByte(): Byte = notImplemented()
    override fun decodeShort(): Short = notImplemented()
    override fun decodeInt(): Int = notImplemented()
    override fun decodeLong(): Long = notImplemented()
    override fun decodeFloat(): Float = notImplemented()
    override fun decodeDouble(): Double = notImplemented()
    override fun decodeChar(): Char = notImplemented()
    override fun decodeString(): String = notImplemented()
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = notImplemented()

    override fun decodeNotNullMark(): Boolean = notImplemented()
    override fun decodeNull(): Nothing? = notImplemented()

    override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = notImplemented()
    override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = notImplemented()
    override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = notImplemented()
    override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = notImplemented()
    override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = notImplemented()
    override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = notImplemented()
    override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = notImplemented()
    override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = notImplemented()
    override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = notImplemented()

    override fun decodeInlineElement(descriptor: SerialDescriptor, index: Int): Decoder = notImplemented()
    override fun <T : Any?> decodeSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, previousValue: T?): T = decodeSerializableValue(deserializer)
    override fun <T : Any> decodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, previousValue: T?): T? = notImplemented()
}
