/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.CborElementSerializer
import kotlinx.serialization.cbor.internal.encodeNegative
import kotlinx.serialization.cbor.internal.encodePositive
import kotlinx.serialization.encoding.*

/**
 * This interface provides access to the current Cbor instance, so it can be properly taken into account in a
 * custom serializer. For example, a custom serializer can output a byte array using [Cbor.encodeToByteArray]
 * and embed resulting data into the output, as required, by some COSE structures.
 * The actual CBOR Encoder used during serialization implements this interface, so it is possible to cast the encoder
 * passed to [KSerializer.serialize] to [CborEncoder] when implementing such low-level serializers,
 * to access configuration properties:
 *
 * ```kotlin
 * override fun serialize(encoder: Encoder, value: AlgorithmParameters) {
 *   if (encoder is CborEncoder) {
 *     val useDefiniteLengthEncoding = (encoder as CborEncoder).cbor.configuration.writeDefiniteLengths
 *     // Do CBOR-specific low-level stuff
 *     }
 * }
 * ```
 */
@ExperimentalSerializationApi
@SubclassOptInRequired(SealedSerializationApi::class)
public interface CborEncoder : Encoder {
    /**
     * Exposes the current [Cbor] instance and all its configuration flags. Useful for low-level custom serializers.
     */
    public val cbor: Cbor
    /**
     * Appends the given CBOR [element] to the current output.
     * This method is allowed to invoke only as the part of the whole serialization process of the class,
     * calling this method after invoking [beginStructure] or any `encode*` method will lead to unspecified behaviour
     * and may produce an invalid CBOR result.
     * For example:
     * ```
     * class Holder(val value: Int, val list: List<Int>())
     *
     * // Holder serialize method
     * fun serialize(encoder: Encoder, value: Holder) {
     *     // Completely okay, the whole Holder object is read
     *     val cborObject = CborMap(...) // build a CborMap from Holder
     *     (encoder as CborEncoder).encodeCborElement(cborObject) // Write it
     * }
     *
     * // Incorrect Holder serialize method
     * fun serialize(encoder: Encoder, value: Holder) {
     *     val composite = encoder.beginStructure(descriptor)
     *     composite.encodeSerializableElement(descriptor, 0, Int.serializer(), value.value)
     *     val array = CborList(value.list.map { CborInt(it.toLong()) })
     *     // Incorrect, encoder is already in an intermediate state after encodeSerializableElement
     *     (composite as CborEncoder).encodeCborElement(array)
     *     composite.endStructure(descriptor)
     *     // ...
     * }
     * ```
     */
    public fun encodeCborElement(element: CborElement): Unit = encodeSerializableValue(CborElementSerializer, element)

    /**
     * Allows manually encoding CBOR tags. Use with caution, as it is possible to produce invalid CBOR if invoked carelessly!
     */
    public fun encodeTags(@OptIn(kotlin.ExperimentalUnsignedTypes::class) tags: ULongArray): Unit

    /**
     * Encode a CBOR byte string (major type 2).
     *
     * This exists for low-level CBOR serializers and for encoding [CborByteString] values.
     */
    public fun encodeByteString(byteArray: ByteArray)

    /**
     * Encode CBOR `undefined` (simple value 23 / `0xF7`).
     */
    public fun encodeUndefined()

    /**
     * Encode a negative value as [CborInt]. This function exists to encode negative values exceeding [Long.MIN_VALUE]
     */
    public fun encodeNegative(value: ULong)

    /**
     * Encode a positive value as [CborInt]. This function exists to encode positive values exceeding [Long.MAX_VALUE]
     */
    public fun encodePositive(value: ULong)
}
