/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier")

package kotlinx.serialization.json

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.internal.*

/**
 * Class representing single JSON element.
 * Can be [JsonPrimitive], [JsonArray] or [JsonObject].
 *
 * [JsonElement.toString] properly prints JSON tree as valid JSON, taking into account quoted values and primitives.
 * Whole hierarchy is serializable, but only when used with [Json] as [JsonElement] is purely JSON-specific structure
 * which has a meaningful schameless semantics only for JSON.
 */
@Serializable(JsonElementSerializer::class)
public sealed class JsonElement {

    /**
     * Convenience method to get current element as [JsonPrimitive]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonPrimitive]
     */
    public open val primitive: JsonPrimitive
        get() = error("JsonLiteral")

    /**
     * Convenience method to get current element as [JsonObject]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonObject]
     */
    public open val jsonObject: JsonObject
        get() = error("JsonObject")

    /**
     * Convenience method to get current element as [JsonArray]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonArray]
     */
    public open val jsonArray: JsonArray
        get() = error("JsonArray")

    /**
     * Convenience method to get current element as [JsonNull]
     * @throws JsonElementTypeMismatchException is current element is not a [JsonNull]
     */
    public open val jsonNull: JsonNull
        get() = error("JsonPrimitive")

    /**
     * Checks whether current element is [JsonNull]
     */
    public val isNull: Boolean
        get() = this === JsonNull

    private fun error(element: String): Nothing =
        throw JsonElementTypeMismatchException(this::class.toString(), element)
}

/**
 * Class representing JSON primitive value. Can be either [JsonLiteral] or [JsonNull].
 */
@Serializable(JsonPrimitiveSerializer::class)
public sealed class JsonPrimitive : JsonElement() {

    /**
     * Content of given element without quotes. For [JsonNull] this methods returns `null`
     */
    public abstract val content: String

    /**
     * Content of the given element without quotes or `null` if current element is [JsonNull]
     */
    public abstract val contentOrNull: String?

    @Suppress("LeakingThis")
    public final override val primitive: JsonPrimitive = this

    /**
     * Returns content of current element as int
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    public val int: Int get() = content.toInt()

    /**
     * Returns content of current element as int or `null` if current element is not a valid representation of number
     **/
    public val intOrNull: Int? get() = content.toIntOrNull()

    /**
     * Returns content of current element as long
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    public val long: Long get() = content.toLong()

    /**
     * Returns content of current element as long or `null` if current element is not a valid representation of number
     */
    public val longOrNull: Long? get() = content.toLongOrNull()

    /**
     * Returns content of current element as double
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    public val double: Double get() = content.toDouble()

    /**
     * Returns content of current element as double or `null` if current element is not a valid representation of number
     */
    public val doubleOrNull: Double? get() = content.toDoubleOrNull()

    /**
     * Returns content of current element as float
     * @throws NumberFormatException if current element is not a valid representation of number
     */
    public val float: Float get() = content.toFloat()

    /**
     * Returns content of current element as float or `null` if current element is not a valid representation of number
     */
    public val floatOrNull: Float? get() = content.toFloatOrNull()

    /**
     * Returns content of current element as boolean
     * @throws IllegalStateException if current element doesn't represent boolean
     */
    public val boolean: Boolean get() = content.toBooleanStrict()

    /**
     * Returns content of current element as boolean or `null` if current element is not a valid representation of boolean
     */
    public val booleanOrNull: Boolean? get() = content.toBooleanStrictOrNull()

    public override fun toString() = content
}

/**
 * Class representing JSON literals: numbers, booleans and string.
 * Strings are always quoted.
 *
 * [isString] indicates whether literal was explicitly constructed as a [String] and
 * whether it should be serialized as one. E.g. `JsonLiteral("42", false)`
 * and `JsonLiteral("42", true)` will be serialized as `42` and `"42"` respectively.
 */
@Serializable(JsonLiteralSerializer::class) // TODO val for body is workaround for plugin bug
public class JsonLiteral internal constructor(val body: Any, public val isString: Boolean) : JsonPrimitive() {
    public override val content = body.toString()
    public override val contentOrNull: String = content

    /**
     * Creates number literal
     */
    public constructor(number: Number) : this(number, false)

    /**
     * Creates boolean literal
     */
    public constructor(boolean: Boolean) : this(boolean, false)

    /**
     * Creates quoted string literal
     */
    public constructor(string: String) : this(string, true)

    public override fun toString() =
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
    override val jsonNull: JsonNull = this
    override val content: String = "null"
    override val contentOrNull: String? = null
}

/**
 * Class representing JSON object, consisting of name-value pairs, where value is arbitrary [JsonElement]
 */
