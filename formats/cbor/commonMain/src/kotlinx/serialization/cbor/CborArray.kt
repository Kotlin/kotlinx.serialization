package kotlinx.serialization.cbor

import kotlinx.serialization.*

/**
 * Encode a class as a CBOR Array (Major type 4) instead of a CBOR map.
 *
 * Serialization of such a class will skip element names (or labels),
 * only encoding the values (containing explicit nulls where necessary).
 *
 * Example usage:
 *
 * ```
 * @CborArray
 * @Serializable
 * data class DataClass(
 *     val alg: Int,
 *     val kid: String?
 * )
 *
 * Cbor.encodeToByteArray(DataClass(alg = -7, kid = null))
 * ```
 *
 * will produce bytes `0x8226F6`, or in diagnostic notation:
 *
 * ```
 * 82    # array(2)
 *    26 # negative(6)
 *    F6 # primitive(22)
 * ```
 *
 * This may be used to encode COSE structures, see
 * [RFC 9052 2. Basic COSE Structure](https://www.rfc-editor.org/rfc/rfc9052#section-2).
 *
 * @param tag optional tags to add to a [CborArray]
 */
@SerialInfo
@Target(AnnotationTarget.CLASS)
@ExperimentalSerializationApi
public annotation class CborArray(vararg val tag: Tag)


@SerialInfo
@Target(AnnotationTarget.ANNOTATION_CLASS)
@ExperimentalSerializationApi
public annotation class Tag(val tag: ULong)