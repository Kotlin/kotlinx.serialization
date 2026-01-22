/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

/**
 * Generic exception indicating a problem with JSON serialization and deserialization.
 */
public sealed class JsonException(message: String) : SerializationException(message) {
    public abstract val shortMessage: String
    public abstract val hint: String?
}
/**
 * Thrown when [Json] has failed to parse the given JSON string or deserialize it to a target class.
 */
public class JsonDecodingException(
    public override val shortMessage: String,
    public val offset: Int = -1,
    public val path: String? = null,
    public val input: CharSequence? = null, // TODO: minify inputs
    public override val hint: String? = null
) : JsonException(formatDecodingException(offset, shortMessage, path, hint, input)) // TODO: CopyableThrowable?

/**
 * Thrown when [Json] has failed to create a JSON string from the given value.
 */
public class JsonEncodingException(
    public override val shortMessage: String,
    public val classSerialName: String? = null,
    public override val hint: String? = null
) : JsonException(formatEncodingException(shortMessage, hint))

private fun formatEncodingException(shortMessage: String, hint: String?): String {
    return shortMessage + if (hint.isNullOrBlank()) "" else "\n$hint"
}

private fun formatDecodingException(
    offset: Int,
    shortMessage: String,
    path: String?,
    hint: String?,
    input: CharSequence?,
): String = buildString {
    if (offset >= 0) append("Unexpected JSON token at offset $offset: ")
    append(shortMessage)

    if (!path.isNullOrBlank()) {
        append(" at path: ")
        append(path)
    }
    if (!hint.isNullOrBlank()) {
        append("\n$hint")
    }
    if (input != null) {
        append("\nJSON input: ${input.minify(offset)}")
    }
}


internal fun AbstractJsonLexer.invalidTrailingComma(entity: String = "object"): Nothing {
    fail("Trailing comma before the end of JSON $entity",
        position = currentPosition - 1,
        hint = "Trailing commas are non-complaint JSON and not allowed by default. Use 'allowTrailingComma = true' in 'Json {}' builder to support them."
    )
}

internal fun InvalidKeyKindException(keyDescriptor: SerialDescriptor) = JsonEncodingException(
    "Value of type '${keyDescriptor.serialName}' can't be used in JSON as a key in the map. " +
        "It should have either primitive or enum kind, but its kind is '${keyDescriptor.kind}'",
    classSerialName = keyDescriptor.serialName,
    hint = allowStructuredMapKeysHint
)

// Invalid FP messages:
internal fun AbstractJsonLexer.throwInvalidFloatingPointDecoded(result: Number): Nothing {
    fail(nonFiniteFpMessage(result, null), hint = specialFlowingValuesHint)
}

internal fun InvalidFloatingPointEncoded(value: Number, key: String? = null) =
    JsonEncodingException(nonFiniteFpMessage(value, key), hint = specialFlowingValuesHint)

internal fun InvalidFloatingPointDecoded(value: Number, key: String, input: String) =
    JsonDecodingException(nonFiniteFpMessage(value, key), input = input, hint = specialFlowingValuesHint)

private fun nonFiniteFpMessage(value: Number, key: String?): String =
    "Unexpected special floating-point value $value" + if (key != null) " with key $key. " else ". " + "By default, " +
        "non-finite floating point values are prohibited because they do not conform JSON specification."

internal fun CharSequence.minify(offset: Int = -1): CharSequence {
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
