/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kotlinx.serialization

import kotlinx.serialization.CompositeDecoder.Companion.READ_ALL
import kotlinx.serialization.context.EmptyContext
import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.internal.EnumDescriptor
import kotlinx.serialization.internal.UnitSerializer

abstract class ElementValueEncoder : Encoder, CompositeEncoder {
    override var context: SerialContext = EmptyContext

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder {
        return this
    }

    /**
     * Always invoked before writing each element to determine if it should be encoded.
     *
     * @return True if value should be encoded, false otherwise
     */
    open fun encodeElement(desc: SerialDescriptor, index: Int): Boolean = true

    /**
     * Encodes that following value is not null.
     * No-op by default.
     */
    override fun encodeNotNullMark() {}

    open fun encodeValue(value: Any): Unit
            = throw SerializationException("Non-serializable ${value::class} is not supported by ${this::class} encoder")

    override fun encodeNull() {
        throw SerializationException("null is not supported")
    }

    override fun encodeUnit() {
        val encoder = beginStructure(UnitSerializer.descriptor); encoder.endStructure(UnitSerializer.descriptor)
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

    override fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int) = encodeValue(ordinal)

    // Delegating implementation of CompositeEncoder

    final override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) { if (encodeElement(desc, index)) encodeValue(value) }
    final override fun encodeUnitElement(desc: SerialDescriptor, index: Int) { if (encodeElement(desc, index)) encodeUnit() }
    final override fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean) { if (encodeElement(desc, index)) encodeBoolean(value) }
    final override fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte) { if (encodeElement(desc, index)) encodeByte(value) }
    final override fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short) { if (encodeElement(desc, index)) encodeShort(value) }
    final override fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int) { if (encodeElement(desc, index)) encodeInt(value) }
    final override fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long) { if (encodeElement(desc, index)) encodeLong(value) }
    final override fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float) { if (encodeElement(desc, index)) encodeFloat(value) }
    final override fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double) { if (encodeElement(desc, index)) encodeDouble(value) }
    final override fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char) { if (encodeElement(desc, index)) encodeChar(value) }
    final override fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String) { if (encodeElement(desc, index)) encodeString(value) }

    final override fun <T : Any?> encodeSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T) {
        if (encodeElement(desc, index))
            encodeSerializableValue(serializer, value)
    }
    final override fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?) {
        if (encodeElement(desc, index))
            encodeNullableSerializableValue(serializer, value)
    }
}

abstract class ElementValueDecoder : Decoder, CompositeDecoder {
    override var context: SerialContext = EmptyContext
    override val updateMode: UpdateMode = UpdateMode.UPDATE
    // ------- implementation API -------

    /**
     * Assumes that all elements go in order by default.
     */
    override fun decodeElementIndex(desc: SerialDescriptor): Int = READ_ALL

    override fun decodeNotNullMark(): Boolean = true
    override fun decodeNull(): Nothing? = null

    open fun decodeValue(): Any = throw SerializationException("${this::class} can't retrieve untyped values")

    override fun decodeUnit() {
        val reader = beginStructure(UnitSerializer.descriptor); reader.endStructure(UnitSerializer.descriptor)
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

    override fun decodeEnum(enumDescription: EnumDescriptor): Int = decodeValue() as Int

    // Delegating implementation of CompositeEncoder

    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder {
        return this
    }

    final override fun decodeUnitElement(desc: SerialDescriptor, index: Int) = decodeUnit()
    final override fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean = decodeBoolean()
    final override fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte = decodeByte()
    final override fun decodeShortElement(desc: SerialDescriptor, index: Int): Short = decodeShort()
    final override fun decodeIntElement(desc: SerialDescriptor, index: Int): Int = decodeInt()
    final override fun decodeLongElement(desc: SerialDescriptor, index: Int): Long = decodeLong()
    final override fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float = decodeFloat()
    final override fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double = decodeDouble()
    final override fun decodeCharElement(desc: SerialDescriptor, index: Int): Char = decodeChar()
    final override fun decodeStringElement(desc: SerialDescriptor, index: Int): String = decodeString()

    final override fun <T: Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T =
        decodeSerializableValue(deserializer)
    final override fun <T: Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T? =
        decodeNullableSerializableValue(deserializer)
    final override fun <T> updateSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T =
        updateSerializableValue(deserializer, old)
    final override fun <T: Any> updateNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T? =
        updateNullableSerializableValue(deserializer, old)
}
