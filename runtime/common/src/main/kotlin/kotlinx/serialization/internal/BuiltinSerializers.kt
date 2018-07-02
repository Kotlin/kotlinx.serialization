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

    override val kind: SerialKind = PrimitiveKind.PRIMITIVE

    override fun getElementName(index: Int) = throw IllegalStateException("Primitives do not have fields")

    override fun getElementIndex(name: String) = throw IllegalStateException("Primitives do not have fields")

    override fun getElementDescriptor(index: Int): SerialDescriptor =
        throw IllegalStateException("Primitives does not have elements")
}

object UnitSerializer : KSerializer<Unit> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Unit")

    override fun serialize(output: Encoder, obj: Unit) = output.encodeUnit()
    override fun deserialize(input: Decoder): Unit = input.decodeUnit()
}

object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Boolean")

    override fun serialize(output: Encoder, obj: Boolean) = output.encodeBoolean(obj)
    override fun deserialize(input: Decoder): Boolean = input.decodeBoolean()
}

object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Byte")

    override fun serialize(output: Encoder, obj: Byte) = output.encodeByte(obj)
    override fun deserialize(input: Decoder): Byte = input.decodeByte()
}

object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Short")

    override fun serialize(output: Encoder, obj: Short) = output.encodeShort(obj)
    override fun deserialize(input: Decoder): Short = input.decodeShort()
}

object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Int")

    override fun serialize(output: Encoder, obj: Int) = output.encodeInt(obj)
    override fun deserialize(input: Decoder): Int = input.decodeInt()
}

object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Long")

    override fun serialize(output: Encoder, obj: Long) = output.encodeLong(obj)
    override fun deserialize(input: Decoder): Long = input.decodeLong()
}

object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Float")

    override fun serialize(output: Encoder, obj: Float) = output.encodeFloat(obj)
    override fun deserialize(input: Decoder): Float = input.decodeFloat()
}

object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Double")

    override fun serialize(output: Encoder, obj: Double) = output.encodeDouble(obj)
    override fun deserialize(input: Decoder): Double = input.decodeDouble()
}

object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.Char")

    override fun serialize(output: Encoder, obj: Char) = output.encodeChar(obj)
    override fun deserialize(input: Decoder): Char = input.decodeChar()
}

object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveDesc("kotlin.String")

    override fun serialize(output: Encoder, obj: String) = output.encodeString(obj)
    override fun deserialize(input: Decoder): String = input.decodeString()
}


internal class EnumDesc(override val name: String) : SerialDescriptor {

    override val kind: SerialKind = UnionKind.ENUM_KIND

    override fun getElementName(index: Int) = throw IllegalStateException("Primitives does not have fields")

    override fun getElementIndex(name: String) = throw IllegalStateException("Primitives does not have fields")

    override fun getElementDescriptor(index: Int) = throw IllegalStateException("Enums does not have serializable elements")
}

// note, that it is instantiated in a special way
@Deprecated("Not supported in Native", replaceWith = ReplaceWith("ModernEnumSerializer()"))
class EnumSerializer<T : Enum<T>>(val serializableClass: KClass<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor = EnumDesc(serializableClass.enumClassName())
    override fun serialize(output: Encoder, obj: T) = output.encodeEnum(serializableClass, obj)
    override fun deserialize(input: Decoder): T = input.decodeEnum(serializableClass)
}

class ModernEnumSerializer<T : Enum<T>>(className: String, val creator: EnumCreator<T>) : KSerializer<T> {
    override val descriptor: SerialDescriptor = EnumDesc(className)
    override fun serialize(output: Encoder, obj: T) = output.encodeEnum(obj)
    override fun deserialize(input: Decoder): T = input.decodeEnum(creator)

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
    override val descriptor: SerialDescriptor = object : SerialDescriptor by element.descriptor {
        override val isNullable: Boolean
            get() = true
    }

    override fun serialize(output: Encoder, obj: T?) {
        if (obj != null) {
            output.encodeNotNullMark();
            element.serialize(output, obj)
        }
        else {
            output.encodeNull()
        }
    }

    override fun deserialize(input: Decoder): T? = if (input.decodeNotNullMark()) element.deserialize(input) else input.decodeNull()

    override fun patch(input: Decoder, old: T?): T? {
        return when {
            old == null -> deserialize(input)
            input.decodeNotNullMark() -> element.patch(input, old)
            else -> input.decodeNull().let { old }
        }
    }
}

