/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization

/**
 * A generic exception indicating the problem in serialization or deserialization process.
 * This is a generic exception type that can be thrown during the problem at any stage of the serialization,
 * including encoding, decoding, serialization, deserialization.
 * [SerialFormat] implementors should throw subclasses of this exception at any unexpected event,
 * whether it is a malformed input or unsupported class layout.
 */
public open class SerializationException : IllegalArgumentException {
    /*
     * Rationale behind making it IllegalArgumentException:
     * Any serialization exception is triggered by the illegal argument, whether
     * it is a serializer that does not support specific structure or an invalid input.
     * Making it IAE just aligns the implementation with this fact.
     *
     * Another point is input validation. The simplest way to validate
     * deserialized data is `require` in `init` block:
     * ```
     * @Serializable class Foo(...) {
     *     init {
     *         required(age > 0) { ... }
     *         require(name.isNotBlank()) { ... }
     *     }
     * }
     * ```
     * While clearly being serialization error (when compromised data was deserialized),
     * Kotlin way is to throw IAE here instead of using library-specific SerializationException.
     *
     * Also, any production-grade system has a general try-catch around deserialization of potentially
     * untrusted/invalid/corrupted data with the corresponding logging, error reporting and diagnostic.
     * Such handling should catch some subtype of exception (e.g. it's unlikely that catching OOM is desirable).
     * Taking it into account, it becomes clear that SE should be subtype of IAE.
     */

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
internal class MissingFieldException(fieldName: String) :
    SerializationException("Field '$fieldName' is required, but it was missing")

/**
 * Thrown when [KSerializer] received unknown property index from [CompositeDecoder.decodeElementIndex].
 *
 * This exception means that data schema has changed in backwards-incompatible way.
 */
@PublishedApi
internal class UnknownFieldException(index: Int) : SerializationException("An unknown field for index $index")
