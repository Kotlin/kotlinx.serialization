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

import kotlinx.serialization.context.SerialContext
import kotlinx.serialization.internal.EnumDescriptor

interface Encoder {
    val context: SerialContext

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

    fun encodeEnum(enumDescription: EnumDescriptor, ordinal: Int)

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
    val context: SerialContext

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


interface Decoder {
    val context: SerialContext

    /**
     * Returns true if the current value in decoder is not null, false otherwise
     */
    fun decodeNotNullMark(): Boolean

    /**
     * Consumes null, returns null, will be called when [decodeNotNullMark] is false
     */
    fun decodeNull(): Nothing?

    fun decodeUnit()
    fun decodeBoolean(): Boolean
    fun decodeByte(): Byte
    fun decodeShort(): Short
    fun decodeInt(): Int
    fun decodeLong(): Long
    fun decodeFloat(): Float
    fun decodeDouble(): Double
    fun decodeChar(): Char
    fun decodeString(): String

    fun decodeEnum(enumDescription: EnumDescriptor): Int


    fun <T : Any?> decodeSerializableValue(deserializer: DeserializationStrategy<T>): T = deserializer.deserialize(this)

    fun <T : Any> decodeNullableSerializableValue(deserializer: DeserializationStrategy<T?>): T? =
        if (decodeNotNullMark()) decodeSerializableValue(deserializer) else decodeNull()

    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder

    val updateMode: UpdateMode

    fun <T> updateSerializableValue(deserializer: DeserializationStrategy<T>, old: T): T {
        return when(updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(deserializer.descriptor.name)
            UpdateMode.OVERWRITE -> decodeSerializableValue(deserializer)
            UpdateMode.UPDATE -> deserializer.patch(this, old)
        }
    }

    fun <T: Any> updateNullableSerializableValue(deserializer: DeserializationStrategy<T?>, old: T?): T? {
        return when {
            updateMode == UpdateMode.BANNED -> throw UpdateNotSupportedException(deserializer.descriptor.name)
            updateMode == UpdateMode.OVERWRITE || old == null -> decodeNullableSerializableValue(deserializer)
            decodeNotNullMark() -> deserializer.patch(this, old)
            else -> decodeNull().let { old }
        }
    }
}

interface CompositeDecoder {
    val context: SerialContext
    fun endStructure(desc: SerialDescriptor) {}

    /**
     * Results of [decodeElementIndex]
     */
    companion object {
        const val READ_DONE = -1
        const val READ_ALL = -2
        const val UNKNOWN_NAME = -3
    }

    /**
     *  Returns either index or one of READ_XXX constants
     */
    fun decodeElementIndex(desc: SerialDescriptor): Int

    /**
     * Optional method to specify collection size to pre-allocate memory.
     * If decoder specifies stream reading ([READ_ALL] is returned from [decodeElementIndex], then
     * correct implementation of this method is mandatory.
     *
     * @return Collection size or -1 if not available.
     */
    fun decodeCollectionSize(desc: SerialDescriptor): Int = -1

    fun decodeUnitElement(desc: SerialDescriptor, index: Int)
    fun decodeBooleanElement(desc: SerialDescriptor, index: Int): Boolean
    fun decodeByteElement(desc: SerialDescriptor, index: Int): Byte
    fun decodeShortElement(desc: SerialDescriptor, index: Int): Short
    fun decodeIntElement(desc: SerialDescriptor, index: Int): Int
    fun decodeLongElement(desc: SerialDescriptor, index: Int): Long
    fun decodeFloatElement(desc: SerialDescriptor, index: Int): Float
    fun decodeDoubleElement(desc: SerialDescriptor, index: Int): Double
    fun decodeCharElement(desc: SerialDescriptor, index: Int): Char
    fun decodeStringElement(desc: SerialDescriptor, index: Int): String

    fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>): T
    fun <T : Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>): T?

    val updateMode: UpdateMode

    fun <T> updateSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T>, old: T): T
    fun <T: Any> updateNullableSerializableElement(desc: SerialDescriptor, index: Int, deserializer: DeserializationStrategy<T?>, old: T?): T?
}
