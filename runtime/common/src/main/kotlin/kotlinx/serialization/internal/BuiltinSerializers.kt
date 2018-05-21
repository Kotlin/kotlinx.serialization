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

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.KClass

class PrimitiveDesc(override val name: String) : SerialDescriptor {

    override val kind: SerialKind = SerialKind.PRIMITIVE

    override fun getElementName(index: Int) = throw IllegalStateException("Primitives do not have fields")

    override fun getElementIndex(name: String) = throw IllegalStateException("Primitives do not have fields")
}

object UnitSerializer : KSerializer<Unit> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Unit")

    override fun serialize(output: KOutput, obj: Unit) = output.writeUnitValue()
    override fun deserialize(input: KInput): Unit = input.readUnitValue()
}

object BooleanSerializer : KSerializer<Boolean> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Boolean")

    override fun serialize(output: KOutput, obj: Boolean) = output.writeBooleanValue(obj)
    override fun deserialize(input: KInput): Boolean = input.readBooleanValue()
}

object ByteSerializer : KSerializer<Byte> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Byte")

    override fun serialize(output: KOutput, obj: Byte) = output.writeByteValue(obj)
    override fun deserialize(input: KInput): Byte = input.readByteValue()
}

object ShortSerializer : KSerializer<Short> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Short")

    override fun serialize(output: KOutput, obj: Short) = output.writeShortValue(obj)
    override fun deserialize(input: KInput): Short = input.readShortValue()
}

object IntSerializer : KSerializer<Int> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Int")

    override fun serialize(output: KOutput, obj: Int) = output.writeIntValue(obj)
    override fun deserialize(input: KInput): Int = input.readIntValue()
}

object LongSerializer : KSerializer<Long> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Long")

    override fun serialize(output: KOutput, obj: Long) = output.writeLongValue(obj)
    override fun deserialize(input: KInput): Long = input.readLongValue()
}

object FloatSerializer : KSerializer<Float> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Float")

    override fun serialize(output: KOutput, obj: Float) = output.writeFloatValue(obj)
    override fun deserialize(input: KInput): Float = input.readFloatValue()
}

object DoubleSerializer : KSerializer<Double> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Double")

    override fun serialize(output: KOutput, obj: Double) = output.writeDoubleValue(obj)
    override fun deserialize(input: KInput): Double = input.readDoubleValue()
}

object CharSerializer : KSerializer<Char> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.Char")

    override fun serialize(output: KOutput, obj: Char) = output.writeCharValue(obj)
    override fun deserialize(input: KInput): Char = input.readCharValue()
}

object StringSerializer : KSerializer<String> {
    override val serialClassDesc: SerialDescriptor = PrimitiveDesc("kotlin.String")

    override fun serialize(output: KOutput, obj: String) = output.writeStringValue(obj)
    override fun deserialize(input: KInput): String = input.readStringValue()
}


internal class EnumDesc(override val name: String) : SerialDescriptor {

    override val kind: SerialKind = SerialKind.KIND_ENUM

    override fun getElementName(index: Int) = throw IllegalStateException("Primitives does not have fields")

    override fun getElementIndex(name: String) = throw IllegalStateException("Primitives does not have fields")
}

// note, that it is instantiated in a special way
@Deprecated("Not supported in Native", replaceWith = ReplaceWith("ModernEnumSerializer()"))
class EnumSerializer<T : Enum<T>>(val serializableClass: KClass<T>) : KSerializer<T> {
    override val serialClassDesc: SerialDescriptor = EnumDesc(serializableClass.enumClassName())
    override fun serialize(output: KOutput, obj: T) = output.writeEnumValue(serializableClass, obj)
    override fun deserialize(input: KInput): T = input.readEnumValue(serializableClass)
}

class ModernEnumSerializer<T : Enum<T>>(className: String, val creator: EnumCreator<T>) : KSerializer<T> {
    override val serialClassDesc: SerialDescriptor = EnumDesc(className)
    override fun serialize(output: KOutput, obj: T) = output.writeEnumValue(obj)
    override fun deserialize(input: KInput): T = input.readEnumValue(creator)

    companion object {
        inline operator fun <reified E : Enum<E>> invoke(): ModernEnumSerializer<E> {
            return ModernEnumSerializer(E::class.enumClassName(), object : EnumCreator<E> {
                override fun createFromOrdinal(ordinal: Int): E {
                    return enumValues<E>()[ordinal]
                }

                override fun createFromName(name: String): E {
                    return enumValueOf<E>(name)
                }
            })
        }
    }
}

fun <T : Any> makeNullable(element: KSerializer<T>): KSerializer<T?> = NullableSerializer(element)

class NullableSerializer<T : Any>(private val element: KSerializer<T>) : KSerializer<T?> {
    override val serialClassDesc: SerialDescriptor
        get() = element.serialClassDesc

    override fun serialize(output: KOutput, obj: T?) {
        if (obj != null) {
            output.writeNotNullMark();
            element.serialize(output, obj)
        }
        else {
            output.writeNullValue()
        }
    }

    override fun deserialize(input: KInput): T? = if (input.readNotNullMark()) element.deserialize(input) else input.readNullValue()

    override fun patch(input: KInput, old: T?): T? {
        return when {
            old == null -> deserialize(input)
            input.readNotNullMark() -> element.patch(input, old)
            else -> input.readNullValue().let { old }
        }
    }
}

