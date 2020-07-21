/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.json.*
import kotlinx.serialization.modules.*

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
        ReplaceWith("Json(configuration, context).decodeFromDynamic(value)", "kotlinx.serialization.json.Json"),
        level = DeprecationLevel.ERROR
    )
    public inline fun <reified T : Any> parse(value: dynamic): T = noImpl()

    @Deprecated(
        parserMessage,
        ReplaceWith(
            "Json(configuration, context).decodeFromDynamic(deserializer, obj)",
            "kotlinx.serialization.json.Json"
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

@Deprecated(serializerMessage, ReplaceWith("encodeToDynamic(strategy, obj)"), level = DeprecationLevel.ERROR)
public fun <T> Json.serialize(strategy: SerializationStrategy<T>, obj: T): dynamic = noImpl()

@Deprecated(serializerMessage, ReplaceWith("encodeToDynamic<T>(obj)"), level = DeprecationLevel.ERROR)
public inline fun <reified T : Any> Json.serialize(obj: T): dynamic = noImpl()
