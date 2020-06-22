/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.*

private const val message =
    "Top-level JSON instances are deprecated for removal in the favour of user-configured one. " +
            "You can either use a Json top-level object, configure your own instance  via 'Json {}' builder-like constructor, " +
            "'Json(JsonConfiguration)' constructor or by tweaking stable configuration 'Json(JsonConfiguration.Stable.copy(prettyPrint = true))'"

private fun noImpl(): Nothing = throw UnsupportedOperationException("Not implemented, should not be called")

@UnstableDefault
@Deprecated(message = message, level = DeprecationLevel.ERROR)
public val Json.Default.plain: Json
    get() = noImpl()

@UnstableDefault
@Deprecated(message = message, level = DeprecationLevel.ERROR)
public val Json.Default.unquoted: Json
    get() = noImpl()

@UnstableDefault
@Deprecated(message = message, level = DeprecationLevel.ERROR)
public val Json.Default.indented: Json
    get() = noImpl()

@UnstableDefault
@Deprecated(message = message, level = DeprecationLevel.ERROR)
public val Json.Default.nonstrict: Json
    get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was renamed to encodeToJsonElement during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("encodeToJsonElement(serializer, value)")
)
public fun <T> Json.toJson(serializer: SerializationStrategy<T>, value: T): JsonElement = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was renamed to encodeToJsonElement during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("encodeToJsonElement(value)")
)
public fun <T : Any> Json.toJson(value: T): JsonElement = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was renamed to parseJsonElement during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("parseJsonElement(string)")
)
public fun Json.parseJson(string: String): JsonElement = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was renamed to decodeFromJsonElement during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("decodeFromJsonElement(deserializer, value)")
)
public fun <T> Json.fromJson(deserializer: DeserializationStrategy<T>, json: JsonElement): T = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was renamed to decodeFromJsonElement during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("decodeFromJsonElement(value)")
)
public fun <T : Any> Json.fromJson(tree: JsonElement): T = noImpl()
