/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlin.reflect.*

private class PrimitiveSerialDescriptor(
    override val serialName: String,
    override val kind: PrimitiveKind
) : SerialDescriptor {
    override val elementsCount: Int get() = 0
    override fun getElementName(index: Int): String = error()
    override fun getElementIndex(name: String): Int = error()
    override fun isElementOptional(index: Int): Boolean = error()
    override fun getElementDescriptor(index: Int): SerialDescriptor = error()
    override fun getElementAnnotations(index: Int): List<Annotation> = error()
    override fun toString(): String = "PrimitiveDescriptor($serialName)"
    private fun error(): Nothing = throw IllegalStateException("Primitive descriptor does not have elements")
}

internal fun PrimitiveDescriptor(serialName: String, kind: PrimitiveKind): SerialDescriptor = PrimitiveSerialDescriptor(serialName, kind)

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> KClass<T>.primitiveSerializerOrNull(): KSerializer<T>? = when (this) {
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

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object UnitSerializer : KSerializer<Unit> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Unit", PrimitiveKind.UNIT)
    override fun serialize(encoder: Encoder, obj: Unit) = encoder.encodeUnit()
    override fun deserialize(decoder: Decoder): Unit = decoder.decodeUnit()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Boolean", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, obj: Boolean) = encoder.encodeBoolean(obj)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeBoolean()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Byte", PrimitiveKind.BYTE)
    override fun serialize(encoder: Encoder, obj: Byte) = encoder.encodeByte(obj)
    override fun deserialize(decoder: Decoder): Byte = decoder.decodeByte()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Short", PrimitiveKind.SHORT)
    override fun serialize(encoder: Encoder, obj: Short) = encoder.encodeShort(obj)
    override fun deserialize(decoder: Decoder): Short = decoder.decodeShort()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Int", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, obj: Int) = encoder.encodeInt(obj)
    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Long", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, obj: Long) = encoder.encodeLong(obj)
    override fun deserialize(decoder: Decoder): Long = decoder.decodeLong()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Float", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, obj: Float) = encoder.encodeFloat(obj)
    override fun deserialize(decoder: Decoder): Float = decoder.decodeFloat()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Double", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, obj: Double) = encoder.encodeDouble(obj)
    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Char", PrimitiveKind.CHAR)
    override fun serialize(encoder: Encoder, obj: Char) = encoder.encodeChar(obj)
    override fun deserialize(decoder: Decoder): Char = decoder.decodeChar()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.String", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, obj: String) = encoder.encodeString(obj)
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
}
