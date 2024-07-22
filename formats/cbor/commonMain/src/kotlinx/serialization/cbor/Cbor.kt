/*
 * Copyright 2017-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package kotlinx.serialization.cbor

import kotlinx.serialization.*
import kotlinx.serialization.cbor.internal.*
import kotlinx.serialization.modules.*

/**
 * Implements [encoding][encodeToByteArray] and [decoding][decodeFromByteArray] classes to/from bytes
 * using [CBOR](https://tools.ietf.org/html/rfc7049) specification.
 * It is typically used by constructing an application-specific instance, with configured behaviour, and,
 * if necessary, registered custom serializers (in [SerializersModule] provided by [serializersModule] constructor parameter).
 *
 * ### Known caveats and limitations:
 * Can be used to produce fully [COSE](https://datatracker.ietf.org/doc/html/rfc8812)-compliant data
 * but canonical sorting of map keys needs to be done manually by specifying members in appropriate order.
 * Fully support CBOR maps, which, unlike JSON ones, may contain keys of non-primitive types, and may produce such maps
 * from corresponding Kotlin objects. However, other 3rd-party parsers (e.g. `jackson-dataformat-cbor`) may not accept such maps.
 */
@ExperimentalSerializationApi
public sealed class Cbor(
    public val configuration: CborConfiguration,
    override val serializersModule: SerializersModule
) : BinaryFormat {

    /**
     * The default instance of [Cbor]. Neither writes nor verifies tags. Uses indefinite length encoding by default.
     */
    public companion object Default :
        Cbor(
            CborConfiguration(
                encodeDefaults = false,
                ignoreUnknownKeys = false,
                encodeKeyTags = false,
                encodeValueTags = false,
                encodeObjectTags = false,
                verifyKeyTags = false,
                verifyValueTags = false,
                verifyObjectTags = false,
                useDefiniteLengthEncoding = false,
                preferCborLabelsOverNames = false,
                alwaysUseByteString = false
            ), EmptySerializersModule()
        ) {

        /**
         * Preconfigured instance of [Cbor] for COSE compliance. Encodes and verifies all tags, uses definite length
         * encoding and prefers labels to serial names. **DOES NOT** sort CBOR map keys; declare them in canonical order
         * for full cbor compliance!
         */
        public val CoseCompliant: Cbor =
            Cbor {
                encodeDefaults = false
                ignoreUnknownKeys = false
                encodeKeyTags = true
                encodeValueTags = true
                encodeObjectTags = true
                verifyKeyTags = true
                verifyValueTags = true
                verifyObjectTags = true
                useDefiniteLengthEncoding = true
                preferCborLabelsOverNames = true
                alwaysUseByteString = false
                serializersModule = EmptySerializersModule()
            }
    }

    override fun <T> encodeToByteArray(serializer: SerializationStrategy<T>, value: T): ByteArray {
        val output = ByteArrayOutput()
        val dumper = if (configuration.useDefiniteLengthEncoding) DefiniteLengthCborWriter(
            this,
            output
        ) else IndefiniteLengthCborWriter(
            this,
            output
        )
        dumper.encodeSerializableValue(serializer, value)

        return output.toByteArray()

    }

    override fun <T> decodeFromByteArray(deserializer: DeserializationStrategy<T>, bytes: ByteArray): T {
        val stream = ByteArrayInput(bytes)
        val reader = CborReader(this, CborParser(stream, configuration.verifyObjectTags))
        return reader.decodeSerializableValue(deserializer)
    }
}

@OptIn(ExperimentalSerializationApi::class)
private class CborImpl(
    configuration: CborConfiguration,
    serializersModule: SerializersModule
) :
    Cbor(
        configuration,
        serializersModule
    )

/**
 * Creates an instance of [Cbor] configured from the optionally given [Cbor instance][from]
 * and adjusted with [builderAction].
 */
@ExperimentalSerializationApi
public fun Cbor(from: Cbor = Cbor, builderAction: CborBuilder.() -> Unit): Cbor {
    val builder = CborBuilder(from)
    builder.builderAction()
    return CborImpl(CborConfiguration(
        builder.encodeDefaults,
        builder.ignoreUnknownKeys,
        builder.encodeKeyTags,
        builder.encodeValueTags,
        builder.encodeObjectTags,
        builder.verifyKeyTags,
        builder.verifyValueTags,
        builder.verifyObjectTags,
        builder.useDefiniteLengthEncoding,
        builder.preferCborLabelsOverNames,
        builder.alwaysUseByteString),
        builder.serializersModule
    )
}

/**
 * Builder of the [Cbor] instance provided by `Cbor` factory function.
 */
@ExperimentalSerializationApi
public class CborBuilder internal constructor(cbor: Cbor) {

    /**
     * Specifies whether default values of Kotlin properties should be encoded.
     */
    public var encodeDefaults: Boolean = cbor.configuration.encodeDefaults

    /**
     * Specifies whether encounters of unknown properties in the input CBOR
     * should be ignored instead of throwing [SerializationException].
     * `false` by default.
     */
    public var ignoreUnknownKeys: Boolean = cbor.configuration.ignoreUnknownKeys

    /**
     * Specifies whether tags set using the [KeyTags] annotation should be written.
     * CBOR allows for optionally defining *tags* for properties and their values. When this switch is set to `true` tags on
     * CBOR map keys (i.e. properties) are encoded into the resulting byte string to transport additional information.
     * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.
     */
    public var encodeKeyTags: Boolean = cbor.configuration.encodeKeyTags

