/*
 * Copyright 2018 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 * @throws NumberFormatException is current element is [JsonPrimitive] but is is not a valid representation of number
 */
val JsonElement.int: Int get() = primitive.int

/**
 * Convenience method, returns content of current element as [Int] or `null` if element is not a valid representation of number
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 */
val JsonElement.intOrNull: Int? get() = primitive.intOrNull

/**
 * Convenience method, returns content of current element as [Long]
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 * @throws NumberFormatException is current element is [JsonPrimitive] but is is not a valid representation of number
 */
val JsonElement.long: Long get() = primitive.long

/**
 * Convenience method, returns content of current element as [Long] or `null` if element is not a valid representation of number
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 */
val JsonElement.longOrNull: Long? get() = primitive.longOrNull

/**
 * Convenience method, returns content of current element as [Double]
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 * @throws NumberFormatException is current element is [JsonPrimitive] but is not a valid representation of number
 */
val JsonElement.double: Double get() = primitive.double

/**
 * Convenience method, returns content of current element as [Double] or `null` if element is not a valid representation of number
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 */
val JsonElement.doubleOrNull: Double? get() = primitive.doubleOrNull

/**
 * Convenience method, returns content of current element as [Float]
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 * @throws NumberFormatException is current element is [JsonPrimitive], but is not a valid representation of number
 */
val JsonElement.float: Float get() = primitive.float

/**
 * Convenience method, returns content of current element as [Float] or `null` if element is not a valid representation of number
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 */
val JsonElement.floatOrNull: Float? get() = primitive.floatOrNull

/**
 * Convenience method, returns content of current element as [Boolean]
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 * @throws NumberFormatException is current element is [JsonPrimitive], but is not a valid boolean value
 */
val JsonElement.boolean: Boolean get() = primitive.boolean

/**
 * Convenience method, returns content of current element as [Boolean] or `null` if element is not a valid boolean value
 *
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 */
val JsonElement.booleanOrNull: Boolean? get() = primitive.booleanOrNull

/**
 * Convenience method, returns content of current element
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 */
val JsonElement.content: String get() = primitive.content

/**
 * Convenience method, returns content of current element or `null` if element is [JsonNull]
 * @throws IllegalStateException is current element is not a [JsonPrimitive]
 */
val JsonElement.contentOrNull: String? get() = primitive.contentOrNull
