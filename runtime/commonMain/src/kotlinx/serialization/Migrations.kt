/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED", "UNUSED_PARAMETER", "DEPRECATION_ERROR")
package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.encoding.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.internal.*
import kotlin.reflect.*

@PublishedApi
internal fun noImpl(): Nothing = throw UnsupportedOperationException("Not implemented, should not be called")

@Deprecated(
    message = "Deprecated in the favour of PrimitiveDescriptor factory function",
    level = DeprecationLevel.ERROR
)
public class PrimitiveDescriptorWithName
@Deprecated(
    message = "Deprecated in the favour of PrimitiveDescriptor factory function",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("PrimitiveDescriptor(name, original.kind)")
)
constructor(override val serialName: String, private val original: SerialDescriptor) : SerialDescriptor by original

@Deprecated(
    message = "Deprecated in the favour of PrimitiveDescriptor factory function",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("PrimitiveDescriptor(name, this.kind)")
)
public fun SerialDescriptor.withName(name: String): SerialDescriptor = error("No longer supported")

@Deprecated(level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of the same extension from builtins package",
    replaceWith = ReplaceWith("nullable", imports = ["kotlinx.serialization.builtins.nullable"]))
@LowPriorityInOverloadResolution
public val <T : Any> KSerializer<T>.nullable: KSerializer<T?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return if (descriptor.isNullable) (this as KSerializer<T?>) else NullableSerializer(this)
    }

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of the same extension from builtins package",
    replaceWith = ReplaceWith("list", imports = ["kotlinx.serialization.builtins.list"])
)
@LowPriorityInOverloadResolution
public val <T> KSerializer<T>.list: KSerializer<List<T>>
    get() = ArrayListSerializer(this)

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of the same extension from builtins package",
    replaceWith = ReplaceWith("set", imports = ["kotlinx.serialization.builtins.set"])
)
@LowPriorityInOverloadResolution
public val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = LinkedHashSetSerializer(this)

@Deprecated(
    level = DeprecationLevel.ERROR, message = "Deprecated in the favour of the MapSerializer() factory function",
    replaceWith = ReplaceWith(
        "MapSerializer(this.first, this.second)",
        imports = ["kotlinx.serialization.builtins.MapSerializer"]
    )
)
public val <K, V> Pair<KSerializer<K>, KSerializer<V>>.map: KSerializer<Map<K, V>>
    get() = LinkedHashMapSerializer(this.first, this.second)

@Deprecated(
    "Renamed to AbstractEncoder",
    replaceWith = ReplaceWith("AbstractEncoder", imports = ["kotlinx.serialization.encoding.AbstractEncoder"])
)
public typealias ElementValueEncoder = AbstractEncoder

@Deprecated(
    "Renamed to AbstractDecoder",
    replaceWith = ReplaceWith("AbstractDecoder", imports = ["kotlinx.serialization.encoding.AbstractDecoder"])
)
public typealias ElementValueDecoder = AbstractDecoder

@Deprecated(
    "This function accidentally slipped to a public API surface and is not intended for public use " +
            "since it does not have clear specification.",
    ReplaceWith("serializerOrNull"),
    level = DeprecationLevel.ERROR
)
public fun <T : Any> KClass<T>.compiledSerializer(): KSerializer<T>? = compiledSerializerImpl()

private const val message =
    "Mapper was renamed to Properties to better reflect its semantics and extracted to separate artifact kotlinx-serialization-properties"

@Deprecated(message = message, level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("Properties"))
public class Mapper()

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@Deprecated(
    message = "SerialId is renamed to ProtoNumber to better reflect its semantics and extracted to separate artifact kotlinx-serialization-protobuf",
    level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("ProtoNumber", imports = ["kotlinx.serialization.protobuf.*"])
)
public annotation class SerialId @Deprecated(
    message = "SerialId is renamed to ProtoId to better reflect its semantics and extracted to separate artifact kotlinx-serialization-protobuf",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ProtoNumber(id)", imports = ["kotlinx.serialization.protobuf.*"])
) constructor(public val id: Int)


@Deprecated(level = DeprecationLevel.WARNING, message = "Use default parse overload instead", replaceWith = ReplaceWith("parse(objects)"))
public inline fun <reified T : Any> StringFormat.parseList(objects: String): List<T> =
    decodeFromString(ListSerializer(serializersModule.serializer<T>()), objects)

@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Use default decodeFromString overload instead",
    replaceWith = ReplaceWith("decodeFromString(map)")
)
public inline fun <reified K : Any, reified V : Any> StringFormat.parseMap(map: String): Map<K, V> =
    decodeFromString(MapSerializer(serializersModule.serializer<K>(), serializersModule.serializer<V>()), map)

// ERROR migrations that affect **only** users that called these functions with named parameters

