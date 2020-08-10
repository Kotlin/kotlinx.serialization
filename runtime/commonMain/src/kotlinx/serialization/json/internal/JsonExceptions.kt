/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

/**
 * Generic exception indicating a problem with JSON serialization and deserialization.
 */
internal open class JsonException(message: String) : SerializationException(message)

/**
 * Thrown when [Json] has failed to parse the given JSON string or deserialize it to a target class.
 */
internal class JsonDecodingException(offset: Int, message: String) :
    JsonException(if (offset >= 0) "Unexpected JSON token at offset $offset: $message" else message)

/**
 * Thrown when [Json] has failed to create a JSON string from the given value.
 */
internal class JsonEncodingException(message: String) : JsonException(message)

internal fun JsonDecodingException(offset: Int, message: String, input: String) =
    JsonDecodingException(offset, "$message\nJSON input: ${input.minify(offset)}")

internal fun InvalidFloatingPointEncoded(value: Number, output: String) = JsonEncodingException(
    "Unexpected special floating-point value $value. By default, " +
            "non-finite floating point values are prohibited because they do not conform JSON specification. " +
            "$specialFlowingValuesHint\n" +
            "Current output: ${output.minify()}"
)

internal fun InvalidFloatingPointEncoded(value: Number, key: String, output: String) =
    JsonEncodingException(unexpectedFpErrorMessage(value, key, output))

internal fun InvalidFloatingPointDecoded(value: Number, key: String, output: String) =
    JsonDecodingException(-1, unexpectedFpErrorMessage(value, key, output))

// Extension on JSON reader and fail immediately
internal fun JsonReader.throwInvalidFloatingPointDecoded(result: Number): Nothing {
    fail("Unexpected special floating-point value $result. By default, " +
            "non-finite floating point values are prohibited because they do not conform JSON specification. " +
            specialFlowingValuesHint
    )
}

private fun unexpectedFpErrorMessage(value: Number, key: String, output: String): String {
    return "Unexpected special floating-point value $value with key $key. By default, " +
            "non-finite floating point values are prohibited because they do not conform JSON specification. " +
            "$specialFlowingValuesHint\n" +
            "Current output: ${output.minify()}"
}

internal fun UnknownKeyException(key: String, input: String) = JsonDecodingException(
    -1,
    "Encountered unknown key '$key'.\n" +
            "$ignoreUnknownKeysHint\n" +
            "Current input: ${input.minify()}"
)

internal fun InvalidKeyKindException(keyDescriptor: SerialDescriptor) = JsonEncodingException(
    "Value of type '${keyDescriptor.serialName}' can't be used in JSON as a key in the map. " +
            "It should have either primitive or enum kind, but its kind is '${keyDescriptor.kind}'.\n" +
            allowStructuredMapKeysHint
)

private fun String.minify(offset: Int = -1): String {
    if (length < 200) return this
    if (offset == -1) {
        val start = this.length - 60
        if (start <= 0) return this
        return "....." + substring(start)
    }

    val start = offset - 30
    val end = offset + 30
    val prefix = if (start <= 0) "" else "....."
    val suffix = if (end >= length) "" else "....."
    return prefix + substring(start.coerceAtLeast(0), end.coerceAtMost(length)) + suffix
}
