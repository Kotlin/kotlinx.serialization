/*
 * Copyright 2017-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.SerialDescriptor
import kotlinx.serialization.SerializationException

/**
 * Exception thrown when [Json] is unable to read or write a JSON.
 */
public sealed class JsonException(message: String) : SerializationException(message)

/**
 * Exception thrown when [Json] encounters `NaN` or infinite floating-point value in a strict mode.
 */
public class JsonInvalidValueInStrictModeException(value: Any, valueDescription: String) : JsonException(
    "$value is not a valid $valueDescription as per JSON spec.\n" +
            "You can disable strict mode to serialize such values") {
    constructor(floatValue: Float) : this(floatValue, "float")
    constructor(doubleValue: Double) : this(doubleValue, "double")
}

/**
 * Exception thrown when [Json] encounters unknown key in a strict mode.
 */
public class JsonUnknownKeyException(key: String) : JsonException(
    "Strict JSON encountered unknown key: $key\n" +
            "You can disable strict mode to skip unknown keys")


/**
 * Exception thrown when [Json] has failed to parse provided JSON.
 * Such exception usually indicate that [Json] input is not a valid JSON.
 */
public class JsonParsingException(position: Int, message: String) : JsonException("Invalid JSON at $position: $message")

/**
 * Exception thrown when requested [JsonElement] type differs from the actual one.
 * E.g.:
 * ```
 * val element: JsonElement = JsonLiteral("value")
 * val array = element.jsonArray // Raise JsonElementTypeMismatchException
 * ```
 */
public class JsonElementTypeMismatchException(key: String, expected: String) : JsonException("Element $key is not a $expected")

public class JsonMapInvalidKeyKind(keyDescriptor: SerialDescriptor) : JsonException(
    "Value of type ${keyDescriptor.name} can't be used in json as map key. " +
            "It should have either primitive or enum kind, but its kind is ${keyDescriptor.kind}.\n" +
            "You can convert such maps to arrays [key1, value1, key2, value2,...] with 'allowStructuredMapKeys' flag in JsonConfiguration."
)
