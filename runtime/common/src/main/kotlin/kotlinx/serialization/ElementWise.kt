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

import kotlinx.serialization.StructureDecoder.Companion.READ_ALL
import kotlinx.serialization.internal.UnitSerializer
import kotlin.reflect.KClass

abstract class ElementValueEncoder : StructureEncoder {

    override var context: SerialContext? = null
    // ------- implementation API -------

    // it is always invoked before writeXxxValue
    override fun encodeElement(desc: SerialDescriptor, index: Int): Boolean = true

    // override for a special representation of nulls if needed (empty object by default)
    override fun encodeNotNullMark() {}

    override fun encodeAnyValue(value: Any) {
        throw SerializationException("\"$value\" has no serializer")
    }

    final override fun encodeNullableValue(value: Any?) {
        if (value == null) {
            encodeNullValue()
        } else {
            encodeNotNullMark()
            encodeValue(value)
        }
    }

    override fun encodeNullValue() {
        throw SerializationException("null is not supported")
    }

    override fun encodeUnitValue() {
        val output = beginStructure(UnitSerializer.serialClassDesc); output.endStructure(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based output, override for performance and custom type representations
    override fun encodeBooleanValue(value: Boolean) = encodeValue(value)
    override fun encodeByteValue(value: Byte) = encodeValue(value)
    override fun encodeShortValue(value: Short) = encodeValue(value)
    override fun encodeIntValue(value: Int) = encodeValue(value)
    override fun encodeLongValue(value: Long) = encodeValue(value)
    override fun encodeFloatValue(value: Float) = encodeValue(value)
    override fun encodeDoubleValue(value: Double) = encodeValue(value)
    override fun encodeCharValue(value: Char) = encodeValue(value)
    override fun encodeStringValue(value: String) = encodeValue(value)
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumValue(value)"))
    final override fun <T : Enum<T>> encodeEnumValue(enumClass: KClass<T>, value: T) = encodeEnumValue(value)

    override fun <T : Enum<T>> encodeEnumValue(value: T) = encodeValue(value)
    // -------------------------------------------------------------------------------------

    final override fun encodeNonSerializableElementValue(desc: SerialDescriptor, index: Int, value: Any) { if (encodeElement(desc, index)) encodeValue(value) }
    final override fun encodeNullableElementValue(desc: SerialDescriptor, index: Int, value: Any?) { if (encodeElement(desc, index)) encodeNullableValue(value) }
    final override fun encodeUnitElementValue(desc: SerialDescriptor, index: Int) { if (encodeElement(desc, index)) encodeUnitValue() }
    final override fun encodeBooleanElementValue(desc: SerialDescriptor, index: Int, value: Boolean) { if (encodeElement(desc, index)) encodeBooleanValue(value) }
    final override fun encodeByteElementValue(desc: SerialDescriptor, index: Int, value: Byte) { if (encodeElement(desc, index)) encodeByteValue(value) }
    final override fun encodeShortElementValue(desc: SerialDescriptor, index: Int, value: Short) { if (encodeElement(desc, index)) encodeShortValue(value) }
    final override fun encodeIntElementValue(desc: SerialDescriptor, index: Int, value: Int) { if (encodeElement(desc, index)) encodeIntValue(value) }
    final override fun encodeLongElementValue(desc: SerialDescriptor, index: Int, value: Long) { if (encodeElement(desc, index)) encodeLongValue(value) }
    final override fun encodeFloatElementValue(desc: SerialDescriptor, index: Int, value: Float) { if (encodeElement(desc, index)) encodeFloatValue(value) }
    final override fun encodeDoubleElementValue(desc: SerialDescriptor, index: Int, value: Double) { if (encodeElement(desc, index)) encodeDoubleValue(value) }
    final override fun encodeCharElementValue(desc: SerialDescriptor, index: Int, value: Char) { if (encodeElement(desc, index)) encodeCharValue(value) }
    final override fun encodeStringElementValue(desc: SerialDescriptor, index: Int, value: String) { if (encodeElement(desc, index)) encodeStringValue(value) }
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("encodeEnumElementValue(desc, index, value)"))
    final override fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>, value: T) {
        encodeEnumElementValue(desc, index, value)
    }

