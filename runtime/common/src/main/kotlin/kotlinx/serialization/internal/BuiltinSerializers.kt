/*
 * Copyright 2017 JetBrains s.r.o.
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

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.KClass

class PrimitiveDesc(override val name: String) : KSerialClassDesc {

    override val kind: KSerialClassKind = KSerialClassKind.PRIMITIVE

    override fun getElementName(index: Int) = throw IllegalStateException("Primitives do not have fields")

    override fun getElementIndex(name: String) = throw IllegalStateException("Primitives do not have fields")
}

object UnitSerializer : KSerializer<Unit> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Unit")

    override fun save(output: KOutput, obj: Unit) = output.writeUnitValue()
    override fun load(input: KInput): Unit = input.readUnitValue()
}

object BooleanSerializer : KSerializer<Boolean> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Boolean")

    override fun save(output: KOutput, obj: Boolean) = output.writeBooleanValue(obj)
    override fun load(input: KInput): Boolean = input.readBooleanValue()
}

object ByteSerializer : KSerializer<Byte> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Byte")

    override fun save(output: KOutput, obj: Byte) = output.writeByteValue(obj)
    override fun load(input: KInput): Byte = input.readByteValue()
}

object ShortSerializer : KSerializer<Short> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Short")

    override fun save(output: KOutput, obj: Short) = output.writeShortValue(obj)
    override fun load(input: KInput): Short = input.readShortValue()
}

object IntSerializer : KSerializer<Int> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Int")

    override fun save(output: KOutput, obj: Int) = output.writeIntValue(obj)
    override fun load(input: KInput): Int = input.readIntValue()
}

object LongSerializer : KSerializer<Long> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Long")

    override fun save(output: KOutput, obj: Long) = output.writeLongValue(obj)
    override fun load(input: KInput): Long = input.readLongValue()
}

object FloatSerializer : KSerializer<Float> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Float")

    override fun save(output: KOutput, obj: Float) = output.writeFloatValue(obj)
    override fun load(input: KInput): Float = input.readFloatValue()
}

object DoubleSerializer : KSerializer<Double> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Double")

    override fun save(output: KOutput, obj: Double) = output.writeDoubleValue(obj)
    override fun load(input: KInput): Double = input.readDoubleValue()
}

object CharSerializer : KSerializer<Char> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.Char")

    override fun save(output: KOutput, obj: Char) = output.writeCharValue(obj)
    override fun load(input: KInput): Char = input.readCharValue()
}

object StringSerializer : KSerializer<String> {
    override val serialClassDesc: KSerialClassDesc = PrimitiveDesc("kotlin.String")

    override fun save(output: KOutput, obj: String) = output.writeStringValue(obj)
    override fun load(input: KInput): String = input.readStringValue()
}


internal class EnumDesc(override val name: String) : KSerialClassDesc {

    override val kind: KSerialClassKind = KSerialClassKind.ENUM

    override fun getElementName(index: Int) = throw IllegalStateException("Primitives does not have fields")

    override fun getElementIndex(name: String) = throw IllegalStateException("Primitives does not have fields")
}

// note, that it is instantiated in a special way
class EnumSerializer<T : Enum<T>>(val serializableClass: KClass<T>) : KSerializer<T> {
    override val serialClassDesc: KSerialClassDesc = EnumDesc(serializableClass.enumClassName())
    override fun save(output: KOutput, obj: T) = output.writeEnumValue(serializableClass, obj)
    override fun load(input: KInput): T = input.readEnumValue(serializableClass)
}

fun <T : Any> makeNullable(element: KSerializer<T>): KSerializer<T?> = NullableSerializer(element)

class NullableSerializer<T : Any>(private val element: KSerializer<T>) : KSerializer<T?> {
    override val serialClassDesc: KSerialClassDesc
        get() = element.serialClassDesc

    override fun save(output: KOutput, obj: T?) {
        if (obj != null) {
            output.writeNotNullMark();
            element.save(output, obj)
        }
        else {
            output.writeNullValue()
        }
    }

    override fun load(input: KInput): T? = if (input.readNotNullMark()) element.load(input) else input.readNullValue()
}

