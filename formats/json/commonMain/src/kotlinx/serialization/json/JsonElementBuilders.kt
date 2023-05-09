/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalContracts::class)

package kotlinx.serialization.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlin.contracts.*
import kotlin.jvm.JvmName

/**
 * Builds [JsonObject] with the given [builderAction] builder.
 * Example of usage:
 * ```
 * val json = buildJsonObject {
 *     put("booleanKey", true)
 *     putJsonArray("arrayKey") {
 *         for (i in 1..10) add(i)
 *     }
 *     putJsonObject("objectKey") {
 *         put("stringKey", "stringValue")
 *     }
 * }
 * ```
 */
public inline fun buildJsonObject(builderAction: JsonObjectBuilder.() -> Unit): JsonObject {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = JsonObjectBuilder()
    builder.builderAction()
    return builder.build()
}


/**
 * Builds [JsonArray] with the given [builderAction] builder.
 * Example of usage:
 * ```
 * val json = buildJsonArray {
 *     add(true)
 *     addJsonArray {
 *         for (i in 1..10) add(i)
 *     }
 *     addJsonObject {
 *         put("stringKey", "stringValue")
 *     }
 * }
 * ```
 */
public inline fun buildJsonArray(builderAction: JsonArrayBuilder.() -> Unit): JsonArray {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = JsonArrayBuilder()
    builder.builderAction()
    return builder.build()
}

/**
 * DSL builder for a [JsonObject]. To create an instance of builder, use [buildJsonObject] build function.
 */
@JsonDslMarker
public class JsonObjectBuilder @PublishedApi internal constructor() {

    private val content: MutableMap<String, JsonElement> = linkedMapOf()

    /**
     * Add the given JSON [element] to a resulting JSON object using the given [key].
     *
     * Returns the previous value associated with [key], or `null` if the key was not present.
     */
    public fun put(key: String, element: JsonElement): JsonElement? = content.put(key, element)

    @PublishedApi
    internal fun build(): JsonObject = JsonObject(content)
}

/**
 * Add the [JSON object][JsonObject] produced by the [builderAction] function to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
public fun JsonObjectBuilder.putJsonObject(key: String, builderAction: JsonObjectBuilder.() -> Unit): JsonElement? =
    put(key, buildJsonObject(builderAction))

/**
 * Add the [JSON array][JsonArray] produced by the [builderAction] function to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
public fun JsonObjectBuilder.putJsonArray(key: String, builderAction: JsonArrayBuilder.() -> Unit): JsonElement? =
    put(key, buildJsonArray(builderAction))

/**
 * Add the given boolean [value] to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
public fun JsonObjectBuilder.put(key: String, value: Boolean?): JsonElement? = put(key, JsonPrimitive(value))

/**
 * Add the given numeric [value] to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
public fun JsonObjectBuilder.put(key: String, value: Number?): JsonElement? = put(key, JsonPrimitive(value))

/**
 * Add the given string [value] to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
public fun JsonObjectBuilder.put(key: String, value: String?): JsonElement? = put(key, JsonPrimitive(value))

/**
 * Add `null` to a resulting JSON object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
@ExperimentalSerializationApi
@Suppress("UNUSED_PARAMETER") // allows to call `put("key", null)`
public fun JsonObjectBuilder.put(key: String, value: Nothing?): JsonElement? = put(key, JsonNull)

/**
 * DSL builder for a [JsonArray]. To create an instance of builder, use [buildJsonArray] build function.
 */
@JsonDslMarker
public class JsonArrayBuilder @PublishedApi internal constructor() {

    private val content: MutableList<JsonElement> = mutableListOf()

    /**
     * Adds the given JSON [element] to a resulting JSON array.
     *
     * Always returns `true` similarly to [ArrayList] specification.
     */
    public fun add(element: JsonElement): Boolean {
        content += element
        return true
    }

    /**
     * Adds the given JSON [elements] to a resulting JSON array.
     *
     * @return `true` if the list was changed as the result of the operation.
     */
    @ExperimentalSerializationApi
    public fun addAll(elements: Collection<JsonElement>): Boolean =
        content.addAll(elements)

    @PublishedApi
    internal fun build(): JsonArray = JsonArray(content)
}

/**
 * Adds the given boolean [value] to a resulting JSON array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.add(value: Boolean?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the given numeric [value] to a resulting JSON array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.add(value: Number?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the given string [value] to a resulting JSON array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.add(value: String?): Boolean = add(JsonPrimitive(value))

/**
 * Adds `null` to a resulting JSON array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
@ExperimentalSerializationApi
@Suppress("UNUSED_PARAMETER") // allows to call `add(null)`
public fun JsonArrayBuilder.add(value: Nothing?): Boolean = add(JsonNull)

/**
 * Adds the [JSON object][JsonObject] produced by the [builderAction] function to a resulting JSON array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.addJsonObject(builderAction: JsonObjectBuilder.() -> Unit): Boolean =
    add(buildJsonObject(builderAction))

/**
 * Adds the [JSON array][JsonArray] produced by the [builderAction] function to a resulting JSON array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.addJsonArray(builderAction: JsonArrayBuilder.() -> Unit): Boolean =
    add(buildJsonArray(builderAction))

/**
 * Adds the given string [values] to a resulting JSON array.
 *
 * @return `true` if the list was changed as the result of the operation.
 */
@JvmName("addAllStrings")
@ExperimentalSerializationApi
public fun JsonArrayBuilder.addAll(values: Collection<String?>): Boolean =
    addAll(values.map(::JsonPrimitive))

/**
 * Adds the given boolean [values] to a resulting JSON array.
 *
 * @return `true` if the list was changed as the result of the operation.
 */
@JvmName("addAllBooleans")
@ExperimentalSerializationApi
public fun JsonArrayBuilder.addAll(values: Collection<Boolean?>): Boolean =
    addAll(values.map(::JsonPrimitive))

/**
 * Adds the given numeric [values] to a resulting JSON array.
 *
 * @return `true` if the list was changed as the result of the operation.
 */
@JvmName("addAllNumbers")
@ExperimentalSerializationApi
public fun JsonArrayBuilder.addAll(values: Collection<Number?>): Boolean =
    addAll(values.map(::JsonPrimitive))

@DslMarker
internal annotation class JsonDslMarker
