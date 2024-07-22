package kotlinx.serialization.cbor

import kotlinx.serialization.cbor.internal.SuppressAnimalSniffer
import kotlinx.serialization.*

/**
 * Specifies that a property shall be tagged and the tag is serialized as CBOR major type 6: optional semantic tagging
 * of other major types.
 *
 * Example usage:
 *
 * ```
 * @Serializable
 * data class Data(
 *     @ValueTags(1337uL)
 *     @ByteString
 *     val a: ByteArray, // CBOR major type 6 1337(major type 2: a byte string).
 *
 *     @ValueTags(1234567uL)
 *     val b: ByteArray  // CBOR major type 6 1234567(major type 4: an array of data items).
 * )
 * ```
 *
 * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
@SuppressAnimalSniffer
public annotation class ValueTags(@OptIn(ExperimentalUnsignedTypes::class) vararg val tags: ULong)

/**
 * Contains a set of predefined tags, named in accordance with
 * [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items)
 */
public object CborTag{
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

/**
 * Specifies that a key (i.e. a property identifier) shall be tagged and serialized as CBOR major type 6: optional
 * semantic tagging of other major types.
 *
 * Example usage:
 *
 * ```
 * @Serializable
 * data class Data(
 *     @KeyTags(34uL)
 *     val b: Int = -1   // results in the CBOR equivalent of 34("b"): -1
 * )
 * ```
 *
 * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
@SuppressAnimalSniffer
public annotation class KeyTags(@OptIn(ExperimentalUnsignedTypes::class) vararg val tags: ULong)



/**
 * Specifies that an object of a class annotated using `ObjectTags` shall be tagged and serialized as
 * CBOR major type 6: optional semantic tagging of other major types. Can be combined with [CborArray] and [ValueTags].
 * Note that `ObjectTags` will always be encoded directly before to the data of the tagged object, i.e. a value-tagged
 * property of an object-tagged type will have the value tags preceding the object tags.
 *
 * Example usage:
 *
 * ```
 * @ObjectTags(1337uL)
 * @Serializable
 * data class ClassAsTagged(
 *     @SerialName("alg")
 *     val alg: Int,
 * )
 * ```
 *
 * Encoding to CBOR results in the following byte string:
 * ```
 * D9 0539         # tag(1337)
 *    BF           # map(*)
 *       63        # text(3)
 *          616C67 # "alg"
 *       13        # unsigned(19)
 *       FF        # primitive(*)
 * ```
 *
 * See [RFC 8949 3.4. Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items).
 */
@SerialInfo
@Target(AnnotationTarget.CLASS)
@ExperimentalSerializationApi
@SuppressAnimalSniffer
public annotation class ObjectTags(@OptIn(ExperimentalUnsignedTypes::class) vararg val tags: ULong)

