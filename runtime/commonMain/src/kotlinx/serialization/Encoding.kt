/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.modules.*

interface Encoder {
    val context: SerialModule

    fun encodeNotNullMark()
    fun encodeNull()

    fun encodeUnit()
    fun encodeBoolean(value: Boolean)
    fun encodeByte(value: Byte)
    fun encodeShort(value: Short)
    fun encodeInt(value: Int)
    fun encodeLong(value: Long)
    fun encodeFloat(value: Float)
    fun encodeDouble(value: Double)
    fun encodeChar(value: Char)
    fun encodeString(value: String)

    fun encodeEnum(enumDescription: SerialDescriptor, ordinal: Int)

    fun <T : Any?> encodeSerializableValue(serializer: SerializationStrategy<T>, value: T) {
        serializer.serialize(this, value)
    }

    fun <T : Any> encodeNullableSerializableValue(serializer: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            encodeNull()
        } else {
            encodeNotNullMark()
            encodeSerializableValue(serializer, value)
        }
    }

    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder

    fun beginCollection(desc: SerialDescriptor, collectionSize: Int, vararg typeParams: KSerializer<*>) =
        beginStructure(desc, *typeParams)
}

interface CompositeEncoder {
    val context: SerialModule

    fun endStructure(desc: SerialDescriptor) {}

    fun shouldEncodeElementDefault(desc: SerialDescriptor, index: Int): Boolean = true

    fun encodeUnitElement(desc: SerialDescriptor, index: Int)
    fun encodeBooleanElement(desc: SerialDescriptor, index: Int, value: Boolean)
    fun encodeByteElement(desc: SerialDescriptor, index: Int, value: Byte)
    fun encodeShortElement(desc: SerialDescriptor, index: Int, value: Short)
    fun encodeIntElement(desc: SerialDescriptor, index: Int, value: Int)
    fun encodeLongElement(desc: SerialDescriptor, index: Int, value: Long)
    fun encodeFloatElement(desc: SerialDescriptor, index: Int, value: Float)
    fun encodeDoubleElement(desc: SerialDescriptor, index: Int, value: Double)
    fun encodeCharElement(desc: SerialDescriptor, index: Int, value: Char)
    fun encodeStringElement(desc: SerialDescriptor, index: Int, value: String)

    fun <T : Any?> encodeSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T)

    fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any)

    fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, serializer: SerializationStrategy<T>, value: T?)
}



