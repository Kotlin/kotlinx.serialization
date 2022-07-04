/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

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
 * to avoid catching irrelevant to serializaton errors such as `OutOfMemoryError` or domain-specific ones.
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
 * Thrown when [KSerializer] did not receive property from [Decoder], and this property was not optional.
 */
@PublishedApi
internal class MissingFieldException
// This constructor is used by coroutines exception recovery
internal constructor(message: String?, cause: Throwable?) : SerializationException(message, cause) {
    // This constructor is used by the generated serializers
    constructor(fieldName: String) : this("Field '$fieldName' is required, but it was missing", null)
    internal constructor(fieldNames: List<String>, serialName: String) : this(if (fieldNames.size == 1) "Field '${fieldNames[0]}' is required for type with serial name '$serialName', but it was missing" else "Fields $fieldNames are required for type with serial name '$serialName', but they were missing", null)
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
