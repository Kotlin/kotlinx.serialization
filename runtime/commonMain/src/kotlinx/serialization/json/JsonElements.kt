/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
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
