/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

import kotlinx.serialization.encoding.*
import kotlinx.serialization.descriptors.*

/**
 * A generic exception indicating the problem in serialization or deserialization process.
 *
 * This is a generic exception type that can be thrown during problems at any stage of the serialization,
 * including encoding, decoding, serialization, deserialization, and validation.
 * [SerialFormat] implementors should throw subclasses of this exception at any unexpected event,
 * whether it is a malformed input or unsupported class layout.
 *
 * [SerializationException] is a subclass of [IllegalArgumentException] for the sake of consistency and user-defined validation:
 * Any serialization exception is triggered by the illegal input, whether
 * it is a serializer that does not support specific structure or an invalid input.
 *
 * It is also an established pattern to validate input in user's classes in the following manner:
 * ```
 * @Serializable
 * class Foo(...) {
 *     init {
 *         required(age > 0) { ... }
 *         require(name.isNotBlank()) { ... }
 *     }
 * }
 * ```
 * While clearly being serialization error (when compromised data was deserialized),
 * Kotlin way is to throw `IllegalArgumentException` here instead of using library-specific `SerializationException`.
 *
 * For general "catch-all" patterns around deserialization of potentially
 * untrusted/invalid/corrupted data it is recommended to catch `IllegalArgumentException` type
 * to avoid catching irrelevant to serialization errors such as `OutOfMemoryError` or domain-specific ones.
 */
public open class SerializationException : IllegalArgumentException {

    /**
     * Creates an instance of [SerializationException] without any details.
     */
    public constructor()

    /**
     * Creates an instance of [SerializationException] with the specified detail [message].
     */
    public constructor(message: String?) : super(message)

    /**
     * Creates an instance of [SerializationException] with the specified detail [message], and the given [cause].
     */
    public constructor(message: String?, cause: Throwable?) : super(message, cause)

    /**
     * Creates an instance of [SerializationException] with the specified [cause].
     */
    public constructor(cause: Throwable?) : super(cause)
}

/**
 * Thrown when [KSerializer] did not receive a non-optional property from [CompositeDecoder] and [CompositeDecoder.decodeElementIndex]
 * had already returned [CompositeDecoder.DECODE_DONE].
 *
 * [MissingFieldException] is thrown on missing field from all [auto-generated][Serializable] serializers and it
 * is recommended to throw this exception from user-defined serializers.
 *
 * [MissingFieldException] is constructed from the following properties:
 * - [missingFields] -- fields that were required for the deserialization but have not been found.
 *   They are always non-empty and their names match the corresponding names in [SerialDescriptor.elementNames]
 * - Optional `serialName` -- serial name of the enclosing class that failed to get deserialized.
 *   Matches the corresponding [SerialDescriptor.serialName].
 *
 * @see SerializationException
 * @see KSerializer
 */
@ExperimentalSerializationApi
public class MissingFieldException(
    missingFields: List<String>, message: String?, cause: Throwable?
) : SerializationException(message, cause) {

    /**
     * List of fields that were required but not found during deserialization.
     * Contains at least one element.
     */
    public val missingFields: List<String> = missingFields

    /**
     * Creates an instance of [MissingFieldException] for the given [missingFields] and [serialName] of
     * the corresponding serializer.
     */
    public constructor(
        missingFields: List<String>,
        serialName: String
    ) : this(
        missingFields,
        if (missingFields.size == 1) "Field '${missingFields[0]}' is required for type with serial name '$serialName', but it was missing"
        else "Fields $missingFields are required for type with serial name '$serialName', but they were missing",
        null
    )

    /**
     * Creates an instance of [MissingFieldException] for the given [missingField] and [serialName] of
     * the corresponding serializer.
     */
    public constructor(
        missingField: String,
        serialName: String
    ) : this(
        listOf(missingField),
        "Field '$missingField' is required for type with serial name '$serialName', but it was missing",
        null
    )

    @PublishedApi // Constructor used by the generated serializers
    internal constructor(missingField: String) : this(
        listOf(missingField),
        "Field '$missingField' is required, but it was missing",
        null
    )
}

/**
 * Thrown when [KSerializer] received unknown property index from [CompositeDecoder.decodeElementIndex].
 *
 * This exception means that data schema has changed in backwards-incompatible way.
 */
@PublishedApi
internal class UnknownFieldException
// This constructor is used by coroutines exception recovery
internal constructor(message: String?) : SerializationException(message) {
    // This constructor is used by the generated serializers
    constructor(index: Int) : this("An unknown field for index $index")
}
