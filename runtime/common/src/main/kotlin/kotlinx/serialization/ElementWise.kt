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


open class ElementValueOutput : KOutput() {
    // ------- implementation API -------

    // it is always invoked before writeXxxValue
    override fun writeElement(desc: KSerialClassDesc, index: Int): Boolean = true

    // override for a special representation of nulls if needed (empty object by default)
    override fun writeNotNullMark() {}

    override fun writeNonSerializableValue(value: Any) {
        throw SerializationException("\"$value\" has no serializer")
    }

    final override fun writeNullableValue(value: Any?) {
        if (value == null) {
            writeNullValue()
        } else {
            writeNotNullMark()
            writeValue(value)
        }
    }

    override fun writeNullValue() {
        throw SerializationException("null is not supported")
    }
    override fun writeUnitValue() {
        val output = writeBegin(UnitSerializer.serialClassDesc); output.writeEnd(UnitSerializer.serialClassDesc)
    }

    // type-specific value-based output, override for performance and custom type representations
    override fun writeBooleanValue(value: Boolean) = writeValue(value)
    override fun writeByteValue(value: Byte) = writeValue(value)
    override fun writeShortValue(value: Short) = writeValue(value)
    override fun writeIntValue(value: Int) = writeValue(value)
    override fun writeLongValue(value: Long) = writeValue(value)
    override fun writeFloatValue(value: Float) = writeValue(value)
    override fun writeDoubleValue(value: Double) = writeValue(value)
    override fun writeCharValue(value: Char) = writeValue(value)
    override fun writeStringValue(value: String) = writeValue(value)
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("writeEnumValue(value)"))
    final override fun <T : Enum<T>> writeEnumValue(enumClass: KClass<T>, value: T) = writeEnumValue(value)

    override fun <T : Enum<T>> writeEnumValue(value: T) = writeValue(value)
    // -------------------------------------------------------------------------------------

    final override fun writeNonSerializableElementValue(desc: KSerialClassDesc, index: Int, value: Any) { if (writeElement(desc, index)) writeValue(value) }
    final override fun writeNullableElementValue(desc: KSerialClassDesc, index: Int, value: Any?) { if (writeElement(desc, index)) writeNullableValue(value) }
    final override fun writeUnitElementValue(desc: KSerialClassDesc, index: Int) { if (writeElement(desc, index)) writeUnitValue() }
    final override fun writeBooleanElementValue(desc: KSerialClassDesc, index: Int, value: Boolean) { if (writeElement(desc, index)) writeBooleanValue(value) }
    final override fun writeByteElementValue(desc: KSerialClassDesc, index: Int, value: Byte) { if (writeElement(desc, index)) writeByteValue(value) }
    final override fun writeShortElementValue(desc: KSerialClassDesc, index: Int, value: Short) { if (writeElement(desc, index)) writeShortValue(value) }
    final override fun writeIntElementValue(desc: KSerialClassDesc, index: Int, value: Int) { if (writeElement(desc, index)) writeIntValue(value) }
    final override fun writeLongElementValue(desc: KSerialClassDesc, index: Int, value: Long) { if (writeElement(desc, index)) writeLongValue(value) }
    final override fun writeFloatElementValue(desc: KSerialClassDesc, index: Int, value: Float) { if (writeElement(desc, index)) writeFloatValue(value) }
    final override fun writeDoubleElementValue(desc: KSerialClassDesc, index: Int, value: Double) { if (writeElement(desc, index)) writeDoubleValue(value) }
    final override fun writeCharElementValue(desc: KSerialClassDesc, index: Int, value: Char) { if (writeElement(desc, index)) writeCharValue(value) }
    final override fun writeStringElementValue(desc: KSerialClassDesc, index: Int, value: String) { if (writeElement(desc, index)) writeStringValue(value) }
    @Deprecated("Not supported in Native", replaceWith = ReplaceWith("writeEnumElementValue(desc, index, value)"))
    final override fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>, value: T) {
        writeEnumElementValue(desc, index, value)
    }

    final override fun <T : Enum<T>> writeEnumElementValue(desc: KSerialClassDesc, index: Int, value: T) {
        if (writeElement(desc, index)) writeEnumValue(value)
    }


}

open class ElementValueInput : KInput() {
    // ------- implementation API -------

    // unordered read api, override to read props in arbitrary order
    override fun readElement(desc: KSerialClassDesc): Int = READ_ALL

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
        readEnumValue(LegacyEnumLoader(enumClass))

    @Suppress("UNCHECKED_CAST")
    override fun <T : Enum<T>> readEnumValue(enumLoader: EnumLoader<T>): T = readValue() as T

    // -------------------------------------------------------------------------------------

    final override fun readElementValue(desc: KSerialClassDesc, index: Int): Any = readValue()
    final override fun readNullableElementValue(desc: KSerialClassDesc, index: Int): Any? = readNullableValue()
    final override fun readUnitElementValue(desc: KSerialClassDesc, index: Int) = readUnitValue()
    final override fun readBooleanElementValue(desc: KSerialClassDesc, index: Int): Boolean = readBooleanValue()
    final override fun readByteElementValue(desc: KSerialClassDesc, index: Int): Byte = readByteValue()
    final override fun readShortElementValue(desc: KSerialClassDesc, index: Int): Short = readShortValue()
    final override fun readIntElementValue(desc: KSerialClassDesc, index: Int): Int = readIntValue()
    final override fun readLongElementValue(desc: KSerialClassDesc, index: Int): Long = readLongValue()
    final override fun readFloatElementValue(desc: KSerialClassDesc, index: Int): Float = readFloatValue()
    final override fun readDoubleElementValue(desc: KSerialClassDesc, index: Int): Double = readDoubleValue()
    final override fun readCharElementValue(desc: KSerialClassDesc, index: Int): Char = readCharValue()
    final override fun readStringElementValue(desc: KSerialClassDesc, index: Int): String = readStringValue()
    @Deprecated("Not supported in Native", ReplaceWith("readEnumValue(desc, index, loader)"))
    final override fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumClass: KClass<T>): T =
        readEnumElementValue(desc, index, LegacyEnumLoader(enumClass))

    override fun <T : Enum<T>> readEnumElementValue(desc: KSerialClassDesc, index: Int, enumLoader: EnumLoader<T>): T =
        readEnumValue(enumLoader)

    final override fun <T: Any?> readSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T>): T =
        readSerializableValue(loader)

    final override fun <T: Any> readNullableSerializableElementValue(desc: KSerialClassDesc, index: Int, loader: KSerialLoader<T?>): T? =
        readNullableSerializableValue(loader)
}
