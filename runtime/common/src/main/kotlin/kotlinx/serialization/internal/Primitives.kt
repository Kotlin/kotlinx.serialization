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

sealed class PrimitiveDescriptor(override val name: String, override val kind: PrimitiveKind): SerialDescriptor {
    private fun error(): Nothing = throw IllegalStateException("Primitives does not have elements")

    final override fun getElementName(index: Int): String = error()
    final override fun getElementIndex(name: String): Int = error()
    final override fun isElementOptional(index: Int): Boolean = error()
    final override fun getElementDescriptor(index: Int): SerialDescriptor = error()

    override fun toString(): String = name
}

object IntDescriptor: PrimitiveDescriptor("kotlin.Int", PrimitiveKind.INT) // or just "Int"?
object UnitDescriptor: PrimitiveDescriptor("kotlin.Unit", PrimitiveKind.UNIT)
object BooleanDescriptor: PrimitiveDescriptor("kotlin.Boolean", PrimitiveKind.BOOLEAN)
object ByteDescriptor: PrimitiveDescriptor("kotlin.Byte", PrimitiveKind.BYTE)
object ShortDescriptor: PrimitiveDescriptor("kotlin.Short", PrimitiveKind.SHORT)
object LongDescriptor: PrimitiveDescriptor("kotlin.Long", PrimitiveKind.LONG)
object FloatDescriptor: PrimitiveDescriptor("kotlin.Float", PrimitiveKind.FLOAT)
object DoubleDescriptor: PrimitiveDescriptor("kotlin.Double", PrimitiveKind.DOUBLE)
object CharDescriptor: PrimitiveDescriptor("kotlin.Char", PrimitiveKind.CHAR)
object StringDescriptor: PrimitiveDescriptor("kotlin.String", PrimitiveKind.STRING)

object UnitSerializer : KSerializer<Unit> {
    override val descriptor: SerialDescriptor = UnitDescriptor

    override fun serialize(output: Encoder, obj: Unit) = output.encodeUnit()
    override fun deserialize(input: Decoder): Unit = input.decodeUnit()
}

object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = BooleanDescriptor

    override fun serialize(output: Encoder, obj: Boolean) = output.encodeBoolean(obj)
    override fun deserialize(input: Decoder): Boolean = input.decodeBoolean()
}

object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = ByteDescriptor

    override fun serialize(output: Encoder, obj: Byte) = output.encodeByte(obj)
    override fun deserialize(input: Decoder): Byte = input.decodeByte()
}

object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = ShortDescriptor

    override fun serialize(output: Encoder, obj: Short) = output.encodeShort(obj)
    override fun deserialize(input: Decoder): Short = input.decodeShort()
}

object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = IntDescriptor

    override fun serialize(output: Encoder, obj: Int) = output.encodeInt(obj)
    override fun deserialize(input: Decoder): Int = input.decodeInt()
}

object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = LongDescriptor

    override fun serialize(output: Encoder, obj: Long) = output.encodeLong(obj)
    override fun deserialize(input: Decoder): Long = input.decodeLong()
}

object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = FloatDescriptor

    override fun serialize(output: Encoder, obj: Float) = output.encodeFloat(obj)
    override fun deserialize(input: Decoder): Float = input.decodeFloat()
}

object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = DoubleDescriptor

    override fun serialize(output: Encoder, obj: Double) = output.encodeDouble(obj)
    override fun deserialize(input: Decoder): Double = input.decodeDouble()
}

object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = CharDescriptor

    override fun serialize(output: Encoder, obj: Char) = output.encodeChar(obj)
    override fun deserialize(input: Decoder): Char = input.decodeChar()
}

object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = StringDescriptor

    override fun serialize(output: Encoder, obj: String) = output.encodeString(obj)
    override fun deserialize(input: Decoder): String = input.decodeString()
}


@Suppress("UNCHECKED_CAST")
fun <T : Any> KClass<T>.defaultSerializer(): KSerializer<T>? = when (this) {
    String::class -> StringSerializer
    Char::class -> CharSerializer
    Double::class -> DoubleSerializer
    Float::class -> FloatSerializer
    Long::class -> LongSerializer
    Int::class -> IntSerializer
    Short::class -> ShortSerializer
    Byte::class -> ByteSerializer
    Boolean::class -> BooleanSerializer
    Unit::class -> UnitSerializer
    else -> null
} as KSerializer<T>?
