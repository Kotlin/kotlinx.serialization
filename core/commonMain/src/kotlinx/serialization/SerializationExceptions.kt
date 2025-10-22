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
 * class User(val age: Int, val name: String) {
 *     init {
 *         require(age > 0) { ... }
 *         require(name.isNotBlank()) { ... }
 *     }
 * }
 *
 * Json.decodeFromString<User>("""{"age": -100, "name": ""}""") // throws IllegalArgumentException from require()
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
 * - [serialName] -- a serial name of the enclosing class that failed to get deserialized.
 *   Matches the corresponding [SerialDescriptor.serialName].
 *
 * @see SerializationException
 * @see KSerializer
 */
@ExperimentalSerializationApi
public class MissingFieldException private constructor(
    // only private constructor allows nullable serialName and message
    // reordered things a bit to avoid signature clash with public one
    message: String?, cause: Throwable?, missingFields: List<String>, serialName: String?
) : SerializationException(message, cause) {

    /**
     * List of fields that were required but not found during deserialization.
     * Contains at least one element.
     */
    public val missingFields: List<String> = missingFields

    /**
     * Returns a serial name of a serializable class that cannot be deserialized due to missing fields.
     * Typically, equal to the [SerialDescriptor.serialName] of the serializable class.
     *
     * However, in cases the class was compiled with an old Kotlin serialization plugin,
     * its serial name may be unavailable and this property is `null`.
     */
    public val serialName: String? = serialName

    /**
     * Creates an instance of [MissingFieldException] for the given [missingFields] and [serialName] of
     * the corresponding serializer.
     */
    public constructor(
        missingFields: List<String>,
        serialName: String
    ) : this(
        missingFields = missingFields,
        serialName = serialName,
        message = if (missingFields.size == 1) "Field '${missingFields[0]}' is required for type with serial name '$serialName', but it was missing"
        else "Fields $missingFields are required for type with serial name '$serialName', but they were missing",
        cause = null
    )

    /**
     * Creates an instance of [MissingFieldException] for the given [missingField] and [serialName] of
     * the corresponding serializer.
     */
    public constructor(
        missingField: String,
        serialName: String
    ) : this(
        missingFields = listOf(missingField),
        serialName = serialName,
        message = "Field '$missingField' is required for type with serial name '$serialName', but it was missing",
        cause = null
    )

    // Deprecated since 1.10.0, should be HIDDEN when MFE is stable
    @Deprecated("Use constructor which accepts serialName parameter", ReplaceWith("MissingFieldException(missingFields, descriptor.serialName, message, cause)"), level = DeprecationLevel.ERROR)
    public constructor(
        missingFields: List<String>, message: String?, cause: Throwable?
    ) : this(message, cause, missingFields, serialName = null)

    @PublishedApi
    @Deprecated("Constructor used by the serializers generated by plugins older than Kotlin 1.5", level = DeprecationLevel.HIDDEN)
    internal constructor(missingField: String) : this(
        message = "Field '$missingField' is required, but it was missing",
        cause = null,
        missingFields = listOf(missingField),
        serialName = null
    )

    // It's better to have internal function calling private ctor rather than just internal ctor (because the latter is visible from Java)
    internal fun withNewMessageInternal(newMessage: String): MissingFieldException = MissingFieldException(
        message = newMessage,
        cause = this,
        missingFields = missingFields,
        serialName = serialName
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
