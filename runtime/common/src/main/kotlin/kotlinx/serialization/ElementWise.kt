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

import kotlinx.serialization.internal.UnitSerializer
import kotlin.reflect.KClass


open class ElementValueOutput : StructureEncoder {

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

open class ElementValueInput : KInput() {
    // ------- implementation API -------

    // unordered read api, override to read props in arbitrary order
    override fun readElement(desc: SerialDescriptor): Int = READ_ALL

    // returns true if the following value is not null, false if not null
    override fun readNotNullMark(): Boolean = true
    override fun readNullValue(): Nothing? = null

    override fun readValue(): Any {
        throw SerializationException("Any type is not supported")
    }
    override fun readNullableValue(): Any? = if (readNotNullMark()) readValue() else readNullValue()
    override fun readUnitValue() {
        val reader = readBegin(UnitSerializer.serialClassDesc); reader.readEnd(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based input, override for performance and custom type representations
    override fun readBooleanValue(): Boolean = readValue() as Boolean
    override fun readByteValue(): Byte = readValue() as Byte
    override fun readShortValue(): Short = readValue() as Short
    override fun readIntValue(): Int = readValue() as Int
    override fun readLongValue(): Long = readValue() as Long
    override fun readFloatValue(): Float = readValue() as Float
    override fun readDoubleValue(): Double = readValue() as Double
    override fun readCharValue(): Char = readValue() as Char
    override fun readStringValue(): String = readValue() as String

    @Deprecated("Not supported in Native", ReplaceWith("readEnumValue(enumLoader)"))
    @Suppress("UNCHECKED_CAST")
    final override fun <T : Enum<T>> readEnumValue(enumClass: KClass<T>): T =
        readEnumValue(LegacyEnumCreator(enumClass))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Enum<T>> readEnumValue(enumCreator: EnumCreator<T>): T = readValue() as T

    // -------------------------------------------------------------------------------------

    final override fun readElementValue(desc: SerialDescriptor, index: Int): Any = readValue()
    final override fun readNullableElementValue(desc: SerialDescriptor, index: Int): Any? = readNullableValue()
    final override fun readUnitElementValue(desc: SerialDescriptor, index: Int) = readUnitValue()
    final override fun readBooleanElementValue(desc: SerialDescriptor, index: Int): Boolean = readBooleanValue()
    final override fun readByteElementValue(desc: SerialDescriptor, index: Int): Byte = readByteValue()
    final override fun readShortElementValue(desc: SerialDescriptor, index: Int): Short = readShortValue()
    final override fun readIntElementValue(desc: SerialDescriptor, index: Int): Int = readIntValue()
    final override fun readLongElementValue(desc: SerialDescriptor, index: Int): Long = readLongValue()
    final override fun readFloatElementValue(desc: SerialDescriptor, index: Int): Float = readFloatValue()
    final override fun readDoubleElementValue(desc: SerialDescriptor, index: Int): Double = readDoubleValue()
    final override fun readCharElementValue(desc: SerialDescriptor, index: Int): Char = readCharValue()
    final override fun readStringElementValue(desc: SerialDescriptor, index: Int): String = readStringValue()
    @Deprecated("Not supported in Native", ReplaceWith("readEnumValue(desc, index, creator)"))
    final override fun <T : Enum<T>> readEnumElementValue(desc: SerialDescriptor, index: Int, enumClass: KClass<T>): T =
        readEnumElementValue(desc, index, LegacyEnumCreator(enumClass))

    override fun <T : Enum<T>> readEnumElementValue(desc: SerialDescriptor, index: Int, enumCreator: EnumCreator<T>): T =
        readEnumValue(enumCreator)

    final override fun <T: Any?> readSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T>): T =
        readSerializableValue(loader)

    final override fun <T: Any> readNullableSerializableElementValue(desc: SerialDescriptor, index: Int, loader: DeserializationStrategy<T?>): T? =
        readNullableSerializableValue(loader)
}