    /**
     * Specifies whether tags set using the [ValueTags] annotation should be written.
     * CBOR allows for optionally defining *tags* for properties and their values. When this switch is set to `true`, tags on
     * CBOR map values (i.e. the values of properties and map entries) are encoded into the resulting byte string to
     * transport additional information. Well-known tags are specified in [CborTag].
     * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.
     */
    public var encodeValueTags: Boolean = cbor.configuration.encodeValueTags

    /**
     * Specifies whether tags set using the [ObjectTags] annotation should be written.
     * When this switch is set to `true` , it is possible to directly declare classes to always be tagged.
     * This then applies to isolated objects of such a tagged class being serialized and to objects of such a class used as
     * values in a list, but also or when they are used as a property in another class.
     * Forcing objects to always be tagged in such a manner is accomplished by the [ObjectTags] annotation,
     * which works just as [ValueTags], but for class definitions.
     * When serializing, object tags will always be encoded directly before to the data of the tagged object, i.e. a
     * value-tagged property of an object-tagged type will have the value tags preceding the object tags.
     * Well-known tags are specified in [CborTag].
     * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.
     */
    public var encodeObjectTags: Boolean = cbor.configuration.encodeObjectTags

    /**
     * Specifies whether tags preceding map keys (i.e. properties) should be matched against the
     * [KeyTags] annotation during the deserialization process.
     * CBOR allows for optionally defining *tags* for properties and their values. When the [encodeKeyTags] switch is set to
     * `true` tags on CBOR map keys (i.e. properties) are encoded into the resulting byte string to transport additional
     * information. Setting [verifyKeyTags] to `true` forces strict verification of such tags during deserialization.
     * I.e. tags must be present on all properties of a class annotated with [KeyTags] in the CBOR byte stream
     * **in full and in order**.
     * See [RFC 8949 Tagging of Items](https://datatracker.ietf.org/doc/html/rfc8949#name-tagging-of-items) for more info.
     */
    public var verifyKeyTags: Boolean = cbor.configuration.verifyKeyTags

    /**
     * Specifies whether tags preceding values should be matched against the [ValueTags]
     * annotation during the deserialization process.
     * CBOR allows for optionally defining *tags* for properties and their values. When [encodeValueTags] is set to `true`,
     * tags on CBOR map values (i.e. the values of properties and map entries) are encoded into the resulting byte string to
     * transport additional information.
     * Setting [verifyValueTags] to `true` forces verification of such tags during deserialization. I.e. tags must be
     * present on all values annotated with [ValueTags] in the CBOR byte stream **in full and in order**.
     * See also [verifyObjectTags], since a value may have both kinds of tags. [ValueTags] precede [ObjectTags] in the CBOR
     * byte stream. [verifyValueTags] and [verifyObjectTags] can be toggled independently.
     * Well-known tags are specified in [CborTag].
     */
    public var verifyValueTags: Boolean = cbor.configuration.verifyValueTags

    /**
     * Specifies whether tags preceding values should be matched against the [ObjectTags]
     * annotation during the deserialization process. [ObjectTags] are applied when serializing classes tagged using this
     * annotation. This applies to isolated objects of such a class and properties, whose values are of such a tagged class.
     * [verifyValueTags] and [verifyObjectTags] can be toggled independently. Hence, it is possible to only partially verify
     * tags on values (if only one such configuration switch is set to true). [ValueTags] precede [ObjectTags] in the CBOR
     * byte stream.
     * Well-known tags are specified in [CborTag].
     */
    public var verifyObjectTags: Boolean = cbor.configuration.verifyObjectTags

    /**
     * Specifies whether the definite length encoding should be used (as required for COSE, for example).
     * CBOR supports two encodings for maps and arrays: definite and indefinite length encoding. kotlinx.serialization defaults
     * to the latter, which means that a map's or array's number of elements is not encoded, but instead a terminating byte is
     * appended after the last element.
     * Definite length encoding, on the other hand, omits this terminating byte, but instead prepends number of elements
     * to the contents of a map or array. This configuration switch allows for toggling between the
     * two modes of encoding.
     */
    public var useDefiniteLengthEncoding: Boolean = cbor.configuration.useDefiniteLengthEncoding

    /**
     * Specifies whether to serialize element labels (i.e. Long from [CborLabel])
     * instead of the element names (i.e. String from [SerialName]). CBOR supports keys of all types which work just as
     * `SerialName`s.
     * COSE restricts this again to strings and numbers and calls these restricted map keys *labels*. String labels can be
     * assigned by using `@SerialName`, while number labels can be assigned using the [CborLabel] annotation.
     * The [preferCborLabelsOverNames] configuration switch can be used to prefer number labels over SerialNames in case both
     * are present for a property. This duality allows for compact representation of a type when serialized to CBOR, while
     * keeping expressive diagnostic names when serializing to JSON.
     */
    public var preferCborLabelsOverNames: Boolean = cbor.configuration.preferCborLabelsOverNames

    /**
     * Specifies whether to always use the compact [ByteString] encoding when serializing
     * or deserializing byte arrays.
     * By default, Kotlin `ByteArray` instances are encoded as **major type 4**.
     * When **major type 2** is desired, then the [`@ByteString`][ByteString] annotation can be used on a case-by-case
     * basis. The [alwaysUseByteString] configuration switch allows for globally preferring **major type 2** without needing
     * to annotate every `ByteArray` in a class hierarchy.
     */
    public var alwaysUseByteString: Boolean = cbor.configuration.alwaysUseByteString

    /**
     * Module with contextual and polymorphic serializers to be used in the resulting [Cbor] instance.
     */
    public var serializersModule: SerializersModule = cbor.serializersModule
}
