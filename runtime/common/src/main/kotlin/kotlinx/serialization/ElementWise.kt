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
import kotlinx.serialization.internal.UnitSerializer
import kotlin.reflect.KClass

open class ElementValueOutput : CompositeEncoder {

    override var context: SerialContext? = null
    // ------- implementation API -------

    // it is always invoked before writeXxxValue
    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean = true

    // override for a special representation of nulls if needed (empty object by default)
    override fun encodeNotNullMark() {}

    override fun encodeNonSerializableValue(value: Any) {
        throw SerializationException("\"$value\" has no serializer")
    }

    final override fun encodeNullableValue(value: Any?) {
        if (value == null) {
            encodeNull()
        } else {
            encodeNotNullMark()
            encodeValue(value)
        }
    }

    override fun encodeNull() {
        throw SerializationException("null is not supported")
    }

    override fun encodeUnit() {
        val output = beginStructure(UnitSerializer.serialClassDesc); output.endStructure(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based output, override for performance and custom type representations
    override fun encodeBoolean(value: Boolean) = encodeValue(value)
    override fun encodeByte(value: Byte) = encodeValue(value)
    override fun encodeShort(value: Short) = encodeValue(value)
    override fun encodeInt(value: Int) = encodeValue(value)
    override fun encodeLong(value: Long) = encodeValue(value)
    override fun encodeFloat(value: Float) = encodeValue(value)
    override fun encodeDouble(value: Double) = encodeValue(value)
    override fun encodeChar(value: Char) = encodeValue(value)
    override fun encodeString(value: String) = encodeValue(value)
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnum(value)"))
    final override fun <T : Enum<T>> encodeEnum(enumClass: KClass<T>, value: T) = encodeEnum(value)

    override fun <T : Enum<T>> encodeEnum(value: T) = encodeValue(value)
    // -------------------------------------------------------------------------------------

    final override fun encodeNonSerializableElement(desc: SerialDescriptor, index: Int, value: Any) { if (encodeElement(desc, index)) encodeValue(value) }
    final override fun encodeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?) { if (encodeElement(desc, index)) encodeNullableValue(value) }
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
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumElement(desc, index, value)"))
    final override fun <T : Enum<T>> encodeEnumElement(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T) {
        encodeEnumElement(desc, index, value)
    }

    final override fun <T : Enum<T>> encodeEnumElement(desc: SerialDescriptor, index: Int, value: T) {
        if (encodeElement(desc, index)) encodeEnum(value)
    }
}

open class ElementValueInput : CompositeDecoder {
    override var context: SerialContext? = null
    override val updateMode: UpdateMode = UpdateMode.UPDATE
    // ------- implementation API -------

    // unordered read api, override to read props in arbitrary order
    override fun decodeElementIndex(desc: SerialDescriptor): Int = READ_ALL

    // returns true if the following value is not null, false if not null
    override fun decodeNotNullMark(): Boolean = true
    override fun decodeNull(): Nothing? = null

    override fun decodeValue(): Any {
        throw SerializationException("Any type is not supported")
    }
    override fun decodeNullableValue(): Any? = if (decodeNotNullMark()) decodeValue() else decodeNull()
    override fun decodeUnit() {
        val reader = beginStructure(UnitSerializer.serialClassDesc); reader.endStructure(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based input, override for performance and custom type representations
    override fun decodeBoolean(): Boolean = decodeValue() as Boolean
    override fun decodeByte(): Byte = decodeValue() as Byte
    override fun decodeShort(): Short = decodeValue() as Short
    override fun decodeInt(): Int = decodeValue() as Int
    override fun decodeLong(): Long = decodeValue() as Long
    override fun decodeFloat(): Float = decodeValue() as Float
    override fun decodeDouble(): Double = decodeValue() as Double
    override fun decodeChar(): Char = decodeValue() as Char
    override fun decodeString(): String = decodeValue() as String

    @Deprecated("Not supported in Native", ReplaceWith("decodeEnum(enumLoader)"))
    @Suppress("UNCHECKED_CAST")
    final override fun <T : Enum<T>> decodeEnum(enumClass: KClass<T>): T =
        decodeEnum(LegacyEnumCreator(enumClass))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Enum<T>> decodeEnum(enumCreator: EnumCreator<T>): T = decodeValue() as T

    // -------------------------------------------------------------------------------------

    final override fun decodeElementValue(desc: SerialDescriptor, index: Int): Any = decodeValue()
    final override fun decodeNullableElementValue(desc: SerialDescriptor, index: Int): Any? = decodeNullableValue()
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
    @Deprecated("Not supported in Native", ReplaceWith("decodeEnum(desc, index, creator)"))
    final override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T =
        decodeEnumElementValue(desc, index, LegacyEnumCreator(enumClass))

    override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T =
        decodeEnum(enumCreator)

    final override fun <T: Any?> decodeSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T =
        decodeSerializableValue(loader)

    final override fun <T: Any> decodeNullableSerializableElement(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T? =
        decodeNullableSerializableValue(loader)
}
