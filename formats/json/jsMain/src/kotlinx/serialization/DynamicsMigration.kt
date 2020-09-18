/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "EXPERIMENTAL_API_USAGE")
package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlinx.serialization.json.decodeFromDynamic as dfd
import kotlinx.serialization.json.encodeToDynamic as etd
import kotlinx.serialization.modules.*
import kotlin.internal.*

private const val parserMessage = "DynamicObjectParser and its 'parse' method were unified with Json operations. " +
        "Please use Json's 'decodeFromDynamic' extension."

// ReplaceWith/typealias is missing intentionally because if we replace all instances of DynamicObjectParser with Json,
// we'll get incorrect `parse` deprecation (because of StringFormat.parse(serializer, string) overload)
@Deprecated(parserMessage, level = DeprecationLevel.ERROR)
@Suppress("DEPRECATION_ERROR")
public class DynamicObjectParser constructor(
    public val context: SerializersModule = EmptySerializersModule,
    public val configuration: JsonConfiguration = JsonConfiguration.Default
) {
    @Deprecated(
        parserMessage,
        ReplaceWith("Json(configuration, context).decodeFromDynamic(value)", imports = ["kotlinx.serialization.json.Json", "kotlinx.serialization.json.decodeFromDynamic"]),
        level = DeprecationLevel.ERROR
    )
    public inline fun <reified T : Any> parse(value: dynamic): T = noImpl()

    @Deprecated(
        parserMessage,
        ReplaceWith(
            "Json(configuration, context).decodeFromDynamic(deserializer, obj)",
            imports = ["kotlinx.serialization.json.Json", "kotlinx.serialization.json.decodeFromDynamic"]
        ),
        level = DeprecationLevel.ERROR
    )
    public fun <T> parse(obj: dynamic, deserializer: DeserializationStrategy<T>): T = noImpl()
}

private const val serializerMessage =
    "DynamicObjectSerializer and its 'serialize' method were unified with Json operations. " +
            "Please use Json's 'encodeToDynamic' extension."

@Deprecated(serializerMessage, ReplaceWith("Json", "kotlinx.serialization.json.Json"), level = DeprecationLevel.ERROR)
public typealias DynamicObjectSerializer = Json

@Deprecated(serializerMessage, ReplaceWith("encodeToDynamic(strategy, obj)", imports = ["kotlinx.serialization.json.decodeFromDynamic"]), level = DeprecationLevel.ERROR)
public fun <T> Json.serialize(strategy: SerializationStrategy<T>, obj: T): dynamic = noImpl()

@Deprecated(serializerMessage, ReplaceWith("encodeToDynamic<T>(obj)", imports = ["kotlinx.serialization.json.encodeToDynamic"]), level = DeprecationLevel.ERROR)
public inline fun <reified T : Any> Json.serialize(obj: T): dynamic = noImpl()

@PublishedApi
internal fun noImpl(): Nothing = throw UnsupportedOperationException("Not implemented, should not be called")

@Deprecated(
    "decodeFromDynamic was moved to kotlinx.serialization.json package",
    ReplaceWith("decodeFromDynamic(deserializer, dynamic)", imports = ["kotlinx.serialization.json.decodeFromDynamic"]),
    level = DeprecationLevel.ERROR
)
@LowPriorityInOverloadResolution
public fun <T> Json.decodeFromDynamic(deserializer: DeserializationStrategy<T>, dynamic: dynamic): T {
    return Json.dfd(deserializer, dynamic)
}

@Deprecated(
    "decodeFromDynamic was moved to kotlinx.serialization.json package",
    ReplaceWith("decodeFromDynamic(dynamic)", imports = ["kotlinx.serialization.json.decodeFromDynamic"]),
    level = DeprecationLevel.ERROR
)
@LowPriorityInOverloadResolution
public inline fun <reified T> Json.decodeFromDynamic(dynamic: dynamic): T =
    dfd(serializersModule.serializer(), dynamic)

@Deprecated(
    "encodeToDynamic was moved to kotlinx.serialization.json package",
    ReplaceWith("encodeToDynamic(serializer, value)", imports = ["kotlinx.serialization.json.encodeToDynamic"]),
    level = DeprecationLevel.ERROR
)
@LowPriorityInOverloadResolution
public fun <T> Json.encodeToDynamic(serializer: SerializationStrategy<T>, value: T): dynamic {
    return etd(serializer, value)
}

@Deprecated(
    "encodeToDynamic was moved to kotlinx.serialization.json package",
    ReplaceWith("encodeToDynamic(value)", imports = ["kotlinx.serialization.json.encodeToDynamic"]),
    level = DeprecationLevel.ERROR
)
@LowPriorityInOverloadResolution
public inline fun <reified T> Json.encodeToDynamic(value: T): dynamic =
    etd(serializersModule.serializer(), value)
