package kotlinx.serialization.cbor

import kotlinx.serialization.*

/**
 * CBOR supports *labels*, which work just as `SerialNames`. The key difference is that labels are not strings,
 * but integer numbers.
 *
 * Set the `preferCborLabelsOverNames` configuration switch to prefer them over serial names in case both are present
 * for a property.
 *
 * Example usage:
 * ```
 * @Serializable
 * data class DataClass(
 *     @CborLabel(1)
 *     @SerialName("alg")
 *     val alg: Int
 * )
 * ```
 *
 * serializing `DataClass(alg = -7)` with `Cbor { preferCborLabelsOverNames = true }` will
 * output `0xbf0126ff`, or in diagnostic notation:
 *
 * ```
 * BF    # map(*)
 *    01 # unsigned(1)
 *    26 # negative(6)
 *    FF # primitive(*)
 * ```
 *
 * instead of the traditional `0xbf63616c6726ff`, or in diagnostic notation:
 *
 * ```
 * BF           # map(*)
 *    63        # text(3)
 *       616C67 # "alg"
 *    26        # negative(6)
 *    FF        # primitive(*)
 * ```
 */
@SerialInfo
@Target(AnnotationTarget.PROPERTY)
@ExperimentalSerializationApi
public annotation class CborLabel(val label: Long)