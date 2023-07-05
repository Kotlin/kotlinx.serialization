package kotlinx.serialization.cbor

import kotlinx.serialization.*

/**
 * Specifies that a property shall be tagged and serialized as CBOR major type 6: optional semantic tagging
 * of other major types.
 * For types other than [ByteArray], [ByteString] will have no effect.
 *
 * Example usage:
 *
 * ```
 * @Serializable
 * data class Data(
 *     @Tagged(1337uL)
 *     @ByteString
 *     val a: ByteArray, // CBOR major type 6 1337(major type 2: a byte string).
 *
 *     @Tagged(1234567uL)
 *     val b: ByteArray  // CBOR major type 6 1234567(major type 4: an array of data items).
 * )
 * ```
 *
 * See [RFC 7049 2.4. Optional Tagging of Items](https://datatracker.ietf.org/doc/html/rfc7049#section-2.4).
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class Tagged(@OptIn(ExperimentalUnsignedTypes::class) vararg val tags: ULong) {
    public companion object {
        public const val DATE_TIME_STANDARD: ULong = 0u;
        public const val DATE_TIME_EPOCH: ULong = 1u;
        public const val BIGNUM_POSITIVE: ULong = 2u;
        public const val BIGNUM_NEGAIVE: ULong = 3u;
        public const val DECIMAL_FRACTION: ULong = 4u;
        public const val BIGFLOAT: ULong = 5u;
        public const val BASE64_URL: ULong = 21u;
        public const val BASE64: ULong = 22u;
        public const val BASE16: ULong = 23u;
        public const val CBOR_ENCODED_DATA: ULong = 24u;
        public const val URI: ULong = 32u;
        public const val STRING_BASE64_URL: ULong = 33u;
        public const val STRING_BASE64: ULong = 34u;
        public const val REGEX: ULong = 35u;
        public const val MIME_MESSAGE: ULong = 36u;
        public const val CBOR_SELF_DESCRIBE: ULong = 55799u;
    }
}

@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class KeyTags(@OptIn(ExperimentalUnsignedTypes::class) vararg val tags: ULong)