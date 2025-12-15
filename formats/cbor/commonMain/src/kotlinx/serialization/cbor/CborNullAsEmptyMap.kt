package kotlinx.serialization.cbor

import kotlinx.serialization.*

/**
 * Marks a complex property to be encoded as an empty map when null, instead of CBOR `null`.
 *
 * This is useful for COSE encoding, because COSE known protected and unprotected headers, for example,
 * and the compiler handles null checks, while checks for empty maps would lead to duplicated spaghetti code.
 *
 * Example usage:
 *
 * ```
 *
 * @Serializable
 * data class ClassWNullableAsMap(
 *     @SerialName("nullable")
 *     @CborNullAsEmptyMap
 *     val nullable: NullableClass?
 * )
 *
 * @Serializable
 * data class NullableClass(
 *     val property: String
 * )
 *
 * Cbor.encodeToByteArray(ClassWNullableAsMap(nullable = null))
 * ```
 *
 * will produce bytes `0xbf686e756c6c61626c65a0ff`, or in diagnostic notation:
 *
 * ```
 *a1                     # map(1)
 *    68                  #   text(8)
 *       6e756c6c61626c65 #     "nullable"
 *    a0                  #   map(0)
 * ```
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class CborNullAsEmptyMap
