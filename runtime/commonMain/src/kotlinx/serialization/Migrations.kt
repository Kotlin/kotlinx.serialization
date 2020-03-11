/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED", "UNUSED_PARAMETER", "DEPRECATION_ERROR")
package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import kotlin.internal.*
import kotlin.reflect.*

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
constructor(override val name: String, private val original: SerialDescriptor) : SerialDescriptor by original

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

@ImplicitReflectionSerializer
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
