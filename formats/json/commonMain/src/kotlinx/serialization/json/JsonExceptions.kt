/*
 * Copyright 2017-2026 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.json

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.internal.formatEncodingException


/**
 * Base type for all JSON-specific exceptions thrown by [Json].
 *
 * [message] always includes [shortMessage]. It typically also includes a [hint] and additional exception-specific information,
 * such as [JsonDecodingException.path].
 *
 * The [shortMessage] is intended to be concise and aimed at the application users,
 * while [hint] may contain actionable advice for the developer, e.g., enabling a specific
 * configuration option.
 *
 * @property shortMessage short, human-readable description of the error.
 * @property hint optional suggestions for the developer that can help fix or diagnose the problem.
 */
@ExperimentalSerializationApi
public sealed class JsonException(override val message: String) : SerializationException(message) {
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
 *   are shortened with context around [offset]. Input is provided on a best-effort basis,
 *   so it may be incomplete. Input is only included when [JsonConfiguration.exceptionsWithDebugInfo] is enabled.
 * - [hint]: optional suggestions for the developer, e.g., enabling certain [Json] configuration options.
 *
 * @property shortMessage short, human-readable description of the decoding error.
 * @property offset zero-based index of the error position in the input, or `-1` if unknown.
 * @property path JSON path to the failing element when available, or `null`.
 * @property input original input or its excerpt.
 * @property hint optional suggestions for the developer that can help fix or diagnose the problem.
 */
@ExperimentalSerializationApi
public class JsonDecodingException @Deprecated(
    "Use decodingExceptionOf() factory methods",
    level = DeprecationLevel.ERROR
) internal constructor(
    fullMessage: String,
    public override val shortMessage: String,
    public val offset: Int,
    public val path: String?,
    public val input: String?,
    public override val hint: String?,
) : JsonException(fullMessage)

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
 * @property classSerialName serial name of the affected class, if known.
 * @property hint optional suggestions for the developer that can help fix or diagnose the problem.
 */
@ExperimentalSerializationApi
public class JsonEncodingException internal constructor(
    public override val shortMessage: String,
    public val classSerialName: String? = null,
    public override val hint: String? = null
) : JsonException(formatEncodingException(shortMessage, hint))
