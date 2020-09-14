/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")
@file:OptIn(ExperimentalSerializationApi::class)

package kotlinx.serialization.internal

import kotlinx.serialization.*
import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlin.native.concurrent.*
import kotlin.reflect.*

@SharedImmutable
private val BUILTIN_SERIALIZERS = mapOf(
    String::class to String.serializer(),
    Char::class to Char.serializer(),
    CharArray::class to CharArraySerializer(),
    Double::class to Double.serializer(),
    DoubleArray::class to DoubleArraySerializer(),
    Float::class to Float.serializer(),
    FloatArray::class to FloatArraySerializer(),
    Long::class to Long.serializer(),
    LongArray::class to LongArraySerializer(),
    Int::class to Int.serializer(),
    IntArray::class to IntArraySerializer(),
    Short::class to Short.serializer(),
    ShortArray::class to ShortArraySerializer(),
    Byte::class to Byte.serializer(),
    ByteArray::class to ByteArraySerializer(),
    Boolean::class to Boolean.serializer(),
    BooleanArray::class to BooleanArraySerializer(),
    Unit::class to Unit.serializer()
)

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
    private fun error(): Nothing = throw IllegalStateException("Primitive descriptor does not have elements")
}

internal fun PrimitiveDescriptorSafe(serialName: String, kind: PrimitiveKind): SerialDescriptor {
    checkName(serialName)
    return PrimitiveSerialDescriptor(serialName, kind)
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
    BUILTIN_SERIALIZERS[this] as KSerializer<T>?

@Deprecated(
    message = "Deprecated in the favour of Unit.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Unit.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
@Suppress("DEPRECATION_ERROR")
public object UnitSerializer : KSerializer<Unit> by ObjectSerializer("kotlin.Unit", Unit)

@Deprecated(
    message = "Deprecated in the favour of Boolean.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Boolean.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object BooleanSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Boolean", PrimitiveKind.BOOLEAN)
    override fun serialize(encoder: Encoder, value: Boolean): Unit = encoder.encodeBoolean(value)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeBoolean()
}
@Deprecated(
    message = "Deprecated in the favour of Byte.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Byte.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object ByteSerializer : KSerializer<Byte> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Byte", PrimitiveKind.BYTE)
    override fun serialize(encoder: Encoder, value: Byte): Unit = encoder.encodeByte(value)
    override fun deserialize(decoder: Decoder): Byte = decoder.decodeByte()
}

@Deprecated(
    message = "Deprecated in the favour of Short.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Short.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object ShortSerializer : KSerializer<Short> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Short", PrimitiveKind.SHORT)
    override fun serialize(encoder: Encoder, value: Short): Unit = encoder.encodeShort(value)
    override fun deserialize(decoder: Decoder): Short = decoder.decodeShort()
}

@Deprecated(
    message = "Deprecated in the favour of Int.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Int.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object IntSerializer : KSerializer<Int> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Int", PrimitiveKind.INT)
    override fun serialize(encoder: Encoder, value: Int): Unit = encoder.encodeInt(value)
    override fun deserialize(decoder: Decoder): Int = decoder.decodeInt()
}

@Deprecated(
    message = "Deprecated in the favour of Long.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Long.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object LongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Long", PrimitiveKind.LONG)
    override fun serialize(encoder: Encoder, value: Long): Unit = encoder.encodeLong(value)
    override fun deserialize(decoder: Decoder): Long = decoder.decodeLong()
}

@Deprecated(
    message = "Deprecated in the favour of Float.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Float.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object FloatSerializer : KSerializer<Float> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Float", PrimitiveKind.FLOAT)
    override fun serialize(encoder: Encoder, value: Float): Unit = encoder.encodeFloat(value)
    override fun deserialize(decoder: Decoder): Float = decoder.decodeFloat()
}

@Deprecated(
    message = "Deprecated in the favour of Double.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Double.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object DoubleSerializer : KSerializer<Double> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Double", PrimitiveKind.DOUBLE)
    override fun serialize(encoder: Encoder, value: Double): Unit = encoder.encodeDouble(value)
    override fun deserialize(decoder: Decoder): Double = decoder.decodeDouble()
}

@Deprecated(
    message = "Deprecated in the favour of Char.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Char.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object CharSerializer : KSerializer<Char> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.Char", PrimitiveKind.CHAR)
    override fun serialize(encoder: Encoder, value: Char): Unit = encoder.encodeChar(value)
    override fun deserialize(decoder: Decoder): Char = decoder.decodeChar()
}

@Deprecated(
    message = "Deprecated in the favour of String.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("String.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
public object StringSerializer : KSerializer<String> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("kotlin.String", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: String): Unit = encoder.encodeString(value)
    override fun deserialize(decoder: Decoder): String = decoder.decodeString()
}

public open class Migration : SerialDescriptor {
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

private const val message = "Top level primitive descriptors are unavailable to avoid accidental misuage. " +
        "Please use kind for comparison and primitive descriptor with a unique name for implementation"

@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.INT)"))
public object IntDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.UNIT)"))
public object UnitDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.BOOLEAN)"))
public object BooleanDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.BYTE)"))
public object ByteDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.SHORT)"))
public object ShortDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.LONG)"))
public object LongDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.FLOAT)"))
public object FloatDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.DOUBLE)"))
public object DoubleDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.CHAR)"))
public object CharDescriptor : Migration()
@Deprecated(message = message,
    replaceWith = ReplaceWith("PrimitiveDescriptor(\"yourSerializerUniqueName\", PrimitiveKind.STRING)"))
public object StringDescriptor : Migration()
