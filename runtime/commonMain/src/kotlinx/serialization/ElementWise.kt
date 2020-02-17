/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.modules.*

abstract class ElementValueEncoder : Encoder, CompositeEncoder {
    override val context: SerialModule
        get() = EmptyModule

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeSerializers: KSerializer<*>): CompositeEncoder {
        return this
    }

    /**
     * Always invoked before writing each element to determine if it should be encoded.
     * @return `true` if value should be encoded, false otherwise
     */
    open fun encodeElement(descriptor: SerialDescriptor, index: Int): Boolean = true

    open fun encodeValue(value: Any): Unit
            = throw SerializationException("Non-serializable ${value::class} is not supported by ${this::class} encoder")

    override fun encodeNull() {
        throw SerializationException("null is not supported")
    }

    override fun encodeUnit() {
        UnitSerializer().serialize(this, Unit)
    }

    override fun encodeBoolean(value: Boolean) = encodeValue(value)
    override fun encodeByte(value: Byte) = encodeValue(value)
    override fun encodeShort(value: Short) = encodeValue(value)
    override fun encodeInt(value: Int) = encodeValue(value)
    override fun encodeLong(value: Long) = encodeValue(value)
    override fun encodeFloat(value: Float) = encodeValue(value)
    override fun encodeDouble(value: Double) = encodeValue(value)
    override fun encodeChar(value: Char) = encodeValue(value)
    override fun encodeString(value: String) = encodeValue(value)

    override fun encodeEnum(enumDescriptor: SerialDescriptor, index: Int) = encodeValue(index)

    // Delegating implementation of CompositeEncoder
    @Deprecated(message = unitDeprecated, level = DeprecationLevel.ERROR)
    final override fun encodeUnitElement(descriptor: SerialDescriptor, index: Int) {
        if (encodeElement(descriptor, index)) {
            @Suppress("DEPRECATION_ERROR")
            encodeUnit()
        }
    }
    final override fun encodeBooleanElement(descriptor: SerialDescriptor, index: Int, value: Boolean) { if (encodeElement(descriptor, index)) encodeBoolean(value) }
    final override fun encodeByteElement(descriptor: SerialDescriptor, index: Int, value: Byte) { if (encodeElement(descriptor, index)) encodeByte(value) }
    final override fun encodeShortElement(descriptor: SerialDescriptor, index: Int, value: Short) { if (encodeElement(descriptor, index)) encodeShort(value) }
    final override fun encodeIntElement(descriptor: SerialDescriptor, index: Int, value: Int) { if (encodeElement(descriptor, index)) encodeInt(value) }
    final override fun encodeLongElement(descriptor: SerialDescriptor, index: Int, value: Long) { if (encodeElement(descriptor, index)) encodeLong(value) }
    final override fun encodeFloatElement(descriptor: SerialDescriptor, index: Int, value: Float) { if (encodeElement(descriptor, index)) encodeFloat(value) }
    final override fun encodeDoubleElement(descriptor: SerialDescriptor, index: Int, value: Double) { if (encodeElement(descriptor, index)) encodeDouble(value) }
    final override fun encodeCharElement(descriptor: SerialDescriptor, index: Int, value: Char) { if (encodeElement(descriptor, index)) encodeChar(value) }
    final override fun encodeStringElement(descriptor: SerialDescriptor, index: Int, value: String) { if (encodeElement(descriptor, index)) encodeString(value) }

    final override fun <T : Any?> encodeSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        if (encodeElement(descriptor, index))
            encodeSerializableValue(serializer, value)
    }
    final override fun <T : Any> encodeNullableSerializableElement(descriptor: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        if (encodeElement(descriptor, index))
            encodeNullableSerializableValue(serializer, value)
    }
}

abstract class ElementValueDecoder : Decoder, CompositeDecoder {
    override val context: SerialModule
        get() = EmptyModule

    override val updateMode: UpdateMode = UpdateMode.UPDATE
    // ------- implementation API -------

    override fun decodeNotNullMark(): Boolean = true
    override fun decodeNull(): Nothing? = null

    open fun decodeValue(): Any = throw SerializationException("${this::class} can't retrieve untyped values")

    @Deprecated(message = unitDeprecated, level = DeprecationLevel.ERROR)
    override fun decodeUnit() {
        UnitSerializer().deserialize(this)
    }

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

    // Delegating implementation of CompositeEncoder

    override fun beginStructure(descriptor: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return this
    }

    override fun endStructure(descriptor: SerialDescriptor) {
    }

    @Deprecated(message = unitDeprecated, level = DeprecationLevel.ERROR)
    @Suppress("DEPRECATION_ERROR")
    final override fun decodeUnitElement(descriptor: SerialDescriptor, index: Int) = decodeUnit()
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
