/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.builtins

import kotlinx.serialization.*
import kotlinx.serialization.modules.*

/**
 * A skeleton implementation of both [Decoder] and [CompositeDecoder] that can be used
 * for simple formats and for testability purpose.
 * Most of the `decode*` methods have default implementation that delegates `decodeValue(value: Any) as TargetType`.
 * See [Decoder] documentation for information about each particular `decode*` method.
 */
public abstract class AbstractDecoder : Decoder, CompositeDecoder {
    override val context: SerialModule
        get() = EmptyModule

    override val updateMode: UpdateMode = UpdateMode.UPDATE

    /**
     * Invoked to decode a value when specialized `encode*` method was not overridden.
     */
    public open fun decodeValue(): Any = throw SerializationException("${this::class} can't retrieve untyped values")

    override fun decodeNotNullMark(): Boolean = true
    override fun decodeNull(): Nothing? = null
    override fun decodeUnit(): Unit = UnitSerializer().deserialize(this)

    override fun decodeBoolean(): Boolean = decodeValue() as Boolean
    override fun decodeByte(): Byte = decodeValue() as Byte
    override fun decodeShort(): Short = decodeValue() as Short
    override fun decodeInt(): Int = decodeValue() as Int
    override fun decodeLong(): Long = decodeValue() as Long
    override fun decodeFloat(): Float = decodeValue() as Float
    override fun decodeDouble(): Double = decodeValue() as Double
    override fun decodeChar(): Char = decodeValue() as Char
    override fun decodeString(): String = decodeValue() as String
    override fun decodeEnum(enumDescriptor: SerialDescriptor): Int = decodeValue() as Int

    // Delegating implementation of CompositeDecoder

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }

    final override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int): Unit = decodeUnit()
    final override fun decodeBooleanElement(descriptor: SerialDescriptor, index: Int): Boolean = decodeBoolean()
    final override fun decodeByteElement(descriptor: SerialDescriptor, index: Int): Byte = decodeByte()
    final override fun decodeShortElement(descriptor: SerialDescriptor, index: Int): Short = decodeShort()
    final override fun decodeIntElement(descriptor: SerialDescriptor, index: Int): Int = decodeInt()
    final override fun decodeLongElement(descriptor: SerialDescriptor, index: Int): Long = decodeLong()
    final override fun decodeFloatElement(descriptor: SerialDescriptor, index: Int): Float = decodeFloat()
    final override fun decodeDoubleElement(descriptor: SerialDescriptor, index: Int): Double = decodeDouble()
    final override fun decodeCharElement(descriptor: SerialDescriptor, index: Int): Char = decodeChar()
    final override fun decodeStringElement(descriptor: SerialDescriptor, index: Int): String = decodeString()

    final override fun <T: Any?> decodeSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T =
        decodeSerializableValue(deserializer)
    final override fun <T: Any> decodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T? =
        decodeNullableSerializableValue(deserializer)
    final override fun <T> updateSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T =
        updateSerializableValue(deserializer, old)
    final override fun <T: Any> updateNullableSerializableElement(descriptor: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T? =
        updateNullableSerializableValue(deserializer, old)
}
