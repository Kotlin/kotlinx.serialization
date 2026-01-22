/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*

/**
 * Base type for all JSON-specific exceptions thrown by [Json].
 *
 * This class extends [SerializationException] and provides a uniform, user-friendly
 * error message along with a compact [shortMessage] and an optional [hint].
 *
 * Notes:
 * - The concrete subclasses format a verbose exception message that may include
 *   additional context such as input offset, JSON path, and a minified input excerpt.
 * - The [shortMessage] is intended to be concise and human-readable; it is included
 *   in the full exception message as well.
 * - The [hint] may contain actionable advice, e.g. enabling a specific
 *   configuration option.
 *
 *
 * @property shortMessage short, human-readable description of the error.
 * @property hint optional suggestions for the developer that can help fix or diagnose the problem.
 */
public sealed class JsonException(message: String) : SerializationException(message) {
    public abstract val shortMessage: String
    public abstract val hint: String?
}
/**
 * Thrown when [Json] fails to parse the given JSON or to deserialize it into a target type.
 *
 * The exception [message] is formatted to include, when available, the character [offset],
 * the JSON [path] to the failing element, a [hint] with actionable guidance, and a
 * minified excerpt of the original [input].
 *
 * Typical cases include malformed JSON, unexpected tokens, missing required fields,
 * or values that cannot be read for the declared type.
 *
 * Notes about properties:
 * - [offset]: zero-based character index in the input where the failure was detected,
 *   or `-1` when the position is unknown.
 * - [path]: JSON path to the element that failed to decode (e.g. `$.user.address[0].city`),
 *   when available.
 * - [input]: the original JSON input (or its minified excerpt in the message). Large inputs
 *   are shortened with context around [offset].
 * - [hint]: optional suggestions for the developer, e.g., enabling certain [Json] configuration options.
 *
 * @property shortMessage short, human-readable description of the decoding error.
 * @property offset zero-based index of the error position in the input, or `-1` if unknown.
 * @property path JSON path to the failing element when available, or `null`.
 * @property input original input or its excerpt; used to build a contextual message.
 * @property hint optional suggestions for the developer that can help fix or diagnose the problem.
 */
public class JsonDecodingException(
    public override val shortMessage: String,
    public val offset: Int = -1,
    public val path: String? = null,
    input: CharSequence? = null,
    public override val hint: String? = null
) : JsonException( // TODO: CopyableThrowable (see StackTraceRecoveryTest.kt)?
    formatDecodingException(offset, shortMessage, path, hint, input)
) {
    // Sadly, there's no way to minify it before passing to superclass, so we have to do it twice :(
    public val input: CharSequence? = input?.minify(offset)
}

/**
 * Thrown when [Json] fails to encode a value to a JSON string.
 *
 * Typical cases include encountering values that cannot be represented in JSON
 * (e.g., non-finite floating-point numbers when they are not allowed) or using
 * unsupported types as map keys.
 *
 * The exception [message] includes [shortMessage] and, when present, a [hint] with
 * actionable guidance for the developer.
 *
 * @property shortMessage short, human-readable description of the encoding error.
 * @property classSerialName serial name of the affected class, if known; used for diagnostics.
 * @property hint optional suggestions for the developer that can help fix or diagnose the problem.
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