    final override fun <T : Enum<T>> encodeEnumElementValue(desc: SerialDescriptor, index: Int, value: T) {
        if (encodeElement(desc, index)) encodeEnumValue(value)
    }
}

abstract class ElementValueDecoder : StructureDecoder {
    override var context: SerialContext? = null
    override val updateMode: UpdateMode = UpdateMode.UPDATE
    // ------- implementation API -------

    // unordered read api, override to read props in arbitrary order
    override fun decodeElement(desc: SerialDescriptor): Int = READ_ALL

    // returns true if the following value is not null, false if not null
    override fun decodeNotNullMark(): Boolean = true
    override fun decodeNullValue(): Nothing? = null

    override fun decodeAnyValue(): Any {
        throw SerializationException("Any type is not supported")
    }

    override fun decodeNullableValue(): Any? = if (decodeNotNullMark()) decodeAnyValue() else decodeNullValue()
    override fun decodeUnitValue() {
        val reader = beginStructure(UnitSerializer.serialClassDesc); reader.endStructure(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based input, override for performance and custom type representations
    override fun decodeBooleanValue(): Boolean = decodeAnyValue() as Boolean
    override fun decodeByteValue(): Byte = decodeAnyValue() as Byte
    override fun decodeShortValue(): Short = decodeAnyValue() as Short
    override fun decodeIntValue(): Int = decodeAnyValue() as Int
    override fun decodeLongValue(): Long = decodeAnyValue() as Long
    override fun decodeFloatValue(): Float = decodeAnyValue() as Float
    override fun decodeDoubleValue(): Double = decodeAnyValue() as Double
    override fun decodeCharValue(): Char = decodeAnyValue() as Char
    override fun decodeStringValue(): String = decodeAnyValue() as String

    @Deprecated("Not supported in Native", ReplaceWith("decodeEnumValue(enumLoader)"))
    @Suppress("UNCHECKED_CAST")
    final override fun <T : Enum<T>> decodeEnumValue(enumClass: KClass<T>): T =
        decodeEnumValue(LegacyEnumCreator(enumClass))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Enum<T>> decodeEnumValue(enumCreator: EnumCreator<T>): T = decodeAnyValue() as T

    // -------------------------------------------------------------------------------------

    final override fun decodeAnyElementValue(desc: SerialDescriptor, index: Int): Any = decodeAnyValue()
    final override fun decodeNullableElementValue(desc: SerialDescriptor, index: Int): Any? = decodeNullableValue()
    final override fun decodeUnitElementValue(desc: SerialDescriptor, index: Int) = decodeUnitValue()
    final override fun decodeBooleanElementValue(desc: SerialDescriptor, index: Int): Boolean = decodeBooleanValue()
    final override fun decodeByteElementValue(desc: SerialDescriptor, index: Int): Byte = decodeByteValue()
    final override fun decodeShortElementValue(desc: SerialDescriptor, index: Int): Short = decodeShortValue()
    final override fun decodeIntElementValue(desc: SerialDescriptor, index: Int): Int = decodeIntValue()
    final override fun decodeLongElementValue(desc: SerialDescriptor, index: Int): Long = decodeLongValue()
    final override fun decodeFloatElementValue(desc: SerialDescriptor, index: Int): Float = decodeFloatValue()
    final override fun decodeDoubleElementValue(desc: SerialDescriptor, index: Int): Double = decodeDoubleValue()
    final override fun decodeCharElementValue(desc: SerialDescriptor, index: Int): Char = decodeCharValue()
    final override fun decodeStringElementValue(desc: SerialDescriptor, index: Int): String = decodeStringValue()
    @Deprecated("Not supported in Native", ReplaceWith("decodeEnumValue(desc, index, creator)"))
    final override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T =
        decodeEnumElementValue(desc, index, LegacyEnumCreator(enumClass))

    final override fun <T : Enum<T>> decodeEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T =
        decodeEnumValue(enumCreator)

    final override fun <T: Any?> decodeSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T =
        decodeSerializableValue(loader)

    final override fun <T: Any> decodeNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T? =
        decodeNullableSerializableValue(loader)
}
