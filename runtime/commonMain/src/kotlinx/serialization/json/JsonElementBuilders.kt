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
 * val json = buildJson {
 *     add("booleanKey", true)
 *     addArray("arrayKey") {
 *         for (i in 1..10) add(i)
 *     }
 *     addJson("objectKey") {
 *         add("stringKey", "stringValue")
 *     }
 * }
 * ```
 */
public fun buildJson(builderAction: JsonObjectBuilder.() -> Unit): JsonObject {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = JsonObjectBuilder()
    builder.builderAction()
    return builder.build()
}

@Deprecated(
    "json function deprecated for removal to be consistent with a standard library",
    replaceWith = ReplaceWith("buildJson"),
    level = DeprecationLevel.ERROR
)
public fun json(init: JsonObjectBuilder.() -> Unit): JsonObject = buildJson(init)

/**
 * Builds [JsonArray] with the given [builderAction] builder.
 * Example of usage:
 * ```
 * val json = buildJsonArray {
 *     add(true)
 *     addArray {
 *         for (i in 1..10) add(i)
 *     }
 *     addJson {
 *         add("stringKey", "stringValue")
 *     }
 * }
 * ```
 */
public fun buildJsonArray(builderAction: JsonArrayBuilder.() -> Unit): JsonArray {
    contract { callsInPlace(builderAction, InvocationKind.EXACTLY_ONCE) }
    val builder = JsonArrayBuilder()
    builder.builderAction()
    return builder.build()
}

@Deprecated(
    "jsonArray function deprecated for removal to be consistent with a standard library",
    replaceWith = ReplaceWith("buildJsonArray"),
    level = DeprecationLevel.ERROR
)
public fun jsonArray(init: JsonArrayBuilder.() -> Unit): JsonArray = buildJsonArray(init)

/**
 * DSL builder for a [JsonObject]. To create an instance of builder, use [buildJson] build function.
 */
@JsonDslMarker
public class JsonObjectBuilder internal constructor() {

    private val content: MutableMap<String, JsonElement> = linkedMapOf()

    /**
     * Add the [JSON][JsonObject] produced by the [builderAction] function
     * to a resulting json object using the given [key].
     */
    public fun addJson(key: String, builderAction: JsonObjectBuilder.() -> Unit) {
        content[key] = buildJson(builderAction)
    }

    /**
     * Add the [JSON array][JsonArray] produced by the [builderAction] function
     * to a resulting json object using the given [key].
     */
    public fun addArray(key: String, builderAction: JsonArrayBuilder.() -> Unit) {
        content[key] = buildJsonArray(builderAction)
    }

    /**
     * Add the given JSON [element] to a resulting JSON object using the given [key].
     */
    public fun add(key: String, element: JsonElement) {
        content[key] = element
    }

    /**
     * Add the given boolean [value] to a resulting JSON object using the given [key].
     */
    public fun add(key: String, value: Boolean?) {
        content[key] = JsonPrimitive(value)
    }

    /**
     * Add the given numeric [value] to a resulting JSON object using the given [key].
     */
    public fun add(key: String, value: Number?) {
        content[key] = JsonPrimitive(value)
    }

    /**
     * Add the given string [value] to a resulting JSON object using the given [key].
     */
    public fun add(key: String, value: String?) {
        content[key] = JsonPrimitive(value)
    }

    internal fun build(): JsonObject = JsonObject(content)

    /**
     * Adds given [value] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(message = infixToDeprecated, replaceWith = ReplaceWith("add(this, value)"))
    public infix fun String.to(value: JsonElement) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = value
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(message = infixToDeprecated, replaceWith = ReplaceWith("add(this, value)"))
    public infix fun String.to(value: Number?) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(message = infixToDeprecated, replaceWith = ReplaceWith("add(this, value)"))
    public infix fun String.to(value: Boolean?) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    @Deprecated(message = infixToDeprecated, replaceWith = ReplaceWith("add(this, value)"))
    public infix fun String.to(value: String?) {
        require(content[this] == null) { "Key $this is already registered in builder" }
        content[this] = JsonPrimitive(value)
    }
}

/**
 * DSL builder for a [JsonArray]. To create an instance of builder, use [buildJsonArray] build function.
 */
@JsonDslMarker
public class JsonArrayBuilder internal constructor() {

    private val content: MutableList<JsonElement> = mutableListOf()


    /**
     * Add the [JSON][JsonObject] produced by the [builderAction] function to a resulting array.
     */
    public fun addJson(builderAction: JsonObjectBuilder.() -> Unit) {
        content += buildJson(builderAction)
    }

    /**
     * Add the [JSON][JsonArray] produced by the [builderAction] function to a resulting array.
     */
    public fun addArray(builderAction: JsonArrayBuilder.() -> Unit) {
        content.add(buildJsonArray(builderAction))
    }

    /**
     * Add the given boolean [value] to a resulting array.
     */
    public fun add(value: Boolean?) {
        content += JsonPrimitive(value)
    }

    /**
     * Add the given numeric [value] to a resulting array.
     */
    public fun add(value: Number?) {
        content += JsonPrimitive(value)
    }

    /**
     * Add the given string [value] to a resulting array.
     */
    public fun add(value: String?) {
        content += JsonPrimitive(value)
    }

    /**
     * Add the given JSON [element] to a resulting array.
     */
    public fun add(element: JsonElement) {
        content += element
    }

    internal fun build(): JsonArray = JsonArray(content)

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"))
    public operator fun String?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"))
    public operator fun Number?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"))
    public operator fun Boolean?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray].
     */
    @Deprecated(message = unaryPlusDeprecated, replaceWith = ReplaceWith("add(this)"))
    public operator fun JsonElement.unaryPlus() {
        this@JsonArrayBuilder.content.add(this)
    }
}


private const val infixToDeprecated = "Infix 'to' operator is deprecated for removal for the favour of 'add'"
private const val unaryPlusDeprecated = "Unary plus is deprecated for removal for the favour of 'add'"

@DslMarker
internal annotation class JsonDslMarker
