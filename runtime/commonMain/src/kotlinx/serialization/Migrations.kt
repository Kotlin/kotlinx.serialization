/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE")
package kotlinx.serialization

import kotlinx.serialization.internal.*
import kotlin.internal.LowPriorityInOverloadResolution

public interface MigrationAid<T> : KSerializer<T> {
    override val descriptor: SerialDescriptor
        get() = error("Please migrate deprecated API")

    override fun serialize(encoder: Encoder, value: T) {
        error("Please migrate deprecated API")
    }

    override fun deserialize(decoder: Decoder): T {
        error("Please migrate deprecated API")
    }
}

@Deprecated(
    message = "Deprecated in the favour of top-level UnitSerializer() function",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("UnitSerializer()", imports = ["kotlinx.serialization.builtins.UnitSerializer"])
)
object UnitSerializer : MigrationAid<Unit>

@Deprecated(
    message = "Deprecated in the favour of Boolean.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Boolean.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object BooleanSerializer : MigrationAid<Boolean>

@Deprecated(
    message = "Deprecated in the favour of Byte.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Byte.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object ByteSerializer : MigrationAid<Byte>

@Deprecated(
    message = "Deprecated in the favour of Short.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Short.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object ShortSerializer : MigrationAid<Short>

@Deprecated(
    message = "Deprecated in the favour of Int.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Int.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object IntSerializer : MigrationAid<Int>

@Deprecated(
    message = "Deprecated in the favour of Long.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Long.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object LongSerializer : MigrationAid<Long>

@Deprecated(
    message = "Deprecated in the favour of Float.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Float.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object FloatSerializer : MigrationAid<Float>

@Deprecated(
    message = "Deprecated in the favour of Double.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Double.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object DoubleSerializer : MigrationAid<Double>

@Deprecated(
    message = "Deprecated in the favour of Char.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("Char.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object CharSerializer : MigrationAid<Char>

@Deprecated(
    message = "Deprecated in the favour of String.serializer() extension",
    level = DeprecationLevel.ERROR,
    replaceWith = ReplaceWith("String.serializer()", imports = ["kotlinx.serialization.builtins.serializer"])
)
object StringSerializer : MigrationAid<String>


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

@Deprecated(level = DeprecationLevel.ERROR,
    message = "Deprecated in the favour of the same extension from builtins package",
    replaceWith = ReplaceWith("nullable", imports = ["kotlinx.serialization.builtins.set"]))
@LowPriorityInOverloadResolution
val <T> KSerializer<T>.set: KSerializer<Set<T>>
    get() = LinkedHashSetSerializer(this)


// TODO
//val <K, V> Pair<KSerializer<K>, KSerializer<V>>.map: KSerializer<Map<K, V>>
//    get() = LinkedHashMapSerializer(this.first, this.second)
