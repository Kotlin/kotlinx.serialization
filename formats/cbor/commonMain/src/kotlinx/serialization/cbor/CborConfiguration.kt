/*
 * Copyright 2017-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*

/**
 * Configuration of the current [Cbor] instance available through [Cbor.configuration].
 *
 *  * Can be used for debug purposes and for custom Cbor-specific serializers
 *  * via [CborEncoder] and [CborDecoder].
 *
 * @param encodeDefaults specifies whether default values of Kotlin properties are encoded.
 * False by default; meaning that properties with values equal to defaults will be elided.
 * @param ignoreUnknownKeys specifies if unknown CBOR elements should be ignored (skipped) when decoding.
 * @param encodeKeyTags Specifies whether tags set using the [KeyTags] annotation should be written.
 * CBOR allows for optionally defining *tags* for properties and their values. When this switch is set to `true` tags on
 * CBOR map keys (i.e. properties) are encoded into the resulting byte string to transport additional information.
 * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.
 *
 * @param encodeValueTags Specifies whether tags set using the [ValueTags] annotation should be written.
 * CBOR allows for optionally defining *tags* for properties and their values. When this switch is set to `true`, tags on
 * CBOR map values (i.e. the values of properties and map entries) are encoded into the resulting byte string to
 * transport additional information. Well-known tags are specified in [CborTag].
 * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.
 *
 * @param encodeObjectTags Specifies whether tags set using the [ObjectTags] annotation should be written.
 * When this switch is set to `true` , it is possible to directly declare classes to always be tagged.
 * This then applies to isolated objects of such a tagged class being serialized and to objects of such a class used as
 * values in a list, but also or when they are used as a property in another class.
 * Forcing objects to always be tagged in such a manner is accomplished by the [ObjectTags] annotation,
 * which works just as [ValueTags], but for class definitions.
 * When serializing, object tags will always be encoded directly before to the data of the tagged object, i.e. a
 * value-tagged property of an object-tagged type will have the value tags preceding the object tags.
 * Well-known tags are specified in [CborTag].
 * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.

 * @param verifyKeyTags Specifies whether tags preceding map keys (i.e. properties) should be matched against the
 * [KeyTags] annotation during the deserialization process.
 * CBOR allows for optionally defining *tags* for properties and their values. When the [encodeKeyTags] switch is set to
 * `true` tags on CBOR map keys (i.e. properties) are encoded into the resulting byte string to transport additional
 * information. Setting [verifyKeyTags] to `true` forces strict verification of such tags during deserialization.
 * I.e. tags must be present on all properties of a class annotated with [KeyTags] in the CBOR byte stream
 * **in full and in order**.
 * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.
 *
 * @param verifyValueTags Specifies whether tags preceding values should be matched against the [ValueTags]
 * annotation during the deserialization process.
 * CBOR allows for optionally defining *tags* for properties and their values. When [encodeValueTags] is set to `true`,
 * tags on CBOR map values (i.e. the values of properties and map entries) are encoded into the resulting byte string to
 * transport additional information.
 * Setting [verifyValueTags] to `true` forces verification of such tags during deserialization. I.e. tags must be
 * present on all values annotated with [ValueTags] in the CBOR byte stream **in full and in order**.
 * See also [verifyObjectTags], since a value may have both kinds of tags. [ValueTags] precede [ObjectTags] in the CBOR
 * byte stream. [verifyValueTags] and [verifyObjectTags] can be toggled independently.
 * Well-known tags are specified in [CborTag].
 *
 * @param verifyObjectTags Specifies whether tags preceding values should be matched against the [ObjectTags]
 * annotation during the deserialization process. [ObjectTags] are applied when serializing classes tagged using this
 * annotation. This applies to isolated objects of such a class and properties, whose values are of such a tagged class.
 * [verifyValueTags] and [verifyObjectTags] can be toggled independently. Hence, it is possible to only partially verify
 * tags on values (if only one such configuration switch is set to true). [ValueTags] precede [ObjectTags] in the CBOR
 * byte stream.
 * Well-known tags are specified in [CborTag].
 *
 * @param useDefiniteLengthEncoding Specifies whether the definite length encoding should be used (as required for COSE, for example).
 * CBOR supports two encodings for maps and arrays: definite and indefinite length encoding. kotlinx.serialization defaults
 * to the latter, which means that a map's or array's number of elements is not encoded, but instead a terminating byte is
 * appended after the last element.
 * Definite length encoding, on the other hand, omits this terminating byte, but instead prepends number of elements
 * to the contents of a map or array. This configuration switch allows for toggling between the
 * two modes of encoding.
 *
 * @param preferCborLabelsOverNames Specifies whether to serialize element labels (i.e. Long from [CborLabel])
 * instead of the element names (i.e. String from [SerialName]). CBOR supports keys of all types which work just as
 * `SerialName`s.
 * COSE restricts this again to strings and numbers and calls these restricted map keys *labels*. String labels can be
 * assigned by using `@SerialName`, while number labels can be assigned using the [CborLabel] annotation.
 * The [preferCborLabelsOverNames] configuration switch can be used to prefer number labels over SerialNames in case both
 * are present for a property. This duality allows for compact representation of a type when serialized to CBOR, while
 * keeping expressive diagnostic names when serializing to JSON.
 *
 * @param alwaysUseByteString Specifies whether to always use the compact [ByteString] encoding when serializing
 * or deserializing byte arrays.
 * By default, Kotlin `ByteArray` instances are encoded as **major type 4**.
 * When **major type 2** is desired, then the [`@ByteString`][ByteString] annotation can be used on a case-by-case
 * basis. The [alwaysUseByteString] configuration switch allows for globally preferring **major type 2** without needing
 * to annotate every `ByteArray` in a class hierarchy.
 *
 */
@ExperimentalSerializationApi
public class CborConfiguration internal constructor(
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
) {
    override fun toString(): String {
        return "CborConfiguration(encodeDefaults=$encodeDefaults, ignoreUnknownKeys=$ignoreUnknownKeys, " +
            "encodeKeyTags=$encodeKeyTags, encodeValueTags=$encodeValueTags, encodeObjectTags=$encodeObjectTags, " +
            "verifyKeyTags=$verifyKeyTags, verifyValueTags=$verifyValueTags, verifyObjectTags=$verifyObjectTags, " +
            "useDefiniteLengthEncoding=$useDefiniteLengthEncoding, " +
            "preferCborLabelsOverNames=$preferCborLabelsOverNames, alwaysUseByteString=$alwaysUseByteString)"
    }
}