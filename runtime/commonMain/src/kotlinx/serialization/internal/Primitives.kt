/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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

    override fun serialize(encoder: Encoder, obj: Unit) = encoder.encodeUnit()
    override fun deserialize(decoder: Decoder): Unit = decoder.decodeUnit()
}

object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = BooleanDescriptor

    override fun serialize(encoder: Encoder, obj: Boolean) = encoder.encodeBoolean(obj)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeBoolean()
}

object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = ByteDescriptor

    override fun serialize(encoder: Encoder, obj: Byte) = encoder.encodeByte(obj)
    override fun deserialize(decoder: Decoder): Byte = decoder.decodeByte()
}

object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = ShortDescriptor

    override fun serialize(encoder: Encoder, obj: Short) = encoder.encodeShort(obj)
    override fun deserialize(decoder: Decoder): Short = decoder.decodeShort()
}

object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = IntDescriptor

    override fun serialize(encoder: Encoder, obj: Int) = encoder.encodeInt(obj)
    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}

object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = LongDescriptor

    override fun serialize(encoder: Encoder, obj: Long) = encoder.encodeLong(obj)
    override fun deserialize(decoder: Decoder): Long = decoder.decodeLong()
}

object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = FloatDescriptor

    override fun serialize(encoder: Encoder, obj: Float) = encoder.encodeFloat(obj)
    override fun deserialize(decoder: Decoder): Float = decoder.decodeFloat()
}

object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = DoubleDescriptor

    override fun serialize(encoder: Encoder, obj: Double) = encoder.encodeDouble(obj)
    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}

object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = CharDescriptor

    override fun serialize(encoder: Encoder, obj: Char) = encoder.encodeChar(obj)
    override fun deserialize(decoder: Decoder): Char = decoder.decodeChar()
}

object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = StringDescriptor

    override fun serialize(encoder: Encoder, obj: String) = encoder.encodeString(obj)
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
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