@LowPriorityInOverloadResolution
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Use default encodeToString overload instead",
    replaceWith = ReplaceWith("encodeToString(objects)")
)
public inline fun <reified T : Any> StringFormat.stringify(objects: List<T>): String =
    encodeToString(ListSerializer(serializersModule.serializer<T>()), objects)

@LowPriorityInOverloadResolution
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Use default stringify overload instead",
    replaceWith = ReplaceWith("stringify(map)")
)
public inline fun <reified K : Any, reified V : Any> StringFormat.stringify(map: Map<K, V>): String =
    encodeToString(MapSerializer(serializersModule.serializer<K>(), serializersModule.serializer<V>()), map)

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method is deprecated for removal. Please use reified serializer<T>() instead",
    replaceWith = ReplaceWith("serializer<T>()")
)
public fun <T : Any> SerializersModule.getContextualOrDefault(klass: KClass<T>): KSerializer<T> =
    noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method is deprecated for removal. Please use reified serializer<T>() instead",
    replaceWith = ReplaceWith("serializer<T>()")
)
public fun <T : Any> SerializersModule.getContextualOrDefault(value: T): KSerializer<T> =
    getContextual(value::class)?.cast() ?: value::class.serializer().cast()

@Suppress("UNUSED", "DeprecatedCallableAddReplaceWith")
@Deprecated(
    message = "Top-level polymorphic descriptor is deprecated, use descriptor from the instance of PolymorphicSerializer or" +
            "check for descriptor kind instead", level = DeprecationLevel.ERROR
)
public val PolymorphicClassDescriptor: SerialDescriptor
    get() = error("This property is no longer supported")

@Deprecated(
    "Deprecated for removal since it is indistinguishable from SerialFormat interface. " +
            "Use SerialFormat instead.", ReplaceWith("SerialFormat"), DeprecationLevel.ERROR
)
public abstract class AbstractSerialFormat(override val serializersModule: SerializersModule) : SerialFormat

@Deprecated(
    "This method was renamed to encodeToString during serialization 1.0 stabilization",
    ReplaceWith("encodeToString<T>(value)"), DeprecationLevel.ERROR
)
public fun <T : Any> StringFormat.stringify(value: T): String = noImpl()

@Deprecated(
    "This method was renamed to decodeFromString during serialization 1.0 stabilization",
    ReplaceWith("decodeFromString<T>(string)"), DeprecationLevel.ERROR
)
public fun <T : Any> StringFormat.parse(string: String): T = noImpl()

@Deprecated(
    "This method was renamed to encodeToString during serialization 1.0 stabilization",
    ReplaceWith("encodeToString<T>(serializer, value)"), DeprecationLevel.ERROR
)
public fun <T> StringFormat.stringify(serializer: SerializationStrategy<T>, value: T): String = noImpl()

@Deprecated(
    "This method was renamed to decodeFromString during serialization 1.0 stabilization",
    ReplaceWith("decodeFromString<T>(deserializer, string)"), DeprecationLevel.ERROR
)
public fun <T> StringFormat.parse(deserializer: DeserializationStrategy<T>, string: String): T = noImpl()

