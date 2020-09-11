/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("unused")

package kotlinx.serialization.json

import kotlinx.serialization.*
import kotlinx.serialization.json.internal.*

/**
 * Class representing single JSON element.
 * Can be [JsonPrimitive], [JsonArray] or [JsonObject].
 *
 * [JsonElement.toString] properly prints JSON tree as valid JSON, taking into account quoted values and primitives.
 * Whole hierarchy is serializable, but only when used with [Json] as [JsonElement] is purely JSON-specific structure
 * which has a meaningful schemaless semantics only for JSON.
 *
 * The whole hierarchy is [serializable][Serializable] only by [Json] format.
 */
@Serializable(JsonElementSerializer::class)
public sealed class JsonElement

/**
 * Class representing JSON primitive value.
 * JSON primitives include numbers, strings, booleans and special null value [JsonNull].
 */
@Serializable(JsonPrimitiveSerializer::class)
public sealed class JsonPrimitive : JsonElement() {

    /**
     * Indicates whether the primitive was explicitly constructed from [String] and
     * whether it should be serialized as one. E.g. `JsonPrimitive("42")` is represented
     * by a string, while `JsonPrimitive(42)` is not.
     * These primitives will be serialized as `42` and `"42"` respectively.
     */
    public abstract val isString: Boolean

    /**
     * Content of given element without quotes. For [JsonNull] this methods returns `null`
     */
    public abstract val content: String

    public override fun toString(): String = content
}

/**
 * Creates [JsonPrimitive] from the given boolean.
 */
public fun JsonPrimitive(value: Boolean?): JsonPrimitive {
    if (value == null) return JsonNull
    return JsonLiteral(value, isString = false)
}

/**
 * Creates [JsonPrimitive] from the given number.
 */
public fun JsonPrimitive(value: Number?): JsonPrimitive {
    if (value == null) return JsonNull
    return JsonLiteral(value, isString = false)
}

/**
 * Creates [JsonPrimitive] from the given string.
 */
public fun JsonPrimitive(value: String?): JsonPrimitive {
    if (value == null) return JsonNull
    return JsonLiteral(value, isString = true)
}

// JsonLiteral is deprecated for public use and no longer available. Please use JsonPrimitive instead
internal class JsonLiteral internal constructor(
    body: Any,
    public override val isString: Boolean
) : JsonPrimitive() {
    public override val content: String = body.toString()

    public override fun toString(): String =
        if (isString) buildString { printQuoted(content) }
        else content

    // Compare by `content` and `isString`, because body can be kotlin.Long=42 or kotlin.String="42"
    public override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as JsonLiteral
        if (isString != other.isString) return false
        if (content != other.content) return false
        return true
    }

    public override fun hashCode(): Int {
        var result = isString.hashCode()
        result = 31 * result + content.hashCode()
        return result
    }
}

/**
 * Class representing JSON `null` value
 */
@Serializable(JsonNullSerializer::class)
public object JsonNull : JsonPrimitive() {
    override val isString: Boolean get() = false
    override val content: String = "null"
}

/**
 * Class representing JSON object, consisting of name-value pairs, where value is arbitrary [JsonElement]
 *
 * Since this class also implements [Map] interface, you can use
 * traditional methods like [Map.get] or [Map.getValue] to obtain Json elements.
 */
@Serializable(JsonObjectSerializer::class)
public class JsonObject(private val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {
    public override fun equals(other: Any?): Boolean = content == other
    public override fun hashCode(): Int = content.hashCode()
    public override fun toString(): String {
        return content.entries.joinToString(
            separator = ",",
            prefix = "{",
            postfix = "}",
            transform = {(k, v) -> """"$k":$v"""}
        )
    }
}

/**
 * Class representing JSON array, consisting of indexed values, where value is arbitrary [JsonElement]
 *
 * Since this class also implements [List] interface, you can use
 * traditional methods like [List.get] or [List.getOrNull] to obtain Json elements.
 */
@Serializable(JsonArraySerializer::class)
public class JsonArray(private val content: List<JsonElement>) : JsonElement(), List<JsonElement> by content {
    public override fun equals(other: Any?): Boolean = content == other
    public override fun hashCode(): Int = content.hashCode()
    public override fun toString(): String = content.joinToString(prefix = "[", postfix = "]", separator = ",")
}

/**
 * Convenience method to get current element as [JsonPrimitive]
 * @throws IllegalArgumentException if current element is not a [JsonPrimitive]
 */
public val JsonElement.jsonPrimitive: JsonPrimitive
    get() = this as? JsonPrimitive ?: error("JsonPrimitive")

/**
 * Convenience method to get current element as [JsonObject]
 * @throws IllegalArgumentException if current element is not a [JsonObject]
 */
public val JsonElement.jsonObject: JsonObject
    get() = this as? JsonObject ?: error("JsonObject")

/**
 * Convenience method to get current element as [JsonArray]
 * @throws IllegalArgumentException if current element is not a [JsonArray]
 */
public val JsonElement.jsonArray: JsonArray
    get() = this as? JsonArray ?: error("JsonArray")

/**
 * Convenience method to get current element as [JsonNull]
 * @throws IllegalArgumentException if current element is not a [JsonNull]
 */
public val JsonElement.jsonNull: JsonNull
    get() = this as? JsonNull ?: error("JsonNull")

/**
 * Returns content of the current element as int
 * @throws NumberFormatException if current element is not a valid representation of number
 */
public val JsonPrimitive.int: Int get() = content.toInt()

/**
 * Returns content of the current element as int or `null` if current element is not a valid representation of number
 */
public val JsonPrimitive.intOrNull: Int? get() = content.toIntOrNull()

/**
 * Returns content of current element as long
 * @throws NumberFormatException if current element is not a valid representation of number
 */
public val JsonPrimitive.long: Long get() = content.toLong()

/**
 * Returns content of current element as long or `null` if current element is not a valid representation of number
 */
public val JsonPrimitive.longOrNull: Long? get() = content.toLongOrNull()

/**
 * Returns content of current element as double
 * @throws NumberFormatException if current element is not a valid representation of number
 */
public val JsonPrimitive.double: Double get() = content.toDouble()

/**
 * Returns content of current element as double or `null` if current element is not a valid representation of number
 */
public val JsonPrimitive.doubleOrNull: Double? get() = content.toDoubleOrNull()

/**
 * Returns content of current element as float
 * @throws NumberFormatException if current element is not a valid representation of number
 */
public val JsonPrimitive.float: Float get() = content.toFloat()

/**
 * Returns content of current element as float or `null` if current element is not a valid representation of number
 */
public val JsonPrimitive.floatOrNull: Float? get() = content.toFloatOrNull()

/**
 * Returns content of current element as boolean
 * @throws IllegalStateException if current element doesn't represent boolean
 */
public val JsonPrimitive.boolean: Boolean get() = content.toBooleanStrict()

/**
 * Returns content of current element as boolean or `null` if current element is not a valid representation of boolean
 */
public val JsonPrimitive.booleanOrNull: Boolean? get() = content.toBooleanStrictOrNull()

/**
 * Content of the given element without quotes or `null` if current element is [JsonNull]
 */
public val JsonPrimitive.contentOrNull: String? get() = if (this is JsonNull) null else content

private fun JsonElement.error(element: String): Nothing =
    throw IllegalArgumentException("Element ${this::class} is not a $element")

@PublishedApi
internal fun unexpectedJson(key: String, expected: String): Nothing =
    throw IllegalArgumentException("Element $key is not a $expected")
