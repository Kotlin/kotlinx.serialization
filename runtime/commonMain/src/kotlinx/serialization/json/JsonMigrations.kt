/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:Suppress("INVISIBLE_MEMBER", "INVISIBLE_REFERENCE", "UNUSED", "UNUSED_PARAMETER", "DEPRECATION_ERROR")

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
    message = "This method was renamed to parseToJsonElement during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("parseToJsonElement(string)")
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

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This interface was renamed to JsonDecoder during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("JsonDecoder")
)
public interface JsonInput {
    public fun decodeJson(): JsonElement
}

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This interface was renamed to JsonEncoder during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("JsonEncoder")
)
public interface JsonOutput {
    public fun encodeJson(element: JsonElement)
}


@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this is JsonNull")
)
public val JsonElement.isNull: Boolean
    get() = noImpl()


// Array
@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[index].jsonPrimitive")
)
public fun JsonArray.getPrimitive(index: Int): JsonPrimitive = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[index].jsonObject")
)
public fun JsonArray.getObject(index: Int): JsonObject = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[index].jsonArray")
)
public fun JsonArray.getArray(index: Int): JsonArray = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this.getOrNull(index)?.jsonPrimitive")
)
public fun JsonArray.getPrimitiveOrNull(index: Int): JsonPrimitive? = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this.getOrNull(index)?.jsonObject")
)
public fun JsonArray.getObjectOrNull(index: Int): JsonObject? = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this.getOrNull(index)?.jsonArray")
)
public fun JsonArray.getArrayOrNull(index: Int): JsonArray? = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[index] as J")
)
public fun <J : JsonElement> JsonArray.getAs(index: Int): J = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this.getOrNull(index) as? J")
)
public fun <J : JsonElement> JsonArray.getAsOrNull(index: Int): J? = noImpl()

// Object

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("getValue(key).jsonPrimitive")
)
public fun JsonObject.getPrimitive(key: String): JsonPrimitive = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("getValue(key).jsonObject")
)
public fun JsonObject.getObject(key: String): JsonObject = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("getValue(key).jsonArray")
)
public fun JsonObject.getArray(key: String): JsonArray = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[key]?.jsonPrimitive")
)
public fun JsonObject.getPrimitiveOrNull(key: String): JsonPrimitive? = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[key]?.jsonObject")
)
public fun JsonObject.getObjectOrNull(key: String): JsonObject? = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[key]?.jsonArray")
)
public fun JsonObject.getArrayOrNull(key: String): JsonArray? = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("getValue(key) as J")
)
public fun <J : JsonElement> JsonObject.getAs(key: String): J = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("this[key] as? J")
)
public fun <J : JsonElement> JsonObject.getAsOrNull(key: String): J? = noImpl()


@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive")
)
public val JsonElement.primitive: JsonPrimitive get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This method was deprecated during serialization 1.0 stabilization"
)
@kotlin.internal.LowPriorityInOverloadResolution // to work with JsonObject properly
public operator fun JsonElement.contains(key: String): Boolean = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.int")
)
public val JsonElement.int: Int get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.intOrNull")
)
public val JsonElement.intOrNull: Int? get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.long")
)
public val JsonElement.long: Long get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.longOrNull")
)
public val JsonElement.longOrNull: Long? get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.double")
)
public val JsonElement.double: Double get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.doubleOrNull")
)
public val JsonElement.doubleOrNull: Double? get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.float")
)
public val JsonElement.float: Float get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.floatOrNull")
)
public val JsonElement.floatOrNull: Float? get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.boolean")
)
public val JsonElement.boolean: Boolean get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.booleanOrNull")
)
public val JsonElement.booleanOrNull: Boolean? get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.content")
)
public val JsonElement.content: String get() = noImpl()

@Deprecated(
    level = DeprecationLevel.ERROR,
    message = "This property was deprecated during serialization 1.0 stabilization",
    replaceWith = ReplaceWith("jsonPrimitive.contentOrNull")
)
public val JsonElement.contentOrNull: String? get() = noImpl()
