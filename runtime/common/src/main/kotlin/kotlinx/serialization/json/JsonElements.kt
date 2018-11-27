/*
 * Copyright 2017-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("RedundantVisibilityModifier", "FunctionName")

package kotlinx.serialization.json

/**
 * Convenience method to create [JsonPrimitive] from given boolean.
 * Returns [JsonNull] if [value] is `null` or [JsonPrimitive] otherwise
 */
public fun JsonPrimitive(value: Boolean?): JsonPrimitive {
    if (value == null) return JsonNull
    return JsonLiteral(value)
}

/**
 * Convenience method to create [JsonPrimitive] from given number.
 * Returns [JsonNull] if [value] is `null` or [JsonPrimitive] otherwise
 */
public fun JsonPrimitive(value: Number?): JsonPrimitive {
    if (value == null) return JsonNull
    return JsonLiteral(value)
}

/**
 * Convenience method to create [JsonPrimitive] from given string literal.
 * Returns [JsonNull] if [value] is `null` or [JsonPrimitive] otherwise
 */
public fun JsonPrimitive(value: String?): JsonPrimitive {
    if (value == null) return JsonNull
    return JsonLiteral(value)
}

/**
 * Convenience method, returns content of current element as [Int]
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 * @throws NumberFormatException if current element is [JsonPrimitive] but it is not a valid representation of number
 */
public val JsonElement.int: Int get() = primitive.int

/**
 * Convenience method, returns content of current element as [Int] or `null` if element is not a valid representation of number
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 */
public val JsonElement.intOrNull: Int? get() = primitive.intOrNull

/**
 * Convenience method, returns content of current element as [Long]
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 * @throws NumberFormatException if current element is [JsonPrimitive] but it is not a valid representation of number
 */
public val JsonElement.long: Long get() = primitive.long

/**
 * Convenience method, returns content of current element as [Long] or `null` if element is not a valid representation of number
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 */
public val JsonElement.longOrNull: Long? get() = primitive.longOrNull

/**
 * Convenience method, returns content of current element as [Double]
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 * @throws NumberFormatException if current element is [JsonPrimitive] but it is not a valid representation of number
 */
public val JsonElement.double: Double get() = primitive.double

/**
 * Convenience method, returns content of current element as [Double] or `null` if element is not a valid representation of number
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 */
public val JsonElement.doubleOrNull: Double? get() = primitive.doubleOrNull

/**
 * Convenience method, returns content of current element as [Float]
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 * @throws NumberFormatException if current element is [JsonPrimitive], but it is not a valid representation of number
 */
public val JsonElement.float: Float get() = primitive.float

/**
 * Convenience method, returns content of current element as [Float] or `null` if element is not a valid representation of number
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 */
public val JsonElement.floatOrNull: Float? get() = primitive.floatOrNull

/**
 * Convenience method, returns content of current element as [Boolean]
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 * @throws IllegalStateException if current element is [JsonPrimitive], but it is not a valid boolean value
 */
public val JsonElement.boolean: Boolean get() = primitive.boolean

/**
 * Convenience method, returns content of current element as [Boolean] or `null` if element is not a valid boolean value
 *
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 */
public val JsonElement.booleanOrNull: Boolean? get() = primitive.booleanOrNull

/**
 * Convenience method, returns content of current element
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 */
public val JsonElement.content: String get() = primitive.content

/**
 * Convenience method, returns content of current element or `null` if element is [JsonNull]
 * @throws JsonElementTypeMismatchException if current element is not a [JsonPrimitive]
 */
public val JsonElement.contentOrNull: String? get() = primitive.contentOrNull
