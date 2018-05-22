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

    fun encodeValue(value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) encodeSerializableValue(s, value)
        else encodeAnyValue(value)
    }

    fun encodeAnyValue(value: Any)

    fun encodeNotNullMark()
    fun encodeNullValue()

    fun encodeNullableValue(value: Any?)
    fun encodeUnitValue()
    fun encodeBooleanValue(value: Boolean)
    fun encodeByteValue(value: Byte)
    fun encodeShortValue(value: Short)
    fun encodeIntValue(value: Int)
    fun encodeLongValue(value: Long)
    fun encodeFloatValue(value: Float)
    fun encodeDoubleValue(value: Double)
    fun encodeCharValue(value: Char)
    fun encodeStringValue(value: String)

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumValue(value)"))
    fun <T : Enum<T>> encodeEnumValue(enumClass: KClass<T>, value: T)

    fun <T : Enum<T>> encodeEnumValue(value: T)

    fun <T : Any?> encodeSerializableValue(saver: SerializationStrategy<T>, value: T) {
        saver.serialize(this, value)
    }

    fun <T : Any> encodeNullableSerializableValue(saver: SerializationStrategy<T>, value: T?) {
        if (value == null) {
            encodeNullValue()
        } else {
            encodeNotNullMark()
            encodeSerializableValue(saver, value)
        }
    }

    // composite value delimiter api (endStructure ends composite object)
    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder

    fun beginCollection(desc: SerialDescriptor, collectionSize: Int, vararg typeParams: KSerializer<*>) =
        beginStructure(desc, *typeParams)
}

interface StructureEncoder: Encoder {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureEncoder = this
    fun endStructure(desc: SerialDescriptor) {}

    // it is always invoked before encodeSerializableValue, shall return false if no need to encode (skip this value)
    // todo: move down to elementwise?
    fun encodeElement(desc: SerialDescriptor, index: Int): Boolean

    fun encodeElementValue(desc: SerialDescriptor, index: Int, value: Any) {
        val s = context?.getSerializerByValue(value)
        if (s != null) encodeSerializableElementValue(desc, index, s, value)
        else encodeNonSerializableElementValue(desc, index, value)
    }

    fun encodeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?)

    fun encodeUnitElementValue(desc: SerialDescriptor, index: Int)
    fun encodeBooleanElementValue(desc: SerialDescriptor, index: Int, value: Boolean)
    fun encodeByteElementValue(desc: SerialDescriptor, index: Int, value: Byte)
    fun encodeShortElementValue(desc: SerialDescriptor, index: Int, value: Short)
    fun encodeIntElementValue(desc: SerialDescriptor, index: Int, value: Int)
    fun encodeLongElementValue(desc: SerialDescriptor, index: Int, value: Long)
    fun encodeFloatElementValue(desc: SerialDescriptor, index: Int, value: Float)
    fun encodeDoubleElementValue(desc: SerialDescriptor, index: Int, value: Double)
    fun encodeCharElementValue(desc: SerialDescriptor, index: Int, value: Char)
    fun encodeStringElementValue(desc: SerialDescriptor, index: Int, value: String)

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumElementValue(desc, index, value)"))
    fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T)
    fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, value: T)

    fun <T : Any?> encodeSerializableElementValue(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T) {
        if (encodeElement(desc, index))
            encodeSerializableValue(saver, value)
    }

    fun encodeNonSerializableElementValue(desc: SerialDescriptor, index: Int, value: Any)

    fun <T : Any> encodeNullableSerializableElementValue(desc: SerialDescriptor, index: Int, saver: SerializationStrategy<T>, value: T?) {
        if (encodeElement(desc, index))
            encodeNullableSerializableValue(saver, value)
    }
}


interface Decoder {
    var context: SerialContext?

    // returns true if the following value is not null, false if not null
    fun decodeNotNullMark(): Boolean

    fun decodeNullValue(): Nothing? // consumes null, returns null, will be called when decodeNotNullMark() is false

    // todo: almost impossible to implement, should it be here?
    fun decodeAnyValue(): Any

    fun <T : Any> decodeValue(klass: KClass<T>): T {
        val s = context?.getSerializerByClass(klass)
        @Suppress("UNCHECKED_CAST")
        return if (s != null)
            decodeSerializableValue(s)
        else
            decodeAnyValue() as T
    }

