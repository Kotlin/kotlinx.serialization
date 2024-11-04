/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.reflect.*
import kotlin.time.Duration
import kotlin.uuid.*

private val BUILTIN_SERIALIZERS = initBuiltins()

internal expect fun initBuiltins(): Map<KClass<*>, KSerializer<*>>

internal class PrimitiveSerialDescriptor(
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
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is PrimitiveSerialDescriptor) return false
        if (serialName == other.serialName && kind == other.kind) return true
        return false
    }
    override fun hashCode() = serialName.hashCode() + 31 * kind.hashCode()
    private fun error(): Nothing = throw IllegalStateException("Primitive descriptor $serialName does not have elements")
}

internal fun PrimitiveDescriptorSafe(serialName: String, kind: PrimitiveKind): SerialDescriptor {
    checkNameIsNotAPrimitive(serialName)
    return PrimitiveSerialDescriptor(serialName, kind)
}

internal fun checkNameIsNotAPrimitive(serialName: String) {
    val values = BUILTIN_SERIALIZERS.values
    for (primitive in values) {
        val primitiveName = primitive.descriptor.serialName
        if (serialName == primitiveName) {
            throw IllegalArgumentException("""
                The name of serial descriptor should uniquely identify associated serializer.
                For serial name $serialName there already exists ${primitive::class.simpleName}.
                Please refer to SerialDescriptor documentation for additional information.
            """.trimIndent())
        }
    }
}

private fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> KClass<T>.builtinSerializerOrNull(): KSerializer<T>? =
    BUILTIN_SERIALIZERS[this] as KSerializer<T>?

@PublishedApi
internal object UnitSerializer : KSerializer<Unit> by ObjectSerializer("kotlin.Unit", Unit)

@PublishedApi
internal object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Boolean", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, value: Boolean): Unit = encoder.encodeBoolean(value)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeBoolean()
}

@PublishedApi
internal object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Byte", PrimitiveKind.BYTE)
    override fun serialize(encoder: Encoder, value: Byte): Unit = encoder.encodeByte(value)
    override fun deserialize(decoder: Decoder): Byte = decoder.decodeByte()
}

@PublishedApi
internal object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Short", PrimitiveKind.SHORT)
    override fun serialize(encoder: Encoder, value: Short): Unit = encoder.encodeShort(value)
    override fun deserialize(decoder: Decoder): Short = decoder.decodeShort()
}

@PublishedApi
internal object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Int", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Int): Unit = encoder.encodeInt(value)
    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}

@PublishedApi
internal object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Long", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Long): Unit = encoder.encodeLong(value)
    override fun deserialize(decoder: Decoder): Long = decoder.decodeLong()
}

@PublishedApi
internal object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Float", PrimitiveKind.FLOAT)
    override fun serialize(encoder: Encoder, value: Float): Unit = encoder.encodeFloat(value)
    override fun deserialize(decoder: Decoder): Float = decoder.decodeFloat()
}

@PublishedApi
internal object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Double", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double): Unit = encoder.encodeDouble(value)
    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}

@PublishedApi
internal object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Char", PrimitiveKind.CHAR)
    override fun serialize(encoder: Encoder, value: Char): Unit = encoder.encodeChar(value)
    override fun deserialize(decoder: Decoder): Char = decoder.decodeChar()
}

@PublishedApi
internal object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.String", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String): Unit = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
}