@Deprecated(
    "This method was renamed to decodeFromByteArray during serialization 1.0 stabilization",
    ReplaceWith("decodeFromByteArray<T>(deserializer, bytes)"), DeprecationLevel.ERROR
)
public fun <T> BinaryFormat.load(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T = noImpl()

@Deprecated(
    "This method was renamed to encodeToByteArray during serialization 1.0 stabilization",
    ReplaceWith("encodeToByteArray<T>(serializer, value)"), DeprecationLevel.ERROR
)
public fun <T> BinaryFormat.dump(serializer: SerializationStrategy<T>, value: T): ByteArray = noImpl()

@Deprecated(
    "This method was renamed to encodeToHexString during serialization 1.0 stabilization",
    ReplaceWith("encodeToHexString<T>(serializer, value)"), DeprecationLevel.ERROR
)
public fun <T> BinaryFormat.dumps(serializer: SerializationStrategy<T>, value: T): String = noImpl()

@Deprecated(
    "This method was renamed to decodeFromHexString during serialization 1.0 stabilization",
    ReplaceWith("decodeFromHexString<T>(deserializer, hex)"), DeprecationLevel.ERROR
)
public fun <T> BinaryFormat.loads(deserializer: DeserializationStrategy<T>, hex: String): T = noImpl()

@Deprecated(
    "This method was renamed to encodeToByteArray during serialization 1.0 stabilization",
    ReplaceWith("encodeToByteArray<T>(value)"), DeprecationLevel.ERROR
)
public fun <T : Any> BinaryFormat.dump(value: T): ByteArray = noImpl()

@Deprecated(
    "This method was renamed to encodeToHexString during serialization 1.0 stabilization",
    ReplaceWith("encodeToHexString<T>(value)"), DeprecationLevel.ERROR
)
public fun <T : Any> BinaryFormat.dumps(value: T): String = noImpl()

@Deprecated(
    "This method was renamed to decodeFromByteArray during serialization 1.0 stabilization",
    ReplaceWith("this.decodeFromByteArray<T>(raw)"), DeprecationLevel.ERROR
)
public fun <T : Any> BinaryFormat.load(raw: ByteArray): T = noImpl()

@Deprecated(
    "This method was renamed to decodeFromHexString during serialization 1.0 stabilization",
    ReplaceWith("decodeFromHexString<T>(hex)"), DeprecationLevel.ERROR
)
public fun <T : Any> BinaryFormat.loads(hex: String): T = noImpl()

@Deprecated(
    "This method was deprecated during serialization 1.0 API stabilization",
    ReplaceWith("decodeSerializableValue(deserializer)"), DeprecationLevel.ERROR
) // TODO make internal when migrations are removed
public fun <T : Any?> Decoder.decode(deserializer: DeserializationStrategy<T>): T = noImpl()

@Deprecated(
    "This method was deprecated during serialization 1.0 API stabilization",
    ReplaceWith("decodeSerializableValue<T>(serializer())"), DeprecationLevel.ERROR
) // TODO make internal when migrations are removed
public fun <T : Any> Decoder.decode(): T = noImpl()

@Deprecated(
    "This method was deprecated during serialization 1.0 API stabilization",
    ReplaceWith("encodeSerializableValue(strategy, value)"), DeprecationLevel.ERROR
) // TODO make internal when migrations are removed
public fun <T : Any?> Encoder.encode(strategy: SerializationStrategy<T>, value: T): Unit = noImpl()

@Deprecated(
    "This method was deprecated during serialization 1.0 API stabilization",
    ReplaceWith("encodeSerializableValue<T>(serializer(), value)"), DeprecationLevel.ERROR
) // TODO make internal when migrations are removed
public fun <T : Any> Encoder.encode(obj: T): Unit = noImpl()

@Deprecated(
    "This method was renamed to buildClassSerialDescriptor during serialization 1.0 API stabilization",
    replaceWith = ReplaceWith(
        "buildClassSerialDescriptor(serialName, *typeParameters, builderAction)",
        imports = ["kotlinx.serialization.descriptors.buildClassSerialDescriptor"]
    ),
    DeprecationLevel.ERROR
)
public fun SerialDescriptor(
    serialName: String,
    vararg typeParameters: SerialDescriptor,
    builderAction: ClassSerialDescriptorBuilder.() -> Unit = {}
): SerialDescriptor = noImpl()

@Deprecated(
    "Builder with SerialKind was deprecated during serialization 1.0 API stabilization. ",
    replaceWith = ReplaceWith(
        "buildSerialDescriptor(serialName, kind, *typeParameters, builderAction)",
        imports = ["kotlinx.serialization.descriptors.buildSerialDescriptor"]
    ),
    DeprecationLevel.ERROR
)
public fun SerialDescriptor(
    serialName: String,
    kind: SerialKind,
    vararg typeParameters: SerialDescriptor,
    builderAction: ClassSerialDescriptorBuilder.() -> Unit = {}
): SerialDescriptor = noImpl()

@Deprecated(
    "This method was renamed to PrimitiveSerialDescriptor during serialization 1.0 API stabilization",
    ReplaceWith(
        "PrimitiveSerialDescriptor(serialName, kind)",
        imports = ["kotlinx.serialization.descriptors.PrimitiveSerialDescriptor"]
    ),
    DeprecationLevel.ERROR
)
public fun PrimitiveDescriptor(serialName: String, kind: PrimitiveKind): SerialDescriptor = noImpl()

@Deprecated(
    "This property was renamed to serializersModule during serialization 1.0 stabilization",
    ReplaceWith("serializersModule"), DeprecationLevel.ERROR
)
public val SerialFormat.context: SerializersModule
    get() = noImpl()

@Deprecated(
    "This method was replaced with property during serialization 1.0 stabilization",
    ReplaceWith("elementDescriptors.toList()",
        imports = ["kotlinx.serialization.descriptors.elementNames"]
    ), DeprecationLevel.ERROR
)
public fun SerialDescriptor.elementDescriptors(): List<SerialDescriptor> = noImpl()

@Deprecated(
    "This method was replaced with property during serialization 1.0 stabilization",
    ReplaceWith(
        "elementNames.toList()",
        imports = ["kotlinx.serialization.descriptors.elementNames"]
    ), DeprecationLevel.ERROR
)
public fun SerialDescriptor.elementNames(): List<String> = noImpl()


@RequiresOptIn
@Deprecated(level = DeprecationLevel.ERROR, message = "This annotation is obsolete and deprecated for removal")
public annotation class ImplicitReflectionSerializer

@RequiresOptIn(level = RequiresOptIn.Level.WARNING)
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated for removal during serialization 1.0 API stabilization")
public annotation class UnstableDefault
