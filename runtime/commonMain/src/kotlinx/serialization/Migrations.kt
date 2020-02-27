/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED", "UNUSED_PARAMETER")
package kotlinx.serialization

import kotlinx.serialization.builtins.*
import kotlinx.serialization.internal.*
import kotlin.internal.*

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

@Deprecated(
    message = "Deprecated in the favour of PrimitiveDescriptor factory function",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("PrimitiveDescriptor(name, this.kind)")
)
fun SerialDescriptor.withName(name: String): SerialDescriptor = error("No longer supported")

@Deprecated(level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of the same extension from builtins package",
    replaceWith = ReplaceWith("nullable", imports = ["kotlinx.serialization.builtins.nullable"]))
@LowPriorityInOverloadResolution
public val <T : Any> KSerializer<T>.nullable: KSerializer<T?>
    get() {
        @Suppress("UNCHECKED_CAST")
        return if (descriptor.isNullable) (this as KSerializer<T?>) else NullableSerializer(this)
    }

@Deprecated(level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of the same extension from builtins package",
    replaceWith = ReplaceWith("list", imports = ["kotlinx.serialization.builtins.list"]))
@LowPriorityInOverloadResolution
val <T> KSerializer<T>.list: KSerializer<List<T>>
    get() = ArrayListSerializer(this)

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of the same extension from builtins package",
    replaceWith = ReplaceWith("set", imports = ["kotlinx.serialization.builtins.set"])
)
@LowPriorityInOverloadResolution
val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = LinkedHashSetSerializer(this)

@Deprecated(
    level = DeprecationLevel.ERROR, message = "Deprecated in the favour of the MapSerializer() factory function",
    replaceWith = ReplaceWith("MapSerializer(this.first, this.second)", imports = ["kotlinx.serialization.builtins.MapSerializer"])
)
val <K, V> Pair<KSerializer<K>, KSerializer<V>>.map: KSerializer<Map<K, V>>
    get() = LinkedHashMapSerializer(this.first, this.second)

@Deprecated(
    "Renamed to AbstractEncoder",
    replaceWith = ReplaceWith("AbstractEncoder", imports = ["kotlinx.serialization.builtins.AbstractEncoder"])
)
typealias ElementValueEncoder = AbstractEncoder

@Deprecated(
    "Renamed to AbstractDecoder",
    replaceWith = ReplaceWith("AbstractDecoder", imports = ["kotlinx.serialization.builtins.AbstractDecoder"])
)
typealias ElementValueDecoder = AbstractDecoder
