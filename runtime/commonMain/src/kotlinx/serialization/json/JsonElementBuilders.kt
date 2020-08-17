/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */
@file:OptIn(ExperimentalContracts::class)

package kotlinx.serialization.json

import kotlin.contracts.*

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

    /**
     * Adds given [value] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(
        message = infixToDeprecated,
        replaceWith = ReplaceWith("put(this, value)"),
        level = DeprecationLevel.ERROR
    )
    public infix fun String.to(value: JsonElement) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = value
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(
        message = infixToDeprecated,
        replaceWith = ReplaceWith("put(this, value)"),
        level = DeprecationLevel.ERROR
    )
    public infix fun String.to(value: Number?) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(
        message = infixToDeprecated,
        replaceWith = ReplaceWith("put(this, value)"),
        level = DeprecationLevel.ERROR
    )
    public infix fun String.to(value: Boolean?) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(
        message = infixToDeprecated,
        replaceWith = ReplaceWith("put(this, value)"),
        level = DeprecationLevel.ERROR
    )
    public infix fun String.to(value: String?) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = JsonPrimitive(value)
    }
}

/**
 * Add the [JSON][JsonObject] produced by the [builderAction] function to a resulting json object using the given [key].
 *
 * Returns the previous value associated with [key], or `null` if the key was not present.
 */
public fun JsonObjectBuilder.putJsonObject(key: String, builderAction: JsonObjectBuilder.() -> Unit): JsonElement? =
    put(key, buildJsonObject(builderAction))

/**
 * Add the [JSON array][JsonArray] produced by the [builderAction] function to a resulting json object using the given [key].
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
 * DSL builder for a [JsonArray]. To create an instance of builder, use [buildJsonArray] build function.
 */
@JsonDslMarker
public class JsonArrayBuilder @PublishedApi internal constructor() {

    private val content: MutableList<JsonElement> = mutableListOf()

    /**
     * Adds the given JSON [element] to a resulting array.
     *
     * Always returns `true` similarly to [ArrayList] specification.
     */
    public fun add(element: JsonElement): Boolean {
        content += element
        return true
    }

    @PublishedApi
    internal fun build(): JsonArray = JsonArray(content)

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"), level = DeprecationLevel.ERROR)
    public operator fun String?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"), level = DeprecationLevel.ERROR)
    public operator fun Number?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"), level = DeprecationLevel.ERROR)
    public operator fun Boolean?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"), level = DeprecationLevel.ERROR)
    public operator fun JsonElement.unaryPlus() {
        this@JsonArrayBuilder.content.add(this)
    }
}

/**
 * Adds the given boolean [value] to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.add(value: Boolean?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the given numeric [value] to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.add(value: Number?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the given string [value] to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.add(value: String?): Boolean = add(JsonPrimitive(value))

/**
 * Adds the [JSON][JsonObject] produced by the [builderAction] function to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.addJsonObject(builderAction: JsonObjectBuilder.() -> Unit): Boolean =
    add(buildJsonObject(builderAction))

/**
 * Adds the [JSON][JsonArray] produced by the [builderAction] function to a resulting array.
 *
 * Always returns `true` similarly to [ArrayList] specification.
 */
public fun JsonArrayBuilder.addJsonArray(builderAction: JsonArrayBuilder.() -> Unit): Boolean =
    add(buildJsonArray(builderAction))

private const val infixToDeprecated = "Infix 'to' operator is deprecated for removal for the favour of 'add'"
private const val unaryPlusDeprecated = "Unary plus is deprecated for removal for the favour of 'add'"

@DslMarker
internal annotation class JsonDslMarker
