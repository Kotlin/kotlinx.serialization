/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.internal.*

/**
 * Built-in serializer for [Unit] type.
 */
public object UnitSerializer : KSerializer<Unit> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Unit", PrimitiveKind.UNIT)
    override fun serialize(encoder: Encoder, value: Unit) = encoder.encodeUnit()
    override fun deserialize(decoder: Decoder): Unit = decoder.decodeUnit()
}

/**
 * Built-in serializer for [Boolean] type.
 */
public object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Boolean", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeBoolean()
}

/**
 * Built-in serializer for [Byte] type.
 */
public object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Byte", PrimitiveKind.BYTE)
    override fun serialize(encoder: Encoder, value: Byte) = encoder.encodeByte(value)
    override fun deserialize(decoder: Decoder): Byte = decoder.decodeByte()
}

/**
 * Built-in serializer for [Short] type.
 */
public object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Short", PrimitiveKind.SHORT)
    override fun serialize(encoder: Encoder, value: Short) = encoder.encodeShort(value)
    override fun deserialize(decoder: Decoder): Short = decoder.decodeShort()
}

/**
 * Built-in serializer for [Int] type.
 */
public object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Int", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}

/**
 * Built-in serializer for [Long] type.
 */
public object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Long", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
    override fun deserialize(decoder: Decoder): Long = decoder.decodeLong()
}

/**
 * Built-in serializer for [Float] type.
 */
public object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Float", PrimitiveKind.FLOAT)
    override fun serialize(encoder: Encoder, value: Float) = encoder.encodeFloat(value)
    override fun deserialize(decoder: Decoder): Float = decoder.decodeFloat()
}

/**
 * Built-in serializer for [Double] type.
 */
public object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Double", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}

/**
 * Built-in serializer for [Char] type.
 */
public object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.Char", PrimitiveKind.CHAR)
    override fun serialize(encoder: Encoder, value: Char) = encoder.encodeChar(value)
    override fun deserialize(decoder: Decoder): Char = decoder.decodeChar()
}

/**
 * Built-in serializer for [String] type.
 */
public object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = BuiltinDescriptor("kotlin.String", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
}

/**
 * Factory to create a trivial primitive descriptors.
 * Primitive descriptors should be used when the serialized form of the data has a primitive form, for example:
 * ```
 * object LongAsStringSerializer : KSerializer<Long> {
 *     override val descriptor: SerialDescriptor =
 *         PrimitiveDescriptor("kotlinx.serialization.LongAsStringSerializer", PrimitiveKind.STRING)
 *
 *     override fun serialize(encoder: Encoder, obj: Long) {
 *         encoder.encodeString(obj.toString())
 *     }
 *
 *     override fun deserialize(decoder: Decoder): Long {
 *         return decoder.decodeString().toLong()
 *     }
 * }
 * ```
 */
public fun PrimitiveDescriptor(serialName: String, kind: PrimitiveKind): SerialDescriptor = PrimitiveDescriptorSafe(serialName, kind)

public fun String.Companion.serializer(): KSerializer<String> = StringSerializer
public fun Char.Companion.serializer(): KSerializer<Char> = CharSerializer
public fun Byte.Companion.serializer(): KSerializer<Byte> = ByteSerializer
public fun Short.Companion.serializer(): KSerializer<Short> = ShortSerializer
public fun Int.Companion.serializer(): KSerializer<Int> = IntSerializer
public fun Long.Companion.serializer(): KSerializer<Long> = LongSerializer
public fun Float.Companion.serializer(): KSerializer<Float> = FloatSerializer
public fun Double.Companion.serializer(): KSerializer<Double> = DoubleSerializer
public fun Boolean.Companion.serializer(): KSerializer<Boolean> = BooleanSerializer

// Source-level migration aids


@Deprecated(
    message = "Deprecated in the favour of PrimitiveDescriptor factory function",
    level = DeprecationLevel.ERROR
)
class PrimitiveDescriptorWithName
@Deprecated(
    message = "Deprecated in the favour of PrimitiveDescriptor factory function",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("PrimitiveDescriptor(name, original.kind)")
)
constructor(override val name: String, val original: SerialDescriptor) : SerialDescriptor by original

@Suppress("UNUSED") // compiler still complains about unused parameter
@Deprecated(
    message = "Deprecated in the favour of PrimitiveDescriptor factory function",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("PrimitiveDescriptor(name, this.kind)")
)
fun SerialDescriptor.withName(name: String): SerialDescriptor = error("No longer supported")