@Serializable(JsonObjectSerializer::class)
public data class JsonObject(val content: Map<String, JsonElement>) : JsonElement(), Map<String, JsonElement> by content {

    override val jsonObject: JsonObject = this

    /**
     * Returns [JsonElement] associated with given [key]
     * @throws NoSuchElementException if element is not present
     */
    public override fun get(key: String): JsonElement = content[key] ?: throw NoSuchElementException("Element $key is missing")

    /**
     * Returns [JsonElement] associated with given [key] or `null` if element is not present
     */
    public fun getOrNull(key: String): JsonElement? = content[key]

    /**
     * Returns [JsonPrimitive] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    public fun getPrimitive(key: String): JsonPrimitive = get(key) as? JsonPrimitive
            ?: unexpectedJson(key, "JsonPrimitive")

    /**
     * Returns [JsonObject] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    public fun getObject(key: String): JsonObject = get(key) as? JsonObject
            ?: unexpectedJson(key, "JsonObject")

    /**
     * Returns [JsonArray] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    public fun getArray(key: String): JsonArray = get(key) as? JsonArray
            ?: unexpectedJson(key, "JsonArray")

    /**
     * Returns [JsonPrimitive] associated with given [key] or `null` if element
     * is not present or has different type
     */
    public fun getPrimitiveOrNull(key: String): JsonPrimitive? = content[key] as? JsonPrimitive

    /**
     * Returns [JsonObject] associated with given [key] or `null` if element
     * is not present or has different type
     */
    public fun getObjectOrNull(key: String): JsonObject? = content[key] as? JsonObject

    /**
     * Returns [JsonArray] associated with given [key] or `null` if element
     * is not present or has different type
     */
    public fun getArrayOrNull(key: String): JsonArray? = content[key] as? JsonArray

    /**
     * Returns [J] associated with given [key]
     *
     * @throws NoSuchElementException if element is not present
     * @throws JsonElementTypeMismatchException if element is present, but has invalid type
     */
    public inline fun <reified J : JsonElement> getAs(key: String): J = get(key) as? J
            ?: unexpectedJson(key, J::class.toString())

    /**
     * Returns [J] associated with given [key] or `null` if element
     * is not present or has different type
     */
    public inline fun <reified J : JsonElement> lookup(key: String): J? = content[key] as? J

    public override fun toString(): String {
        return content.entries.joinToString(
            prefix = "{",
            postfix = "}",
            transform = {(k, v) -> """"$k": $v"""}
        )
    }
}

@Serializable(JsonArraySerializer::class)
public data class JsonArray(val content: List<JsonElement>) : JsonElement(), List<JsonElement> by content {

    public override val jsonArray: JsonArray = this

    /**
     * Returns [index]-th element of an array as [JsonPrimitive]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    public fun getPrimitive(index: Int) = content[index] as? JsonPrimitive
            ?: unexpectedJson("at $index", "JsonPrimitive")

    /**
     * Returns [index]-th element of an array as [JsonObject]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    public fun getObject(index: Int) = content[index] as? JsonObject
            ?: unexpectedJson("at $index", "JsonObject")

    /**
     * Returns [index]-th element of an array as [JsonArray]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    public fun getArray(index: Int) = content[index] as? JsonArray
            ?: unexpectedJson("at $index", "JsonArray")

    /**
     * Returns [index]-th element of an array as [JsonPrimitive] or `null` if element is missing or has different type
     */
    public fun getPrimitiveOrNull(index: Int) = content.getOrNull(index) as? JsonPrimitive

    /**
     * Returns [index]-th element of an array as [JsonObject] or `null` if element is missing or has different type
     */
    public fun getObjectOrNull(index: Int) = content.getOrNull(index) as? JsonObject

    /**
     * Returns [index]-th element of an array as [JsonArray] or `null` if element is missing or has different type
     */
    public fun getArrayOrNull(index: Int) = content.getOrNull(index) as? JsonArray

    /**
     * Returns [index]-th element of an array as [J]
     * @throws JsonElementTypeMismatchException if element has invalid type
     */
    public inline fun <reified J : JsonElement> getAs(index: Int): J = content[index] as? J
            ?: unexpectedJson("at $index", J::class.toString())

    /**
     * Returns [index]-th element of an array as [J] or `null` if element is missing or has different type
     */
    public inline fun <reified J : JsonElement> getAsOrNull(index: Int): J? = content.getOrNull(index) as? J

    public override fun toString() = content.joinToString(prefix = "[", postfix = "]")
}

@PublishedApi
internal fun unexpectedJson(key: String, expected: String): Nothing =
    throw JsonElementTypeMismatchException(key, expected)
