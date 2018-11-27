/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.json

/**
 * Builds [JsonObject] with given [init] builder.
 */
public fun json(init: JsonObjectBuilder.() -> Unit): JsonObject {
    val builder = JsonObjectBuilder()
    builder.init()
    return JsonObject(builder.content)
}

/**
 * Builds [JsonArray] with given [init] builder.
 */
public fun jsonArray(init: JsonArrayBuilder.() -> Unit): JsonArray {
    val builder = JsonArrayBuilder()
    builder.init()
    return JsonArray(builder.content)
}

/**
 * DSL builder for a [JsonArray].
 */
public class JsonArrayBuilder internal constructor() {

    internal val content: MutableList<JsonElement> = mutableListOf()

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    public operator fun String?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    public operator fun Number?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray] as [JsonPrimitive].
     */
    public operator fun Boolean?.unaryPlus() {
        content.add(JsonPrimitive(this))
    }

    /**
     * Adds [this] value to the current [JsonArray].
     */
    public operator fun JsonElement.unaryPlus() {
        this@JsonArrayBuilder.content.add(this)
    }
}

/**
 * DSL builder for a [JsonObject].
 */
public class JsonObjectBuilder internal constructor() {

    internal val content: MutableMap<String, JsonElement> = linkedMapOf()

    /**
     * Adds given [value] to the current [JsonObject] with [this] as a key.
     */
    public infix fun String.to(value: JsonElement) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = value
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    public infix fun String.to(value: Number?) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    public infix fun String.to(value: Boolean?) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = JsonPrimitive(value)
    }

    /**
     * Adds given [value] as [JsonPrimitive] to the current [JsonObject] with [this] as a key.
     */
    public infix fun String.to(value: String?) {
        require(content[this] == null) {"Key $this is already registered in builder"}
        content[this] = JsonPrimitive(value)
    }
}
