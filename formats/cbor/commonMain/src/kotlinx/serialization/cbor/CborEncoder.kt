/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
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
@OptIn(AdvancedEncodingApi::class)
@SubclassOptInRequired(SealedSerializationApi::class)
public interface CborEncoder : Encoder {
    /**
     * Exposes the current [Cbor] instance and all its configuration flags. Useful for low-level custom serializers.
     */
    public val cbor: Cbor
}
