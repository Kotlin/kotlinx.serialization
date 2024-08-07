/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.encoding.*

/**
 * This interface provides access to the current Cbor instance, so it can be properly taken into account in a
 * custom serializer. For example, a custom serializer can decode CBOR data wrapped into a byte array using
 * [Cbor.decodeFromByteArray] as required by some COSE structures.
 * The actual CBOR Decoder used during deserialization implements this interface, so it is possible to cast the decoder
 * passed to [KSerializer.deserialize] to [CborDecoder] when implementing such low-level serializers,
 * to access configuration properties:
 *
 * ```kotlin
 * override fun deserialize(decoder: Decoder): AlgorithmParameters {
 *   if(decoder is CborDecoder){
 *     val useDefiniteLengthEncoding = (decoder as CborDecoder).cbor.configuration.writeDefiniteLengths
 *     // Do CBOR-specific low-level stuff
 *   }
 * }
 * ```
 */
@ExperimentalSerializationApi
@OptIn(AdvancedEncodingApi::class)
@SubclassOptInRequired(SealedSerializationApi::class)
public interface CborDecoder : Decoder {
    /**
     * Exposes the current [Cbor] instance and all its configuration flags. Useful for low-level custom serializers.
     */
    public val cbor: Cbor
}
