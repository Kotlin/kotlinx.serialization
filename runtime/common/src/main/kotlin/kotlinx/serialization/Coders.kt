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

import kotlin.reflect.KClass

interface Encoder {
    var context: SerialContext?

    @Deprecated("Untyped encoding will be removed")
    fun encodeValue(value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) encodeSerializableValue(s, value)
        else encodeNonSerializableValue(value)
    }

    @Deprecated("Untyped encoding will be removed")
    fun encodeNonSerializableValue(value: Any)

    fun encodeNotNullMark()
    fun encodeNull()

    @Deprecated("Untyped encoding will be removed")
    fun encodeNullableValue(value: Any?)

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

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnum(value)"))
    fun <T : Enum<T>> encodeEnum(enumClass: KClass<T>, value: T)

    fun <T : Enum<T>> encodeEnum(value: T)

    fun <T : Any?> encodeSerializableValue(saver: SerializationStrategy<T>, value: T) {
        saver.serialize(this, value)
    }

    fun <T : Any> encodeNullableSerializableValue(saver: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            encodeNull()
        } else {
            encodeNotNullMark()
            encodeSerializableValue(saver, value)
        }
    }

    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeEncoder

    fun beginCollection(desc: SerialDescriptor, collectionSize: Int, vararg typeParams: KSerializer<*>) =
        beginStructure(desc, *typeParams)
}

interface CompositeEncoder {
    var context: SerialContext?

    fun endStructure(desc: SerialDescriptor) {}

    @Deprecated("Untyped encoding will be removed")
    fun encodeElementValue(desc: SerialDescriptor, index: Int, value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) encodeSerializableElement(desc, index, s, value)
        else encodeNonSerializableElement(desc, index, value)
    }

    @Deprecated("Untyped encoding will be removed")
    fun encodeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?)

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

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumElement(desc, index, value)"))
    fun <T : Enum<T>> encodeEnumElement(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T)
    fun <T : Enum<T>> encodeEnumElement(desc: SerialDescriptor, index: Int, value: T)

    fun <T : Any?> encodeSerializableElement(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T)

    fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any)

    fun <T : Any> encodeNullableSerializableElement(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T?)
}


interface Decoder {
    var context: SerialContext?

    /**
     * Returns true if the current value in decoder is not null, false otherwise
     */
    fun decodeNotNullMark(): Boolean

    /**
     * Consumes null, returns null, will be called when [decodeNotNullMark] is false
     */
    fun decodeNull(): Nothing?

    @Deprecated("Untyped decoding will be removed")
    fun decodeValue(): Any

    @Deprecated("Untyped decoding will be removed")
    fun <T : Any> decodeValue(klass: KClass<T>): T {
        val s = context?.getSerializerByClass(klass)
        @Suppress("UNCHECKED_CAST")
        return if (s != null)
            decodeSerializableValue(s)
        else
            decodeValue() as T
    }

    @Deprecated("Untyped decoding will be removed")
    fun decodeNullableValue(): Any?

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

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnum(enumCreator)"))
    fun <T : Enum<T>> decodeEnum(enumClass: KClass<T>): T

    fun <T : Enum<T>> decodeEnum(enumCreator: EnumCreator<T>): T


    fun <T : Any?> decodeSerializableValue(loader: DeserializationStrategy<T>): T = loader.deserialize(this)

    fun <T : Any> decodeNullableSerializableValue(loader: DeserializationStrategy<T?>): T? =
        if (decodeNotNullMark()) decodeSerializableValue(loader) else decodeNull()

    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): CompositeDecoder

    val updateMode: UpdateMode

    fun <T> updateSerializableValue(loader: DeserializationStrategy<T>, old: T): T {
        return when(updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(loader.descriptor.name)
            UpdateMode.OVERWRITE -> decodeSerializableValue(loader)
            UpdateMode.UPDATE -> loader.patch(this, old)
        }
    }

    fun <T: Any> updateNullableSerializableValue(loader: DeserializationStrategy<T?>, old: T?): T? {
        return when {
            updateMode == UpdateMode.BANNED -> throw UpdateNotSupportedException(loader.descriptor.name)
            updateMode == UpdateMode.OVERWRITE || old == null -> decodeNullableSerializableValue(loader)
            decodeNotNullMark() -> loader.patch(this, old)
            else -> decodeNull().let { old }
        }
    }
}

interface CompositeDecoder {
    var context: SerialContext?
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

    @Deprecated("Untyped decoding will be removed")
    fun decodeElementValue(desc: SerialDescriptor, index: Int): Any

    @Deprecated("Untyped decoding will be removed")
    fun decodeElementValue(desc: SerialDescriptor, index: Int, klass: KClass<*>): Any {
        val s = context?.getSerializerByClass(klass)
        return if (s != null) decodeSerializableElement(desc, index, s)
        else decodeElementValue(desc, index)
    }

    @Deprecated("Untyped decoding will be removed")
    fun decodeNullableElementValue(desc: SerialDescriptor, index: Int): Any?

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

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnum(desc, index, enumLoader)"))
    fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T

    fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T

    fun <T : Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T
    fun <T : Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T?

    val updateMode: UpdateMode

    fun <T> updateSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>, old: T): T
    fun <T: Any> updateNullableSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>, old: T?): T?
}
