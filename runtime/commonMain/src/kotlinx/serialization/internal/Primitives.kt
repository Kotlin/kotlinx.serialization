/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.BooleanSerializer
import kotlinx.serialization.ByteSerializer
import kotlinx.serialization.CharSerializer
import kotlinx.serialization.DoubleSerializer
import kotlinx.serialization.FloatSerializer
import kotlinx.serialization.IntSerializer
import kotlinx.serialization.LongSerializer
import kotlinx.serialization.ShortSerializer
import kotlinx.serialization.StringSerializer
import kotlinx.serialization.UnitSerializer
import kotlin.native.concurrent.*
import kotlin.reflect.*

@SharedImmutable
private val BUILTIN_SERIALIZERS = mapOf(
    String::class to StringSerializer,
    Char::class to CharSerializer,
    Double::class to DoubleSerializer,
    Float::class to FloatSerializer,
    Long::class to LongSerializer,
    Int::class to IntSerializer,
    Short::class to ShortSerializer,
    Byte::class to ByteSerializer,
    Boolean::class to BooleanSerializer,
    Unit::class to UnitSerializer
)

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

internal fun BuiltinDescriptor(serialName: String, kind: PrimitiveKind): SerialDescriptor = PrimitiveSerialDescriptor(serialName, kind)

internal fun PrimitiveDescriptorSafe(serialName: String, kind: PrimitiveKind): SerialDescriptor {
    checkName(serialName)
    return BuiltinDescriptor(serialName, kind)
}

private fun checkName(serialName: String) {
    val keys = BUILTIN_SERIALIZERS.keys
    for (primitive in keys) {
        val simpleName = primitive.simpleName!!.capitalize()
        val qualifiedName = "kotlin.$simpleName" // KClass.qualifiedName is not supported in JS
        if (serialName.equals(qualifiedName, ignoreCase = true) || serialName.equals(simpleName, ignoreCase = true)) {
            throw IllegalArgumentException("""
                The name of serial descriptor should uniquely identify associated serializer.
                For serial name $serialName there already exist ${simpleName.capitalize()}Serializer.
                Please refer to SerialDescriptor documentation for additional information.
            """.trimIndent())
        }
    }
}

@Suppress("UNCHECKED_CAST")
internal fun <T : Any> KClass<T>.builtinSerializerOrNull(): KSerializer<T>? =
    BUILTIN_SERIALIZERS[this] as KSerializer<T>

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object UnitSerializer : KSerializer<Unit> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Unit", PrimitiveKind.UNIT)
    override fun serialize(encoder: Encoder, value: Unit) = encoder.encodeUnit()
    override fun deserialize(decoder: Decoder): Unit = decoder.decodeUnit()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Boolean", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, value: Boolean) = encoder.encodeBoolean(value)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeBoolean()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Byte", PrimitiveKind.BYTE)
    override fun serialize(encoder: Encoder, value: Byte) = encoder.encodeByte(value)
    override fun deserialize(decoder: Decoder): Byte = decoder.decodeByte()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Short", PrimitiveKind.SHORT)
    override fun serialize(encoder: Encoder, value: Short) = encoder.encodeShort(value)
    override fun deserialize(decoder: Decoder): Short = decoder.decodeShort()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Int", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Int) = encoder.encodeInt(value)
    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Long", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Long) = encoder.encodeLong(value)
    override fun deserialize(decoder: Decoder): Long = decoder.decodeLong()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Float", PrimitiveKind.FLOAT)

    override fun serialize(encoder: Encoder, value: Float) = encoder.encodeFloat(value)
    override fun deserialize(decoder: Decoder): Float = decoder.decodeFloat()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Double", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double) = encoder.encodeDouble(value)
    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Char", PrimitiveKind.CHAR)
    override fun serialize(encoder: Encoder, value: Char) = encoder.encodeChar(value)
    override fun deserialize(decoder: Decoder): Char = decoder.decodeChar()
}

@Deprecated(level = DeprecationLevel.HIDDEN, message = "Binary compatibility")
object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.String", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String) = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
}


open class Migration : SerialDescriptor {
    override val serialName: String
        get() = error("Class used only for source-level migration")
    override val kind: SerialKind
        get() = error("Class used only for source-level migration")
    override val elementsCount: Int
        get() = error("Class used only for source-level migration")
    override fun getElementName(index: Int): String {
        error("Class used only for source-level migration")
    }

    override fun getElementIndex(name: String): Int {
        error("Class used only for source-level migration")
    }

    override fun getElementAnnotations(index: Int): List<Annotation> {
        error("Class used only for source-level migration")
    }

    override fun getElementDescriptor(index: Int): SerialDescriptor {
        error("Class used only for source-level migration")
    }

    override fun isElementOptional(index: Int): Boolean {
        error("Class used only for source-level migration")
    }
}

const val message = "Top level primitive descriptors are unavailable to avoid accidental misuage. " +
        "Please use kind for comparison and primitive descriptor with a unique name for implementation"

@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.INT)"))
object IntDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.UNIT)"))
object UnitDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.BOOLEAN)"))
object BooleanDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.BYTE)"))
object ByteDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.SHORT)"))
object ShortDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.LONG)"))
object LongDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.FLOAT)"))
object FloatDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.DOUBLE)"))
object DoubleDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.CHAR)"))
object CharDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.STRING)"))
object StringDescriptor : Migration()
