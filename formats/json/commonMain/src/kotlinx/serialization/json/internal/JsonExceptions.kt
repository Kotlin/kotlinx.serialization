/*
 * Copyright 2017-2021 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

@file:Suppress("FunctionName")

package kotlinx.serialization.json.internal

import kotlinx.serialization.*
import kotlinx.serialization.descriptors.*
import kotlinx.serialization.json.*


@OptIn(ExperimentalSerializationApi::class)
@Suppress("DEPRECATION_ERROR")
internal fun decodingExceptionOf(shortMessage: String): JsonDecodingException =
    JsonDecodingException(formatDecodingException(-1, shortMessage, null, null, null), shortMessage, -1, null, null, null)

@Suppress("DEPRECATION_ERROR")
@OptIn(ExperimentalSerializationApi::class)
internal inline fun JsonDecoder.decodingExceptionOf(
    shortMessage: String,
    path: String? = null, // no offset because it is used with JsonElement, not the whole input
    hint: String? = null,
    input: () -> CharSequence
): JsonDecodingException {
    val inputValue = json.configuration.ifDebugInput { input().minify().toString() }
    return JsonDecodingException(
        formatDecodingException(-1, shortMessage, path, hint, inputValue),
        shortMessage,
        -1,
        path,
        inputValue,
        hint
    )
}

@Suppress("DEPRECATION_ERROR")
@OptIn(ExperimentalSerializationApi::class)
internal fun AbstractJsonLexer.decodingExceptionOf(
    shortMessage: String,
    offset: Int,
    path: String,
    hint: String?,
    input: CharSequence,
): JsonDecodingException {
    val inputValue = configuration.ifDebugInput { input.minify(offset).toString() }
    return JsonDecodingException(
        formatDecodingException(offset, shortMessage, path, hint, inputValue),
        shortMessage,
        offset,
        path,
        inputValue,
        hint
    )
}

@OptIn(ExperimentalSerializationApi::class)
private inline fun JsonConfiguration.ifDebugInput(block: () -> String): String? =
    if (exceptionsWithDebugInfo) block() else null

internal fun formatEncodingException(shortMessage: String, hint: String?): String {
    return shortMessage + if (hint.isNullOrBlank()) "" else "\n$hint"
}

private fun formatDecodingException(
    offset: Int,
    shortMessage: String,
    path: String?,
    hint: String?,
    input: String?,
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
        append("\nJSON input: ")
        append(input)
    }
}


internal fun AbstractJsonLexer.invalidTrailingComma(entity: String = "object"): Nothing {
    fail("Trailing comma before the end of JSON $entity",
        position = currentPosition - 1,
        hint = "Trailing commas are non-complaint JSON and not allowed by default. Use 'allowTrailingComma = true' in 'Json {}' builder to support them."
    )
}

@OptIn(ExperimentalSerializationApi::class)
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

@OptIn(ExperimentalSerializationApi::class)
internal fun InvalidFloatingPointEncoded(value: Number, key: String? = null) =
    JsonEncodingException(nonFiniteFpMessage(value, key), hint = specialFlowingValuesHint)

@OptIn(ExperimentalSerializationApi::class)
internal inline fun JsonDecoder.InvalidFloatingPointDecoded(value: Number, key: String, input: () -> CharSequence) =
    decodingExceptionOf(nonFiniteFpMessage(value, key), hint = specialFlowingValuesHint, input = input)

private fun nonFiniteFpMessage(value: Number, key: String?): String =
    "Unexpected special floating-point value $value" + (if (key != null) " with key $key. " else ". ") + "By default, " +
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
