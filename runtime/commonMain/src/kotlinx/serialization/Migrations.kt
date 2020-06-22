/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED", "UNUSED_PARAMETER", "DEPRECATION_ERROR")
package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import kotlinx.serialization.modules.*
import kotlin.internal.*
import kotlin.reflect.*

private fun noImpl(): Nothing = throw UnsupportedOperationException("Not implemented, should not be called")

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
    replaceWith = ReplaceWith("AbstractEncoder", imports = ["kotlinx.serialization.builtins.AbstractEncoder"])
)
public typealias ElementValueEncoder = AbstractEncoder

@Deprecated(
    "Renamed to AbstractDecoder",
    replaceWith = ReplaceWith("AbstractDecoder", imports = ["kotlinx.serialization.builtins.AbstractDecoder"])
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
    message = "SerialId is renamed to ProtoId to better reflect its semantics and extracted to separate artifact kotlinx-serialization-protobuf",
    level = DeprecationLevel.ERROR, replaceWith = ReplaceWith("ProtoId", imports = ["kotlinx.serialization.protobuf.*"])
)
public annotation class SerialId @Deprecated(
    message = "SerialId is renamed to ProtoId to better reflect its semantics and extracted to separate artifact kotlinx-serialization-protobuf",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("ProtoId(id)", imports = ["kotlinx.serialization.protobuf.*"])
) constructor(public val id: Int)


@Deprecated(level = DeprecationLevel.WARNING, message = "Use default parse overload instead", replaceWith = ReplaceWith("parse(objects)"))
public inline fun <reified T : Any> StringFormat.parseList(objects: String): List<T> =
    decodeFromString(context.getContextualOrDefault<T>().list, objects)

@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "Use default parse overload instead",
    replaceWith = ReplaceWith("parse(map)")
)
public inline fun <reified K : Any, reified V : Any> StringFormat.parseMap(map: String): Map<K, V> =
    decodeFromString(MapSerializer(context.getContextualOrDefault<K>(), context.getContextualOrDefault<V>()), map)

// ERROR migrations that affect **only** users that called these functions with named parameters

@LowPriorityInOverloadResolution
@Deprecated(level = DeprecationLevel.ERROR, message = "Use default stringify overload instead", replaceWith = ReplaceWith("stringify(objects)"))
public inline fun <reified T : Any> StringFormat.stringify(objects: List<T>): String =
    encodeToString(context.getContextualOrDefault<T>().list, objects)

@LowPriorityInOverloadResolution
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Use default stringify overload instead",
    replaceWith = ReplaceWith("stringify(map)")
)
public inline fun <reified K : Any, reified V : Any> StringFormat.stringify(map: Map<K, V>): String =
    encodeToString(MapSerializer(context.getContextualOrDefault<K>(), context.getContextualOrDefault<V>()), map)

@ImplicitReflectionSerializer
@OptIn(UnsafeSerializationApi::class)
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "This method is deprecated for removal. Please use reified getContextualOrDefault<T>() instead",
    replaceWith = ReplaceWith("getContextual(klass) ?: klass.serializer()")
)
public fun <T : Any> SerialModule.getContextualOrDefault(klass: KClass<T>): KSerializer<T> =
    getContextual(klass) ?: klass.serializer()

@ImplicitReflectionSerializer
@OptIn(UnsafeSerializationApi::class)
@Deprecated(
    level = DeprecationLevel.WARNING,
    message = "This method is deprecated for removal. Please use reified getContextualOrDefault<T>() instead",
    replaceWith = ReplaceWith("getContextualOrDefault<T>()")
)
public fun <T : Any> SerialModule.getContextualOrDefault(value: T): KSerializer<T> =
    getContextual(value) ?: value::class.serializer().cast()

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
public abstract class AbstractSerialFormat(override val context: SerialModule) : SerialFormat

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