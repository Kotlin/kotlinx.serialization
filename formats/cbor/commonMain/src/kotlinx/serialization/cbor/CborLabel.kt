package kotlinx.serialization.cbor

import kotlinx.serialization.*

/**
 * CBOR supports keys of all sorts, not just Strings.
 * In the COSE context, these keys are called *labels* and are limited to Strings and 64-bit negative integers
 * and 64-bit unsigned integers.
 * Conceptually, these work just as `SerialName`s, but to also support numbers in addition to Strings, this annotation
 * can be used.
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