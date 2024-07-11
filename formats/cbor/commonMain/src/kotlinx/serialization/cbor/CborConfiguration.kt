/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

/**
 * Configuration of the current [Cbor] instance available through [Cbor.configuration].
 *
 *  * Can be used for debug purposes and for custom Json-specific serializers
 *  * via [CborEncoder] and [CborDecoder].
 *
 * @param encodeDefaults specifies whether default values of Kotlin properties are encoded.
 *                       False by default; meaning that properties with values equal to defaults will be elided.
 * @param ignoreUnknownKeys specifies if unknown CBOR elements should be ignored (skipped) when decoding.
 * @param encodeKeyTags Specifies whether tags set using the [KeyTags] annotation should be written (or omitted)
 * @param encodeValueTags Specifies whether tags set using the [ValueTags] annotation should be written (or omitted)
 * @param encodeObjectTags Specifies whether tags set using the [ObjectTags] annotation should be written (or omitted)
 * @param verifyKeyTags Specifies whether tags preceding map keys (i.e. properties) should be matched against the
 *                      [KeyTags] annotation during the deserialization process. Useful for lenient parsing
 * @param verifyValueTags Specifies whether tags preceding values should be matched against the [ValueTags]
 *                      annotation during the deserialization process. Useful for lenient parsing.
 * @param verifyObjectTags Specifies whether tags preceding values should be matched against the [ObjectTags]
 *                      annotation during the deserialization process. Useful for lenient parsing.
 * @param alwaysUseByteString Specifies whether to always use the compact [ByteString] encoding when serializing
 *                            or deserializing byte arrays.
 *
 * @param useDefiniteLengthEncoding Specifies whether the definite length encoding should be used (as required for COSE, for example)
 * @param preferCborLabelsOverNames Specifies whether to serialize element labels (i.e. Long from [CborLabel])
 *                                    instead of the element names (i.e. String from [SerialName]) for map keys
 */
public class CborConfiguration(
    public val encodeDefaults: Boolean,
    public val ignoreUnknownKeys: Boolean,
    public val encodeKeyTags: Boolean,
    public val encodeValueTags: Boolean,
    public val encodeObjectTags: Boolean,
    public val verifyKeyTags: Boolean,
    public val verifyValueTags: Boolean,
    public val verifyObjectTags: Boolean,
    public val useDefiniteLengthEncoding: Boolean,
    public val preferCborLabelsOverNames: Boolean,
    public val alwaysUseByteString: Boolean,
)