    fun decodeNullableValue(): Any?
    fun decodeUnitValue()
    fun decodeBooleanValue(): Boolean
    fun decodeByteValue(): Byte
    fun decodeShortValue(): Short
    fun decodeIntValue(): Int
    fun decodeLongValue(): Long
    fun decodeFloatValue(): Float
    fun decodeDoubleValue(): Double
    fun decodeCharValue(): Char
    fun decodeStringValue(): String

    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnumValue(enumCreator)"))
    fun <T : Enum<T>> decodeEnumValue(enumClass: KClass<T>): T

    fun <T : Enum<T>> decodeEnumValue(enumCreator: EnumCreator<T>): T


    fun <T : Any?> decodeSerializableValue(loader: DeserializationStrategy<T>): T = loader.deserialize(this)

    fun <T : Any> decodeNullableSerializableValue(loader: DeserializationStrategy<T?>): T? =
        if (decodeNotNullMark()) decodeSerializableValue(loader) else decodeNullValue()

    // -------------------------------------------------------------------------------------
    // methods below this line are invoked by compiler-generated KSerializer implementation

    // composite value delimiter api (endStructure ends composite object)
    fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureDecoder
}

interface StructureDecoder: Decoder {
    override fun beginStructure(desc: SerialDescriptor, vararg typeParams: KSerializer<*>): StructureDecoder = this
    fun endStructure(desc: SerialDescriptor) {}

    // decodeElement results
    companion object {
        const val READ_DONE = -1
        const val READ_ALL = -2
        const val UNKNOWN_NAME = -3
    }

    // returns either index or one of READ_XXX constants
    fun decodeElement(desc: SerialDescriptor): Int

    fun decodeAnyElementValue(desc: SerialDescriptor, index: Int): Any

    fun decodeElementValue(desc: SerialDescriptor, index: Int, klass: KClass<*>): Any {
        val s = context?.getSerializerByClass(klass)
        return if (s != null) decodeSerializableElementValue(desc, index, s)
        else decodeAnyElementValue(desc, index)
    }

    fun decodeNullableElementValue(desc: SerialDescriptor, index: Int): Any?
    fun decodeUnitElementValue(desc: SerialDescriptor, index: Int)
    fun decodeBooleanElementValue(desc: SerialDescriptor, index: Int): Boolean
    fun decodeByteElementValue(desc: SerialDescriptor, index: Int): Byte
    fun decodeShortElementValue(desc: SerialDescriptor, index: Int): Short
    fun decodeIntElementValue(desc: SerialDescriptor, index: Int): Int
    fun decodeLongElementValue(desc: SerialDescriptor, index: Int): Long
    fun decodeFloatElementValue(desc: SerialDescriptor, index: Int): Float
    fun decodeDoubleElementValue(desc: SerialDescriptor, index: Int): Double
    fun decodeCharElementValue(desc: SerialDescriptor, index: Int): Char
    fun decodeStringElementValue(desc: SerialDescriptor, index: Int): String
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("decodeEnumValue(desc, index, enumLoader)"))
    fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T

    fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T


    fun <T : Any?> decodeSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T
    fun <T : Any> decodeNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T?

    val updateMode: UpdateMode

    fun <T> updateSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>, old: T): T {
        return updateSerializableValue(loader, desc, old)
    }

    fun <T: Any> updateNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>, old: T?): T? {
        return updateNullableSerializableValue(loader, desc, old)
    }

    fun <T> updateSerializableValue(loader: DeserializationStrategy<T>, desc: SerialDescriptor, old: T): T {
        return when(updateMode) {
            UpdateMode.BANNED -> throw UpdateNotSupportedException(desc.name)
            UpdateMode.OVERWRITE -> decodeSerializableValue(loader)
            UpdateMode.UPDATE -> loader.patch(this, old)
        }
    }

    fun <T: Any> updateNullableSerializableValue(loader: DeserializationStrategy<T?>, desc: SerialDescriptor, old: T?): T? {
        return when {
            updateMode == UpdateMode.BANNED -> throw UpdateNotSupportedException(desc.name)
            updateMode == UpdateMode.OVERWRITE || old == null -> decodeNullableSerializableValue(loader)
            decodeNotNullMark() -> loader.patch(this, old)
            else -> decodeNullValue().let { old }
        }
    }
}
